# Minecraft 1.12.2 · 航天玩法与共享任务

用于开发和测试 Minecraft 1.12.2 模组的源码仓库，集中维护 Galacticraft / GalaxySpace 拓展、FTB Quests 与 NewEarth。通过 `tools/build.py` 统一构建，输出 GSLaunchCompat、FTB Quests 和 NewEarth；`Galacticraft-Legacy/` 与 `ExtraPlanets/` 保留为参考源码。

[快速开始](#快速开始) · [构建目标](#构建目标) · [安装与测试](#安装与测试) · [源码结构](#源码结构) · [开发说明](#开发说明)

## 分支与资料入口

代码、任务配置、研究资料与新地球方向文档统一在 [main](https://github.com/Neosamzzz/Minecraft/tree/main) 维护。原 `codex/ftb`、`codex/newearth-items`、`codex/newearth-dimension` 已合并归档，不再作为独立维护入口。

| 内容 | 目录 |
| --- | --- |
| FTB 功能代码 | `FTB-Quests/` |
| 六章任务配置 | `quest-config/solar-system/normal/` |
| 任务定稿与预备稿 | `docs/` |
| 星球共用研究资料 | `docs/research/` |
| 新地球物品、飞船与能源核心设计及进度 | `NewEarth/items/` |
| 终章维度与结尾演出方向 | `NewEarth/dimension/` |

历史分支名、提交号与部署记录保留原意，不作为当前维护入口。新地球仍按物品与维度划分工作范围，但无需切换分支查阅资料。

## 固定配置交付约定

本整合包的 config 是玩法与代码共同依赖的固定组成部分，取舍完成后按写死的整合规则使用，不支持靠更换 config 切回原 EP／GS 内容组合。

- 仓库中纳入维护的配置是正式来源；禁止用原版默认配置、历史备份或测试实例配置整份替换，避免恢复已裁装备、旧火箭路线和重复天体。
- 后续确需调整时，在正式配置上逐项修改、检查关联并随整合包版本交付。
- 部署时将同版本正式配置同步到实例，不能只换 JAR；实例中的其他有效调整须比较差异后合入，不能反向整份覆盖仓库。

这是维护与交付约定，不表示代码已锁定所有开关或能自动阻止外部覆盖。

## 当前进展

文档核对日期：**2026-11-02**。本仓库集中管理源码、研究资料与任务配置，不是可直接启动的完整客户端或服务端整合包。

| 开发线 | 已有内容 | 尚待完成 |
| --- | --- | --- |
| GSLC | 1.4.8 发射兼容、维度遥感、地图与时钟补丁 | 对应游戏画面、性能及多人场景验收 |
| FTB 功能 | 公共团队与共享进度；本机开发包 1202.9.5 恢复任务书内编辑开关，侧栏仅保留任务；无奖励详情继续精简 | 双人联机、重启后进度与旧队数据验收 |
| FTB 内容 | [太阳系任务定稿表](docs/ftb-quest-content.md)已落实为六章、44 个可见目标；另 16 项留兼容归档；大六边形火箭与分叉布局已配置，后期保留预备稿 | 任务判定、多人共享与进度保存验收；后期载具与能源升级接入 |
| 星球资料 | GC / GS / EP 静态调查、EP 0.8.0 源码快照与部分运行日志核对 | 正常飞行、资源加工、防护与返航验证 |
| 新地球 | 已部署0.2.9（修复GC图纸注册空指针），包含太阳系整合；取消确定剔除的装备、旧EP火箭等物品及火箭实体／NASA页面注册，裁剪旧制造并撤掉七阶过渡配方；常规系外T7衔接保留 | 0.2.9启动验收、七阶新制造与奖励路线；系外工具、九／十阶载具与核心；终章维度与演出 |

> [!IMPORTANT]
> **FTB 代码、任务配置和配套文档已通过 [PR #15](https://github.com/Neosamzzz/Minecraft/pull/15) 合入 `main`，后续代码、任务配置与文档统一在 `main` 维护。** 主线包含 1202.9.5 公共团队代码、任务书内编辑开关与侧栏精简、六章任务配置及当时的配套正文；现行内容审阅入口为任务定稿表。FTB 与新地球开发分支已统一合入 `main`，不再分别维护。客户端和服务端应使用同一版本，构建通过与游戏验收分别记录。

文档入口见 [FTB 资料区](docs/README.md)，当前本机模组清单见[整合包基线](changelog/modpack.md)。玩法与任务资料含作者侧终章设定，不直接作为玩家可见文本发布。

[整合包玩法 Wiki](docs/modpack-gameplay.md)按天体、物品与材料、设备、火箭与引擎、装备、任务系统分类，条目通过链接关联；后续补齐具体装备、温控等级、配方等内容。通过相应检查、当前无已知问题即可称为定稿，代表当前采用版本，后续仍可修改。实现与验收状态在各开发线 README 维护。

| 产物 | 用途 |
| --- | --- |
| **GSLaunchCompat（GSLC）** | GC / GS 发射控制兼容与遥感显示补丁 |
| **FTB Quests** | 任务书、完成条件、奖励与团队进度系统 |
| **NewEarth** | 七阶制造与系外准入衔接，后续承载物品、飞船与核心 |

## 快速开始

需要 **64 位 JDK 8**（含 `java` 和 `javac`）、**Python 3.8+** 和 **Git**。GSLC 与 FTB 使用各自的 Gradle Wrapper，NewEarth 复用 GalaxySpace 的 Wrapper，无需安装全局 Gradle 或 Maven。访问本私有仓库需要相应的 GitHub 权限和本机 Git 凭据。

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

成功后，按命令打印的 `Built:` 路径取包。GSLC 当前为 1.4.8；FTB 文件名以 `FTB-Quests/gradle.properties` 中的 `mod_version` 为准，不以本文的本机安装版本推定：

```text
build/libs/
├── GSLaunchCompat-1.4.8.jar
├── FTBQuests-<mod_version>.jar
└── NewEarth-<mod_version>.jar
```

## 构建目标

以下命令均在仓库根目录执行。macOS / Linux 将 `py -3` 替换为 `python3`。

| 命令 | 作用 |
| --- | --- |
| `py -3 tools/build.py` | 默认构建 GSLC，保持原有用法 |
| `py -3 tools/build.py --target gslc` | 只构建 GSLC |
| `py -3 tools/build.py --target ftbquests` | 只构建 FTB Quests |
| `py -3 tools/build.py --target newearth` | 只构建 NewEarth，自动运行七阶与太阳系隔离检查 |
| `py -3 tools/build.py --target all` | 依次构建 GSLC、FTB Quests、NewEarth，任一目标失败就停止 |
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
- **NewEarth**：客户端与服务端均需配套安装根目录 [config](config/) 中的两份运行配置，不能只换 JAR；具体步骤见物品线。版本来自 `NewEarth/gradle.properties`，复用 GalaxySpace 的 Wrapper 和 GC 编译依赖，输出独立模组。依赖、安装、恢复及待验项见 [物品线](NewEarth/items/README.md#实现状态与构建)。

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
| FTB Quests | 主线源码构建的 1202.9.5，两端使用同一版本 | 必需 | 必需 |
| [FTB Library](https://www.curseforge.com/minecraft/mc-mods/ftb-library-legacy-forge/files/2985811) | 5.4.7.2 | 必需 | 必需 |
| [Item Filters](https://www.curseforge.com/minecraft/mc-mods/item-filters/files/3003364) | 1.0.4.2 | 必需 | 必需 |

FTB Utilities、KubeJS、JEI 等属于可选兼容项；是否安装取决于整合包和任务内容的需要。

仓库已包含[六章太阳系任务配置](quest-config/solar-system/README.md)；现行[任务定稿表](docs/ftb-quest-content.md)采用 44 个可见目标并已部署本机；原 16 项移入兼容归档章，60 个原条件 ID 均保留，不消耗物品、没有奖励。最后的布局微调需重进存档读取。七阶起的内容仍留在[作者草稿](docs/ftb-quest-manuscript.md)，未加入游戏。安装到已有任务书时，先备份并核对章节索引和 ID，不直接覆盖不相关内容。默认目录为 `config/ftbquests/normal/`，单人游戏使用本机实例目录；游戏规则或 PackMode 可改变该子目录。

物品、维度等基础目标优先使用现有任务类型；控制器绑定、遥感扫描等特殊行为另行适配。物品检测不证明设备已运行或装备已正确穿戴，到达检测不证明使用了正常生存路线。

`1202.9.5` 默认启用熟人服公共团队：自动同队、共享进度，左上角侧栏只留“任务”。任务书内提供“编辑任务 / 结束编辑”开关，熟人均可使用；客户端设置、管理面板及队伍入口隐藏，已有配置保留。无奖励任务详情自动收起空奖励区。配置与旧存档处理见[公共团队方案](docs/ftb-shared-team-plan.md#最小版的使用方式)。

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
├── NewEarth/                # 独立附加模组源码与构建；items/、dimension/ 维护对应方向
├── config/                  # 整合包固定运行配置，与 NewEarth 配套分发
├── quest-config/solar-system/ # 当前太阳系任务配置与 ID 映射
├── docs/                    # FTB 方案、内容稿与星球研究
├── changelog/               # 各开发线改动记录与本机基线
├── dev-tools/               # 构建必需的固定材料，用途与来源见目录说明
├── tools/build.py           # 统一构建入口
├── tools/tests/             # 构建与打包工具测试
├── upstream.json            # 上游来源和导入提交
├── version.txt              # GSLC 版本号
├── .local/                  # 本机开发环境与辅助资料，不提交 Git
├── server/                  # 本机服务端及管理工具，不提交 Git
└── build/                   # 可重新生成的输出，不提交 Git
    ├── libs/                # 最终模组包
    └── checks/<模块>/<检查项>/ # 专项检查最近一次输出
```

目录职责、命名与新增模块约定见[项目目录规范](docs/project-layout.md)。构建底包用途见 [dev-tools 说明](dev-tools/README.md)。

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
- [新地球支线](NewEarth/README.md)：物品/火箭与新维度在 `main` 中按目录组织，优先复用现有星球材料；对应 [Project「新地球」](https://github.com/users/Neosamzzz/projects/4/views/1)。
- [FTB 公共团队方案](docs/ftb-shared-team-plan.md)与[任务内容方向](docs/ftb-content-direction.md)：公共团队实现、版本边界、维护方式及轻量无奖励的内容规则。
- [任务定稿表](docs/ftb-quest-content.md)与[太阳系配置](quest-config/solar-system/README.md)：当前采用的任务文字、目标与部署说明；未落地内容见[后期预备稿](docs/ftb-quest-manuscript.md)。
- [星球现状审计](docs/research/planet-gameplay-audit.md)与[玩法详解](docs/research/planet-gameplay-guide.md)：配置、源码层面的目的地与制造关系，含待运行验证事项。
- [开发记录](changelog/README.md)：GSLC 更新历史、整合包基线、FTB Quests 本地改动。
- [遥感补丁说明与专项检查](GalaxySpace/tests/telemetry_dimension/README.md)：遥感功能实现、构建方式与验证手段，版本历史见开发记录。
- [公共控制器补丁检查](GalaxySpace/tests/public_controller/README.md)：控制器兼容探针与验证方式。

在根目录统一操作 Git；修改前保护已有工作，按项目约定直接在 `main` 维护，未经允许不开新分支。提交信息、PR 标题和描述使用中文，附必要的验证结果；只提交源码、资源、必要构建材料和文档。正式版本发布由主维护者负责。
