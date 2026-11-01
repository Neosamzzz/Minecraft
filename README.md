# Minecraft 模组开发

本仓库集中维护 Minecraft 1.12.2 的 GC / GS 拓展及 FTB Quests 源码。所有目录通过同一个 Git 仓库管理，统一使用 `tools/build.py` 选择构建目标。

## 项目组成

| 目录 / 组件 | 用途 | 构建目标 |
| --- | --- | --- |
| `GalaxySpace/` | GC / GS 拓展的开发源码、编译依赖和检查工具 | `gslc` |
| GSLaunchCompat（GSLC） | 将编译出的拓展类打入既有补丁底包，生成独立模组 JAR | `gslc` |
| `Galacticraft-Legacy/` | Galacticraft 参考源码 | 不单独构建 |
| `FTB-Quests/` | 任务书模组源码，包含旧依赖及资源下载的兼容修复 | `ftbquests` |

`all` 会依次构建 GSLC 和 FTB Quests，生成两个独立模组包。FTB 源码已纳入本仓库，具体的 GC / GS 任务联动需要另行实现。

## 源码来源

| 上游 | 导入分支 | 本地目录 |
| --- | --- | --- |
| [GalaxySpace](https://github.com/BlesseNtumble/GalaxySpace) | `dev_1.12.2` | `GalaxySpace/` |
| [Galacticraft-Legacy](https://github.com/TeamGalacticraft/Galacticraft-Legacy) | `master-1.12` | `Galacticraft-Legacy/` |
| [FTB Quests](https://github.com/FTBTeam/FTB-Quests) | `1.12` | `FTB-Quests/` |

精确的导入提交记录在 [upstream.json](upstream.json)。克隆本仓库即可获取这些源码，无需额外克隆上游或初始化子模块。本仓库包含本地修改，更新上游时需要审阅差异。

## 目录结构

```text
Minecraft/
├── GalaxySpace/             # GC / GS 拓展源码、编译依赖和检查工具
├── Galacticraft-Legacy/     # GC 参考源码
├── FTB-Quests/              # FTB Quests 源码和 Gradle 构建配置
├── dev-tools/               # GSLC 打包底包
├── tools/
│   ├── build.py             # 统一构建入口
│   └── tests/               # 构建与打包工具测试
├── upstream.json            # 上游来源和导入提交
├── version.txt              # GSLC 版本号
└── build/libs/              # 统一输出的最终模组包，不提交 Git
```

## 获取代码

```sh
git clone https://github.com/Neosamzzz/Minecraft.git
cd Minecraft
```

访问私有仓库需要相应的 GitHub 权限和本机 Git 凭据。已有 Git 工作副本时，先检查 `git status`，保护本地改动后再同步；已有 ZIP 或普通文件夹时，先比较内容，不直接覆盖。

## 环境要求

- 64 位 JDK 8，必须同时包含 `java` 和 `javac`。
- Python 3.8 或更新版本。
- Git。

将 `JAVA_HOME` 指向 JDK 8。构建脚本优先使用该目录中的工具；未设置时才从 `PATH` 查找。不要把 `JAVA_HOME` 指向 JRE 或 JDK 的 `bin` 子目录。

Windows PowerShell 示例，路径按实际安装位置调整，变量只影响当前终端：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_8u202_x64'
& "$env:JAVA_HOME\bin\java.exe" -version
& "$env:JAVA_HOME\bin\javac.exe" -version
py -3 --version
```

macOS / Linux：

```sh
export JAVA_HOME=/path/to/jdk8
"$JAVA_HOME/bin/java" -version
"$JAVA_HOME/bin/javac" -version
python3 --version
```

Java 和 Javac 应报告 `1.8`。两个子项目各自使用仓库内的 Gradle Wrapper，无需安装全局 Gradle 或 Maven：

| 构建项目 | Gradle | ForgeGradle | 编译用 Forge |
| --- | --- | --- | --- |
| GalaxySpace / GSLC | 6.8.3 | 4.0.23 | 1.12.2-14.23.5.2860 |
| FTB Quests | 4.9 | 2.3 | 1.12.2-14.23.5.2784 |

首次构建需要联网下载 Gradle 和依赖，缓存默认位于用户目录的 `.gradle/`。`--offline` 仅适用于所需工具和依赖已完整缓存的情况。旧工具链可能输出弃用或版本支持警告，实际是否成功以任务退出状态及构建结果为准。

## 统一构建

以下命令均在 Minecraft 仓库根目录执行。

Windows：

```powershell
# 默认构建 GSLC，保持原有命令用法
py -3 tools/build.py

# 分别选择构建目标
py -3 tools/build.py --target gslc
py -3 tools/build.py --target ftbquests

# 依次构建两者；任一目标失败就停止并返回错误
py -3 tools/build.py --target all

# 使用已缓存依赖离线构建，适用于所有 target
py -3 tools/build.py --target all --offline

# 查看完整参数
py -3 tools/build.py --help
```

macOS / Linux 将 `py -3` 替换为 `python3`，例如：

```sh
python3 tools/build.py --target all --offline
```

### 产物与版本

最终模组包统一放在仓库根目录的 `build/libs/`：

| 目标 | 输出文件 | 版本来源 |
| --- | --- | --- |
| `gslc` | `GSLaunchCompat-<版本>.jar` | 根目录 `version.txt`，当前为 `1.4.8` |
| `ftbquests` | `FTBQuests-<版本>.jar` | `FTB-Quests/gradle.properties` 的 `mod_version`，当前为 `1202.9.0` |
| `all` | 上述两个模组包 | 分别采用各自版本 |

GSLC 先编译并重混淆 GalaxySpace，再使用 `dev-tools/GSLaunchCompat-1.3-public-controller.jar` 作为底包打包，文件名和包内实现版本同步。FTB Quests 执行完整 Gradle `build`，然后把对应版本的模组 JAR 复制到统一输出目录。

FTB 设置环境变量 `BUILD_NUMBER` 时，版本会追加 `.<BUILD_NUMBER>`；统一入口沿用该规则。版本及构建编号仅使用字母、数字、点、下划线、加号和连字符。FTB 源码包等其他产物仍保留在 `FTB-Quests/build/libs/`，不复制到统一输出目录。输出目录可以同时保留多个历史版本，安装时按本次命令打印的 `Built:` 路径选择文件。

### FTB 构建说明

FTB 的旧 `maven.latmod.com` 依赖已替换为固定 CurseForge 文件编号；原 CurseMaven 1.2.x 下载插件已移除。FTB Library 使用 5.4.7.2，Item Filters 使用 1.0.4.2，其他兼容模块保留原有依赖版本。Java 编译采用 UTF-8。

`FTB-Quests/gradle/legacy-assets.gradle` 修复旧 ForgeGradle 的 HTTP 资源下载，改用 Mojang 官方 HTTPS 并检查大小与 SHA-1。详细记录见 [FTB 开发说明](FTB-Quests/DEVELOPMENT.md)。日常构建统一使用根目录的 Python 入口。

## 验证与游戏测试

### 构建工具测试

```powershell
py -3 -m unittest discover -s tools/tests
```

macOS / Linux 使用 `python3 -m unittest discover -s tools/tests`。这些测试覆盖构建入口和打包逻辑，不代替实际 Gradle 编译或游戏测试。GC / GS 功能的专项检查位于 `GalaxySpace/tests/`，具体要求参见对应子目录说明。

### GSLC 测试环境

现有 GSLC 测试配置使用下列运行版本及其依赖：

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.12.2 |
| Forge | 14.23.5.2864 |
| Galacticraft | 4.0.6 |
| GalaxySpace | 2.2.0，与项目对应的 flight-fix 包 |
| AsmodeusCore | 1.0.5 |
| MixinBooter | 10.7 |

编译用依赖与实际运行模组用途不同，不直接用开发依赖替换运行环境中的模组。

### 安装与检查

1. 使用独立客户端或测试服，先退出客户端或停止服务器。
2. 从根目录 `build/libs/` 选择本次生成的模组包，放入测试实例的 `mods/`，移出同一模组的旧版本。
3. 测试 FTB Quests 时准备对应的 Minecraft 1.12.2 Forge 环境、FTB Library 和 Item Filters；按需要安装其他兼容模组。
4. 启动并检查相关行为。FTB 至少验证任务书打开、任务判定、进度保存和奖励领取；GSLC 按改动验证发射控制、遥感或显示功能。
5. 涉及服务端行为时同步更新测试服，并检查客户端与服务端日志。GSLC 可搜索 `[GSLC]`，同时检查 `ERROR` 和异常堆栈。

编译成功只说明编译和打包通过。渲染改动需要查看实际画面，联网同步及存档行为需要游戏内验证。正式版本发布由主维护者负责。

## 开发与 Git 协作

- 在 Minecraft 根目录统一操作 Git，各源码目录不单独初始化仓库。
- 开始修改前检查分支、工作区和远端更新，保护已有未提交改动。
- 使用功能分支开发，通过 PR 合并。
- 提交源码、资源、必要构建脚本和文档；不提交缓存、日志、存档、个人配置和生成的模组包。Gradle Wrapper 和 GSLC 底包属于必要构建材料。
- 提交说明和 PR 描述使用中文，写清改动、验证结果及客户端 / 服务端要求；有对应 Issue 时关联。
- 修改 GSLC 版本时更新根目录 `version.txt`；修改 FTB 版本时更新其 `gradle.properties`。
- 使用 GitHub Projects 管理任务时，可将本仓库的 Issue 和 PR 加入看板；源码仍在本仓库维护。
