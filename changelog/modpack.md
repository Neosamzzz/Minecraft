# 整合包基线

记录本机实际在玩的整合包组成，用于换机器、重装实例、分发同版本包和排查"对方那边为什么不一样"。

实例目录：`C:\Users\qq106\Desktop\游戏\Release\.minecraft\versions\1.12.2-Forge_14.23.5.2864`（客户端）。远程服务器的实际版本尚未核对。

记录日期：2026-11-01。本页只记文件与配置层面的现状，玩法结论见 [docs/research/](../docs/research/)。

## 模组清单

客户端 `mods/` 当前 11 个文件：

| 模组 | 文件 | 说明 |
| --- | --- | --- |
| Forge | 14.23.5.2864 | 实例版本 |
| Galacticraft | `Galacticraft-1.12.2-4.0.6.jar` | GC 本体 |
| GalaxySpace | `GalaxySpace-1.12.2-2.2.0-flight-fix.jar` | 已改的 flight-fix 包，非上游原版 |
| ExtraPlanets | `ExtraPlanets-1.12.2-0.8.0.jar` | 多数天体已关闭，见下 |
| AsmodeusCore | `AsmodeusCore-1.12.2-1.0.5.jar` | GS/EP 的依赖 |
| Galacticraft Compatibility | `galacticraftcompatibility-1.1.2.jar` | |
| MJRLegendsLib | `MJRLegendsLib-1.12.2-1.2.1.jar` | |
| MixinBooter | `!mixinbooter-10.7.jar` | GSLC 运行所需 |
| GSLaunchCompat | `GSLaunchCompat-1.4.8.jar` | 本项目产物，见 [gslc.md](gslc.md) |
| FTB Quests | `FTBQuests-1202.9.0.jar` | 本项目产物，见 [ftb-quests.md](ftb-quests.md) |
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
| `ZollernGalaxy`、`sol`、`jei` | 其他模组配置 |

具体开关取值与影响见 [星球与玩法现状](../docs/research/planet-gameplay-audit.md)，那里逐项列出了当前开着的天体与地形方式。

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
- 当前这套配置尚未在游戏内启动验证，见 [audit 的调查边界](../docs/research/planet-gameplay-audit.md)。
