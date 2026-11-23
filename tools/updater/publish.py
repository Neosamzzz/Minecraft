#!/usr/bin/env python3
"""把客户端实例 mods/ 下的 JAR 发布到 OSS，并生成 manifest.json。

用法：
    py -3 tools/updater/publish.py --local build/checks/updater/out
    py -3 tools/updater/publish.py --dry-run
    py -3 tools/updater/publish.py --jobs 16

OSS 上传需要环境变量：
    ALIYUN_OSS_AK / ALIYUN_OSS_SK

发布顺序固定为：先传文件，最后传 manifest.json。
manifest 里的 removed 记录过往版本分发过、当前版本已移除的 JAR 路径，
客户端据此删除；玩家自己添加、从未进入清单的文件不受影响。
"""

import argparse
import hashlib
import json
import os
import stat
import sys
import tempfile
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

INSTANCE_FILE = Path(__file__).resolve().parents[2] / ".local" / "test-instance.txt"
DEFAULT_BUCKET = "java-studyweb"
DEFAULT_ENDPOINT = "https://oss-cn-beijing.aliyuncs.com"
DEFAULT_PREFIX = "newearth/"
MANIFEST_NAME = "manifest.json"
MB = 1024 * 1024

# 服务端专用或临时文件，客户端不需要。
EXCLUDE_PREFIXES = ("NewEarth-Server-Pause", "newearth-server-admin")


def log(message):
    print(message, flush=True)


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(MB), b""):
            digest.update(block)
    return digest.hexdigest()


def collect(instance):
    mods = instance / "mods"
    if not mods.is_dir():
        sys.exit("找不到 mods 目录：" + str(mods))
    if is_link(mods):
        sys.exit("发布源的 mods 目录是链接：" + str(mods))
    files = []
    for root, directories, names in os.walk(mods, onerror=raise_walk_error):
        parent = Path(root)
        directories[:] = [
            name for name in sorted(directories) if not name.startswith(".") and not is_link(parent / name)
        ]
        for name in sorted(names):
            if name.startswith(".") or not name.lower().endswith(".jar"):
                continue
            if name.startswith(EXCLUDE_PREFIXES):
                continue
            path = parent / name
            if is_link(path) or not path.is_file():
                continue
            digest = sha256(path)
            files.append(
                {
                    "path": path.relative_to(instance).as_posix(),
                    "key": "files/sha256/" + digest,
                    "sha256": digest,
                    "size": path.stat().st_size,
                }
            )
    if len({item["path"].casefold() for item in files}) != len(files):
        raise RuntimeError("发布源存在仅大小写不同的 Windows 路径")
    return files


def is_link(path):
    metadata = path.lstat()
    return stat.S_ISLNK(metadata.st_mode) or bool(
        getattr(metadata, "st_file_attributes", 0) & stat.FILE_ATTRIBUTE_REPARSE_POINT
    )


def raise_walk_error(error):
    raise error


def load_previous_local(out_dir):
    manifest = out_dir / MANIFEST_NAME
    if not manifest.is_file():
        return {}, []
    data = json.loads(manifest.read_text(encoding="utf-8"))
    files = {item["path"]: item.get("key") for item in data.get("files", [])}
    return files, list(data.get("removed", []))


def load_previous_oss(bucket, prefix):
    import oss2

    try:
        body = bucket.get_object(prefix + MANIFEST_NAME).read()
    except oss2.exceptions.NoSuchKey:
        return {}, []
    data = json.loads(body.decode("utf-8"))
    files = {item["path"]: item.get("key") for item in data.get("files", [])}
    return files, list(data.get("removed", []))


def compute_removed(files, previous, previous_removed):
    """累积历史移除项，避免玩家跨版本升级时漏删。"""
    current = {item["path"].casefold() for item in files}
    historical = {
        path.casefold(): path
        for path in [*previous_removed, *previous]
        if path.lower().endswith(".jar")
    }
    return sorted(path for folded, path in historical.items() if folded not in current)


