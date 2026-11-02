# EP 物品取舍与裁剪清单

更新：2026-11-03。关联 [Issue #8](https://github.com/Neosamzzz/Minecraft/issues/8)。本清单供后续裁剪实现和验收持续维护，路线见[路线手稿](route-manuscript.md)，原料与机器配方见[星球指南](../../docs/research/planet-gameplay-guide.md)。

## 范围与状态

首轮已清点 EP 三个物品定义入口的全部 **328 个静态物品字段**：杂项与航天156、工具武器80、护甲92。数字是源码字段数，不是当前实例已注册数量，也不是含元数据后的 JEI 项数。方块物品、机器、流体桶和运行时动态注册不计入这328项；后续作为独立范围继续筛选。

本轮完成清点与取舍建议，尚未修改游戏注册、配置、配方、掉落或 JEI。下表“剔除候选”依据不用 EP 大部分装备的方向列出，不代表每一件已被用户逐项确认。待定项不擅自删除，特色原料不随装备一起删除。

实施需分别处理配方、非合成获取和 JEI。现有存档物品的处置另核，不默认扫描删除玩家物品或取消全部注册。开启保留星球不应自动恢复不采用的整套装备。

## 第一批：工具、武器、护甲

工具入口共有16套材质，每套镐、斧、锄、锹、剑5件，共80件；普通护甲同样16套，每套头盔、胸甲、护腿、靴4件，共64件。以下均为剔除候选；材料本身另行保留／统一。

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

其余28件护甲为零级宇航服4件，以及一至四级每级6件（普通四件套、喷气胸甲、重力靴）。均列为剔除候选，由 GS 宇航服和改装体系承接；其压力／辐射接口及装备依赖必须随环境适配处理，不能直接关闭环境系统来替代取舍。

配置依据：[Config.java](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/Config.java)；工具护甲条件分别用于初始化及配方条件。原料不由这些工具护甲开关一并删除。关闭开关后的注册、配方和引用仍需启动验证。

## 第二批：156个杂项与航天字段

以源码字段定位，涉及元数据的项目必须继续拆分。状态为本轮建议，实际裁剪尚未执行。

| 源码字段 | 建议处理 | 理由／实施边界 |
| --- | --- | --- |
| `TIER_4_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_5_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_6_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_7_ROCKET` | 保留／适配 | 七阶火箭本体复用；鼻锥是否沿用待配件设计 |
| `TIER_8_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_9_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_10_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_10_ELECTRIC_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_4_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_5_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_6_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_7_SCHEMATIC` | 保留／适配 | 七阶解锁复用候选，产出改为冥王星 |
| `TIER_8_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_9_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_10_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_10_ELECTRIC_ROCKET_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_4_NOSE_CONE` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_5_NOSE_CONE` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_6_NOSE_CONE` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_7_NOSE_CONE` | 保留／适配 | 七阶火箭本体复用；鼻锥是否沿用待配件设计 |
| `TIER_8_NOSE_CONE` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_9_NOSE_CONE` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_10_NOSE_CONE` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_4_ITEMS` | 按元数据拆分 | 原料与旧发动机／助推器／尾翼／强化板混装，不可整ID删除 |
| `TIER_5_ITEMS` | 按元数据拆分 | 原料与旧发动机／助推器／尾翼／强化板混装，不可整ID删除 |
| `TIER_6_ITEMS` | 按元数据拆分 | 原料与旧发动机／助推器／尾翼／强化板混装，不可整ID删除 |
| `TIER_7_ITEMS` | 按元数据拆分 | 原料与旧发动机／助推器／尾翼／强化板混装，不可整ID删除 |
| `TIER_8_ITEMS` | 按元数据拆分 | 原料与旧发动机／助推器／尾翼／强化板混装，不可整ID删除 |
| `TIER_9_ITEMS` | 按元数据拆分 | 原料与旧发动机／助推器／尾翼／强化板混装，不可整ID删除 |
| `TIER_10_ITEMS` | 按元数据拆分 | 原料与旧发动机／助推器／尾翼／强化板混装，不可整ID删除 |
| `TIER_11_ITEMS` | 按元数据拆分 | 原料与旧发动机／助推器／尾翼／强化板混装，不可整ID删除 |
| `TIER_4_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_5_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_6_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_7_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_8_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_9_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `TIER_10_KEY` | 保留 | 地牢钥匙；等级与火箭进度不同，奖励替换后仍须开箱 |
| `BATTERY_NICKEL` | 待定：按容量取舍 | 容量85000，电力等级2；可供兼容机器用，先核对GS电池与仍保留设备引用 |
| `BATTERY_ZINC` | 待定：按容量取舍 | 容量125000，电力等级2；可供兼容机器用，先核对GS电池与仍保留设备引用 |
| `BATTERY_MERCURY` | 待定：按容量取舍 | 容量100000，电力等级2；可供兼容机器用，先核对GS电池与仍保留设备引用 |
| `BATTERY_ADVANCED` | 待定：按容量取舍 | 容量150000，电力等级2；可供兼容机器用，先核对GS电池与仍保留设备引用 |
| `BATTERY_ULTIMATE` | 待定：按容量取舍 | 容量200000，电力等级2；可供兼容机器用，先核对GS电池与仍保留设备引用 |
| `BATTERY_MASSIVE` | 待定：按容量取舍 | 容量500000，电力等级2；可供兼容机器用，先核对GS电池与仍保留设备引用 |
| `OXYGEN_TANK_VERY_HEAVY` | 待定 | 超重两档氧气罐，与普通彩色罐分开取舍 |
| `OXYGEN_TANK_EXTREMELY_HEAVY` | 待定 | 超重两档氧气罐，与普通彩色罐分开取舍 |
| `CANNED_FOOD` | 可选保留 | 4种脱水肉食罐头，普通食物功能，食用掉落GC空罐；不参与主线解锁 |
| `DIAMOND_APPLE` | 剔除候选 | 普通与附魔两个子项，提供多种增益；非航天必需品，属额外战斗补给 |
| `IRON_APPLE` | 剔除候选 | 普通与附魔两个子项，提供多种增益；非航天必需品 |
| `COMPRESSED_MERCURY` | 保留／拆分用途 | 特色加工原料或多子项容器；不是整组装备废料，按具体配方决定用途 |
| `INGOT_URANIUM` | 核对统一 | 检查已有模组同类资源及加工，不凭同名直接互换 |
| `INGOT_MERCURY` | 保留／拆分用途 | 特色加工原料或多子项容器；不是整组装备废料，按具体配方决定用途 |
| `INGOT_LEAD` | 核对统一 | 检查已有模组同类资源及加工，不凭同名直接互换 |
| `TOOLS` | 保留／拆分用途 | 特色加工原料或多子项容器；不是整组装备废料，按具体配方决定用途 |
| `CLOTH` | 剔除候选 | EP普通布，区别于温控布；已查用途集中在宇航服压力层链，保留原料与否另定 |
| `GRAVITY_CONTROLLER` | 剔除候选 | 原配方用途为EP重力靴升级；GS有独立重力改装，不是同一接口 |
| `TIER_1_PRESSURE_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_2_PRESSURE_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_3_PRESSURE_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_4_PRESSURE_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_1_RADIATION_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_2_RADIATION_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_3_RADIATION_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_4_RADIATION_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_1_ARMOR_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_2_ARMOR_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_3_ARMOR_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_4_ARMOR_LAYER` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `THERMAL_CLOTH` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_3_THERMAL_PADDING` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_4_THERMAL_PADDING` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_5_THERMAL_PADDING` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_1_UNPREPARED_SPACE_SUIT_HELMET` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_1_UNPREPARED_SPACE_SUIT_CHEST` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_1_UNPREPARED_SPACE_SUIT_LEGINGS` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_1_UNPREPARED_SPACE_SUIT_BOOTS` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_2_UNPREPARED_SPACE_SUIT_HELMET` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_2_UNPREPARED_SPACE_SUIT_CHEST` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_2_UNPREPARED_SPACE_SUIT_LEGINGS` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_2_UNPREPARED_SPACE_SUIT_BOOTS` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_3_UNPREPARED_SPACE_SUIT_HELMET` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_3_UNPREPARED_SPACE_SUIT_CHEST` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_3_UNPREPARED_SPACE_SUIT_LEGINGS` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_3_UNPREPARED_SPACE_SUIT_BOOTS` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_4_UNPREPARED_SPACE_SUIT_HELMET` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_4_UNPREPARED_SPACE_SUIT_CHEST` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_4_UNPREPARED_SPACE_SUIT_LEGINGS` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `TIER_4_UNPREPARED_SPACE_SUIT_BOOTS` | 剔除候选 | 随EP装备路线清理，GS防护适配完成前不直接移除必需防护 |
| `ENVIRO_EMERGENCY_KIT` | 剔除候选 | 直接给予EP零级宇航服四件套；另有gckit指令事件入口，不能仅删配方 |
| `WAFERS` | 保留／拆分用途 | 特色加工原料或多子项容器；不是整组装备废料，按具体配方决定用途 |
| `IODIDE_SALT` | 保留／拆分用途 | 特色加工原料或多子项容器；不是整组装备废料，按具体配方决定用途 |
| `POTASSIUM_IODIDE` | 保留／拆分用途 | 特色加工原料或多子项容器；不是整组装备废料，按具体配方决定用途 |
| `POTASSIUM` | 保留／拆分用途 | 特色加工原料或多子项容器；不是整组装备废料，按具体配方决定用途 |
| `POTASH_SHARDS` | 保留／拆分用途 | 特色加工原料或多子项容器；不是整组装备废料，按具体配方决定用途 |
| `ANTI_RADIATION` | 环境决定 | 有实际JSON配方与饮用逻辑；只改变EP辐射能力值，不能处理GS辐射 |
| `MARS_ROVER` | 可选保留 | 独立地面载具，NASA图纸与电力部件／充能器需成套取舍 |
| `VENUS_ROVER` | 可选保留 | 独立地面载具，NASA图纸与电力部件／充能器需成套取舍 |
| `MARS_ROVER_SCHEMATIC` | 随载具取舍 | 原加入GC二级特殊池；删除载具时同步移除图纸产出 |
| `VENUS_ROVER_SCHEMATIC` | 随载具取舍 | 原加入GC三级特殊池；删除载具时同步移除图纸产出 |
| `ELECTRIC_PARTS` | 随载具取舍 | 4子项：两级电池部件、两级电动车轮，不等同于6种手持电池 |
| `TIER_1_EQUIPMENT_KIT` | 剔除／改造候选 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |
| `TIER_2_EQUIPMENT_KIT` | 剔除／改造候选 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |
| `TIER_4_EQUIPMENT_KIT` | 剔除／改造候选 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |
| `TIER_3_EQUIPMENT_KIT` | 剔除／改造候选 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |
| `TIER_5_EQUIPMENT_KIT` | 剔除／改造候选 | 6个元数据版本，直接填充GC扩展装备槽；3级起涉及EP温控，4／5级涉及超重氧气罐 |
| `GEIGER_COUNTER` | 环境决定 | 读取EP辐射累积值，EP水星特殊池也产出；保留EP辐射时有用，不是GS通用检测器 |
| `DEBUG_WORLD_FILE_GEN_ITEM` | 退出生存／隐藏候选 | 调试用途，与正常制造无关 |
| `WHITE_SUGAR_CANE` | 可选保留 | 实际为绑定方块的种植物品，随开普勒生态统一取舍，不和装备一起删 |
| `CHOCOLATE_BAR` | 可选保留 | 普通食品，饥饿值1／饱和参数0.3；随生态食品取舍 |
| `CARAMEL_BAR` | 可选保留 | 普通食品，饥饿值1／饱和参数0.25；随生态食品取舍 |
| `LIGHT_OXYGEN_TANK_BLACK` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_RED` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_GREEN` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_BROWN` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_BLUE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_PURPLE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_CYAN` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_LIGHT_GRAY` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_GRAY` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_PINK` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_YELLOW` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_LIGHT_BLUE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_MAGENTA` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_ORANGE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `LIGHT_OXYGEN_TANK_WHITE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_BLACK` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_RED` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_GREEN` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_BROWN` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_BLUE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_PURPLE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_CYAN` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_LIGHT_GRAY` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_GRAY` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_PINK` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_YELLOW` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_LIGHT_BLUE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_MAGENTA` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_LIME` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MED_OXYGEN_TANK_WHITE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_BLACK` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_LIME` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_GREEN` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_BROWN` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_BLUE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_PURPLE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_CYAN` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_LIGHT_GRAY` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_GRAY` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_PINK` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_YELLOW` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_LIGHT_BLUE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_MAGENTA` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_ORANGE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `HEAVY_OXYGEN_TANK_WHITE` | 剔除候选 | 45种彩色普通氧气罐，优先统一GC／GS；不受自定义超重氧气罐开关控制 |
| `MODULE_ITEMS` | 剔除候选 | 0免摔靴、1氧气控制器、2太空装备控制器；服务EP专属模块体系，不能直接装GS |

## 杂项用途复核（第二轮，2026-11-03）

本轮细化套件、模块、电池、氧气罐、食品和载具的取舍依据，不增加上述328字段总数。以下是建议，不是已执行的删除清单。

### 套件：直接产出装备的入口

`ItemBasicKit` 共5档，每档6个子项，共30种套件。右键后填充空的 GC 扩展装备槽，不是只显示一张物资清单；不自动给玩家穿 EP 普通宇航服。六个子项为完整、基础、无供氧、无温控、仅供氧、仅防护组合，逐项提供降落伞、频率模块、温控、防护控制器和供氧装备的不同子集。

| 套件档位 | 温控来源 | 氧气罐来源 |
| --- | --- | --- |
| 1 | GC 一级温控 | GC 轻型罐 |
| 2 | GC 金星二级温控 | GC 中型罐 |
| 3 | EP 三级温控 | GC 重型罐 |
| 4 | EP 四级温控 | EP Very Heavy罐 |
| 5 | EP 五级温控 | EP Extremely Heavy罐 |

二级及以上的相关套件还会放入金星防护控制器。普通套件本身不等于零级宇航服；`ItemEnviroEmergencyKit` 才是给予／自动穿戴 EP 零级宇航服四件套，已有装备时放进背包。EP 的 `gckit` 指令事件也会给予紧急套件；指令入口单列，不能当成普通生存漏洞，也不能声称删合成后所有入口都消失。

### 模块：和 GS 改装体系分开

EP 实际注册9种模块：探测目镜、免摔、盖革计数、发射清单、便携NASA、氧气罐切换、自动装备太空装备、基础／高级太阳能。`TestModule` 没有启用。`module_items` 只有3个制作素材子项（免摔靴、氧气控制器、太空装备控制器），9种模块不等于9件独立注册物品。

`ModuleHelper` 要求 `IModularArmor`；当前GS源码没有实现该接口。太阳能模块还直接判断EP `ElectricArmorBase`。因此不能把这些功能列成GS改装台可直接安装的模块。EP宇航服不采用时，可把专用素材和模块入口列入裁剪；确实想保留的功能需要另行兼容，而非只放宽配方。

### 氧气罐容量与重复性

| 类别 | 数量 | 代码容量／作用 |
| --- | --- | --- |
| EP彩色轻／中／重罐 | 各15种，共45字段 | 900／1800／2700，主要是同容量颜色变体；右键填入空供氧槽 |
| EP Very Heavy／Extremely Heavy | 2字段 | 7200／9000，确有容量提升，保留与否按补给平衡决定 |
| GS四／五／六级罐 | 对照 | 源码默认3500／4000／4500，可由配置改变；EPP默认2500，另有环境防护用途 |

容量是内部储量，不换算未经实测的持续时间。彩色罐属于优先精简候选，超重两档不是单纯换色。GS高级电池原容量60000，低于EP六档；若选择只留GS外观，也需要决定是否提高容量或保留后期电池升级，不能说所有电池功能等同。

### 电池、检测与食品

- 六种EP手持电池容量为85000／100000／125000／150000／200000／500000，电力等级均为2。它们有实际储电用途，不属于无功能杂物；是否保留全部档位或用GS替代，要看仍保留设备的配方。电动车辆部件内另有两种电池素材，不要混计。
- 盖革计数器读取EP自己的辐射能力值；抗辐射饮料也只处理EP能力值。饮料有实际 `anti_rad.json`，不能因配方生成Java里对应段落被注释就认定它造不出来。是否采用它们随最终环境系统决定。
- 抗辐射饮料配方使用碘化钾4、EP压缩镁2、净水桶2、玻璃瓶1；镁统一时这里也需替换。饮用代码设置剩余辐射为“原值×配置百分比”，提示文字却引用另一配置项，不能直接照说明承诺任意配置下的减少百分比。
- 罐头4种为猪肉、鱼、鲑鱼、鸡肉；实际可食并掉落GC空罐。铁／钻石苹果各普通与附魔两种，提供药水增益；是可裁的额外战斗补给，不是制造前置。
- 白甘蔗物品绑定生态方块，巧克力／焦糖为普通食物；和开普勒生态一起取舍，不把它们误算成装备材料。
- 火星／金星漫游车、对应图纸、4种电力部件以及充能器／平台作为一组待定。原图纸分别进入GC二级／三级特殊池，若裁载具需要同步清理图纸，不能只隐藏成品。

依据：[套件](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ItemBasicKit.java)、[紧急套件](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ItemEnviroEmergencyKit.java)、[模块注册及接口](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/armor/modules/)、[物品初始化](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ExtraPlanets_Items.java)、[抗辐射配方](../../ExtraPlanets/src/main/resources/assets/extraplanets/recipes/anti_rad.json)、[饮用逻辑](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ItemBasicItem.java)。

## 元数据拆分：火箭材料容器

以下为物品类 `getItemList()` 中的实际子项；同一个ID里的旧火箭部件和独立矿物不能一起清理。

| 容器 | 元数据与子项 | 处理边界 |
| --- | --- | --- |
| `tier4_items` | 0: `tier4engine`；1: `tier4booster`；2: `tier4fin`；3: `tier4heavy_duty_plate`；4: `compressed_carbon`；5: `ingot_carbon`；6: `diamond_shard` | 0～3为原火箭部件／强化板；其余原料逐项保留、统一或裁剪 |
| `tier5_items` | 0: `tier5engine`；1: `tier5booster`；2: `tier5fin`；3: `tier5heavy_duty_plate`；4: `compressed_palladium`；5: `ingot_palladium`；6: `compressed_nickel`；7: `ingot_nickel`；8: `red_gem`；9: `ash`；10: `ash_shard`；11: `volcanic_shard`；12: `ingot_volcanic` | 0～3为原火箭部件／强化板；其余原料逐项保留、统一或裁剪 |
| `tier6_items` | 0: `tier6engine`；1: `tier6booster`；2: `tier6fin`；3: `tier6heavy_duty_plate`；4: `compressed_magnesium`；5: `ingot_magnesium` | 0～3为原火箭部件／强化板；其余原料逐项保留、统一或裁剪 |
| `tier7_items` | 0: `tier7engine`；1: `tier7booster`；2: `tier7fin`；3: `tier7heavy_duty_plate`；4: `compressed_crystal`；5: `ingot_crystal`；6: `compressed_reinforced_crystal`；7: `white_gem` | 0～3为原火箭部件／强化板；其余原料逐项保留、统一或裁剪 |
| `tier8_items` | 0: `tier8engine`；1: `tier8booster`；2: `tier8fin`；3: `tier8heavy_duty_plate`；4: `compressed_zinc`；5: `ingot_zinc`；6: `blue_gem` | 0～3为原火箭部件／强化板；其余原料逐项保留、统一或裁剪 |
| `tier9_items` | 0: `tier9engine`；1: `tier9booster`；2: `tier9fin`；3: `tier9heavy_duty_plate`；4: `compressed_tungsten`；5: `ingot_tungsten` | 0～3为原火箭部件／强化板；其余原料逐项保留、统一或裁剪 |
| `tier10_items` | 0: `tier10engine`；1: `tier10booster`；2: `tier10fin`；3: `tier10heavy_duty_plate`；4: `compressed_dark_iron`；5: `ingot_dark_iron` | 0～3为原火箭部件／强化板；其余原料逐项保留、统一或裁剪 |
| `tier11_items` | 0: `blue_diamond`；1: `red_diamond`；2: `purple_diamond`；3: `yellow_diamond`；4: `green_diamond`；5: `ingot_platinum`；6: `compressed_platinum`；7: `blue_sticks`；8: `red_sticks`；9: `purple_sticks`；10: `yellow_sticks`；11: `green_sticks`；12: `brown_sticks` | 0～6五色钻石与铂材料保留；7～12木棍子项随生态／配方取舍 |

## 获取入口与实施顺序

1. 工具护甲先按材质开关与配方条件实施，但不能连带关闭所保留星球。防护装备等 GS 兼容完成后再清理。
2. 子项裁剪按 item＋meta 处理；JEI 隐藏只是展示，仍需移除制造、地牢／结构战利品及其他生存来源。
3. 源码地牢普通表目前有 Mercury、Jupiter 两份 JSON；Boss 特殊奖励另由注册代码处理，不能只改 loot_tables。装备套件、紧急套件也可能重新产出已裁物品，需要逐项检查。
4. 氧气罐配置 `OXYGEN_TANKS` 仅覆盖 Very Heavy／Extremely Heavy 两档；45种彩色罐在初始化中单独创建，须另处理。关闭电池开关同样会影响仍需电池的设备配方，暂不执行。
5. 新七阶尚未确定的配件和九／十阶素材先保留引用，不能为了清列表破坏当前0.1.0启动；旧获取入口在新路线接入时替换。
6. 本轮未清点方块物品／机器／桶的完整注册表，未验证全部结构、套件、交易和运行时掉落。下一轮针对确定剔除项追踪这些入口，再执行裁剪与启动验证。

## 来源与验收

- [主物品定义](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ExtraPlanets_Items.java)
- [工具武器定义](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/tools/ExtraPlanets_Tools.java)
- [护甲定义](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/armor/ExtraPlanets_Armor.java)
- [配方条件](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/recipes/RecipeEnabledCondition.java)
- [机器与其他原配方](../../ExtraPlanets/src/main/resources/assets/extraplanets/recipes/)

已用声明字段交叉核对16套工具与16套普通护甲数量，逐项列出156个主物品字段，并拆出8组火箭材料容器。未将源码存在写成当前已加载；尚无裁剪后游戏验收。
