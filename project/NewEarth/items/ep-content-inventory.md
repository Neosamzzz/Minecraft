# EP 内容取舍清单

取舍按装备、载具、材料、设备和世界内容分类。移除项取消注册及制造；GS宇航服与温控装备承接环境防护。非合成获取入口和旧存档迁移另行核查。

## 装备

### 普通工具、武器与护甲

16种材质，共80件工具武器、64件普通护甲，全部移除注册。每种材质包含镐、斧、锹、锄、剑及四件护甲。材质开关关闭，原料保留。

| 材质 | 工具／武器 | 普通护甲 | 原配置控制 |
| --- | --- | --- | --- |
| 汞 | 5件，`MERCURY_*` | 4件，`MERCURY_*` | `ITEMS_MERCURY` |
| 碳 | 5件，`CARBON_*` | 4件，`CARBON_*` | `ITEMS_CARBON` |
| 铀 | 5件，`URANIUM_*` | 4件，`URANIUM_*` | `ITEMS_URANIUM` |
| 钯 | 5件，`PALLADIUM_*` | 4件，`PALLASIUM_*` | `ITEMS_PALLADIUM` |
| 镁 | 5件，`MAGNESIUM_*` | 4件，`MAGNESIUM_*` | `ITEMS_MAGNESIUM` |
| 水晶 | 5件，`CRYSTAL_*` | 4件，`CRYSTAL_*` | `ITEMS_CRYSTAL` |
| 钨 | 5件，`TUNGSTEN_*` | 4件，`TUNGSTEN_*` | `ITEMS_TUNGSTEN` |
| 锌 | 5件，`ZINC_*` | 4件，`ZINC_*` | `ITEMS_ZINC` |
| 红色宝石 | 5件，`RED_GEM_*` | 4件，`RED_GEM_*` | `ITEMS_GEM_RED` |
| 蓝色宝石 | 5件，`BLUE_GEM_*` | 4件，`BLUE_GEM_*` | `ITEMS_GEM_BLUE` |
| 白色宝石 | 5件，`WHITE_GEM_*` | 4件，`WHITE_GEM_*` | `ITEMS_GEM_WHITE` |
| 蓝钻石 | 5件，`BLUE_DIAMOND_*` | 4件，`BLUE_DIAMOND_*` | `ITEMS_KEPLER22B` |
| 红钻石 | 5件，`RED_DIAMOND_*` | 4件，`RED_DIAMOND_*` | `ITEMS_KEPLER22B` |
| 紫钻石 | 5件，`PURPLE_DIAMOND_*` | 4件，`PURPLE_DIAMOND_*` | `ITEMS_KEPLER22B` |
| 黄钻石 | 5件，`YELLOW_DIAMOND_*` | 4件，`YELLOW_DIAMOND_*` | `ITEMS_KEPLER22B` |
| 绿钻石 | 5件，`GREEN_DIAMOND_*` | 4件，`GREEN_DIAMOND_*` | `ITEMS_KEPLER22B` |

### 宇航服与温控

| 对象 | 源码字段或范围 | 取舍与依赖 |
| --- | --- | --- |
| 宇航服 | 零级4件；一至四级各6件，含喷气胸甲与重力靴 | 移除注册；由GS普通／轻型宇航服承接气压与辐射防护 |
| 未完成宇航服 | `TIER_1_UNPREPARED_SPACE_SUIT_*` 至 `TIER_4_UNPREPARED_SPACE_SUIT_*`，共16件 | 移除注册及制造 |
| 气压层 | `TIER_1_PRESSURE_LAYER` 至 `TIER_4_PRESSURE_LAYER` | 移除注册及制造 |
| 辐射层 | `TIER_1_RADIATION_LAYER` 至 `TIER_4_RADIATION_LAYER` | 移除注册及制造 |
| 护甲层 | `TIER_1_ARMOR_LAYER` 至 `TIER_4_ARMOR_LAYER` | 移除注册及压缩配方 |
| 温控材料 | `THERMAL_CLOTH` | 移除注册及制造 |
| 温控装备 | `TIER_3_THERMAL_PADDING`、`TIER_4_THERMAL_PADDING`、`TIER_5_THERMAL_PADDING` | 移除注册及装备槽注册；采用GC一／二级、GS三／四级温控装备 |

### 装备模块

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `CLOTH` | 移除 | 原用于宇航服防护层与过滤器；过滤器改用任意颜色羊毛 |
| `GRAVITY_CONTROLLER` | 移除 | EP重力靴专用部件；重力改装采用GS体系 |
| `MODULE_ITEMS` | 移除 | EP宇航服模块素材；模块注册同步移除 |