def build_manifest(files, removed):
    version = time.strftime("%Y%m%d-%H%M%S", time.localtime())
    return {
        "version": version,
        "generated": time.strftime("%Y-%m-%dT%H:%M:%S%z", time.localtime()),
        "removed": removed,
        "files": files,
    }


def pending_files(files, previous):
    known = set(previous.values())
    pending = []
    for item in files:
        if item["key"] not in known:
            pending.append(item)
            known.add(item["key"])
    return pending


def read_verified(instance, item):
    data = (instance / item["path"]).read_bytes()
    if hashlib.sha256(data).hexdigest() != item["sha256"]:
        raise RuntimeError("生成清单后源文件发生变化：" + item["path"])
    return data


def oss_error(error):
    return " ".join(str(value) for value in (
        type(error).__name__, getattr(error, "status", None), getattr(error, "code", None),
    ) if value is not None)


def oss_credentials():
    names = ("ALIYUN_OSS_AK", "ALIYUN_OSS_SK")
    current = tuple(os.environ.get(name) for name in names)
    if all(current) or os.name != "nt":
        return current

    # A desktop process opened before the variables were set may have an older environment.
    import winreg

    for hive, path in (
        (winreg.HKEY_CURRENT_USER, r"Environment"),
        (winreg.HKEY_LOCAL_MACHINE, r"SYSTEM\CurrentControlSet\Control\Session Manager\Environment"),
    ):
        try:
            with winreg.OpenKey(hive, path) as key:
                pair = tuple(winreg.QueryValueEx(key, name)[0] for name in names)
        except FileNotFoundError:
            continue
        if all(isinstance(value, str) and value.strip() for value in pair):
            return pair
    return current


def summarize(files, pending, removed):
    total = sum(item["size"] for item in pending) / MB
    log(
        "扫描 %d 个文件；需写入 %d 个内容对象（%.1f MB）"
        % (len(files), len(pending), total)
    )
    if removed:
        log("清单中标记为移除的路径（客户端会删除）：")
        for path in removed:
            log("  " + path)


def publish_local_run(instance, out_dir):
    files = collect(instance)
    previous, previous_removed = load_previous_local(out_dir)
    existing = {path: key for path, key in previous.items() if key and (out_dir / key).is_file()}
    pending = pending_files(files, existing)
    removed = compute_removed(files, previous, previous_removed)
    summarize(files, pending, removed)

    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "files").mkdir(exist_ok=True)
    for index, item in enumerate(pending, 1):
        target = out_dir / item["key"]
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(read_verified(instance, item))
        if index % 50 == 0 or index == len(pending):
            log("  写入 %d/%d" % (index, len(pending)))

    manifest = build_manifest(files, removed)
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=out_dir, delete=False) as handle:
        path = Path(handle.name)
        try:
            json.dump(manifest, handle, ensure_ascii=False, indent=2)
            handle.write("\n")
            handle.close()
            path.replace(out_dir / MANIFEST_NAME)
        finally:
            path.unlink(missing_ok=True)
    log("本地输出：" + str(out_dir))


