# 本机整合包星球与玩法现状

对应 [Issue #4：查清楚现有星球情况](https://github.com/Neosamzzz/Minecraft/issues/4)。

逐个目的地的资源、探索理由、准备、加工关系、地牢内容和 FTB 使用边界，见[星球玩法详解与 FTB 内容依据](planet-gameplay-guide.md)。本页保留配置、注册、共存和进度问题总览。

调查日期：2026-11-01。范围是本机当前整合包的星球、到达条件和玩法内容；暂不修改游戏配置或设计 FTB 任务线。

## 调查依据与验证边界

- 实例目录：`C:\Users\qq106\Desktop\游戏\Release\.minecraft\versions\1.12.2-Forge_14.23.5.2864`。
- 读取重新导入后的 `config/GalaxySpace/`、`config/ExtraPlanets.cfg`、`config/AsmodeusCore/core.conf` 和 Galacticraft 配置。
- 对照本仓库 GalaxySpace、Galacticraft-Legacy 源码；对本机实际安装的 GalaxySpace、ExtraPlanets、AsmodeusCore JAR 使用 `javap` 读取关键注册与等级判断逻辑。
- 当前相关 JAR：Galacticraft 4.0.6、GalaxySpace 2.2.0 flight-fix、ExtraPlanets 0.8.0、AsmodeusCore 文件名版本 1.0.5、Galacticraft Compatibility 1.1.2、GSLaunchCompat 1.4.8。
- 用户确认：改配置后已退出并重新导入，尚未重新启动游戏。因此下文“开启”表示磁盘配置，不表示本次配置已在游戏中验证。
- `latest.log` 对应 18:41 启动、19:27 退出的旧会话。它早于本次配置生效，不能用旧注册清单证明新配置的星球状态。
- 本轮未启动游戏、修改存档、改写模组或配置。源码中的资源与结构属于实现层面的依据，生成效果和实际到达仍需游戏验证。

## 当前整体情况

本机同时安装 Galacticraft（GC）、GalaxySpace（GS）和 ExtraPlanets（EP）。同名天体可能有不同模组的实现，判断时必须同时看模组来源和维度 ID。

当前配置以 GC 与 GS 的可探索内容为主：

- GC：月球、火星、小行星带、金星。
- GS：太阳系外侧的行星与卫星，以及比邻星 b、巴纳德 C、巴纳德 C1、鲸鱼座 τ f 等系外目的地。
- EP：多数太阳系主星与卫星已关闭，仍开启 Kepler-22b、Phobos（火卫一）和部分星图展示天体。
- 木星、土星、天王星、海王星在当前 GS 实现中是星图上的母星；EP 的同名可登陆实现现已关闭。它们的卫星仍可有独立地表。

**“星图可见”“配置中有 ID”“有可登陆维度”“正常生存流程能到达”是四件不同的事。**

## 可探索目的地清单

表中 T 为天体注册逻辑里的最低等级，用来整理进度层次。实际还受火箭实现、星图界面、跨星系规则和装备影响；本次未完成飞行实测。资源列用于说明探索价值，不是完整掉落或配方清单，也不表示资源只在该星球存在。

### Galacticraft 基础目的地

| 目的地 | 当前维度 ID | 注册等级 | 主要玩法与需要关注的内容 |
| --- | --- | --- | --- |
| 月球 | -28 | T1 | 首次太空生存、氧气保障、陨铁及月球地牢；后续火箭发展起点 |
| 火星 | -29 | T2 | 戴斯资源、地牢与火箭发展；当前 GS 的 `enableWorldEngine` 和 `enableMarsWorldEngine` 均开启，按 GS 的 World Engine 火星地形处理 |
| 小行星带 | -30 | T3 | 漂浮小行星、钛铁矿等资源与废弃基地探索；GS 的额外零重力开关当前为 false，具体移动体验需实测 |
| 金星 | -31 | T3 | 高温、恶劣大气与气体资源、地牢；GS 会调整金星大气参数 |

GC 基础维度之外还有地球轨道空间站。空间站的实际存档维度与创建状态单独处理，不作为一颗可采矿星球计数。

### GalaxySpace 太阳系目的地

以下具备实际世界 Provider 的目标，在当前 GS 配置中均开启。

| 目的地 | 维度 ID | 注册等级 | 已查到的资源、地形或结构特点 |
| --- | --- | --- | --- |
| 水星 Mercury | -1005 | T3 | 铁、镍、镁；无可呼吸大气，温度防护重要 |
| 谷神星 Ceres | -1007 | T3 | 白云石、陨铁和冰类生成；有专用地牢与宝箱实现 |
| 火卫一 Phobos | -1012 | T2 | 铁、镍、戴斯、陨铁；适合作为火星阶段的资源探索补充 |
| 木卫一 Io | -1014 | T4 | 铜、硫，火山与喷口环境；当前注册的是普通 `WorldProviderIo`，有专用地牢实现 |
| 木卫二 Europa | -1015 | T4 | 冰原、裂谷、冰刺，绿宝石、硅、铝；当前注册的是 World Engine 版本 |
| 木卫三 Ganymede | -1016 | T4 | 镁与钛铁矿生成，补充外太阳系材料来源 |
| 木卫四 Callisto | -1022 | T4 | 陨石坑、洞穴、冰与干冰相关地层；当前装饰器未见专属矿石生成，主要价值偏地形和冰资源 |
| 土卫二 Enceladus | -1017 | T5 | 冰质平原、山脉、裂谷和煤矿；当前 `enableWorldEngine=true`，使用 WE 版本 |
| 土卫六 Titan | -1018 | T5 | 蓝宝石、绿宝石、钻石、煤、青金石、红石；有氮气大气，但不可直接据此认定可呼吸 |
| 天卫五 Miranda | -1024 | T5 | 铁、白云石、钻石、石英、钴、镍等多种资源与地层 |
| 海卫一 Triton | -1021 | T6 | 极寒地形、平原和山地；当前 WE Provider 没有配置通用矿脉列表，具体地表喷口和采集价值需实测 |
| 冥王星 Pluto | -1008 | T6 | 极寒地表与不同地层；当前 `BiomeDecoratorPluto` 没有启用专属矿石生成，不能套用 EP 冥王星的钨矿玩法 |
| 柯伊伯带 Kuiper Belt | -1009 | T6 | 小行星式世界，含特殊矿核、密实冰以及废弃基地生成实现；需检查实际生成与进入方式 |
| 妊神星 Haumea | -1023 | T6 | 氧冰、氮冰、干冰与铝矿；主要是极寒资源探索，没有配置专用地牢 |

不要因为全局 `enableWorldEngine=true`，就一律引用所有 `*_WE` 类：Io 仍注册普通 Provider；Europa 固定注册 WE；Enceladus 才按全局开关切换。

### GalaxySpace 系外目的地

半人马座、巴纳德、鲸鱼座 τ 三组星系开关目前均为 `true`。

| 目的地 | 维度 ID | 注册等级 | 主要内容与环境 |
| --- | --- | --- | --- |
| 比邻星 b Proxima b | -1025 | T6 | 山地、冰原、海岸与海洋等地形；金、锡、铜、煤、硅、钻石。大气含氧，但注册的“可呼吸”标记为 false |
| 巴纳德 C Barnarda C | -1030 | T6 | 海洋、森林、河流、沼泽、丛林、山地、雪原、沙丘等；多种基础矿物及硅、铝、钴、镍，是内容较丰富的生态探索目的地；注册为可呼吸大气 |
| 巴纳德 C1 Barnarda C1 | -1031 | T6 | 冰雪地表、山地、洞穴，铁、铜、锡与缟玛瑙矿；Provider 定义了地牢间距和月球宝箱类型，结构完整性与战利品需实测 |
| 鲸鱼座 τ f Tau Ceti f | -1338 | T6 | 海洋、珊瑚、水草等水下环境；大气含氧但不可直接呼吸，适合单列水下探索主题；矿物和长期生存体验需进一步验证 |

`survivalModeOnBarnarda=false`，因此 Barnarda C、C1 当前注册等级为 6；源码中的 T1 分支不适用于当前配置。

### ExtraPlanets 保留的内容

| 内容 | 当前配置/实现 | 结论 |
| --- | --- | --- |
| Kepler-22b | 开启，维度 -22，注册等级 T10 | 有实际地表，含多种资源型生物群系、材料树、材料球、建筑和糖果主题内容；生成开关大多开启。但正常到达链有明显缺口，见下文 |
| Phobos | 开启，维度 -1502，注册等级 T2 | 与 GS 的 -1012 是两个实现，需要启动后核实星图选中结果及是否出现重名入口 |
| Haumea | 开启 | EP 这项是不可到达天体的展示实现，不是第二套可登陆妊神星；注册助手会先检查同星系同名天体，不能仅凭开关断言必然出现两个入口 |
| Makemake | 开启 | 此版本 EP 为星图展示天体，未提供可登陆 Provider |
| Kepler SolarSystems | 开启 | Kepler-47、62、69 中多颗天体为不可到达的展示内容；不能把它们都当成可探索星球 |
| Mercury、Jupiter、Saturn、Uranus、Neptune、Pluto、Eris | 关闭 | EP 对应地表不属于当前配置的探索主线；相应 T4～T10 普通火箭也受这些成组开关控制 |
| Ceres、Deimos、Europa、Ganymede、Io、Callisto、Rhea、Titan、Oberon、Titania、Iapetus、Triton | 关闭 | EP 这些卫星/星球实现不应按当前可用目的地列入 |

## 星图展示与未开放内容

GS 的 `enableUnreachable=true`，所以仍会显示多颗尚未实现可登陆地表的天体。

- 太阳系：木星、土星、天王星、海王星本体；火卫二，以及土卫一、土卫三、土卫四、土卫五、土卫八、天卫一、天卫二、天卫三、天卫四、海卫八、冥卫一等展示卫星。
- 半人马座/比邻星：`centauri_b`、Proxima c、Proxima d 未配置地表 Provider。
- 巴纳德：Barnarda B、B1、D、E 未配置地表 Provider；C2 只有调试分支配置地表。
- 鲸鱼座 τ：g、h、e 为星图展示，f 才有地表。

`enableDeimos=true`、`enableOberon=true`、`enableProteus=true` 等值不能使展示天体自动获得地表。`enableBarnardaC2=true` 也不能越过 `GalaxySpace.debug` 分支。

类似地，GS 配置里的 `dimensionIDVenus=-1006` 和 `dimensionIDJupiter=-1026` 只是配置项；当前注册逻辑中的金星仍来自 GC 的 -31，木星本体没有 GS 地表 Provider。

## 与实际游戏流程有关的重点

### 1. 可先按注册等级理解探索层次

| 阶段 | 主要候选目的地 |
| --- | --- |
| T1 | 月球、地球轨道空间站 |
| T2 | 火星、火卫一 |
| T3 | 金星、小行星带、水星、谷神星 |
| T4 | 木星的四颗可登陆卫星 |
| T5 | 土卫二、土卫六、天卫五 |
| T6 | 冥王星、柯伊伯带、妊神星、海卫一、四个已实现的 GS 系外目的地 |
| T10 | EP Kepler-22b；当前正常生存到达链未打通 |

这是一张内容层次表，不是已经验收的通关顺序。GS 自带 T4、T5、T6 火箭与组装配方，关闭 EP 的 T4～T6 火箭并不等于取消 GS 的同等级发展。

### GS T4～T6 火箭的实际发展方式

当前 `enableAdvancedRocketCraft=false`、`enableDuplicateSchematic=true`。按本项目源码，并核对本机 JAR 中同名配方注册方法，当前采用以下发展方式：

1. 基础材料先加工，普通部件在 GS 装配机（Assembler）中合成。
2. 火箭部件包括头锥、箭体、引擎、助推器和稳定翼；最终火箭由 GS 火箭装配机（Rocket Assembler）合成。
3. 每级火箭还需要对应许可，许可由空白图纸与前一级完整火箭合成。

| 火箭 | 许可来源 | 主要板材与探索关联 |
| --- | --- | --- |
| GS T4 | 空白图纸 + GC T3 火箭 → `rocket_modules:8` | HDP:0，由镁板、SDHD120 复合板、GC T3 强化板合成。水星 T3 的镁矿是前期可达来源；木卫三虽有镁，但不能作为首次解锁 T4 的唯一来源 |
| GS T5 | 空白图纸 + GS T4 火箭 → `rocket_modules:9` | HDP:1，在 HDP:0 基础上加入钴板、镍板和白云石粉。谷神星 T3 提供白云石；当前主世界钴、镍生成也开启，天卫五不是这些材料的唯一前置 |
| GS T6 | 空白图纸 + GS T5 火箭 → `rocket_modules:10` | HDP:2，在 HDP:1 基础上加入 SDHD120 复合板与 GS 火山石。已确认 Io T4 的火山矿掉落此材料，不能等到 T6 目的地再寻找首次解锁材料 |

补充细节：

- SDHD120 复合板由青铜板、煤板、铝板在装配机中加工；镁、钴、镍的板材有对应压缩配方。
- 空白图纸在装配机中以压缩陨铁、染料和纸加工，其配方受 `enableDuplicateSchematic` 控制；当前为 true，所以这项前置在配置层面没有关闭。
- 谷神星白云石矿掉落白云石晶体；回收机有晶体转为白云石粉的配方，因此“获得矿石”与“材料加工完成”是两个步骤。
- 当前 Io 的火山矿由岩浆湖生成逻辑附加，掉落 `BasicItems.VOLCANIC_STONE`；它是 GS 的具体物品，不能用 GC 金星同样带“火山”字样的方块替代。
- 普通不带箱子的火箭配方使用头锥 ×1、箭体 ×2、引擎 ×1、助推器 ×2、稳定翼 ×2、许可 ×1；货舱版本另加箱子。
- 许可配方把前一级完整火箭作为合成输入，源码未见该火箭物品的容器返还处理。应先按会消耗前级火箭来规划，在安全基地办理升级，不把它描述成自动复制图纸或保留旧火箭的操作；最终仍需运行验证。

因此，GS 的后半段可理解为“逐级制造与材料探索”，与 EP 在主星地牢领取下一阶图纸的路线不同。关闭 EP 水星、木星等实现，不会直接移除上述 GS 配方。

### 有材料依据的探索路线与可选内容

下面用于解释现有玩法关系，不表示已替整合包新增强制任务或完成全部配方可达性验证：

```text
GC T1～T3 基础发展
  ├─ 水星：镁 → 为 GS T4 材料加工提供来源
  ├─ 谷神星：白云石 → 加工白云石粉，为 GS T5 材料提供来源
  └─ 小行星带：GC T3 材料基础
       ↓
GS T4：可探索木星卫星
  └─ Io：火山矿 → GS 火山石，为 GS T6 材料提供来源
       ↓
GS T5：可探索土卫二、土卫六、天卫五
       ↓
GS T6：外太阳系与已实现的 GS 系外目的地
```

T5 世界有资源和环境探索价值，但上述 T6 核心板材配方并没有要求先完成每一颗 T5 天体。以后组织内容时可以区分“火箭制造的必要前置”和“获得该等级后可选的探索”；不能仅按等级高低，就认定每颗星球都是下一阶火箭的硬性关卡。

GS T6 与系外天体注册等级相同，只能证明基本等级条件对应；跨星系选择、发动机规则、星图适配和返航仍需实测。Kepler-22b 则属于另一条尚未接通的 EP T10 路线，不能直接接在 GS T6 后宣称可达。

### 2. Kepler-22b 已开，但对应火箭链关闭

本机 EP 配置同时为：

```text
"Kepler 22b"=true
"Eris & Tier 10 Rocket"=false
"Enable Electric Rocket"=true
```

对实际安装 JAR 的检查表明：

- Kepler-22b 的天体等级是 10，世界 Provider 按火箭等级不低于该要求来判断通行。
- EP 的普通 T10 火箭物品受 `ERIS` 开关控制。
- 电动 T10 火箭物品和相关图纸还要求 `ERIS && KEPLER22B && ELECTRIC_ROCKET`，单开电动火箭不足以恢复这条路线。
- GS 最高一档火箭报告等级 6，不能据此宣称已经能到 Kepler-22b。

因此当前应把 Kepler-22b 记为“维度开启、正常生存到达路线未落实”。如何保留该星球并接入进度，是后续配置选择；本轮不代改。

### 3. 仍保留两套 Phobos

GS `enablePhobos=true`，EP `Phobos=true`，维度分别为 -1012、-1502。维度 ID 不同不代表星图体验一定清楚。需要启动后核实显示名称、实际落地维度以及正常火箭选择的目标，再决定是否保留两套实现。

### 4. 含氧不等于可直接呼吸

例如 Proxima b、Tau Ceti f 的大气成分含氧，但注册时可呼吸标记为 false；Barnarda C 标记为 true。玩家是否需要氧气装备必须看游戏规则和实测，不能按天体名称或现实常识判断。

GS 的高级温度、气压、辐射、额外天气均开启；EP 的气压与辐射及其对 GC 星球的应用也开启。但补查源码与本机 GS JAR 后发现，GS 的气压和辐射事件处理对普通非 Android 生存玩家会取消对应的 AsmodeusCore 事件。因此不能仅凭开关断言两套伤害必然叠加，也不能据此认定 EP 的独立机制失效。具体条件与验证边界见[详解中的环境机制说明](planet-gameplay-guide.md#4-新发现gs-气压辐射开关不等于普通玩家实际受害)。温度、防护与各系统效果需分别实测。

### 5. 不应把每颗星球都设计为“打 Boss 拿下一阶图纸”

GS 多颗目的地明确返回地牢间距 0、宝箱类型 null，玩法偏资源或生态探索；谷神星、Io 等才有相应结构实现。不同目的地应根据实际内容组织玩法，不能套用 EP 已关闭星球的矿物、Boss 和解锁链。

进一步确认：谷神星 Boss 为 `EntityBossBlaze`，特殊奖励取自等级 4 池；Io Boss 为 `EntityBossGhast`，取自等级 5 池。GS 向两池分别加入临时防护/应急传送工具和引擎模块等内容。这些奖池等级不同于星球的最低登陆等级，也不同于用前级火箭合成的升级许可。两个普通宝箱表已与本机 JAR 按 JSON 内容比对一致；特殊池和普通表分别记录，随机奖励不保证每次齐全。

### 6. 空间站与星球分开

GS 火星、金星空间站开关为 true；EP 对应两种空间站开关为 false。EP 还保留若干其他母星的空间站开关，但这些开关与母星注册有联动，不能仅看该值就断言可创建。空间站属于建造/轨道玩法，实际创建、配方、维度和返回路径另列验证。

## 新配置首次启动后的检查

- [ ] 确认启动时读取的确实是本次实例及其配置，记录新的模组列表和维度注册清单。
- [ ] 星图中逐一核对 GS 实际目的地、展示天体、Kepler 系统与两个 Phobos 的显示和选中结果。
- [ ] 用对应等级火箭验证目标能否选择、落地及返回；优先抽查月球、火星、水星、Io、Titan、Barnarda C、Kepler-22b。
- [ ] 检查装配机的空白图纸、板材和部件配方，以及火箭装配机的 T4～T6 配方；核实许可合成和旧火箭消耗，确认实际 JAR 中的完整配方链可用。
- [ ] 区分旧存档已经生成的区块与新生成区块，再检查地形、矿物、地牢和战利品；不为验证删除原世界。
- [ ] 检查氧气、温度、气压与辐射，确认 GS/EP 两套规则下装备是否有效。
- [ ] 确认 GS 火星/金星空间站的创建与返航路径。

旧日志中存在 `WorldProvider type -1234`、`-1032` 查询失败。源码的测试星与 Barnarda C2 恰有调试注册限制，因此目前更像查询了未注册的调试维度；这不是已经证实的 ID 冲突。应看新日志中是否仍出现，并结合调用位置核实。

本次完成配置与源码层面的清单、内容分层和进度缺口定位。新配置的实际生效、飞行、地形与生存验证尚未完成，Issue #4 保持进行中。

## Wiki 与作者说明补充

### 玩家视角下要弄清楚的内容

[Galacticraft 入门教程](https://galacticraft.mods.wiki/wiki/Tutorials/Galacticraft_Getting_Started_Guide)与[氧气教程](https://galacticraft.mods.wiki/wiki/Tutorials/Oxygen)把探索流程拆成火箭、燃料、供电、随身氧气和基地供氧。星球清单因此还应关联以下问题：出发带什么、到达后如何补给、能获得什么、如何发展下一阶段，以及如何返航。

官方旧 Wiki 的[月球页](https://galacticraft.mods.wiki/wiki/Moon)介绍了陨石、村庄、地牢和进化骷髅 Boss；[火星页](https://galacticraft.mods.wiki/wiki/Mars)补充了戴斯、细菌泥、宠物 Slimeling 和地牢 Boss 等玩法。它们可以补充探索目标，但页面包含旧版本内容；尤其本机 GS 已替换火星地形，不能把 Wiki 的地形、生成与全部配方直接当成本机结果。

### GS 的“未实现天体”是作者明确区分的类别

[GalaxySpace 作者发布页](https://www.curseforge.com/minecraft/mc-mods/galaxy-space-addon-for-galacticraft)区分已实现天体、GC 内容、本体未实现但卫星已实现的天体，以及未实现/计划内容。这与本轮源码调查中“可见不等于可登陆”的分类一致；发布页的总名单跨版本，仍需以本机 1.12.2 的注册逻辑筛选。

### EP 的攻略路线不等于本机路线

[作者 Wiki 的地牢与图纸页](https://docs.mjrlegends.com/ExtraPlanets/dungeons/)将 EP 主星与逐级火箭图纸串联，卫星和 Kepler-22b 列为可选内容，T10 图纸位于阋神星地牢。这能解释 Kepler 的发展背景，但本机已关闭多数 EP 主星与对应火箭，不能直接照搬整条路线或把其战利品归给 GS 的同名星球。

### 共存配置还有一项明确候选检查

[EP 作者的 MC 1.12.2 共存指南](https://docs.mjrlegends.com/ExtraPlanets/1.12.2compact/)涉及兼容选项、星图二选一、避免重复空间站，以及两套宇航服互认。对照本机文件：

| 项目 | 本机文件状态 | 后续判断 |
| --- | --- | --- |
| EP 的 `Enable Galaxy Space Compatibility` | false | 与作者共存指引的兼容选项方向不同，应结合保留两套 Phobos 的需求检查 |
| 星图 | EP 自定义星图 true；AsmodeusCore 新星图 false | 文件层面已选择 EP 星图，实际启动后确认 |
| 火星与金星空间站 | GS true，EP false | 已在开关层面避免同时启用两套 |
| 防护服互认 | EP 列有 GS 普通/轻型宇航服；GS 列有 EP T1～T4 宇航服 | 列表已填，实际防护效果仍需测试 |

兼容开关并非只控制显示。EP [1.12.2 上游配置源码](https://github.com/MJRLegends/ExtraPlanets/blob/dev_1.12.2/src/main/java/com/mjr/extraplanets/Config.java)说明它会调整同名天体并为 EP 重复版本添加区别名称，本机 JAR 也存在对应命名分支。因此将其记录为候选调整项，本轮没有直接改为 true；若之后调整，需同时复查存档中的目的地与星图选择。

## 源码定位

- [GS 太阳系天体注册](../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/SolarSystemBodies.java)：Provider、等级、展示卫星、空间站与火星地形替换。
- [GS 半人马座/比邻星注册](../GalaxySpace/src/main/java/galaxyspace/systems/ACentauriSystem/ACentauriSystemBodies.java)。
- [GS 巴纳德注册](../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/BarnardsSystemBodies.java)。
- [GS 鲸鱼座 τ 注册](../GalaxySpace/src/main/java/galaxyspace/systems/TauCetiSystem/TauCetiSystemBodies.java)。
- [GS 火箭配方](../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/recipes/CraftingRecipesOverworld.java)。
- [GS 冥王星矿物装饰器](../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/pluto/world/gen/BiomeDecoratorPluto.java)。
- [GS 妊神星地形与资源](../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/haumea/dimension/WorldProviderHaumea_WE.java)。
- [GS 巴纳德 C 地形与矿物](../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/planets/barnarda_c/dimension/WorldProviderBarnarda_C_WE.java)。
- [GS 巴纳德 C1 地形与矿物](../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/moons/barnarda_c1/dimension/WorldProviderBarnarda_C1_WE.java)。

EP 没有导入主项目源码，本轮对应证据来自本机 `ExtraPlanets-1.12.2-0.8.0.jar` 的 `ExtraPlanets_Planets`、`ExtraPlanets_Moons`、`ExtraPlanets_Items`、`GCRegisterUtilities` 及 Kepler-22b Provider 字节码。AsmodeusCore 的 `BodiesRegistry.setProviderData` 用于核对注册参数中的等级含义。