EP专属模块及素材随宇航服移除；改装采用GS接口。

### 装备套件

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `ENVIRO_EMERGENCY_KIT` | 移除 | 取消EP的gckit附加发放，保留GC原命令行为 |
| `TIER_1_EQUIPMENT_KIT` | 移除 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |
| `TIER_2_EQUIPMENT_KIT` | 移除 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |
| `TIER_4_EQUIPMENT_KIT` | 移除 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |
| `TIER_3_EQUIPMENT_KIT` | 移除 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |
| `TIER_5_EQUIPMENT_KIT` | 移除 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |

套件注册与制造移除；EP的`gckit`附加发放移除。

## 补给

### 氧气罐

| 对象 | 源码字段 | 容量 | 取舍 |
| --- | --- | --- | --- |
| 彩色轻型罐 | `LIGHT_OXYGEN_TANK_*`，15种 | 900 | 移除物品及左右罐位注册 |
| 彩色中型罐 | `MED_OXYGEN_TANK_*`，15种 | 1800 | 移除物品及左右罐位注册 |
| 彩色重型罐 | `HEAVY_OXYGEN_TANK_*`，15种 | 2700 | 移除物品及左右罐位注册 |

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `OXYGEN_TANK_VERY_HEAVY` | 保留 | 容量7200；容量升级档位 |
| `OXYGEN_TANK_EXTREMELY_HEAVY` | 保留 | 容量9000；容量升级档位 |

彩色普通罐由GC同容量罐承接。`OXYGEN_TANKS`仅控制两种超重罐。GS四／五／六级罐默认容量3500／4000／4500，EPP默认2500，实际容量由配置决定。

### 电池

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `BATTERY_NICKEL` | 保留 | 容量85000，电力等级2；用于机器供电 |
| `BATTERY_ZINC` | 保留 | 容量125000，电力等级2；用于机器供电 |
| `BATTERY_MERCURY` | 保留 | 容量100000，电力等级2；用于机器供电 |
| `BATTERY_ADVANCED` | 保留 | 容量150000，电力等级2；用于机器供电 |
| `BATTERY_ULTIMATE` | 保留 | 容量200000，电力等级2；用于机器供电 |
| `BATTERY_MASSIVE` | 保留 | 容量500000，电力等级2；用于机器供电 |

GS高级电池容量60000。手持电池与漫游车配方中的电池部件分别维护。

### 环境检测与处理

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `GEIGER_COUNTER` | 保留 | 读取EP辐射累积值，EP水星特殊池也产出；保留EP辐射时有用，作用范围为EP辐射系统 |
| `ANTI_RADIATION` | 保留 | 有实际JSON配方与饮用逻辑；只改变EP辐射能力值，不能处理GS辐射 |

抗辐射饮料配方：碘化钾4、GS镁板2、净水桶2、玻璃瓶1。饮用后EP辐射剩余值为原值乘配置百分比。

### 食品与植物

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `CANNED_FOOD` | 可选保留 | 4种脱水肉食罐头，普通食物功能，食用掉落GC空罐；不参与主线解锁 |
| `DIAMOND_APPLE` | 剔除候选 | 普通与附魔两个子项，提供多种增益；非航天必需品，属额外战斗补给 |
| `IRON_APPLE` | 剔除候选 | 普通与附魔两个子项，提供多种增益；非航天必需品 |
| `WHITE_SUGAR_CANE` | 可选保留 | 实际为绑定方块的种植物品，随开普勒生态统一取舍，不和装备一起删 |
| `CHOCOLATE_BAR` | 可选保留 | 普通食品，饥饿值1／饱和参数0.3；随生态食品取舍 |
| `CARAMEL_BAR` | 可选保留 | 普通食品，饥饿值1／饱和参数0.25；随生态食品取舍 |

肉食罐头包含猪肉、鱼、鲑鱼和鸡肉，食用后返还GC空罐。铁苹果与钻石苹果各含普通、附魔两种子项。

## 载具

### 原EP火箭

| 对象 | 范围 | 取舍 |
| --- | --- | --- |
| 火箭成品 | `TIER_4_ROCKET`至`TIER_10_ROCKET`、`TIER_10_ELECTRIC_ROCKET` | 移除物品与实体注册，清空8张专用NASA配方表 |
| 鼻锥 | `TIER_4_NOSE_CONE`至`TIER_10_NOSE_CONE` | 移除注册与配方 |
| 旧火箭图纸 | `TIER_4_SCHEMATIC`至`TIER_10_SCHEMATIC`、`TIER_10_ELECTRIC_ROCKET_SCHEMATIC` | 移除物品及NASA页面注册 |
| 混装部件 | `tier4_items`至`tier10_items`的meta0～3、`tier7_items:6` | 移除制造与枚举，保留原料容器 |

