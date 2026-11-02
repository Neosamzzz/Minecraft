# 整合包基线

记录本机实际在玩的整合包组成，用于换机器、重装实例、分发同版本包和排查"对方那边为什么不一样"。

实例目录：`C:\Users\qq106\Desktop\游戏\Release\.minecraft\versions\1.12.2-Forge_14.23.5.2864`（客户端）。远程服务器的实际版本尚未核对。

首次记录：2026-11-01；文件与日志复核：2026-11-02。本页记录本机快照，玩法及加载观察见[玩法详解](../docs/research/planet-gameplay-guide.md#十三2026-11-02-运行日志补记)。运行文件存在不代表已经完成兼容性与生存流程验收。

## 模组清单

客户端 `mods/` 当前 11 个 JAR。下面另列 Forge 实例版本；文件显示名可能带中文前缀，版本以包内元数据和日志为准。

| 模组 | 文件 | 说明 |
| --- | --- | --- |
| Forge | 14.23.5.2864 | 实例版本 |
| Galacticraft | `Galacticraft-1.12.2-4.0.6.jar` | GC 本体 |
| GalaxySpace | `GalaxySpace-1.12.2-2.2.0-flight-fix.jar` | 已改的 flight-fix 包，非上游原版 |
| ExtraPlanets | `ExtraPlanets-1.12.2-0.8.0.jar` | 多数天体已关闭，见下 |
| AsmodeusCore | `AsmodeusCore-1.12.2-1.0.5.jar` | GS 运行依赖；EP 同时使用 GC 与 MJRLegendsLib |
| Galacticraft Compatibility | `galacticraftcompatibility-1.1.2.jar` | |
| MJRLegendsLib | `MJRLegendsLib-1.12.2-1.2.1.jar` | |
| MixinBooter | `000mixinbooter-12.2.jar` | 当前本机版本；历史 GSLC 探针使用 10.7，不能视为已在 12.2 重跑 |
| GSLaunchCompat | `GSLaunchCompat-1.4.8.jar` | 本项目产物，见 [gslc.md](gslc.md) |
| FTB Quests | `FTBQuests-1202.9.5.jar` | 已重新构建并部署，当前主线保留对应源码，见 [ftb-quests.md](ftb-quests.md) |
| FTB Library | `[FTB Library（旧版）] FTBLib-5.4.7.0.jar` | 文件名标 5.4.7.0，内部元数据与启动日志为 5.4.7.2 |
| Item Filters | `[物品过滤器] ItemFilters-1.0.4.2.jar` | |

## 配置入口

`config/` 下与本项目相关的项：

| 路径 | 作用 |
| --- | --- |
| `Galacticraft/` | GC 本体开关 |
| `GalaxySpace/` | GS 天体、地形（World Engine）、矿物生成开关 |
| `ExtraPlanets.cfg` | EP 天体开关 |
| `AsmodeusCore/` | 气压、辐射等环境机制 |
| `galacticraftcompatibility.cfg` | 兼容层 |
| `ftbquests-shared-team.cfg` | 公共团队启用开关和指定既有团队 ID，修改后重启 |
| `ftbquests/normal/` | 默认任务定义；游戏规则或 PackMode 可改变模式目录，进度另存在 Library 团队数据中 |
| `ZollernGalaxy`、`sol`、`jei` | 留存配置；不代表对应模组当前已安装 |

天体开关、维度和地形方式见[星球静态调查](../docs/research/planet-gameplay-audit.md)，主体标明首轮快照边界；后续运行与变更见[玩法详解](../docs/research/planet-gameplay-guide.md)。本轮未把最新本机配置导出为可复现的整合包发行配置。

## 已知的本地改动

| 项 | 状态 |
| --- | --- |
| GalaxySpace | 使用 flight-fix 包，非上游 `dev_1.12.2` 原版 |
| GSLaunchCompat | 自行开发，版本号见 [gslc.md](gslc.md) |
| FTB Quests | 本地构建，源码改动见 [ftb-quests.md](ftb-quests.md) |
| EP 天体 | 多数主星与卫星已关闭，保留 Kepler-22b、Phobos 和部分星图展示天体 |
| 其他配置 | 待补：哪些开关是自行改的、为什么改，需要逐项确认后记录 |

"其他配置"一行目前是空的，因为还没有逐项对照默认值。补这份内容时要能回答：改了哪一项、改之前是什么、为什么改。

## 待核对

- 远程服务器的模组清单与本机是否一致。
- 是否有仅在某一端安装的模组（客户端/服务端差异）。
- 本机已有新启动日志，不能继续记为“尚未启动”；小样读入与月球存档加载证据见[运行日志补记](../docs/research/planet-gameplay-guide.md#十三2026-11-02-运行日志补记)。双人联机、正常航行、资源与环境流程仍待验收。
- 当前配置、11 月 1 日调查快照及未来 T7 / T8 规划的差异；规划未自动应用到本机实例。

## 2026-11-02 EP 源码导入

- **改动**：将 ExtraPlanets `dev_1.12.2` 分支、提交 `fc4a4cb830897da032477210db458a0567948957` 的完整源码快照导入根目录 `ExtraPlanets/`，版本声明为 0.8.0；更新来源记录和研究文档中的源码入口。
- **影响面**：源码阅读与玩法调查。原上游文件保持不变，未接入统一构建，未修改游戏配置、存档或已安装 JAR。
- **验证状态**：核对上游文件内容及版本声明；Kepler T10 与 Phobos 化石方块依赖结合实际 JAR 字节码复核。未构建或运行新 EP 模组，详见 [EP 源码说明](../docs/research/extraplanets-source-notes.md)。本页上述 11 月 1 日运行状态属于旧基线，新日志异常见玩法详解的 EP 火卫一条目。
- **是否需要服务端同步**：本次源码导入无需替换客户端或服务端模组。

## 2026-11-02 文档与任务小样同步

- **改动**：更新项目入口、FTB 方案、任务内容稿、三节点小样说明、星球研究及新地球两条开发线的导航。将旧的“零任务”“尚未启动”“未实现公共团队”等表述分别归入历史快照或当前验证范围。
- **影响面**：文档与既有小样配置归档；未修改客户端、服务端、模组代码或存档。小样定义同步到仓库不等于自动部署到另一台机器。
- **验证状态**：静态链接、任务小样语法与当前日志核对；完整游戏验证仍按各文档待验项推进。
- **是否需要服务端同步**：文档本身不需要；任务小样若用于测试服，应停服备份并按说明增量部署，两端 FTB 版本另行核对。
