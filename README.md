# 开发指南

## 源码来源

本仓库基于以下两个上游项目，保留其源码并集中维护本项目的修改：

- [GalaxySpace](https://github.com/BlesseNtumble/GalaxySpace)：对应 `GalaxySpace/`，基于 `dev_1.12.2` 分支。
- [Galacticraft-Legacy](https://github.com/TeamGalacticraft/Galacticraft-Legacy)：对应 `Galacticraft-Legacy/`，参考源码来自 `master-1.12` 分支。

源码来源及导入提交记录见 [upstream.json](upstream.json)。开发时直接使用本仓库，无需另外克隆上游项目。

## 目录

```text
Minecraft/
├── GalaxySpace/             # 开发源码、编译依赖与测试
├── Galacticraft-Legacy/     # GC 参考源码
├── dev-tools/               # 打包所需的基础材料
├── tools/build.py           # 编译与打包入口
├── version.txt              # GSLC 版本号
└── build/libs/              # 构建生成的补丁 JAR
```

拉取本仓库即可获取源码和本地编译依赖。使用 VS Code 时打开 Minecraft 目录，安装 Java 开发支持；主要修改位于 GalaxySpace 中。

## 环境准备

- JDK 8，需包含 `java` 和 `javac`。
- Python 3.8 或更新版本。
- Git。

将 `JAVA_HOME` 指向 JDK 8，并将其 bin 目录加入 PATH。检查：

```sh
java -version
javac -version
```

两项应显示 1.8。Gradle 使用仓库自带 Wrapper，无需另装 Gradle 或 Maven。首次构建需联网下载工具与依赖，Maven Central 使用官方源。

## 构建打包

在 Minecraft 仓库根目录执行。

macOS / Linux：

```sh
python3 tools/build.py
```

Windows：

```powershell
py -3 tools/build.py
```

脚本检查 JDK、调用 Gradle 编译及重混淆，并生成补丁：

```text
build/libs/GSLaunchCompat-x.x.x.jar
```

`x.x.x` 来自根目录 `version.txt`，包内实现版本与文件名同步。更新版本时修改该文件。

依赖已经下载完整后，可加 `--offline` 使用离线构建。构建缓存和产物不提交到 Git。

## 本地测试

准备独立的客户端或测试服，使用以下运行版本及其所需依赖：

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.12.2 |
| Forge | 14.23.5.2864 |
| Galacticraft | 4.0.6 |
| GalaxySpace | 2.2.0，与项目对应的 flight-fix 包 |
| AsmodeusCore | 1.0.5 |
| MixinBooter | 10.7 |

工程中已有的开发依赖用于编译，不直接代替上述运行模组。

1. 退出客户端或停止自己的测试服。
2. 将生成的 GSLC JAR 放入测试实例的 mods 目录，移出旧版 GSLC，同一实例只保留一个版本。
3. 启动游戏并验证修改；涉及服务端逻辑时同步替换测试服的补丁。
4. 查看测试实例的 `logs/latest.log`，搜索 `[GSLC]`、`ERROR` 和异常堆栈。

按改动检查相关行为；渲染修改需实际查看画面，不能仅凭构建成功确认。自动检查源码位于 `GalaxySpace/tests/`，构建工具的检查命令为：

```sh
python3 -m unittest discover -s tools/tests
```

Windows 将 `python3` 替换为 `py -3`。

## Git 同步

在 Minecraft 根目录统一操作，各功能目录通过同一个仓库提交。

- 开始开发前检查本地修改并同步远端代码。
- 使用功能分支，通过 PR 合并。
- 提交源码、资源及必要脚本，不提交个人配置、缓存、日志、存档和构建产物。
- PR 附改动说明、测试结果及客户端／服务端要求；有对应 Linear issue 时关联。
- 可以自行打包并在自己的客户端或测试服验证；正式版本发布由主维护者负责。

## 从 ZIP 下载后接入仓库

ZIP 包不含 `.git`，需要先初始化并连接远端：

```sh
git init -b main
git remote add origin https://github.com/Neosamzzz/Minecraft.git
git fetch --depth=1 origin main
```

之后按上文「Git 同步」使用分支与 PR。