def publish_oss(instance, bucket_name, endpoint, prefix, jobs, dry_run):
    import oss2

    ak, sk = oss_credentials()
    if not ak or not sk:
        if dry_run:
            files = collect(instance)
            summarize(files, files, [])
            log("缺少 ALIYUN_OSS_AK / ALIYUN_OSS_SK，无法读取上一版清单；以上按全量统计")
            return
        sys.exit("缺少 ALIYUN_OSS_AK / ALIYUN_OSS_SK 环境变量")

    if "://" not in endpoint:
        endpoint = "https://" + endpoint
    bucket = oss2.Bucket(oss2.Auth(ak, sk), endpoint, bucket_name)
    files = collect(instance)
    previous, previous_removed = load_previous_oss(bucket, prefix)
    pending = pending_files(files, previous)
    removed = compute_removed(files, previous, previous_removed)
    summarize(files, pending, removed)

    if dry_run:
        log("--dry-run：不上传")
        return
    if not pending:
        log("没有需要上传的文件，只更新 manifest")
    else:
        total = len(pending)
        done = 0
        done_bytes = 0
        lock = threading.Lock()

        def upload(item):
            key = prefix + item["key"]
            data = read_verified(instance, item)
            headers = {
                "Content-Type": "application/java-archive",
                "Cache-Control": "public, max-age=31536000, immutable",
                "x-oss-object-acl": "public-read",
            }
            last_error = None
            for attempt in range(3):
                try:
                    bucket.put_object(key, data, headers=headers)
                    return item["size"]
                except Exception as error:  # noqa: BLE001 - 网络抖动重试
                    last_error = error
                    time.sleep(attempt + 1)
            raise RuntimeError("上传失败：" + item["path"] + " - " + oss_error(last_error)) from None

        with ThreadPoolExecutor(max_workers=jobs) as pool:
            futures = [pool.submit(upload, item) for item in pending]
            for future in as_completed(futures):
                size = future.result()
                with lock:
                    done += 1
                    done_bytes += size
                    if done % 50 == 0 or done == total:
                        log("  上传 %d/%d（%.1f MB）" % (done, total, done_bytes / MB))

    manifest = build_manifest(files, removed)
    key = prefix + MANIFEST_NAME
    payload = json.dumps(manifest, ensure_ascii=False, indent=2).encode("utf-8")
    bucket.put_object(
        key,
        payload,
        headers={
            "Content-Type": "application/json",
            "Cache-Control": "no-cache",
            "x-oss-object-acl": "public-read",
        },
    )
    log("manifest 已更新")
    public = oss2.Bucket(oss2.AnonymousAuth(), endpoint, bucket_name)
    with public.get_object(key, headers={"Cache-Control": "no-cache"}) as response:
        if response.read() != payload:
            raise RuntimeError("已写入清单，但公开地址读回的内容不一致")
    log("公开清单校验通过")
    scheme, host = endpoint.rstrip("/").split("://", 1)
    log("OSS 地址：%s://%s.%s/%s%s" % (scheme, bucket_name, host, prefix, MANIFEST_NAME))


def main():
    parser = argparse.ArgumentParser(description="发布客户端 mods/ 下的 JAR 到 OSS")
    parser.add_argument("--instance", type=Path, help="发布源，默认读取 .local/test-instance.txt")
    parser.add_argument("--local", type=Path, help="只写到本地目录，不上传")
    parser.add_argument("--bucket", default=DEFAULT_BUCKET)
    parser.add_argument("--endpoint", default=DEFAULT_ENDPOINT)
    parser.add_argument("--prefix", default=DEFAULT_PREFIX)
    parser.add_argument("--jobs", type=int, default=8, help="并发上传数，默认 8")
    parser.add_argument("--dry-run", action="store_true", help="只统计，不上传")
    args = parser.parse_args()

    if args.instance is None:
        if not INSTANCE_FILE.is_file():
            parser.error("请用 --instance 指定本地客户端实例，或配置 .local/test-instance.txt")
        args.instance = Path(INSTANCE_FILE.read_text(encoding="utf-8-sig").strip())
    instance = args.instance.resolve()
    if args.local:
        publish_local_run(instance, args.local.resolve())
    else:
        try:
            import oss2
        except ModuleNotFoundError as error:
            if error.name != "oss2":
                raise
            sys.exit("缺少 OSS SDK，请先运行 py -3 -m pip install oss2")

        try:
            publish_oss(instance, args.bucket, args.endpoint, args.prefix, args.jobs, args.dry_run)
        except oss2.exceptions.OssError as error:
            sys.exit("OSS 请求失败：" + oss_error(error))


if __name__ == "__main__":
    main()