### 漫游车

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `MARS_ROVER` | 可选保留 | 独立地面载具，NASA图纸与电力部件／充能器需成套取舍 |
| `VENUS_ROVER` | 可选保留 | 独立地面载具，NASA图纸与电力部件／充能器需成套取舍 |
| `MARS_ROVER_SCHEMATIC` | 随载具取舍 | 原加入GC二级特殊池；删除载具时同步移除图纸产出 |
| `VENUS_ROVER_SCHEMATIC` | 随载具取舍 | 原加入GC三级特殊池；删除载具时同步移除图纸产出 |
| `ELECTRIC_PARTS` | 随载具取舍 | 4子项：两级电池部件、两级电动车轮，不等同于6种手持电池 |

漫游车、电池／车轮部件、充能器和平台成组取舍。

## 材料

### 矿物与加工素材

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `COMPRESSED_MERCURY` | 保留 | 汞锭压缩产物；粉碎机、太阳能蒸发室的制造原料 |
| `INGOT_URANIUM` | 保留，统一项待核对 | 化学注入器的原制造材料之一 |
| `INGOT_MERCURY` | 保留 | 水星汞矿冶炼产物；用于压缩汞材料与汞电池 |
| `INGOT_LEAD` | 移除注册 | 冶炼及电动车辆电池部件配方统一使用GC金星铅锭 |
| `TOOLS` | 保留 | meta0巨锤、1磨轮、2线网、3过滤器；用于加工设备制造 |
| `WAFERS` | 保留 | 钻石、碳、钛、红宝石、蓝宝石、白宝石6类晶圆；用于设备及空间站配方 |
| `IODIDE_SALT` | 保留 | 结晶器产物；供净化器及化学注入器使用 |
| `POTASSIUM_IODIDE` | 保留 | 碘盐与钾在化学注入器中加工；抗辐射饮料原料 |
| `POTASSIUM` | 保留 | 蒸发室加工钾盐碎片获得；化学注入器原料 |
| `POTASH_SHARDS` | 保留 | 粉碎钾盐矿获得；蒸发提钾原料 |

### 多子项材料容器

| 容器 | 元数据与子项 | 处理边界 |
| --- | --- | --- |
| `tier4_items` | 0: `tier4engine`；1: `tier4booster`；2: `tier4fin`；3: `tier4heavy_duty_plate`；4: `compressed_carbon`；5: `ingot_carbon`；6: `diamond_shard` | 0～3移除制造与枚举；矿物及通用压缩材料保留 |
| `tier5_items` | 0: `tier5engine`；1: `tier5booster`；2: `tier5fin`；3: `tier5heavy_duty_plate`；4: `compressed_palladium`；5: `ingot_palladium`；6: `compressed_nickel`；7: `ingot_nickel`；8: `red_gem`；9: `ash`；10: `ash_shard`；11: `volcanic_shard`；12: `ingot_volcanic` | 0～3及6／7移除制造与枚举；其余材料保留 |
| `tier6_items` | 0: `tier6engine`；1: `tier6booster`；2: `tier6fin`；3: `tier6heavy_duty_plate`；4: `compressed_magnesium`；5: `ingot_magnesium` | 整组移除注册，镁改用GS锭与板 |
| `tier7_items` | 0: `tier7engine`；1: `tier7booster`；2: `tier7fin`；3: `tier7heavy_duty_plate`；4: `compressed_crystal`；5: `ingot_crystal`；6: `compressed_reinforced_crystal`；7: `white_gem` | 0～3移除制造与枚举；矿物及通用压缩材料保留 |
| `tier8_items` | 0: `tier8engine`；1: `tier8booster`；2: `tier8fin`；3: `tier8heavy_duty_plate`；4: `compressed_zinc`；5: `ingot_zinc`；6: `blue_gem` | 0～3移除制造与枚举；矿物及通用压缩材料保留 |
| `tier9_items` | 0: `tier9engine`；1: `tier9booster`；2: `tier9fin`；3: `tier9heavy_duty_plate`；4: `compressed_tungsten`；5: `ingot_tungsten` | 0～3移除制造与枚举；矿物及通用压缩材料保留 |
| `tier10_items` | 0: `tier10engine`；1: `tier10booster`；2: `tier10fin`；3: `tier10heavy_duty_plate`；4: `compressed_dark_iron`；5: `ingot_dark_iron` | 0～3移除制造与枚举；矿物及通用压缩材料保留 |
| `tier11_items` | 0: `blue_diamond`；1: `red_diamond`；2: `purple_diamond`；3: `yellow_diamond`；4: `green_diamond`；5: `ingot_platinum`；6: `compressed_platinum`；7: `blue_sticks`；8: `red_sticks`；9: `purple_sticks`；10: `yellow_sticks`；11: `green_sticks`；12: `brown_sticks` | 0～6保留五色钻石与铂；7～12随木材生态保留 |

