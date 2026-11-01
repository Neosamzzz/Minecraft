# Minecraft 1.12.2 · GSLC & FTB Quests

用于开发和测试 Minecraft 1.12.2 模组的源码仓库，集中维护 Galacticraft / GalaxySpace 拓展与 FTB Quests。通过 `tools/build.py` 统一构建，输出 GSLaunchCompat 补丁和 FTB Quests 模组；`Galacticraft-Legacy/` 保留为参考源码。

[快速开始](#快速开始) · [构建目标](#构建目标) · [安装与测试](#安装与测试) · [源码结构](#源码结构) · [开发说明](#开发说明)

| 产物 | 用途 |
| --- | --- |
| **GSLaunchCompat（GSLC）** | GC / GS 发射控制兼容与遥感显示补丁 |
| **FTB Quests** | 任务书、完成条件、奖励与团队进度系统 |

## 快速开始

需要 **64 位 JDK 8**（含 `java` 和 `javac`）、**Python 3.8+** 和 **Git**。两个项目各自使用仓库内的 Gradle Wrapper，无需安装全局 Gradle 或 Maven。访问本私有仓库需要相应的 GitHub 权限和本机 Git 凭据。

```sh
git clone https://github.com/Neosamzzz/Minecraft.git
cd Minecraft
```

选择对应系统设置 `JAVA_HOME` 并构建；示例 JDK 路径请替换为实际安装目录。

<details open>
<summary>Windows · PowerShell</summary>

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_8u202_x64'
py -3 tools/build.py --target all
```

</details>

<details>
<summary>macOS / Linux</summary>

```sh
export JAVA_HOME=/path/to/jdk8
python3 tools/build.py --target all
```

</details>

`JAVA_HOME` 应指向 JDK 根目录。脚本优先使用该目录中的 Java 工具；未设置时从 `PATH` 查找，并检查版本是否为 Java 8。

> [!NOTE]
> 首次构建需要联网下载工具、依赖并处理 Minecraft 开发类库。已安装的游戏实例与这套开发缓存分开管理；缓存默认位于用户目录的 `.gradle/`，后续构建会复用。

成功后，按命令打印的 `Built:` 路径取包。当前默认版本输出为：

```text
build/libs/
├── GSLaunchCompat-1.4.8.jar
└── FTBQuests-1202.9.0.jar
```

## 构建目标

以下命令均在仓库根目录执行。macOS / Linux 将 `py -3` 替换为 `python3`。

| 命令 | 作用 |
| --- | --- |
| `py -3 tools/build.py` | 默认构建 GSLC，保持原有用法 |
| `py -3 tools/build.py --target gslc` | 只构建 GSLC |
| `py -3 tools/build.py --target ftbquests` | 只构建 FTB Quests |
| `py -3 tools/build.py --target all` | 依次构建两者，任一目标失败就停止 |
| `py -3 tools/build.py --help` | 查看完整参数 |

工具与依赖已完整缓存后，可以为任意目标添加 `--offline`：

```powershell
py -3 tools/build.py --target all --offline
```

### 版本与产物

最终模组包统一输出到根目录 `build/libs/`，可以同时保留多个历史版本，安装时按本次 `Built:` 输出选择。

- **GSLC**：版本来自根目录 [version.txt](version.txt)，文件名和包内实现版本同步。
- **FTB Quests**：版本来自 [gradle.properties](FTB-Quests/gradle.properties) 的 `mod_version`。设置 `BUILD_NUMBER` 时追加 `.<BUILD_NUMBER>`，统一入口会复制对应版本的模组包。
- FTB 源码包等其他产物留在 `FTB-Quests/build/libs/`；编译中间产物和缓存不提交到 Git。

> [!IMPORTANT]
> **GSLC 的打包范围是固定的。** 当前流程保留 1.3 底包已有补丁，从新编译结果中选取 `galaxyspace/core/telemetry/`、`MixinTileEntityTelemetry`、`MixinGameScreenText`、`MixinDrawGameScreen` 及遥感语言资源。其他 GalaxySpace 类的修改不会自动进入最终 GSLC，需要同时审阅和调整[打包器](GalaxySpace/tests/telemetry_dimension/build_addon.py)。

<details>
<summary>编译工具链</summary>

| 项目 | Gradle | ForgeGradle | 编译用 Forge |
| --- | --- | --- | --- |
| GalaxySpace / GSLC | 6.8.3 | 4.0.23 | 1.12.2-14.23.5.2860 |
| FTB Quests | 4.9 | 2.3 | 1.12.2-14.23.5.2784 |

GSLC 编译并重混淆 GalaxySpace，再使用 `dev-tools/GSLaunchCompat-1.3-public-controller.jar` 打包。FTB 执行完整 Gradle `build`，然后复制最终模组包。FTB 旧依赖和资源下载的兼容修复见 [FTB 开发说明](FTB-Quests/DEVELOPMENT.md)。

</details>

## 安装与测试

使用独立客户端或测试服；退出游戏或停止服务器后，将本次生成的模组包放入实例的 `mods/`，移出同一模组的旧版本。

### FTB Quests

在 Minecraft **1.12.2 + Forge** 环境中，客户端和服务端都安装下面三个模组，并使用匹配版本：

| 模组 | 版本 | 客户端 | 服务端 |
| --- | --- | --- | --- |
| FTB Quests | 本项目生成的 `1202.9.0`，或带构建编号的对应版本 | 必需 | 必需 |
| [FTB Library](https://www.curseforge.com/minecraft/mc-mods/ftb-library-legacy-forge/files/2985811) | 5.4.7.2 | 必需 | 必需 |
| [Item Filters](https://www.curseforge.com/minecraft/mc-mods/item-filters/files/3003364) | 1.0.4.2 | 必需 | 必需 |

FTB Utilities、KubeJS、JEI 等属于可选兼容项；是否安装取决于整合包和任务内容的需要。

本仓库尚未包含整合包任务配置。物品提交、到达维度等基础任务可以用现有任务类型配置，控制器绑定、遥感扫描等特殊行为按需开发适配。已有任务定义需要部署到服务器，默认目录为 `config/ftbquests/normal/`；单人游戏使用本机实例的对应目录。

安装后至少验证任务书打开、条件判定、奖励领取，以及退出重进后的进度保存。联机时同时检查客户端和服务端日志。

### GSLaunchCompat

沿用项目记录的参考运行组合，实际兼容性仍需在对应实例中验证：

| 组件 | 版本 |
| --- | --- |
| Minecraft / Forge | 1.12.2 / 14.23.5.2864 |
| Galacticraft | 4.0.6 |
| GalaxySpace | 2.2.0，与项目对应的 flight-fix 包 |
| AsmodeusCore | 1.0.5 |
| MixinBooter | 10.7 |

按修改内容验证发射控制、遥感或显示功能；涉及服务端行为时同步部署到测试服。日志中搜索 `[GSLC]`、`ERROR` 和异常堆栈。

编译依赖与运行模组用途不同。安装使用统一输出的最终模组包；构建成功不等于游戏内验证通过，渲染改动需要查看实际画面。

## 源码结构

```text
Minecraft/
├── GalaxySpace/             # GC / GS 拓展源码、编译依赖和专项检查
├── Galacticraft-Legacy/     # GC 参考源码
├── FTB-Quests/              # FTB Quests 源码与构建配置
├── dev-tools/               # GSLC 打包底包
├── tools/build.py           # 统一构建入口
├── tools/tests/             # 构建与打包工具测试
├── upstream.json            # 上游来源和导入提交
├── version.txt              # GSLC 版本号
└── build/libs/              # 最终模组包，不提交 Git
```

| 上游项目 | 导入分支 | 本地目录 |
| --- | --- | --- |
| [GalaxySpace](https://github.com/BlesseNtumble/GalaxySpace) | `dev_1.12.2` | `GalaxySpace/` |
| [Galacticraft-Legacy](https://github.com/TeamGalacticraft/Galacticraft-Legacy) | `master-1.12` | `Galacticraft-Legacy/` |
| [FTB Quests](https://github.com/FTBTeam/FTB-Quests) | `1.12` | `FTB-Quests/` |

精确的导入提交见 [upstream.json](upstream.json)。源码已包含在主仓库中，无需另行克隆上游或初始化子模块；更新上游时应审阅本地修改。

## 开发说明

构建与打包工具的自动测试：

```powershell
py -3 -m unittest discover -s tools/tests
```

macOS / Linux 使用 `python3 -m unittest discover -s tools/tests`。这些检查不代替实际 Gradle 编译和游戏测试。

- [FTB 开发说明](FTB-Quests/DEVELOPMENT.md)：依赖版本、直接调用 Gradle 和兼容修复。
- [遥感补丁说明与专项检查](GalaxySpace/tests/telemetry_dimension/README.md)：遥感功能实现及验证记录，历史版本说明按对应版本阅读。
- [公共控制器补丁检查](GalaxySpace/tests/public_controller/README.md)：控制器兼容探针与验证方式。

在根目录统一操作 Git；修改前保护已有工作，使用功能分支和 PR。提交信息、PR 标题和描述使用中文，附必要的验证结果；只提交源码、资源、必要构建材料和文档。正式版本发布由主维护者负责。
