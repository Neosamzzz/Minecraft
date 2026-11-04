# 整合包依赖与配置

运行版本基线，远程服务器版本需在部署前核实。

## 模组清单


| 模组 | 文件 | 说明 |
| --- | --- | --- |
| Forge | 14.23.5.2864 | 实例版本 |
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

| NewEarth | `NewEarth-0.2.9.jar` | 太阳系整合与内容裁剪 |


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
