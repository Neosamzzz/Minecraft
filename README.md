# Minecraft 1.12.2 · 航天玩法与共享任务

用于开发和测试 Minecraft 1.12.2 模组的源码仓库，集中维护 Galacticraft / GalaxySpace 拓展与 FTB Quests。通过 `tools/build.py` 统一构建，输出 GSLaunchCompat 补丁和 FTB Quests 模组；`Galacticraft-Legacy/` 与 `ExtraPlanets/` 保留为参考源码。

[快速开始](#快速开始) · [构建目标](#构建目标) · [安装与测试](#安装与测试) · [源码结构](#源码结构) · [开发说明](#开发说明)

## 当前进展

文档核对日期：**2026-11-02**。本仓库集中管理源码、研究资料与任务配置，不是可直接启动的完整客户端或服务端整合包。

| 开发线 | 已有内容 | 尚待完成 |
| --- | --- | --- |
| GSLC | 1.4.8 发射兼容、维度遥感、地图与时钟补丁 | 对应游戏画面、性能及多人场景验收 |
| FTB 功能 | 公共团队与共享进度；本机开发包 1202.9.3 关闭游戏内编辑并简化无奖励详情 | 双人联机、重启后进度与旧队数据验收 |
| FTB 内容 | [太阳系说明书](docs/ftb-player-guide.md)与六章、60 个目标配置已完成并部署本机；草稿中的后期内容继续预留 | 六章显示、判定、共享与保存的运行验收；后期载具与能源升级接入 |
| 星球资料 | GC / GS / EP 静态调查、EP 0.8.0 源码快照与部分运行日志核对 | 正常飞行、资源加工、防护与返航验证 |
| 新地球 | 火箭物品、新维度分别建立 Git 分支与方向文档 | 两线实现尚未开始，基础 FTB 内容独立推进 |

> [!IMPORTANT]
> **FTB 统一使用 `codex/ftb-public-team` 分支。** 该分支同时包含 1202.9.3 公共团队代码、关闭游戏内编辑与空奖励栏精简、六章任务配置及正式玩家说明书。原文档分支已整合，不再作为独立构建或维护入口。客户端和服务端应使用同一版本，构建通过与游戏验收分别记录。

文档入口见 [FTB 资料区](docs/README.md)，当前本机模组清单见[整合包基线](changelog/modpack.md)。玩法与任务资料含作者侧终章设定，不直接作为玩家可见文本发布。

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

成功后，按命令打印的 `Built:` 路径取包。GSLC 当前为 1.4.8；FTB 文件名跟随所检出分支的 `mod_version`，不以本文的本机安装版本推定：

```text
build/libs/
├── GSLaunchCompat-1.4.8.jar
└── FTBQuests-<mod_version>.jar
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
| FTB Quests | 统一 FTB 分支构建的 1202.9.3，两端使用同一版本 | 必需 | 必需 |
| [FTB Library](https://www.curseforge.com/minecraft/mc-mods/ftb-library-legacy-forge/files/2985811) | 5.4.7.2 | 必需 | 必需 |
| [Item Filters](https://www.curseforge.com/minecraft/mc-mods/item-filters/files/3003364) | 1.0.4.2 | 必需 | 必需 |

FTB Utilities、KubeJS、JEI 等属于可选兼容项；是否安装取决于整合包和任务内容的需要。

仓库已包含[六章太阳系任务配置](quest-config/solar-system/README.md)及[正式玩家说明书](docs/ftb-player-guide.md)，60 个目标不消耗物品、没有奖励，已部署本机并保留原三个小样条件 ID。七阶起的内容仍留在[作者草稿](docs/ftb-quest-manuscript.md)，未加入游戏。安装到已有任务书时，先备份并核对章节索引和 ID，不直接覆盖不相关内容。默认目录为 `config/ftbquests/normal/`，单人游戏使用本机实例目录；游戏规则或 PackMode 可改变该子目录。

物品、维度等基础目标优先使用现有任务类型；控制器绑定、遥感扫描等特殊行为另行适配。物品检测不证明设备已运行或装备已正确穿戴，到达检测不证明使用了正常生存路线。

`1202.9.3` 默认启用熟人服公共团队：自动同队、共享进度，游戏内隐藏并禁用任务编辑，由配置文件维护任务。无奖励任务详情自动收起空奖励区。配置与旧存档处理见[公共团队方案](docs/ftb-shared-team-plan.md#最小版的使用方式)。

安装后至少验证任务书打开、条件判定、奖励领取，以及退出重进后的进度保存。联机时同时检查客户端和服务端日志。

### GSLaunchCompat

本机 2026-11-02 的运行文件组合如下；历史专项探针主要使用 MixinBooter 10.7，两者的验证范围分别记录：

| 组件 | 版本 |
| --- | --- |
| Minecraft / Forge | 1.12.2 / 14.23.5.2864 |
| Galacticraft | 4.0.6 |
| GalaxySpace | 2.2.0，与项目对应的 flight-fix 包 |
| AsmodeusCore | 1.0.5 |
| MixinBooter | 12.2；历史探针为 10.7 |

按修改内容验证发射控制、遥感或显示功能；涉及服务端行为时同步部署到测试服。日志中搜索 `[GSLC]`、`ERROR` 和异常堆栈。

编译依赖与运行模组用途不同。安装使用统一输出的最终模组包；构建成功不等于游戏内验证通过，渲染改动需要查看实际画面。

## 源码结构

```text
Minecraft/
├── GalaxySpace/             # GC / GS 拓展源码、编译依赖和专项检查
├── Galacticraft-Legacy/     # GC 参考源码
├── FTB-Quests/              # FTB Quests 源码与构建配置
├── ExtraPlanets/            # EP 1.12.2 / 0.8.0 上游源码快照，用于调查与对照
├── NewEarth/                # 新地球支线，分物品/火箭与新维度两条开发线
├── quest-config/preview/    # 已部署本机的三个节点小样，不是完整任务书
├── docs/                    # FTB 方案、内容稿与星球研究
├── changelog/               # 各开发线改动记录与本机基线
├── planning/                # 旧规划入口，转向 NewEarth
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
| [ExtraPlanets](https://github.com/MJRLegends/ExtraPlanets) | `dev_1.12.2` | `ExtraPlanets/` |

精确的导入提交见 [upstream.json](upstream.json)。源码已包含在主仓库中，无需另行克隆上游或初始化子模块；更新上游时应审阅本地修改。

ExtraPlanets 当前作为源码参考导入，未接入 `tools/build.py`，也未替换游戏中的 EP JAR。版本声明、与实际安装包的核对范围及源码入口见 [EP 源码说明](docs/research/extraplanets-source-notes.md)；上游文件与许可说明保持原样。

## 开发说明

构建与打包工具的自动测试：

```powershell
py -3 -m unittest discover -s tools/tests
```

macOS / Linux 使用 `python3 -m unittest discover -s tools/tests`。这些检查不代替实际 Gradle 编译和游戏测试。

- [FTB 开发说明](FTB-Quests/DEVELOPMENT.md)：依赖版本、直接调用 Gradle、兼容修复、任务功能、数据保存和编辑入口。
- [FTB 资料区](docs/README.md)：FTB 接入的主基调、工作路线，以及团队方案、内容方向和星球依据。
- [新地球支线](NewEarth/README.md)：物品/火箭与新维度分别使用 Git 分支推进，优先复用现有星球材料；对应 [Project「新地球」](https://github.com/users/Neosamzzz/projects/4/views/1)。
- [FTB 公共团队方案](docs/ftb-shared-team-plan.md)与[任务内容方向](docs/ftb-content-direction.md)：公共团队实现、版本边界、维护方式及轻量无奖励的内容规则。
- [任务内容稿](docs/ftb-quest-manuscript.md)与[任务小样](quest-config/preview/README.md)：作者侧完整候选内容，以及可增量试用的三个节点。
- [星球现状审计](docs/research/planet-gameplay-audit.md)与[玩法详解](docs/research/planet-gameplay-guide.md)：配置、源码层面的目的地与制造关系，含待运行验证事项。
- [开发记录](changelog/README.md)：GSLC 更新历史、整合包基线、FTB Quests 本地改动。
- [遥感补丁说明与专项检查](GalaxySpace/tests/telemetry_dimension/README.md)：遥感功能实现、构建方式与验证手段，版本历史见开发记录。
- [公共控制器补丁检查](GalaxySpace/tests/public_controller/README.md)：控制器兼容探针与验证方式。

在根目录统一操作 Git；修改前保护已有工作，使用功能分支和 PR。提交信息、PR 标题和描述使用中文，附必要的验证结果；只提交源码、资源、必要构建材料和文档。正式版本发布由主维护者负责。