`tier7_items:6`为旧强化板专用的强化水晶中间料，移除制造与枚举。保留材料继续核对加工与用途。

### 重复材料统一

| 旧材料 | 当前材料 |
| --- | --- |
| `tier6_items:5` 镁锭 | GS镁锭 `galaxyspace:ingots:1` |
| `tier6_items:4` 压缩镁 | GS镁板 `galaxyspace:compressed_plates:2` |
| `tier5_items:7` 镍锭 | GS镍锭 `galaxyspace:ingots:2` |
| `tier5_items:6` 压缩镍 | GS镍板 `galaxyspace:compressed_plates:3` |
| `ingot_lead` 铅锭 | GC金星铅锭 `galacticraftplanets:basic_item_venus:1` |
| `saturn:7` 镁块 | GS镁块 `galaxyspace:blocksmetals:2` |
| `jupiter:13` 镍块 | GS镍块 `galaxyspace:blocksmetals:1` |

保留EP木星镍矿、土星镁矿及铅矿，冶炼统一产出对应GS／GC锭。镁、镍沿用GS的两锭压一板配方；材料块只使用GS版本的合成与拆解，设备制造、空间站建设材料及GC回收产物使用统一材料。EP独立铅锭与原六阶材料容器取消注册；共用容器中的重复镍和储存块子项移除获取、矿词、创造栏及JEI列表入口。不提供旧材料转换或存档迁移。

铀暂保留EP铀锭与GS铀碎片，二者的加工关系待定。EP水晶与GS未知晶体、EP火山锭与GS火山石分别保留。

### 流体与桶

13种流体使用Forge通用桶，按桶内流体区分。旧冻结水桶注册行已注释。

| 流体注册名 | 已查到的处理关系 | 建议 |
| --- | --- | --- |
| `glowstone_fluid` | 浓缩机输出荧石块 | 保留流体／地形，处理用途可复用 |
| `magma_fluid` | 无已知浓缩机处理分支 | 随保留地形保留，不直接等同熔岩或火箭燃料 |
| `nitrogen_fluid` | 浓缩机输出冻结氮 | 保留；不能仅凭名字替换 GS 氮气 |
| `frozen_water_fluid` | 浓缩机输出冰 | 保留，作为原冰类资源加工候选 |
| `salt_fluid` | 结晶器／逆结晶器与碘盐互转 | 随化学链待定，不按名称当作普通水 |
| `radioactive_water_fluid` | 净化器第一段输入 | 随地形和环境适配保留引用 |
| `clean_water_fluid` | 净化器最终输出；抗辐射饮料配方输入 | 随净水及饮料取舍，不等于原版水桶 |
| `infected_water_fluid` | 净化器中间产物及第二段输入 | 与净化链一起决定 |
| `methane_fluid` | 无已知浓缩机处理分支 | 随地形保留，不直接视为 GS 甲烷或可加注燃料 |
| `nitrogen_ice_fluid` | 浓缩机输出冰，与 `nitrogen_fluid` 输出不同 | 保留并分清两种流体 |
| `liquid_hydrocarbon_fluid` | EP 土星地形用液体；浓缩机输出煤 | 保留 |
| `liquid_chocolate_fluid` | 浓缩机输出巧克力条 | 随开普勒食品生态可选保留 |
| `liquid_caramel_fluid` | 浓缩机输出焦糖条 | 随开普勒食品生态可选保留 |

## 设备

### 加工与补给机器

