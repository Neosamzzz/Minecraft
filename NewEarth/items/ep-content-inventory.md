# EP 物品取舍与裁剪清单

更新：2026-11-03。关联 [Issue #8](https://github.com/Neosamzzz/Minecraft/issues/8)。本清单供后续裁剪实现和验收持续维护，路线见[路线手稿](route-manuscript.md)，原料与机器配方见[星球指南](../../docs/research/planet-gameplay-guide.md)。

## 范围与状态

首轮已清点 EP 三个物品定义入口的全部 **328 个静态物品字段**：杂项与航天156、工具武器80、护甲92。数字是源码字段数，不是当前实例已注册数量，也不是含元数据后的 JEI 项数。方块物品、机器、流体桶和运行时动态注册不计入这328项；后续作为独立范围继续筛选。

本清单先记录清点与取舍建议，已实施范围见下方“已落地的首批裁剪”；其余项目仍未修改游戏注册、配方、掉落或 JEI。下表“剔除候选”依据不用 EP 大部分装备的方向列出，不代表每一件已被用户逐项确认。待定项不擅自删除，特色原料不随装备一起删除。

实施需分别处理配方、非合成获取和 JEI。现有存档物品的处置另核，不默认扫描删除玩家物品或取消全部注册。开启保留星球不应自动恢复不采用的整套装备。

**已落地的首批裁剪（2026-11-03）：** 仓库 `config/ExtraPlanets.cfg` 已关闭11个普通工具护甲材质开关，覆盖汞、碳、铀、钯、镁、水晶、钨、红／蓝／白宝石、锌共99件（55工具武器＋44普通护甲）。初始化、注册及99个对应配方条件已核对；未改星球、原料、机器或环境开关，未部署到游戏实例，未完成启动验收。五色钻石45件的配置注册开关继续保留：源码注册受 `ITEMS_KEPLER22B` 控制，但五种钻石工具配方条件直接返回 `KEPLER22B`；0.2.7开发版改为移除其工作台制造配方，不关闭原注册，尚未部署。非合成入口审查交由其他实现方，本文不宣称已排除全部入口。

**原 EP 火箭链取舍更新：** 各阶原火箭（含七阶及电力十阶）、专用部件、专用板材、旧火箭图纸及制造路线统一退出，后续根据新火箭配件设计选择性取回。模型、贴图等可作为素材复用；矿物和通用材料按用途保留，地牢钥匙与宝箱继续承担结构功能。0.2.7开发版已移除原火箭NASA制造、专用部件／鼻锥的工作台配方、强化板等专用子项的压缩配方，以及此前七阶过渡配方；未部署实例。物品注册、JEI物品显示与旧图纸非合成获取仍保留，后两项尚未收尾。

**取舍定稿原则（2026-11-03）：** 保留的材料必须有实际用途，尤其是后段星球的特色材料；删除旧装备、部件后，逐项检查剩余用途，缺失的明确列为待补加工／制造／升级用途。用途可分布于七阶、九阶和十阶，不要求全部挤入七阶，但必须符合原料可达阶段。机器只要仍有加工或补给等实际用途就保留，不因与 GC／GS 功能相近、不是主线必需而裁掉；用途的具体改造可以待定，保留原则不再悬置。原 EP 专用板材和部件是否采用，仍按新路线判断，不因这条原则而全部保留。

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
| `TIER_7_ROCKET` | 退出旧路线，按新设计选回 | 原七阶同样先退出，是否复用本体或模型另选 |
| `TIER_8_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_9_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_10_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_10_ELECTRIC_ROCKET` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_4_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_5_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_6_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_7_SCHEMATIC` | 退出旧路线，按新设计选回 | 新七阶图纸由冥王星产出；具体是否复用此物品另选 |
| `TIER_8_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_9_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_10_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_10_ELECTRIC_ROCKET_SCHEMATIC` | 替换／待定 | 原火箭图纸退出旧奖励路线；漫游车图纸随载具取舍，不提前删除 |
| `TIER_4_NOSE_CONE` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_5_NOSE_CONE` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_6_NOSE_CONE` | 剔除旧获取 | 保留模型素材不等于保留原火箭生存路线；九十阶由新设计承担 |
| `TIER_7_NOSE_CONE` | 退出旧路线，按新设计选回 | 不预先保留原鼻锥，按新配件设计决定 |
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

## 第三轮：机器、平台与流体（2026-11-03）

本轮补齐 `ExtraPlanets_Machines` 的16个方块字段、其19种机器子项，以及 `ExtraPlanets_Fluids` 的13种流体及通用桶入口；另筛查平台、爆炸物和地形／装饰方块。普通方块的全部元数据、结构战利品和运行时注册数量仍未穷尽，不把本节计入前述328个静态物品字段。

### 机器取舍

16个字段中，两个氧气设备字段各分压缩／解压两项，太阳能字段分混合／终极两项，合计19种。以下是裁剪建议，不是已实施或逐项定稿。

| 字段／设备 | 建议 | 实际用途与依赖 |
| --- | --- | --- |
| `REFINERY_ADVANCED`、`REFINERY_ULTIMATE` | 保留 | GC 精炼功能的升级设备；原制造配方见指南，有补给用途即保留 |
| `OXYGEN_COMPRESSOR_ADVANCED`、`OXYGEN_COMPRESSOR_ULTIMATE` | 保留 | 每个字段含 meta0压缩、meta1解压，承担基地供氧补给 |
| `SOLAR_PANEL` | 保留 | meta0混合、meta1终极；承担供电，原配方使用镁／水晶 |
| `FUEL_LOADER_ADVANCED`、`FUEL_LOADER_ULTIMATE` | 保留 | 承担燃料装载；新载具接口适配另行确定 |
| `BASIC_SMASHER` | 保留，加工扩展待定 | 原注册配方：钾盐矿→3碎片；有输入→输出配方表及增删接口，不代表支持多原料合金 |
| `BASIC_SOLAR_EVAPORTATION_CHAMBER` | 保留，加工扩展待定 | 原注册配方：12钾盐碎片→1钾；同样有独立配方表 |
| `BASIC_CRYSALLIZER` | 保留材料加工用途 | `salt_fluid`→碘盐；原处理在 TileEntity 内写定，不是通用配方表 |
| `BASIC_DECRYSALLIZER` | 保留材料加工用途 | 6碘盐→`salt_fluid`；字段拼写保留源码原样 |
| `BASIC_CHEMICAL_INJECTOR` | 保留材料加工用途 | 输入固定检查碘盐和钾，产出碘化钾；改成新七阶材料加工需改实际机器处理逻辑 |
| `BASIC_PURIFIER` | 保留净水用途 | 消耗碘盐，放射性水→污染水→净水两段处理；净水还用于抗辐射饮料 |
| `BASIC_DENSIFIER` | 保留，加工扩展待定 | 7种流体处理分支，提供冰、煤、冻结氮及食品等产出；详见下表 |
| `BASIC_DECONTAMINATION_UNIT` | 有辐射处理用途即保留，适配方式待定 | 服务 EP 玩家辐射能力；若环境适配使其失去作用，先明确承接用途再决定，连同多方块占位体处理 |
| `VEHICLE_CHARGER` | 有载具充能用途即保留 | 当前漫游车取舍未定；和漫游车、电力部件及充能平台一起核对，不因非主线机器而剔除 |

**开关绑定：** 结晶器、逆结晶器、粉碎机、化学注入器、太阳能蒸发室、净化器、辐射消除装置共7个机器字段，初始化和注册均嵌套在 `Config.RADIATION` 下，再检查各自开关。不能为删除 EP 辐射装备直接关闭辐射总开关，同时又要求继续使用这些机器。浓缩机不在这个嵌套分支。复用设备时应由实现方拆开依赖或承接处理功能。

### 13种流体与桶

`ExtraPlanets_Fluids.initFluid()` 对以下13种流体逐一注册并调用 `FluidRegistry.addBucketForFluid`；这是 Forge 通用桶的不同流体内容，不是13个独立 EP 桶物品字段。`ItemBasicItemBucket` 类存在不足以证明它是当前入口；主物品注册中的旧冻结水桶注册行已注释。JEI 隐藏、配方替换需识别桶内流体，不能隐藏整个通用桶物品。

| 流体注册名 | 已查到的处理关系 | 建议 |
| --- | --- | --- |
| `glowstone_fluid` | 浓缩机输出荧石块 | 保留流体／地形，处理用途可复用 |
| `magma_fluid` | 本轮未发现浓缩机处理分支 | 随保留地形保留，不直接等同熔岩或火箭燃料 |
| `nitrogen_fluid` | 浓缩机输出冻结氮 | 保留；不能仅凭名字替换 GS 氮气 |
| `frozen_water_fluid` | 浓缩机输出冰 | 保留，作为原冰类资源加工候选 |
| `salt_fluid` | 结晶器／逆结晶器与碘盐互转 | 随化学链待定，不按名称当作普通水 |
| `radioactive_water_fluid` | 净化器第一段输入 | 随地形和环境适配保留引用 |
| `clean_water_fluid` | 净化器最终输出；抗辐射饮料配方输入 | 随净水及饮料取舍，不等于原版水桶 |
| `infected_water_fluid` | 净化器中间产物及第二段输入 | 与净化链一起决定 |
| `methane_fluid` | 本轮未发现浓缩机处理分支 | 随地形保留，不直接视为 GS 甲烷或可加注燃料 |
| `nitrogen_ice_fluid` | 浓缩机输出冰，与 `nitrogen_fluid` 输出不同 | 保留并分清两种流体 |
| `liquid_hydrocarbon_fluid` | EP 土星地形用液体；浓缩机输出煤 | 保留；原实现不是直接产出火箭燃油 |
| `liquid_chocolate_fluid` | 浓缩机输出巧克力条 | 随开普勒食品生态可选保留 |
| `liquid_caramel_fluid` | 浓缩机输出焦糖条 | 随开普勒食品生态可选保留 |

上述为源码分支关系，不是实机处理验收；未把代码中声明的输出数量直接当作每桶可靠产率。浓缩机和净化器的容器、管道、重进保存仍需在决定复用后验证。

### 平台、杂项方块与地形边界

| 内容 | 建议 | 处理边界 |
| --- | --- | --- |
| `ADVANCED_LAUCHPAD` | 按载具需要拆分，暂不整项删除 | meta0二级平台、1三级平台、2电力充能平台、3火箭充能平台；先核对七阶及未来载具使用哪项 |
| `ADVANCED_LAUCHPAD_FULL`、`FAKE_BLOCK`及辐射装置占位体 | 作为关联实现处理 | 多方块完整体／占位不是额外玩家工作台，不单独设计生存配方 |
| `NUCLEAR_BOMB`、`FIRE_BOMB` | 剔除生存获取候选 | 不承担已定航天制造与图纸主线；保留谷神星／土星不等于要求保留这两种物品，配方与其他获取入口仍需追踪 |
| 保留星球的地表、石头、砂砾、冰、火山岩、矿石及化石 | 保留世界生成引用，矿物逐项统一 | 尤其 EP 火山岩原有冶炼火山锭关系；删除装备不应误删资源方块 |
| 不再启用的 EP 重复卫星地形方块 | 随天体裁剪检查注册与配方引用 | 不默认删除旧存档方块，也不扩展裁剪到未列出的卫星 |
| 石砖、楼梯、装饰块、开普勒木材／植物与糖果方块 | 非优先裁剪项，随地形生态保留 | 它们不属于“乱七八糟的装备”；无主线制造用途不足以证明应该删除 |
| 各级地牢宝箱与 Boss 刷怪方块 | 保留结构功能，改奖励 | 图纸替换不是取消宝箱／刷怪方块注册；保持钥匙与地牢工作 |

## 获取入口与实施顺序

1. 工具护甲先按材质开关与配方条件实施，但不能连带关闭所保留星球。防护装备等 GS 兼容完成后再清理。
2. 子项裁剪按 item＋meta 处理；JEI 隐藏只是展示，仍需移除制造、地牢／结构战利品及其他生存来源。
3. 源码地牢普通表目前有 Mercury、Jupiter 两份 JSON；Boss 特殊奖励另由注册代码处理，不能只改 loot_tables。装备套件、紧急套件也可能重新产出已裁物品，需要逐项检查。
4. 氧气罐配置 `OXYGEN_TANKS` 仅覆盖 Very Heavy／Extremely Heavy 两档；45种彩色罐在初始化中单独创建，须另处理。关闭电池开关同样会影响仍需电池的设备配方，暂不执行。
5. 原EP火箭专用子项先退出制造，按新配件设计再选择性取回；0.2.7已撤掉0.1.0过渡制造，保留物品注册和原料引用保证后续复用。七阶新路线未接入，不能将此开发版作为完整生存进度交付。
6. 机器16个字段及13种流体桶入口已补查；普通方块全部元数据、结构、交易和运行时掉落仍未穷尽。下一轮针对剔除候选追踪非合成获取入口，形成实现清单；裁剪与启动验证由实现方执行。

## 来源与验收

- [主物品定义](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ExtraPlanets_Items.java)
- [工具武器定义](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/tools/ExtraPlanets_Tools.java)
- [护甲定义](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/armor/ExtraPlanets_Armor.java)
- [配方条件](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/recipes/RecipeEnabledCondition.java)
- [机器与其他原配方](../../ExtraPlanets/src/main/resources/assets/extraplanets/recipes/)
- [机器注册与开关依赖](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/blocks/machines/ExtraPlanets_Machines.java)
- [机器配方表](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/recipes/ExtraPlanets_MachineRecipes.java)与[具体处理逻辑](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/tileEntities/machines/)
- [流体与通用桶注册](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/blocks/fluid/ExtraPlanets_Fluids.java)
- [普通方块注册](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/blocks/ExtraPlanets_Blocks.java)

已用声明字段交叉核对16套工具与16套普通护甲数量，逐项列出156个主物品字段，并拆出8组火箭材料容器。未将源码存在写成当前已加载；尚无裁剪后游戏验收。
