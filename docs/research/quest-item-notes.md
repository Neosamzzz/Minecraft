# FTB 任务关键物品核对

核对日期：2026-11-02。服务于[任务内容稿](../ftb-quest-manuscript.md)，对应 [Issue #12](https://github.com/Neosamzzz/Minecraft/issues/12)。本页记录选材与源码依据，不重复维护完整物品百科或游戏实测记录。

## 选取方式

优先选能体现新玩法的成品：载具、移动工具、供氧设备、探索仪器、补给与特殊样本。普通零件留在配方里，不为每个零件或每种颜色建立节点。新增支线不成为火箭主干的统一前置。

“源码注册、有配方”“本机配置开启”“正常生存能做出并使用”分别看待。本轮读取本地 GC / GS / EP 源码和 EP 配置，没有启动游戏或验收这些物品。EP 源码与本机 JAR 的对应边界见[源码说明](extraplanets-source-notes.md)。

## 已写入文稿的新增物品

| 节点 | 模组与物品 | 已核对依据 | 配置与玩法边界 |
| --- | --- | --- | --- |
| A12 | GC 降落伞 `galacticraftcore:parachute` | `GCItems` 注册；`recipes/parachute_0.json` 等配方 | 颜色可选；取得不证明已装备或完成降落 |
| A13 | GC 旗帜 `galacticraftcore:flag` | `GCItems` 注册；`recipes/flag.json` | 仅检测获得，不虚构已插旗判定 |
| A14 | GC 月球车 `galacticraftcore:buggy` | `GCItems` 注册；`RecipeManagerGC` 中调用 `RecipeUtil.addBuggyBenchRecipe` 的 NASA 工作台车辆配方 | 核对图纸解锁；接受货舱变体，不检测实际驾驶 |
| A15 | GC 氧气收集器 `galacticraftcore:collector` | `GCBlocks` 注册；`recipes/collector.json` | 工作依赖供电和环境，获得不等于已有氧气来源 |
| A16 | GC 氧气压缩机 `galacticraftcore:oxygen_compressor` | `GCBlocks` 注册；`recipes/oxygen_compressor_0.json` | 与材料压缩机不同；核对物品元数据，不混入其他机器 |
| B07 | GC 保暖衬垫 `galacticraftplanets:thermal_padding` | `AsteroidsItems` 注册；`ItemThermalPadding` 的可见部位元数据 0～3 | 任一部位即可；不声称穿一件或这一级可覆盖所有星球 |
| B08 | EP 罐头 `extraplanets:canned_food` | `ExtraPlanets_Items` 注册；`ExtraPlanets_RecipeGeneration` 用 GC 空罐和熟食生成 0～3 配方 | 本机 `Custom Canned Food=true`；首版仅匹配 0～3，不把空罐、脱水原料都算作食品 |
| C13 | GC 抓钩 `galacticraftplanets:grapple` | `AsteroidsItems` 注册；`recipes/grapple.json` 为铁锭和线 | 属于可提前制作的移动工具，不表示需先登陆小行星带 |
| C14 | GC 自动采矿机 `galacticraftplanets:astro_miner` | `AsteroidsItems`、`RecipeManagerAsteroids` 的 NASA 工作台配方；`ItemAstroMiner` 的矿机基地检查 | 需要图纸与配套基地；代码拒绝部分部署情形，如轨道空间站，不能承诺全维度可用 |
| C15 | GC 氧气密封器 `galacticraftcore:sealer` | `GCBlocks` 注册；`recipes/sealer.json` | 只证明设备获得，房间密封、通电和供氧另核 |
| C16 | EP 高级电池 `extraplanets:advanced_battery` | `ExtraPlanets_Items` 注册；生成配方使用 GC 电池、压缩钛和 GC 火星方块 | 本机 `Custom Batteries=true`；与 GS 同名电池分开，不连入依赖关闭行星的 EP 终极电池配方 |
| D08 | GS 地质扫描仪 `galaxyspace:geo_scanner` | `GSItems` 注册；`CraftingRecipesOverworld` 的装配配方；`ItemGeologicalScanner.onItemUse` | 工具消耗电量，存在岩层和地下流体等扫描逻辑；不声称能标出全部矿物 |
| D09 | GS 电动喷气背包 `galaxyspace:jetpack` | `GSItems` 注册；`CraftingRecipesOverworld` 普通合成；`ItemJetpack.canFly` 与电量检查 | 需要电量和对应使用状态；不承诺无限飞行、免除氧气或完全替代宇航装备 |
| G07 | EP 铂锭 `extraplanets:tier11_items:5` | `ItemTier11Items.getItemList`；Kepler 的 `BiomeDecoratorKepler22bOres` 有铂矿生成分支 | 系外路线适配与掉落、冶炼待验；压缩铂为同物品元数据 6，不能混成铂锭 |
| G08 | EP 彩色钻石 `extraplanets:tier11_items:0～4` | `ItemTier11Items.getItemList`；Kepler 地形装饰器有多色材料树和材料球实现 | 样本目标任一种即可，不接受普通钻石；最终核心配方另要求五色全部参与 |

其中 G07、G08 随 Kepler 分支待接入，不是当前即可发布的生存任务。推进顺序为先造基础八阶飞船，再到开普勒收集已有特色资源并合成最终能源核心；材料组合确定为蓝、红、紫、黄、绿五种钻石全部加铂制品，不另新增一种“特殊材料”。具体数量、铂制品形式与配方布局待定；这些后期材料不作为基础八阶或装配台的必需输入。已有物品的注册与生成依据不代表新增核心配方已经实现。

## 来源入口

- GC 注册：[GCItems](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/core/GCItems.java)、[GCBlocks](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/core/GCBlocks.java)、[AsteroidsItems](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/planets/asteroids/items/AsteroidsItems.java)。
- GC 配方：[基础配方目录](../../Galacticraft-Legacy/src/main/resources/assets/galacticraftcore/recipes)、[行星配方目录](../../Galacticraft-Legacy/src/main/resources/assets/galacticraftplanets/recipes)、[月球车配方](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/core/recipe/RecipeManagerGC.java)、[自动采矿机配方](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/planets/asteroids/recipe/RecipeManagerAsteroids.java)。
- GS 注册与配方：[GSItems](../../GalaxySpace/src/main/java/galaxyspace/core/GSItems.java)、[主世界配方](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/recipes/CraftingRecipesOverworld.java)。
- GS 使用行为：[扫描仪](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/items/tools/ItemGeologicalScanner.java)、[喷气背包](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/items/armor/ItemJetpack.java)。
- EP 注册与配方：[物品注册](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ExtraPlanets_Items.java)、[配方生成](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/recipes/ExtraPlanets_RecipeGeneration.java)、[材料子项](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/planetAndMoonItems/ItemTier11Items.java)、[Kepler 铂矿](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/planets/Kepler22b/worldgen/BiomeDecoratorKepler22bOres.java)、[Kepler 地形装饰](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/planets/Kepler22b/worldgen/BiomeDecoratorKepler22b.java)。

本轮本机配置路径：`C:\Users\qq106\Desktop\游戏\Release\.minecraft\versions\1.12.2-Forge_14.23.5.2864\config\ExtraPlanets.cfg`。只读核对 `Custom Canned Food`、`Custom Batteries`、`Custom Oxygen Tanks` 开启；不推断已关闭行星的材料也可获取。

## 暂不直接加入的候选

| 物品 | 取舍 |
| --- | --- |
| GS 应急便携传送器、临时防护控制器 | 既有资料有地牢池依据，但传送目的地、触发、消耗和保护效果需验证；先不把它们写成安全保证 |
| GS 亚光速引擎模块 | 有跨系模块描述，现有星图与 T7 准入规则仍需对接；不新增未经确认的强制安装节点 |
| EP 超重型氧气罐 | 本机开关开启，但配方使用 EP `tier5_items:5`，对应 EP 主星组已关闭；不因物品注册就认定生存可合成 |
| EP 终极、巨型电池 | 配方牵涉多个已关闭 EP 行星组的材料，先不接成必需升级链 |
| EP 盖革计数器 | 已注册不等于配方和共存读数均已落实；不以此验收 GS / EP 全部环境防护 |
| GS 多座火箭、流体货运火箭、物质操纵器 | `GSItems.initialize` 中对应注册为注释，不能根据类存在就写成当前可取得物品 |
| 八阶导航、推进、维生部件 | 当前仍是设计候选，留在物品线后续决定；不逐个建立 FTB 节点 |
| 最终能源核心 | 材料组合确定为五种彩色钻石全部加铂制品，用量与配方待实现；G07/G08 记录样本，J02 记录核心，撤销重复的 J01；五色齐备由制造配方保证，安装和资格由飞船代码判断 |

## 转配置时

以上 `命名空间:物品:元数据` 是作者侧记法，不是直接可粘贴的 SNBT 物品字符串。部署时拆成真实注册 ID 与元数据；机器、图纸和板材严格区分子项，颜色、电量、货舱按目标适当放宽。所有目标不消耗物品，无任务奖励。

本次太阳系配置已落实 60 个目标，精确映射见[配置 manifest](../../quest-config/solar-system/manifest.json)。特别核对：材料压缩机为 `galacticraftcore:machine` 元数据 12；土卫六蓝宝石掉落复用 `galacticraftcore:item_basic_moon` 元数据 2；冰类采集产物为 `galaxyspace:gs_basic` 元数据 23、24、27，不把地层方块与晶体混用。图纸严格区分 GC 二级与三级火箭子项。

新增节点只做增量配置，固定既有 ID，保留团队记录。优先验证加载、物品匹配和不消耗，游戏结果统一回填[玩法详解](planet-gameplay-guide.md)，不在本页另立实测口径。