| 字段／设备 | 建议 | 实际用途与依赖 |
| --- | --- | --- |
| `REFINERY_ADVANCED`、`REFINERY_ULTIMATE` | 保留 | GC精炼功能升级；制造材料包含钯、锌 |
| `OXYGEN_COMPRESSOR_ADVANCED`、`OXYGEN_COMPRESSOR_ULTIMATE` | 保留 | 每个字段含 meta0压缩、meta1解压，承担基地供氧补给 |
| `SOLAR_PANEL` | 保留 | meta0混合、meta1终极；承担供电，原配方使用镁／水晶 |
| `FUEL_LOADER_ADVANCED`、`FUEL_LOADER_ULTIMATE` | 保留 | 承担燃料装载；新载具接口适配另行确定 |
| `BASIC_SMASHER` | 保留，加工扩展待定 | 原注册配方：钾盐矿→3碎片；有输入→输出配方表及增删接口，不代表支持多原料合金 |
| `BASIC_SOLAR_EVAPORTATION_CHAMBER` | 保留，加工扩展待定 | 原注册配方：12钾盐碎片→1钾；同样有独立配方表 |
| `BASIC_CRYSALLIZER` | 保留材料加工用途 | `salt_fluid`→碘盐；固定处理写在TileEntity中 |
| `BASIC_DECRYSALLIZER` | 保留材料加工用途 | 6碘盐→`salt_fluid`；字段拼写保留源码原样 |
| `BASIC_CHEMICAL_INJECTOR` | 保留材料加工用途 | 输入固定检查碘盐和钾，产出碘化钾；改成加工扩展需适配机器处理逻辑 |
| `BASIC_PURIFIER` | 保留净水用途 | 消耗碘盐，放射性水→污染水→净水两段处理；净水还用于抗辐射饮料 |
| `BASIC_DENSIFIER` | 保留，加工扩展待定 | 7种流体处理分支，提供冰、煤、冻结氮及食品等产出 |
| `BASIC_DECONTAMINATION_UNIT` | 保留辐射处理用途 | 处理EP玩家已有辐射累积；与多方块占位体共同保留 |
| `VEHICLE_CHARGER` | 随漫游车保留 | 当前漫游车取舍未定；关联漫游车、电力部件及充能平台 |

结晶器、逆结晶器、粉碎机、化学注入器、太阳能蒸发室、净化器及辐射消除装置受 `RADIATION` 与各机器开关共同控制。保留这些设备时维持注册依赖，辐射效果单独适配。浓缩机使用独立开关。

### 平台与多方块

| 对象 | 子项或关联 | 取舍 |
| --- | --- | --- |
| `ADVANCED_LAUCHPAD` | meta0二级平台、1三级平台、2电力充能平台、3火箭充能平台 | 随载具接口分别处理 |
| `ADVANCED_LAUCHPAD_FULL`、`FAKE_BLOCK` | 平台完整体与占位体 | 随平台功能保留 |
| 辐射装置占位体 | `FAKE_BLOCK_DECONTAMINATION_UNIT` | 随辐射消除装置保留 |

## 世界内容

### 地牢结构

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `TIER_4_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_5_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_6_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_7_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_8_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_9_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_10_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |

保留地牢钥匙、宝箱、Boss及刷怪方块。图纸奖励在具体地牢或Boss入口替换。

### 地形与生态

保留天体使用的地表、砂砾、矿石、冰、火山岩、化石、建筑材料与生态方块保留。EP火山岩维持熔炼火山锭的加工关系。关闭的重复卫星按注册与配方依赖检查关联方块。

### 爆炸物

| 对象 | 控制项 | 取舍 |
| --- | --- | --- |
| `NUCLEAR_BOMB` | `Nuclear Bomb=false` | 移除方块、物品、实体与配方 |
| `FIRE_BOMB` | `Fire Bomb=false` | 移除方块、物品、实体与配方 |

## 开发工具

| 源码字段 | 取舍 | 用途与依赖 |
| --- | --- | --- |
| `DEBUG_WORLD_FILE_GEN_ITEM` | 移除 | 原注册行已注释；固定裁剪策略继续拦截debugworldfilegen |

## 代码入口

- [环境防护适配](../src/main/java/newearth/EnvironmentHooks.java)与[EP环境拦截](../src/main/java/newearth/EnvironmentTransformer.java)
- [固定裁剪策略](../src/main/java/newearth/ContentPruning.java)与[注册拦截](../src/main/java/newearth/ContentTransformer.java)
- [EP物品定义](../../../dependencies/ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ExtraPlanets_Items.java)、[工具](../../../dependencies/ExtraPlanets/src/main/java/com/mjr/extraplanets/items/tools/ExtraPlanets_Tools.java)、[护甲](../../../dependencies/ExtraPlanets/src/main/java/com/mjr/extraplanets/items/armor/ExtraPlanets_Armor.java)
- [机器](../../../dependencies/ExtraPlanets/src/main/java/com/mjr/extraplanets/blocks/machines/ExtraPlanets_Machines.java)、[流体](../../../dependencies/ExtraPlanets/src/main/java/com/mjr/extraplanets/blocks/fluid/ExtraPlanets_Fluids.java)、[配方](../../../dependencies/ExtraPlanets/src/main/resources/assets/extraplanets/recipes)
