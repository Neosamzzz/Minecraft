# 客户端 mods 更新器

以指定本地客户端的 `mods/` 为发布源，玩家在 PCL 启动前自动同步。发布范围是 `mods/`，实例的配置、存档和按键由玩家本地管理。

## 结构

```
tools/updater/
├── publish_one_click.py   双击发布
├── publish.py             发布命令和参数
├── update.ps1             客户端同步
└── README.md
```

OSS 上的布局：

```
newearth/manifest.json
newearth/files/sha256/<SHA-256>
```

## 发布

首次准备：

```powershell
py -3 -m pip install oss2
$env:ALIYUN_OSS_AK = "<AccessKeyId>"
$env:ALIYUN_OSS_SK = "<AccessKeySecret>"
```

bucket 设为公共读，RAM 子账号只给 `newearth/` 前缀的读写权限。AK/SK 只放环境变量，不进仓库。

双击 `publish_one_click.py` 可直接发布，窗口会保留结果。脚本先读取当前进程的 OSS 凭据，再查 Windows 用户和系统环境变量。上传完成后会从公开地址读回清单校验。

也可在终端发布：

```powershell
py -3 tools/updater/publish.py
```

- 默认读取 `.local/test-instance.txt` 所指的实例。可用 `--instance <路径>` 显式指定
- 同一哈希的文件共享下载对象，上传完成后再发布本地实例生成的新清单
- 排除备份、禁用文件、服务端专用 JAR、隐藏项和文件系统链接
- 客户端可读取原有按文件名分发的清单，下一次发布统一采用 SHA-256 路径

本地验证（不上传）：

```powershell
py -3 tools/updater/publish.py --local build/checks/updater/out
```

## 客户端

把 `update.ps1` 放到实例目录：

```
versions\1.12.2-NewEarth-Cleanroom\.updater\update.ps1
```

PCL 版本设置 → 启动前执行命令，填：

```
powershell -NoProfile -ExecutionPolicy Bypass -File "C:\...\versions\1.12.2-NewEarth-Cleanroom\.updater\update.ps1"
```

并勾选“等待命令执行完成”。这个命令每台机器只需要配置一次。

## 行为

每次启动对清单中的本地文件计算 SHA-256，并根据发布清单构建候选目录：

- 清单里有、本地缺失或哈希不同 → 下载到暂存目录
- 在清单的 `removed` 列表里、本地还有 → 删除
- 两个列表里都没有 → 不动（玩家自己加的模组保留）
- 候选目录完整通过校验后再切换。下载或校验失败时继续使用原来的 `mods/`
- 两次目录重命名之间若进程中断，下次启动会先恢复旧目录
- 日志：`.updater\update.log`

`removed` 记录历史发布过、当前发布源已移除的路径。文件重新加入本地实例后会从删除清单移出。

## 注意

- `update.ps1` 必须带 UTF-8 BOM，否则 Windows PowerShell 5.1 会按 ANSI 读取，中文字符串报语法错误。修改后重新保存时保留 BOM。
- 模组文件名里有 `[ ]` 和中文。脚本内部读写文件一律用 .NET API（`File.Copy`、`Directory.CreateDirectory`），不要改成 `Move-Item`／`Remove-Item` 的 `-Path`：PowerShell 会把方括号当通配符，直接报「无法找到指定的文件」。
- 发布只覆盖 `mods/`。配置改动仍需单独发给玩家。
