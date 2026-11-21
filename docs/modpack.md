# 整合包依赖与配置

下表区分仓库构建版本与既有实例依赖基线，不代表本机或远程服务器已安装最新包；部署前核实实际模组文件。NewEarth 当前终章运行组合采用 Cleanroom、Java 25、OTG 与 Biome Bundle，具体依赖和安装方式见[终章维度](../project/NewEarth/dimension/README.md#安装与使用)。

## 模组清单


| 模组 | 文件 | 说明 |
| --- | --- | --- |
| Forge | 14.23.5.2864 | 既有 Forge 共存基线；终章组合使用 Cleanroom |
| Galacticraft | `Galacticraft-1.12.2-4.0.6.jar` | GC 本体 |
| GalaxySpace | `GalaxySpace-1.12.2-2.2.0-flight-fix.jar` | 已改的 flight-fix 包，非上游原版 |
| ExtraPlanets | `ExtraPlanets-1.12.2-0.8.0.jar` | 星球、资源与设备 |
| AsmodeusCore | `AsmodeusCore-1.12.2-1.0.5.jar` | GS 运行依赖；EP 同时使用 GC 与 MJRLegendsLib |
| Galacticraft Compatibility | `galacticraftcompatibility-1.1.2.jar` | |
| MJRLegendsLib | `MJRLegendsLib-1.12.2-1.2.1.jar` | |
| MixinBooter | `000mixinbooter-12.2.jar` | Mixin 加载依赖 |
| GSLaunchCompat | `GSLaunchCompat-1.4.8.jar` | 遥感、地图与发射控制补丁 |
| FTB Quests | `FTBQuests-1202.9.6.jar` | 公共团队与任务书 |
| FTB Library | `[FTB Library（旧版）] FTBLib-5.4.7.0.jar` | 文件名标 5.4.7.0，内部元数据与启动日志为 5.4.7.2 |
| Item Filters | `[物品过滤器] ItemFilters-1.0.4.2.jar` | |
| NewEarth | `NewEarth-2.0.4.jar` | 当前仓库构建版本；太阳系整合、制造、终章与可选 AE2 整合 |
| AE2 Unofficial Extended Life | `ae2-uel-v0.56.6.jar` | 可选；仓储配置和用法见 [AE2 仓储](../project/NewEarth/items/README.md#ae2-仓储)，不代表正式服已安装 |
| Saplanting | `saplanting-forge-1.12.2-0.2-serverfix.jar` | [落苗生根服务端修复](../project/Saplanting/README.md)，关闭多线程 |
| FTB Ultimine | `ftb-ultimine-1202.3.5.jar` | 连锁挖掘，客户端与服务端安装 |
| Advanced XRay | `xray-1.12.2-1.6.0.jar` | 矿物透视，仅客户端 |
| Infinity with Mending | `infwithmend-1.12.2-1.0.0.jar` | 允许弓的无限与经验修补共存，客户端与服务端安装 |

### 高版本移植内容

正式服务器使用 NewEarth 1.0.9 和下列高版本移植模组。本机 PCL 使用原有的 `1.12.2-NewEarth-Cleanroom` 实例。其他客户端同步更新 NewEarth，并安装下列三个文件。

| 模组 | 固定版本与用途 |
| --- | --- |
| Aqua Acrobatics Unofficial | [1.17.1](https://www.curseforge.com/minecraft/mc-mods/aqua-acrobatics-unofficial-forge/files/9329485)，游泳和爬行，含 Galacticraft 玩家身高兼容 |
| Future MC | [0.2.21](https://www.curseforge.com/minecraft/mc-mods/future-mc/files/6162819)，三叉戟及其附魔、营火、灯笼、桶等内容 |
| Forgelin-Continuous | [2.4.20.0](https://modrinth.com/mod/forgelin-continuous/version/2.4.20.0)，Future MC 的 Kotlin 运行库 |

两端同步 `aquaacrobaticsunofficial.cfg` 和 `futuremc/futuremc.cfg`。保留原版活板门配方、水体渲染和水下回气，关闭下界合金与远古残骸生成。Future MC 关闭下界合金后仍会尝试读取两条下界合金块配方，启动日志会记录相应的配方错误。Future MC 0.2.21 的 Forge 联机列表显示内部版本 `0.2.6`，更新时以发布文件为准。

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
| `newearth-dimension.cfg` | 终章维度配置，默认维度 ID 为 1020 |
| `newearth-worldgen.cfg` | 主世界孤岛布局开关与尺寸 |
| `AppliedEnergistics2/AppliedEnergistics2.cfg` | 功能裁剪、无频道与关闭放置预览；启动时按项合入，不覆盖其他设置 |

固定太阳系组合由 `ExtraPlanets.cfg` 与 `GalaxySpace/dimensions.conf` 共同定义；NewEarth 启动时校验，不自动重写。OTG 预设覆盖和客户端光影参数随 `NewEarth-BiomeBundle-<版本>.zip` 分发。

## 服务器存档副本

正式服务器每 6 小时在实例内的 `backups/` 生成一份 `world-日期-时间.tar.gz`，保留最近 12 份。归档前先将世界保存到磁盘，归档结束后恢复世界自动保存。运行记录见 `backups/backup.log`。
