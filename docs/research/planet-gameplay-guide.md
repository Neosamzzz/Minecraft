# 星球资源与玩法指南

本指南是星球资源、环境、采集与既有制造关系的重要查阅依据，供整合包维护、NewEarth 物品开发及 FTB 内容编写共用，不限于任务文案。对应 [Issue #4](https://github.com/Neosamzzz/Minecraft/issues/4)；静态配置与维度注册总览见[星球现状审计](planet-gameplay-audit.md)。

首次整理：2026-11-01；本次更新：2026-11-03。本次补充 EP 资源、设备原配方、GS 防护与改装依赖，并对齐太阳系整合方向；没有启动游戏、修改配置或制作新物品。

> [!IMPORTANT]
> **当前方向：以 GC／GS 制造和防护体系为基础，选择性保留 EP 地表、资源与地牢，重新设计配方。** 双水星、双谷神星及 EP 木星、土星、天王星、海王星、冥王星、Eris 纳入设计；GS 土卫二、天卫五拟改挂对应 EP 主星。原始实现与历史配置仍分别记录，正文中的原等级和旧奖池不因方案确认而改写；最新路线和地牢奖励统一见[物品线路线手稿](../../NewEarth/items/route-manuscript.md)。这不是当前运行配置，EP 原七阶材料链不再作为新配方前提。

## 调查基线与临时测试状态

- 主体资料来自 2026-11-01 的本机 GC 4.0.6、GS 2.2.0 flight-fix、EP 0.8.0 调查；当时多数 EP 主星与火箭组关闭，曾复核的 25 个文件哈希未变化只适用于那次快照。
- 文中的 24 个编号保留首轮已调查目的地及共存问题的索引，包含两套 Phobos。它不是当前正式保留天体数量，也不是 24 处已实测可达的名单；EP Phobos 条目主要用于历史问题排查。
- 2026-11-02 已导入 [EP 0.8.0 源码快照](extraplanets-source-notes.md)。本轮复查磁盘发现 EP 主星、火箭组及 GS 兼容开关已开启；用户说明该组合用于那次 EP 访问测试，开关方案尚未确定。因此本指南不据此重排正式星球清单或恢复 EP 整条火箭主线。
- 下文“首轮关闭”等描述用于解释历史调查结果。机制、资源和配方按源码或实际 JAR 的证据分别记录；新的磁盘开关不等于生成、制造、飞行与返回都已验收。
- 已确认的 NewEarth 方向单独列在文末，不能覆盖本文记录的原有等级、配方和采集限制。最新资源与工具的具体设计仍由物品线据此核实。

## 阅读方法与可信程度

文中使用四种标记：

| 标记 | 含义 | 能据此做什么 |
| --- | --- | --- |
| 配置确认 | 已读取本机磁盘上的对应开关和 ID | 判断当前准备启用的内容；不能替代启动结果 |
| 实现确认 | 有当前源码或实际 JAR 中的实现依据 | 说明设计机制、资源来源和制作关系；不能保证生成、交互与共存效果 |
| 编排建议 | 根据已查内容提出的任务组织方式 | 供任务作者选择，不表示已修改任务书或新增规则 |
| 待运行验证 | 需要本次配置真正启动后检查 | 不应写成已经保证成立的玩家承诺 |

本文没有完成完整游戏玩法验收的星球条目。已保留的日志可证明对应会话的部分加载行为和异常，不能替代正常飞行、采矿、防护与返航；历史日志和测试开关均不当作正式配置定案。

“T3 星球”等表述指天体要求的最低火箭等级。它与火箭能否在当前星图选中目标、跨星系能力、降落方式和返航是否可用分别核对。

资料保留地球准备、24 个历史调查目的地条目和轨道空间站。阅读时先看模组与维度 ID，再核对保留方向及证据层级；星图展示、注册地表与正常生存可达分别判断。

## 一、探索前应讲清的通用玩法

### 1. 地球阶段是整条探索链的后勤基础

地球不是一个简单的“造出火箭就离开”的教程区。供电、基础材料加工、燃料与氧气分别服务不同系统，应该先让玩家理解用途。

| 系统 | 玩家要解决的问题 | 对后续探索的作用 |
| --- | --- | --- |
| 矿物与加工 | 铜、锡、铝、硅、钢材等怎么取得并加工 | 机器、电路、板材和火箭部件的基础 |
| 供电与储能 | 发电机、导线、机器接口和电池如何配合 | 维持燃料、供氧和材料加工；远行时提供备用能源 |
| 燃料 | 原油如何收集、精炼并装入火箭 | 决定能否起飞和再次起飞；氧气不能作为火箭燃料 |
| 氧气 | 如何收集、存储、向随身罐补充或供给基地 | 支持无可呼吸大气环境中的活动 |
| 火箭制造 | NASA 工作台、GC 图纸与后续 GS 装配设备的作用 | 让“有材料”“解锁页面”“有制造设备”形成完整链条 |
| 防护与生存 | 氧气装备、温控装备、护甲、补给分别解决什么问题 | 防止把一件宇航服误认为所有环境机制的通用解答 |
| 返航与补给 | 降落后从哪里取回火箭、发射台和物资，怎样重新起飞 | 把第一次登陆变成可持续往返，而不是单程抵达 |

这是对当前玩法的组织方式。具体合成数量以实际安装包的配方为准；不能照抄另一份整合包的物资清单。

当前主世界的 GS 钴、镍等矿物生成开关开启。它们关系到后续火箭材料，因此不能把外太阳系同类矿物都写成唯一来源。生成开关也不会把已经生成的旧区块自动变成新矿区。

参考：[GC 基础配方源码](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/core/recipe/RecipeManagerGC.java)、[GS 主世界配方](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/recipes/CraftingRecipesOverworld.java)。[GC 官方入门教程](https://galacticraft.mods.wiki/wiki/Tutorials/Galacticraft_Getting_Started_Guide)可用于理解供电、加工、燃料、氧气的关系，页面里的旧版本配方和第三方模组组合需另行筛选。

### 2. 出发准备分成“短途勘察”和“建立据点”

短途勘察首先保证能够完成目标并返回；长期据点还要解决再补给。不要把全部后期机器都塞进第一次出发的必备清单。

**短途勘察需要核对的事项：**

1. 所用火箭等级、目标选择、发射台适配和加注状态。
2. 随身氧气装备的安装与罐内余量，而不只是背包里有一个空罐。
3. 所选目的地的温度、防护要求，以及本机 GS/EP 机制实际叠加情况。
4. 食物、采集工具、照明、建材和应对敌人的装备。
5. 降落后如何取回必要设备；为返航保留燃料、能源和物资位置。
6. 记录着陆点或补给点，确定返程操作；不同火箭和落地载具可能不同。

**建立据点再增加：**

- 可持续供电和储能，不把任何远日天体都默认写成太阳能充足。
- 氧气供应、储备与补充途径，以及供氧区域是否实际生效。
- 食物与植物的环境条件；本机 `enableOxygenForPlantsAndFoods=true`，不能默认露天种植等同主世界。
- 安全工作区、资源加工、物资储存和后续往返的发射位置。
- 机器工作与维护条件，以及故障时可带回地球处理的替代办法。

这些是准备检查项。最终“推荐携带多少罐氧气”“哪一套温控服足够”“备用燃料多少”等数值需要实际短途测试后补齐。

### 3. 环境机制必须分别理解

| 机制 | 当前可以确认的事 | 不能直接作出的结论 |
| --- | --- | --- |
| 呼吸 | GS 的天体注册明确区分含氧成分和可呼吸标记 | 看见海洋、森林或大气含氧，就可以摘掉氧气装备 |
| 温度 | GS 高级温控开启；GS 混入 GC 玩家温度检查，读取装备和世界参数 | 一种保温服能覆盖所有星球；代码里的数值就是摄氏度 |
| 气压、辐射 | GS 与 EP 的配置均保留相关系统，但 GS 当前事件取消逻辑有异常，见下节 | 两套系统必然同时造成伤害，或其中一套开关能控制另一套 |
| 重力与移动 | Provider、AsmodeusCore、GC 和补丁都可能参与 | 一律根据现实天体质量推断跳跃高度；把所有小行星世界写成同一种移动体验 |
| 天气与供电 | 天体大气、天气和供电实现可能不同 | 所有行星都适合使用风力，或按现实日照比例直接给出发电量 |
| 地牢与敌人 | 一部分目的地有明确结构和 Boss；另一部分 Provider 没有标准地牢入口 | 每颗天体都有 Boss，或没有标准地牢就没有任何敌人 |

### 4. 新发现：GS 气压、辐射开关不等于普通玩家实际受害

**实现确认：仓库源码与本机实际 GS JAR 的相应方法一致。**

`GSEventHandler.onRadiation` 与 `onPressure` 在生存玩家分支中，都把下面这一类条件纳入事件取消判断：

```java
!(enableEffectForAndroid && CompatibilityManager.isAndroid(player))
```

两种事件分别使用各自的 Android 开关。对于普通的非 Android 玩家，`isAndroid(player)` 为 false，这一项为 true，因此该处理器会调用 `setCanceled(true)`。本机两个 Android 开关目前都是 true，也不会改变上述普通玩家分支的结果。

这里确认的是 **GS 处理器对 AsmodeusCore 对应事件的取消行为**。不能由此宣布玩家免疫 EP 的独立气压/辐射系统，也不能保证没有其他事件订阅者改变最终结果。应在普通生存模式中分别观察 GS/AC 与 EP 的效果；创造模式不适合证明防护有效。

因此，本资料把气压、辐射列作待验机制，不把“开关 true”翻译为某颗星球上一定发生的伤害。此次不修复代码，也不调整防护平衡。

依据：[GS 环境事件处理](../../GalaxySpace/src/main/java/galaxyspace/core/events/GSEventHandler.java)、[GS 温度检查混入](../../GalaxySpace/src/main/java/galaxyspace/core/mixins/MixinGCPlayerHandler.java)。实际 JAR 的 `javap` 输出已保留在本机调查临时目录。

## 二、GC 基础探索：T1～T3

### 1. 月球 Moon：第一次完成太空往返

**身份与前置：** GC；维度 `-28`；T1。配置与实现确认，当前配置下的起飞、登陆、返程待验证。

**已经确认的内容：**

- GC 月球的地表、陨石与陨铁相关资源、村庄及地下地牢构成基础探索内容。
- 地牢有进化骷髅 Boss 和对应宝箱逻辑。
- GC 一级地牢特殊战利品注册包括月球车图纸与 T2 火箭图纸；不能写成每次打开宝箱必定拿到 T2 图纸。
- GS 还向一级地牢池加入自身图纸内容，战利品不应只按纯 GC 环境描述。
- 月球不是可直接呼吸的环境。本机还开启 GS 的月球太阳辐射开关，但该开关的实际伤害表现受前述事件逻辑影响。

**第一次前往的价值：** 练习着陆、回收物资、控制氧气余量、寻找地牢、带回陨铁，并理解图纸如何推动后续制造。第一次往返本身可以是独立里程碑，不必要求一次完成所有探索。

**持续探索的价值：** 收集陨铁、寻找尚未得到的特殊战利品、建造最初的太空补给点。村庄交互是可选观察内容；旧 Wiki 对村民交易的叙述不能直接作为本机结论。

**编排建议：** 准备检查 → 登陆 → 标记着陆点 → 采样 → 地牢支线 → 安全返回。把“找到 T2 图纸”和“制造 T2 火箭”分开，因为获得图纸不等于材料和制造条件齐全。

**待验证：** 月球实际氧气/温度/EP 环境效果；当前地牢池；图纸解锁和制造；落地载具物资回收及返航。

依据：[GC 月球 Provider](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/core/dimension/WorldProviderMoon.java)、[GC 初始化与一级地牢池](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/core/GalacticraftCore.java)、[GS 地牢池扩展](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/SolarSystemBodies.java)。

### 2. 火星 Mars：戴斯、地牢与生态支线

**身份与前置：** GC 天体，维度 `-29`，T2；当前 GS 全局 WE 与火星 WE 均开启，GS 替换世界 Provider。

**已经确认的内容：**

- 戴斯与 GC 火星发展相关；火星洞穴、地牢仍是重要探索内容。
- 当前 GS WE Provider 明确构造 `MapGenDungeonMars`，使用火星 Boss 房与宝箱类型，并保留火星洞穴生成实现。
- GC 二级地牢特殊战利品池含 T3 火箭、货运火箭和 Astro Miner 图纸。
- 细菌泥、Slimeling 是可继续调查的 GC 火星生态内容，不能仅凭它们存在于原模组，就认定每种地形在当前 WE 世界里都会以原版方式生成。

**第一次前往的价值：** 获取戴斯、探索火星地牢，为 T3 制造准备条件。GS 火卫一也有戴斯生成，因此不能把火星地表采矿写成所有戴斯的唯一来源；火星地牢的图纸价值与矿源选择是两件事。

**持续探索的价值：** 补齐图纸、补充戴斯、尝试基地建设与生态支线。宠物收集属于可选玩法，不应未经验证成为主线硬门槛。

**准备重点：** 氧气与温控、洞穴照明和战斗、背包与返航余量。进入地牢前先有可返回的补给点。

**编排建议：** 到达火星 → 戴斯加工 → 地牢/图纸 → T3 制造；另列“火星生命观察”支线，等当前版本生成和交互确认后再写具体目标。

**待验证：** 新区块的 WE 火星地貌、矿脉和地牢；Slimeling/细菌泥实际生成；T3 图纸在当前完整地牢池中的取得方式。

依据：[当前火星 WE Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/mars/dimension/WorldProviderMars_WE.java)、[GC 火星模块](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/planets/mars/MarsModule.java)、[火星图纸类型](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/planets/mars/items/ItemSchematicTier2.java)。

### 3. 小行星带 Asteroids：钛材料与立体探索

**身份与前置：** GC；维度 `-30`；T3。

**已经确认的内容：** GC 的小行星世界提供漂浮地形、钛铁矿等材料和废弃基地相关内容。它不是一颗有连续地面的普通行星。当前 GS `enableZeroGravityOnAsteroids=false`，不额外启用该项所描述的柯伊伯带式零重力。

**第一次前往的价值：** 获得 T3 材料体系所需资源，认识空间中的定位和移动。GS T4 板材配方使用 GC `basicItem:5`，它是 T3 强化板，不能误写成元数据 6 的压缩钛。

**持续探索的价值：** 钛材料供应、基地遗迹与采矿设备玩法。Astro Miner 有图纸实现，但“已解锁”“已建成”“可在本机可靠自动采矿”仍需分别验证。

**准备重点：** 氧气、温控、可靠移动方法、定位与回程。不要未经测试就把某一种抓钩、喷气背包或重力装备列为唯一必需品，也不要默认玩家会像主世界一样自然落到安全地面。

**编排建议：** 寻找可停留地形 → 获取钛铁矿 → 加工并制造 T3 强化材料；废弃基地和自动采矿可独立成支线。

**待验证：** 当前补丁下的移动和着陆表现、矿物产出、废弃基地、采矿设备、回程操作。

依据：[GC 小行星 Provider](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/planets/asteroids/dimension/WorldProviderAsteroids.java)、[GS 小行星混入](../../GalaxySpace/src/main/java/galaxyspace/core/mixins/MixinWorldProviderAsteroids.java)、[GS 火箭板材配方](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/recipes/CraftingRecipesOverworld.java)。

### 4. 金星 Venus：恶劣环境与高级装备材料

**身份与前置：** GC；维度 `-31`；T3。GS 配置中的 `dimensionIDVenus=-1006` 不代表另开了一套可登陆金星。

**已经确认的内容：** GC 金星包含恶劣环境、地牢与气体/特殊材料内容。GS 修改金星大气，并在后续温控材料配方中引用 GC 金星物品。它具有装备发展的意义，不只是 T3 阶段的打卡地点。

**第一次前往的价值：** 调查高温环境，取得相关材料，探索金星地牢。是否把其中某个材料列为某段主线的必要条件，要继续追到具体配方，不能笼统地说“所有高级火箭都要先打金星 Boss”。

**持续探索的价值：** 环境装备、特殊材料与气体设施相关发展。EP 作者 Wiki 对金星战利品的说明属于其组合环境，不能不加筛选地归入本机 GC/GS 金星。

**准备重点：** 单独验证温控、气压与其他伤害；氧气装备只解决呼吸问题。采集和基地机器要验证当前环境中能否正常工作。

**编排建议：** 温控准备 → 短途环境勘察 → 材料采样 → 地牢/设备支线。先让玩家认识危险和应对方式，再要求长期基地建设。

**待验证：** 两套环境系统的实际结果、最低可用装备、具体资源加工、地牢奖励和 GS 金星空间站。

依据：[GC 金星 Provider](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/planets/venus/dimension/WorldProviderVenus.java)、[GS 金星参数及空间站注册](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/SolarSystemBodies.java)、[GS 温控材料配方](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/recipes/CraftingRecipesOverworld.java)。

## 三、GS 在 T2～T3 开放的补充目的地

### 5. 火卫一 Phobos（GS）：火星阶段的矿源选择

**身份与前置：** GS；维度 `-1012`；T2。另有 EP 同名实现，必须按维度区分。

**资源与结构：** 当前矿物装饰器有铁、镍、戴斯、陨铁；Provider 返回地牢间距 0、宝箱类型 null，没有在此定义常规地牢。

**探索价值：** 作为火星阶段的资源补给点或短途卫星探索。戴斯不是只在火星取得，陨铁也不是只靠月球陨石。

**进度关系：** 可补充已有材料，但没有证据证明必须先完成这颗卫星才能造 T3。不要用一个没有确认的 Boss 或下一阶图纸作为探索理由。

**准备与重复探索：** 做好无可呼吸环境的补给和往返；可按缺少的矿物选择是否重复前往。

**编排建议：** “火星邻近资源调查”可选分支；到达检测必须绑定 `-1012`，避免误把 EP 的 `-1502` 算作相同目标。

**待验证：** 星图是否与 EP 重名、实际选中哪一套、矿物和返航表现。

依据：[GS 火卫一 Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/phobos/dimension/WorldProviderPhobos.java)、[矿物生成](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/phobos/world/gen/BiomeDecoratorPhobos.java)。

### 6. 水星 Mercury（GS）：T4 板材的早期镁来源

**身份与前置：** GS；维度 `-1005`；T3。首轮调查时 EP 水星及其 T4 火箭组关闭；临时测试开启不改变本条讨论 GS 水星的身份。

**资源与结构：** 铁、镍、镁；Provider 未定义常规地牢。注册温度参数为正值、不可呼吸，具体装备效果仍要看当前事件与温控实现。

**第一次前往的价值：** 为镁板建立来源，进一步制造 GS HDP:0。这个来源在 T3 即可调查，避免把需要 T4 才能抵达的木卫三误当作首次 T4 制造的唯一镁源。

**后续加工：** 镁矿产出与冶炼 → 镁锭 → 压缩加工为镁板 → 与 SDHD120、GC T3 强化板合成 HDP:0 → 制造 T4 部件。

**重复探索的价值：** 镁、镍等材料补给。火箭和高阶设备可能继续消耗板材，不能只记录一次“拿到矿石”就认为材料生产完整。

**编排建议：** 矿物样本、冶炼、镁板和 HDP:0 各自表达一个加工阶段。不要把 EP 水星地牢的 T4 图纸路线套到 GS 水星。

**待验证：** 矿石实际生成和掉落、压缩机配方、GS 镁矿至板材的全链，以及首次落地所需温控。

依据：[GS 水星 Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/mercury/dimension/WorldProviderMercury.java)、[矿物生成](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/mercury/world/gen/BiomeDecoratorMercury.java)、[板材与火箭配方](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/recipes/CraftingRecipesOverworld.java)。

### 7. 谷神星 Ceres（GS）：白云石与独立地牢

**身份与前置：** GS；维度 `-1007`；T3。首轮调查时 EP 同名目的地关闭；本条指 GS 实现，EP 的后续测试开关不改变资源归属。

**资源与地形：** 白云石、陨铁、冰类生成。白云石矿产出晶体；晶体在回收机中加工成白云石粉，配方产量为一份晶体转六份粉。

**进度价值：** 白云石粉参与 HDP:1，也用于 GS T3 温控布。天卫五同样有白云石，但它是 T5 目的地，不能作为制造第一艘 T5 的唯一矿源。

**地牢内容：** Provider 的地牢间距参数为 700，使用专用谷神星宝箱类型。Boss 刷怪器对应 `EntityBossBlaze`，其特殊奖励从等级 4 的 GS/GC 地牢池选择；这不是“必须 T4 才能登陆谷神星”的含义。

**已查的奖励实现：** GS 向等级 4 池加入临时防护控制器 `gs_basic:16` 和应急便携传送器 `gs_basic:18`。宝箱普通战利品表还包含补给、温控材料、戴斯装备、等离子单元等。奖励池选择和宝箱抽取存在随机性，不能承诺每次拿齐。

**第一次前往的价值：** 先建立白云石采集与加工；地牢可以作为独立探索挑战。材料路线与挑战路线可并列，避免把采矿写成必须先击败 Boss。

**编排建议：** 白云石晶体 → 回收机加工 → 白云石粉；地牢、Boss 和特殊工具另列支线。临时防护和应急传送的具体用法、消耗与配置必须验证后再写成救援保证。

**待验证：** 矿物与地牢实际生成、回收机配方、Boss 与宝箱开启、随机池内容、两个特殊工具的使用和返回位置。

依据：[谷神星 Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/ceres/dimension/WorldProviderCeres.java)、[矿物生成](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/ceres/world/gen/BiomeDecoratorCeres.java)、[Boss 与特殊奖励选择](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/ceres/entities/EntityBossBlaze.java)、[普通战利品表](../../GalaxySpace/src/main/resources/assets/galaxyspace/loot_tables/dungeon_tier_4.json)。实际 JAR 中的两个 GS 宝箱表已按 JSON 内容复核。

## 四、GS T4：木星的四颗卫星

木星本体在本机当前配置中是星图母星，不是先登陆木星再解锁卫星。四颗卫星各有目的，不应仅因同属 T4 就安排四次相同的采矿任务。

### 8. 木卫一 Io：火山石与引擎模块挑战

**身份与前置：** GS；维度 `-1014`；T4。当前明确注册普通 `WorldProviderIo`，不是 `WorldProviderIo_WE`。

**资源与环境：** 铜、硫、岩浆湖与硫酸湖的生成实现；岩浆湖生成器附带火山矿。火山矿掉落 GS `gs_basic:12`，即配方中的火山石。温度不能按“有岩浆所以整颗天体必定高温”推断；GS 注册的大气温度参数实际上为负值。

**进度价值：** 火山石参与 HDP:2，进而关联 GS T6 部件，也参与 T3 温控布。因此有必要在 T4 阶段认识它，而不是等到制造 T6 时才给出没有前置解释的材料要求。

**地牢内容：** 专用地牢与宝箱；间距参数为 900；Boss 刷怪器对应 `EntityBossGhast`。Boss 特殊奖励从等级 5 池选择，GS 向该池加入 `rocket_modules:4`、`:6`，其中 `:6` 对应亚光速引擎模块。宝箱普通池也包含火山石、陨铁碎片与补给。

**第一次前往的价值：** 调查火山地形并获取正确的 GS 火山石。地牢提供额外设备/模块探索，不能把它直接写成 T6 火箭许可的领取地点。

**重复探索的价值：** 火山石与硫等补给，或补齐随机特殊奖励。不能混用 GC 金星中同样带“火山”字样的物品。

**编排建议：** 安全接近火山地形 → 矿物/火山石采样 → 材料加工；Boss 与引擎模块另设挑战。跨星系是否必须使用某模块，仍需核对当前星图与发动机实际规则。

**待验证：** 岩浆湖与火山矿实际分布、掉落、最低可用防护、Boss 战斗、模块安装与作用、返航。

依据：[当前 Io Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/io/dimension/WorldProviderIo.java)、[湖泊及矿物生成](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/io/world/gen/BiomeDecoratorIo.java)、[Io 方块掉落](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/io/blocks/IoBlocks.java)、[Boss 奖励](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/io/entities/EntityBossGhast.java)、[普通战利品表](../../GalaxySpace/src/main/resources/assets/galaxyspace/loot_tables/dungeon_tier_5.json)。

### 9. 木卫二 Europa：冰地貌与硅铝补给

**身份与前置：** GS；维度 `-1015`；T4。当前注册固定 WE 版本。

**资源与地形：** 当前 WE Provider 有裂谷、不同高度的冰原和冰刺地形；矿物生成包含绿宝石、硅、铝。不能只读普通版装饰器就认定当前地图的所有细节。

**探索价值：** 冰质地貌观察、电子与基础工业材料补给，以及外太阳系据点选址。硅铝在其他地方也有来源，因此这不是首次解锁电子工业的唯一星球。

**进度关系：** 已查的 T5/T6 核心板材链没有要求必须完成木卫二。可以给玩家资源或地形方面的理由，自由选择是否深入。

**准备与风险：** 无可呼吸大气；裂谷和冰刺需要移动、定位与保温准备。Provider 的燃料倍率返回 0 是实现细节，不能据此承诺无需燃料即可飞行。

**编排建议：** 地貌调查 → 硅/铝样本 → 可选冰原据点。不要给它添加尚未发现的 Boss 或下一阶图纸。

**待验证：** WE 地形、采集与冰类处理、供电和移动表现、火箭实际燃料行为。

依据：[Europa WE Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/europa/dimension/WorldProviderEuropa_WE.java)。

### 10. 木卫三 Ganymede：镁与钛铁矿的补充来源

**身份与前置：** GS；维度 `-1016`；T4。

**资源与结构：** 装饰器含镁矿与钛铁矿；Provider 未定义常规地牢。

**探索价值：** 在玩家已具备 T4 后，为镁和钛材料提供另一处来源。适合发展资源补给点，也可用于比较小行星带与连续地表采矿的体验。

**进度关系：** 木卫三需要 T4；水星 T3 的镁源才适合解释首次 T4 制造。不要形成“先去木卫三取得制造第一艘 T4 的唯一材料”的循环。

**准备与重复探索：** 依照无可呼吸、低温环境验证装备，记录矿区和返航点。重复价值取决于产量、交通和加工成本；尚无实测依据判断它比其他矿区更高效。

**编排建议：** 可选“替代矿源调查”，通过采集与加工展示资源路线的选择，而不是再次重复整套火箭解锁任务。

**待验证：** 矿物实际产出、与既有材料的配方匹配、出行成本与补给条件。

依据：[Ganymede Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/ganymede/dimension/WorldProviderGanymede.java)、[矿物生成](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/ganymede/world/gen/BiomeDecoratorGanymede.java)。

### 11. 木卫四 Callisto：陨石坑、洞穴与冰资源

**身份与前置：** GS；维度 `-1022`；T4。

**地形与资源：** 有陨石坑、洞穴、冰与干冰相关地层。当前装饰器没有查到专属矿石生成，Provider 未定义常规地牢。

**探索价值：** 地形勘察、冰类采集和据点建设候选。已有实现较少时，应如实介绍它的景观与采集价值，不用一个虚构的稀有矿或 Boss 填补内容。

**进度关系：** 没有已确认的 T5/T6 核心板材独占前置。先归为可选探索；冰、干冰能否支持本机的具体加工设备，需要沿实际配方确认。

**准备与重复探索：** 氧气、温控、洞穴定位；长期据点另验供能与补给。不能把“存在冰”直接等同于“可以不带氧气罐”。

**编排建议：** 地貌/冰类样本、选址记录、可选建造目标。若没有可靠的自动检测方式，可以提供说明，暂不设强制完成条件。

**待验证：** 实际地层、冰类掉落与加工、洞穴内容和基地适用性。

依据：[Callisto Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/callisto/dimension/WorldProviderCallisto.java)、[当前装饰器](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/callisto/world/gen/BiomeDecoratorCallisto.java)。

## 五、GS T5：土星与天王星卫星

取得 T5 火箭是 T6 许可的制作前置，但这不等于必须逐一完成所有 T5 天体。当前已确认的核心材料链可以在更早目的地取得白云石和火山石。

### 12. 土卫二 Enceladus：冰原、山脉和裂谷

**身份与前置：** GS；维度 `-1017`；T5。当前全局 WE 开启，实际采用 WE 版本。

**资源与地形：** 冰质平原、山脉、裂谷、洞穴与煤矿生成；裂谷装饰还放置晶体，普通采集掉落未知晶体，是四级温控布的前置。Provider 未定义常规地牢。

**探索价值：** 冰地貌调查和极寒环境据点尝试。煤矿是已确认资源之一，但主世界也可取得煤，不应把它包装成后期独占材料。

**进度关系：** 当前没有查到必须先完成土卫二才能造 T6 的核心板材条件。适合把探索成就与制造进度分开。

**准备与重复探索：** 寒冷、缺氧、裂谷移动与供能；实际保温方案待验证。重复探索主要取决于采集和基地需求。

**编排建议：** 冰质卫星勘察、裂谷观察、补给站建造候选；不复制木卫二的全部任务。

**待验证：** WE 冰雪地形、煤矿、温控和基地供电。

依据：[Enceladus WE Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/enceladus/dimension/WorldProviderEnceladus_WE.java)。

### 13. 土卫六 Titan：氮气大气与宝石资源

**身份与前置：** GS；维度 `-1018`；T5。

**资源与结构：** 蓝宝石、绿宝石、钻石、煤、青金石、红石。Provider 未定义常规地牢；大气含氮，但可呼吸标记不是 true。

**探索价值：** 宝石与常用工业材料补给、不同大气环境的调查。它不是因为“有大气”就可以不带氧气，也没有已确认的下一阶火箭图纸关卡。

**进度关系：** 目前属于资源/环境探索分支。宝石在具体 GS 装备中的用途应对应实际配方，不泛称“所有高阶设备都需要 Titan 宝石”。

**准备与重复探索：** 呼吸、温控和气压/辐射实测；采集工具、储物与返航。是否适合长期采矿，要结合矿物密度和交通成本。

**编排建议：** 大气认识 → 宝石样本 → 具体用途支线；强调氮气不等于可呼吸氧气。

**待验证：** 宝石掉落、材料用途、气体相关设备是否可采集该大气、实际防护要求。

依据：[Titan Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/titan/dimension/WorldProviderTitan.java)、[矿物生成](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/titan/world/gen/BiomeDecoratorTitan.java)、[大气注册](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/SolarSystemBodies.java)。

### 14. 天卫五 Miranda：多矿种与复杂地层

**身份与前置：** GS；维度 `-1024`；T5。

**资源与地形：** 铁、白云石、钻石、石英、钴、镍，不同矿物以不同地层为生成母岩，另有岩浆湖实现。Provider 未定义常规地牢。

**探索价值：** 将多种后期常用资源集中调查，比较不同地层的采矿收益。不能承诺玩家随便挖一种石头就会得到全部矿物。

**进度关系：** 白云石可从 T3 谷神星取得，钴和镍当前也允许主世界生成，所以 Miranda 是后续补充来源，不能当作制造第一艘 T5 的唯一前置。

**准备与重复探索：** 氧气、温控、照明、不同地层的定位与矿样识别。建立矿区之前先确认实际产出和加工配方。

**编排建议：** 多地层资源勘察 → 钴/镍/白云石加工 → 可选材料补给基地。可用相同材料的替代来源关系避免人为强迫重复跑图。

**待验证：** 新区块矿脉、各矿物母岩、产出、岩浆风险和运输成本。

依据：[Miranda Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/miranda/dimension/WorldProviderMiranda.java)、[矿物及地层生成](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/miranda/world/gen/BiomeDecoratorMiranda.java)。

## 六、GS T6：外太阳系

### 15. 海卫一 Triton：极寒地貌探索

**身份与前置：** GS；维度 `-1021`；T6；当前采用 WE Provider。

**已确认内容：** 平原、山地与洞穴生成。当前 WE Provider 没有配置通用矿脉列表；没有标准地牢宝箱。

**探索价值：** 远日极寒环境、地形调查和探索记录。喷口等地表特征是否在本机正常生成、能采集什么，应继续核对，不能用现实海卫一的地质事实补成游戏机制。

**进度关系：** 没有已确认的独占火箭升级材料。可作为自由远征、长期生存挑战或据点候选；当前任务书不设任务奖励。

**准备与重复探索：** 验证足够的温控、氧气与供能；优先短途勘察，再决定是否建设基地。没有矿源证据时不宣称它是高效资源农场。

**编排建议：** 外太阳系测绘、地貌样本与极寒补给站候选。

**待验证：** 地表特征、可采集物、防护和能源条件。

依据：[Triton WE Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/moons/triton/dimenson/WorldProviderTriton_WE.java)。

### 16. 冥王星 Pluto（GS）：极寒地层，避免混用 EP 攻略

**身份与前置：** GS；维度 `-1008`；T6。首轮调查时 EP 冥王星与 T9 火箭组关闭；本条仍专指 GS 地表。

**已确认内容：** 独立的冥王星地表、地层和地形生成；当前矿物装饰器没有启用专属矿脉，Provider 未定义常规地牢。

**探索价值：** 外太阳系地貌、极寒探索和建造选址。现有实现不足以支持“来这里采钨造下一阶火箭”的叙述，那是把另一套模组的同名星球混了进来。

**进度关系：** GS 当前火箭发展到 T6；不能把它自动接成 EP T9/T10 的升级路线。

**准备与重复探索：** 长途补给、温控、能源与回程；重复价值主要取决于地表材料和建设需求，尚未确认专属核心资源。

**编排建议：** 首次抵达、地层调查、外太阳系据点。没有依据的 Boss 和稀有矿不要写入强制任务。

**待验证：** 实际地形、可采集地层、基地设备和返航。

依据：[GS Pluto Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/pluto/dimension/WorldProviderPluto.java)、[当前矿物装饰器](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/pluto/world/gen/BiomeDecoratorPluto.java)。

### 17. 柯伊伯带 Kuiper Belt：小天体与废弃基地

**身份与前置：** GS；维度 `-1009`；T6。首轮调查时 EP 同名开关关闭；后续临时测试不改变本条的 GS 来源。

**已确认内容：** 小行星式世界、特殊矿核、密实冰和废弃基地生成实现。与 GC 小行星带有相似的空间探索形式，但应按各自类和当前配置分别描述。

**探索价值：** 远方小天体调查、矿核采集与遗迹探索；可与连续地表的外太阳系目的地形成不同体验。

**进度关系：** 没有证据表明进入柯伊伯带就能绕过 Kepler-22b 的 T10 要求；它仍属于 GS T6 内容。

**准备与重复探索：** 稳定移动、定位、氧气、温控、材料运输与返回。当前需要专门检查零重力/漂浮体验，不能用一条普通地表的行走说明覆盖。

**编排建议：** 找到可停留小天体 → 调查矿核 → 可选废弃基地。遗迹奖励和矿核的具体加工用途验证后补齐。

**待验证：** 星图入口、着陆位置、矿核与遗迹生成、移动装备和返回流程。

依据：[Kuiper Belt Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/kuiperbelt/dimension/WorldProviderKuiperBelt.java)、[区块生成](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/kuiperbelt/dimension/ChunkProviderKuiperBelt.java)。

### 18. 妊神星 Haumea（GS）：冰类资源的极寒世界

**身份与前置：** GS；维度 `-1023`；T6；WE 版本。EP Haumea 为展示实现，不能算第二颗同名可登陆星球。

**资源与结构：** 氧冰、氮冰、干冰、铝矿与冰质地层；Provider 未定义常规地牢。

**探索价值：** 冰类资源与极寒地形调查。资源名称表明内容方向，但能否直接转换为可用氧气、氮气及产量多少，要由实际机器配方和运行结果证明。

**进度关系：** 属于 T6 探索和资源补充，当前没有确认首次解锁更高火箭的独占前置。

**准备与重复探索：** 供氧、温控、能源和采样；若要建设冰类加工基地，先验证采集方法与设备链，不能把有氧冰写成可以直接呼吸。

**编排建议：** 不同冰类识别 → 采样 → 经确认的加工实验；之后才决定是否安排稳定产气的建设任务。

**补漏：冰层方块与采集产物不同。** `BlockSurfaceIce` 的普通掉落使用 `GSItems.BASIC`，元数据为 `23 + 冰层元数据`，因此氧冰、氮冰、干冰对应 `gs_basic:23`、`:24`、`:27` 的晶体。源码回收配方分别列出氧流体 25、氮流体 20，干冰晶体则为水 50；这些是配方记录的流体数量，不能把干冰按名称自行写成已有二氧化碳回收路线。实际单位、供能、流体输出与设备衔接待交互验证，普通掉落也不替代精准采集规则。

**待验证：** 普通采集与特殊工具的实际掉落、回收机流体输出及后续设备、装备和基地运行。

补漏依据：[冰层掉落](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/blocks/BlockSurfaceIce.java)、[晶体元数据](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/items/ItemBasicGS.java)、[回收配方](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/recipes/CraftingRecipesOverworld.java)。

依据：[Haumea WE Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/haumea/dimension/WorldProviderHaumea_WE.java)。

## 七、GS T6：系外探索

**附加模组边界（2026-11-02）：** 下列 T6 为原 GS 实现与历史实例依据。新开发包 NewEarth 0.1.0 会在启动时将这四个已注册目的地调整为 T7，并通过 GS 装配机制作图纸、NASA 七阶页面组装的方式加入太阳系材料路线；开发包尚未部署或完成正常航行验收。具体构建与验证状态统一见 [物品线](../../NewEarth/items/README.md#实现状态与构建)，不把本节历史依据直接改写为新运行结果。

本机开启半人马座、巴纳德和鲸鱼座 τ 三组星系。以下四个世界有地表实现，注册等级均为 T6。这个等级匹配还不能证明普通引擎和当前 EP 星图能够完成跨星系往返。

### 19. 比邻星 b Proxima b：生态外观与呼吸规则的区别

**身份与前置：** GS；维度 `-1025`；T6。

**资源与地形：** 山地、冰原、海岸、海洋及植被相关生成；金、锡、铜、煤、硅、钻石等矿物。当前可呼吸标记是 false，含氧成分和有植被都不能替代这个规则。

**探索价值：** 第一次接触明显不同于荒芜卫星的系外生态，调查多种地貌和生物性资源；也有常见工业材料补给。

**基地价值：** 值得调查，但不能据此承诺“适合不带装备移民”。Provider 还调整太阳能倍率，具体表现与世界状态有关，不能照搬地球发电经验。

**进度关系：** 先核实系外到达与返航能力，再把它接入任务。无需把常见矿物都人为设为必须到此采集。

**补漏：当地材料已有回收关系。** `CraftingRecipesProximaB` 列有本地两类木材的回收配方、灰岩回收燧石、冰表层回收表层并输出水，以及表层方块元数据 0 回收 `gs_basic:6`。后者经 `ItemBasicGS` 确认为赤铁矿（`HEMATITE`），不是塑料或新增稀有矿；主世界配方有赤铁矿冶炼为两枚铁锭的实现。说明其资源并非只有直接挖矿，但这些产物不是比邻星独占升级材料。某个方块枚举或回收配方存在，不单独证明当前 WE 地形会生成所有对应输入。

补漏依据：[比邻星回收配方](../../GalaxySpace/src/main/java/galaxyspace/systems/ACentauriSystem/planets/proxima_b/recipes/CraftingRecipesProximaB.java)、[赤铁矿元数据](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/items/ItemBasicGS.java)、[赤铁矿冶炼](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/recipes/CraftingRecipesOverworld.java)。

**编排建议：** 跨星系准备 → 抵达 → 地貌与植物观察 → 采样 → 补给点候选。氧气提醒应明确放在出发与到达说明中。

**待验证：** 当前星图的跨系选择、发动机要求、太阳能表现、植物采集和补种、实际呼吸与温控。

依据：[半人马座天体注册](../../GalaxySpace/src/main/java/galaxyspace/systems/ACentauriSystem/ACentauriSystemBodies.java)、[Proxima b WE Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/ACentauriSystem/planets/proxima_b/dimension/WorldProviderProxima_B_WE.java)。

### 20. 巴纳德 C Barnarda C：生态丰富的长期探索候选

**身份与前置：** GS；维度 `-1030`；当前 T6。`survivalModeOnBarnarda=false`，因此不能引用源码另一分支的 T1 门槛。

**资源与生态：** 深海、海洋、海滩、平原、森林、河流、沼泽、丛林、山地、雪原和沙丘等。矿物表包含多种基础资源及硅、铝、钴、镍等，适合进一步整理采集与建造内容。

**补漏：树汁是已有的当地特色物品。** `BRItems.BASIC` 元数据 2 对应 `tree_juice`。`Barnarda_C_Logs.onBlockActivated` 在服务端检查特定发光紫木树干状态与手持斧，满足后将其换成普通紫木并生成树汁；当前 WE 丛林实现有放置这种发光树干的代码。判断使用具体方块状态，不能扩大成所有树木、所有朝向都能采汁，也不能承诺树汁会自动再生。`BRItems.oreDictRegistration` 还把它登记为 `slimeball`，不是已实现的飞船密封材料；任何新专用用途仍待开发。

依据：[树汁子项](../../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/core/prefab/items/ItemBasicBR.java)、[树干交互](../../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/planets/barnarda_c/blocks/Barnarda_C_Logs.java)、[WE 丛林生成](../../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/planets/barnarda_c/world/gen/we/Barnarda_C_Jungle.java)、[矿物词典登记](../../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/core/BRItems.java)。

**环境区别：** 注册的可呼吸标记为 true。这是当前几颗 GS 系外目的地中的明确区别；实际摘掉装备后的呼吸、温控和 EP 系统仍需一起验证。

**探索价值：** 不同生态区、资源和景观调查，可能支持基地、农业与长期居住内容。Provider 没有常规地牢，因此不应强行写成 Boss 闯关星球。

**进度关系：** 可作为远征成果或居住地候选，是否设为最终基地由后续玩法方向决定，不能仅因为内容丰富就预先指定为终点。

**编排建议：** 生态区调查 → 资源样本 → 补给试验 → 可选定居。长期任务应验证供能、食物和返航，不只检测“进入 -1030”。

**待验证：** 跨系抵达、实际可呼吸性、天气、植物与农业、矿物、发光树干生成及采汁交互、定居设备和返回地球。

依据：[巴纳德天体注册](../../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/BarnardsSystemBodies.java)、[Barnarda C WE Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/planets/barnarda_c/dimension/WorldProviderBarnarda_C_WE.java)。

### 21. 巴纳德 C1 Barnarda C1：冰雪、缟玛瑙与待验地牢

**身份与前置：** GS；维度 `-1031`；当前 T6。

**资源与地形：** 冰雪、山地、洞穴、冰类，铁、铜、锡和缟玛瑙矿。实际安装 JAR 的 Provider 含缟玛瑙矿引用，不只是仓库中未打包的设计。

**结构区别：** Provider 给出地牢间距 704、月球宝箱类型，并将 GC 月球式地牢生成器接入 onChunkProvider、onPopulate 和 recreateStructures。复用月球 Boss 房、Boss 刷怪器和一级宝箱，没有 C1 专属奖励池。这是实际生成接入的源码证据，结构、战斗、开箱仍需游戏验证；不能写成独有的 T7 图纸地牢。

**探索价值：** 冰雪系外卫星、缟玛瑙与结构调查。已补查 `CraftingRecipesBarnarda_C1.addCircuitFabricator`：以钻石、两份 `BRItems.BASIC:3`（缟玛瑙）、红石和 GC 部件制作 GS 现代晶圆。因此缟玛瑙已有电路制造用途，不是只有名字的收藏物；这是原有晶圆路线，不表示已实现新飞船导航部件。

依据：[缟玛瑙掉落](../../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/moons/barnarda_c1/blocks/Barnarda_C1_Blocks.java)、[C1 晶圆配方](../../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/moons/barnarda_c1/recipes/CraftingRecipesBarnarda_C1.java)。

**进度关系：** 与 Barnarda C 可形成不同环境的探索分支，不代表必须先完成 C 上的全部内容才可登陆 C1。

**编排建议：** 环境准备 → 冰雪勘察 → 缟玛瑙采样；地牢先列待验支线。

**待验证：** 冷热与呼吸、矿物加工、地牢结构完整性和实际奖池、跨系与母星/卫星选择。

依据：[Barnarda C1 WE Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/BarnardsSystem/moons/barnarda_c1/dimension/WorldProviderBarnarda_C1_WE.java)。

### 22. 鲸鱼座 τ f Tau Ceti f：水下探索主题

**身份与前置：** GS；维度 `-1338`；T6。

**地形与生态：** 海洋、水下地形、珊瑚和水草相关生成。含氧大气的可呼吸标记为 false；Provider 没有常规地牢。

**探索价值：** 水下环境、海底地貌和装饰/生态样本，与荒芜卫星形成差异。不能直接套用其他版本的深海 Boss、沉船或稀有矿物。

**准备重点：** 太空氧气系统与水下活动是两个需要同时核对的问题。GS 是否覆盖水下呼吸、哪些装备支持长时间下潜、怎样回到着陆点，都要实测，不能仅有满氧气罐就宣布完全准备好。

**补漏：珊瑚存在不等于能取得珊瑚物品。** 仓库 `TauCeti_F_Corals` 中普通掉落方法返回 `null`，`isShearable` 返回 false，`onSheared` 返回 null。由此不能承诺打碎或剪刀采集会得到可用于制造的珊瑚；创造模式选取也不证明生存可取得。该项是源码核对，没有完成当前发行 JAR 全方法比对或生存采集测试；也不据此断言其他水草、全部海洋资源都不可采集。

依据：[珊瑚掉落与剪切接口](../../GalaxySpace/src/main/java/galaxyspace/systems/TauCetiSystem/planets/tauceti_f/blocks/TauCeti_F_Corals.java)。

**进度关系：** 可作为系外海洋探索分支，目前没有已确认的火箭升级独占材料。

**编排建议：** 海面勘察 → 安全下潜 → 珊瑚/水草观察或采样 → 水下据点候选；采样前确认采集是否破坏目标、是否需要特殊工具。

**待验证：** 登陆位置、水面与水下呼吸、珊瑚和水草各自的生成与可采集方式、物品取得后是否有实际配方用途、温控、基地密封及回程。

依据：[Tau Ceti 注册](../../GalaxySpace/src/main/java/galaxyspace/systems/TauCetiSystem/TauCetiSystemBodies.java)、[Tau Ceti f WE Provider](../../GalaxySpace/src/main/java/galaxyspace/systems/TauCetiSystem/planets/tauceti_f/dimensions/WorldProviderTauCeti_F_WE.java)。

## 八、EP 相关地表：共存排查与按需保留

### 23. 火卫一 Phobos（EP）：先辨明入口，再决定内容

**身份与前置：** EP；维度 `-1502`；T2。与 GS `-1012` 同名但不是同一个世界。

**已确认内容：** EP 注册实现有独立 Provider 和维度。首轮配置保留该项、GS 兼容开关为 false；后续访问测试读到兼容开关 true，不能把首轮值继续写成当前正式方案。本条用于同名天体及生成器问题排查；按现在以 GC / GS 为主的方向，不默认将 EP 火卫一保留为正式探索目标。

**源码补充（2026-11-02）：** 已导入的 0.8.0 源码中，普通矿物开关控制铜、锡、铁生成器；砂砾与化石生成器则在构造函数中无条件创建。这些是 EP 自身实现，不能复制 GS 火卫一的矿物清单，也没有依据称为独占资源。

**历史运行问题：** 11 月 2 日 00:00:25 的本机日志记录 `BiomeDecoratorPhobos.java:29` → `WorldGenMinableMeta` 的空指针异常。源码第 29 行传入 `ExtraPlanets_Blocks.FOSSIL`；该字段只在 `MERCURY || CERES || PLUTO || ERIS` 时初始化，当次这四项均关闭。初始化分支和 Phobos 无条件引用已与实际 JAR 核对，能够解释当次异常。关闭 `GENERATE_ORES_PHOBOS` 也不跳过化石生成器。此项是历史问题及依赖依据，未修改代码，不断言后续临时全开配置仍有同样空引用。

**编排建议：** 主线优先采用 GS 火卫一，不同时发布两套同名抵达目标。EP 条目保留作兼容与裁剪依据；若后续明确保留，另核对来源、显示与绑定维度。

**待验证：** 若后续仍需保留或复现 EP 火卫一，先核对生成器依赖，再检查星图入口、名称、落地维度、返航与实际资源。原异常日志后续继续运行，不能将其直接描述为整个游戏崩溃，也不能以维度加载记录证明生成器正常。

依据：[卫星注册](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/moons/ExtraPlanets_Moons.java)、[Phobos 矿物生成器](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/moons/Phobos/worldgen/BiomeDecoratorPhobos.java)、[方块初始化](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/blocks/ExtraPlanets_Blocks.java)，并保留本机实际 JAR 的字节码核对依据。共存事项见[调查总览](planet-gameplay-audit.md)。

### 24. Kepler-22b（EP）：内容存在，正常生存路线尚未落实

**身份与前置：** EP；维度 `-22`；T10。

**已确认内容：** 有实际世界 Provider，包含资源型生物群系、材料树、材料球、建筑和糖果主题内容；相关生成开关大多开启。具体物品、生成密度、采集和加工仍需进一步调查。

**原有实现与历史缺口：** Kepler Provider 要求 T10；普通 T10 受 `ERIS` 控制，电动 T10 还要求 `ERIS && KEPLER22B && ELECTRIC_ROCKET`。首轮关闭 Eris 时据此判断路线有缺口；后续临时访问测试虽开启这些组，也不证明整条生存链已验收，更不代表要恢复 EP 整套发展。正式方向是将开普勒接到自制基础八阶之后，目标最低 T8，具体代码尚未适配。

**补漏：主要特色材料与现有加工。** `extraplanets:tier11_items` 元数据 0～4 依次为蓝、红、紫、黄、绿钻石，5 为铂锭，6 为压缩铂。已有冶炼配方把对应矿石变成这些物品；压缩配方使用六份铂锭得到一份压缩铂。材料树、材料球是生成实现，不能据此当作已验证的可再生种植系统。具体矿物密度、采集与加工仍需运行验证。

依据：[材料子项](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/planetAndMoonItems/ItemTier11Items.java)、[矿物生成](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/planets/Kepler22b/worldgen/BiomeDecoratorKepler22bOres.java)、[冶炼与压缩配方](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/recipes/ExtraPlanets_Recipes.java)。

**内容价值：** 已确定作为基础八阶之后的最终核心材料目的地；不能接成现有 GS T6 的直接下一站。作者原有地牢路线将 Kepler 列为可选、T10 图纸位于 Eris，是理解旧实现的背景，不是本项目必须恢复的路线。[EP 作者地牢与图纸说明](https://docs.mjrlegends.com/ExtraPlanets/dungeons/)

**接入边界：** 正式路线先制造基础八阶，再访问开普勒取得五色钻石与铂制品，制作最终核心；新核心不属于 EP 已有配方。新准入与正常往返未验证前，不把该阶段发布为已保证可完成的当前任务。

**待验证：** 按最终裁剪方案核对 Kepler 及其材料注册、基础八阶准入和返航、矿物与五色物品的对应、铂的冶炼压缩，以及原 EP 开关依赖。管理员访问不能代替正式生存路线验收。

依据：[Kepler Provider](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/planets/Kepler22b/WorldProviderKepler22b.java)、[行星等级注册](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/planets/ExtraPlanets_Planets.java)、[T10 物品注册](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/items/ExtraPlanets_Items.java)，并与本机实际 JAR 对照。详细条件见[调查总览](planet-gameplay-audit.md)，版本对应边界见 [EP 源码说明](extraplanets-source-notes.md)。

### 太阳系 EP 资源补充（2026-11-03，实现依据）

以下表格补充前述历史调查未覆盖的主要地表；等级均为原 EP，不代表整合后的门槛。只列特色资源，普通铁铜锡省略。主要依据：[矿物生成与天体实现](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/planets/)、[冶炼与压缩配方](../../ExtraPlanets/src/main/java/com/mjr/extraplanets/recipes/ExtraPlanets_Recipes.java)。

| 地表 | 原门槛 | 特色资源、地牢与当前设计关系 |
| --- | --- | --- |
| EP 水星 | T3 | 汞矿、碳矿、钾盐矿；岩浆怪 Boss 地牢。与 GS 水星并存，地牢拟给 GS 四阶图纸 |
| EP 谷神星 | T4 | 铀矿、化石、特殊流体与卫星塔；无专属 Boss 地牢。与 GS 谷神星并存 |
| EP 木星 | T4 | 钯、镍、红色宝石矿；火山岩可冶炼火山锭，另有灰烬与结构；火蝙蝠 Boss 地牢拟给 GS 五阶图纸 |
| EP 土星 | T5 | 镁矿、史莱姆相关地层与特殊流体；恶魂 Boss 地牢拟给 GS 六阶图纸，作为唯一来源 |
| EP 天王星 | T6 | 水晶、白色宝石矿，冰地形；冰史莱姆 Boss 地牢拟给特殊道具图纸，道具待定 |
| EP 海王星 | T7 | 锌、蓝色宝石矿、冻结氮；雪人 Boss 地牢拟给特殊制造台图纸，目标调整到六阶阶段 |
| EP 冥王星 | T8 | 钨矿、化石；太空人 Boss 地牢拟给七阶图纸，目标调整到六阶阶段 |
| EP Eris | T9 | 暗铁矿、化石；巨型僵尸 Boss 地牢拟给亚光速引擎图纸，目标要求 GS 六阶＋已安装离子引擎 |

上述 Boss 是 EP 特色 Boss 分支，配置可切换默认 Boss。宝箱特殊池、普通池、钥匙和图纸解锁须分开处理；不能把换一项奖励当成其他旧图纸已全部移除。

**材料辨别和加工：**

- EP 镁锭与 GS 镁锭均登记 `ingotMagnesium`；GS 镁板使用 `plateMagnesium`。两者原压缩步骤均使用两枚镁锭，但 EP 多个配方直接指定自身物品，不能据矿词断言全部自动互换。整合方向为土星镁矿输出 GS 镁、对应设备与板材配方统一；尚未修改。
- EP 镍与 GS 镍属于可统一的重复材料；EP 铀与 GS 铀体系也有重复，但具体掉落、加工和替换范围仍待核对。
- EP 火山岩／火山锭与 GS Io 火山石不同；EP 水晶／强化水晶与 GS 土卫二未知晶体不同；压缩碳也不是煤的现成别名。
- 水晶锭可制作工具护甲、压缩水晶及水晶块；两枚压缩水晶再制强化水晶。压缩水晶用于最终混合型高级太阳能板。白色宝石可制作工具护甲、方块及白色宝石晶圆；后者在已查配方中用于天王星空间站。原工具护甲用途不作为必须保留的理由，不采用的装备将退出生存获取。
- GS 妊神星冰晶体已有回收配方；柯伊伯带矿核是生成形态，使用既有矿石方块，不是可直接写进配方的一个新“矿核”物品。具体原料须追到实际掉落。
- EP 木卫四有干性油、页岩油地层；不采用该地表时是否迁移或替换用途待定。EP 其他未选卫星不默认启用。

### EP 原有制造设备与配方（2026-11-03）

EP 火箭沿用 GC NASA 工作台与图纸页面，没有逐阶独立火箭工作台。下列配方来自[实际配方 JSON](../../ExtraPlanets/src/main/resources/assets/extraplanets/recipes/)；为原 EP 数量，不是新整合配方，是否加载还受配方条件控制。

| 加工设备 | 原制造材料合计 | 主要处理关系 |
| --- | --- | --- |
| 基础粉碎机 | 致密汞板4、铁砧2、活塞3 | 钾盐矿→钾盐碎片等 |
| 基础太阳能蒸发室 | GC 基础太阳能板4、致密汞板3、水星石砖2 | 钾盐碎片→钾 |
| 基础结晶器 | 活塞4、压缩碳3、水星石砖2 | 结晶水→碘盐 |
| 基础逆结晶器 | 碘盐4、压缩碳2、水星地牢砖2、基础结晶器1 | 碘盐→结晶水 |
| 基础化学注入器 | 活塞3、铀锭6；另有钾6替代铀的条件配方 | EP 化学加工 |
| 基础净化器 | 过滤器4、线网2、压缩碳2、GC 基础太阳能板1 | 污染水／放射性水净化 |
| 基础浓缩机 | 过滤器4、巨锤2、压缩碳2、压缩钢1 | 特定流体浓缩成物品 |
| 基础辐射消除装置 | 汞电池2、青金石块2、碘盐2、火山锭2、过滤器1 | 减少玩家辐射累积 |

| 补给设备 | 原制造材料合计 |
| --- | --- |
| 高级精炼机 | GC 精炼机4、钯锭5 |
| 终极精炼机 | 高级精炼机4、锌锭5；关闭前级时有直接使用 GC 机器的条件配方 |
| 高级氧气压缩／解压机 | 对应 GC 机器4、镁锭5 |
| 终极氧气压缩／解压机 | 对应高级机器4、钨锭5；另有直接使用 GC 机器的条件配方 |
| 混合型高级太阳能板 | GC 高级太阳能板4、压缩镁5 |
| 最终混合型高级太阳能板 | 前级太阳能板4、压缩水晶5 |
| 高级燃料装载机 | GC 燃料装载机1、镍锭5、钯锭2、红色宝石1 |
| 终极燃料装载机 | 高级装载机1、钨锭5、锌锭2、蓝色宝石1；另有直接使用 GC 装载机的条件配方 |
| 漫游车充能器 | 戴斯锭6、GC 电池3 |

这些机器不等于现成的通用合金制造台。是否复用它们加工新七阶材料，要核对实际输入槽、流体和配方机制；特殊制造台的形态和加工配方未定。

### GS 防护与改装依赖补充（2026-11-03）

- 三级温控布：白云石粉4、满液氮罐1、GS 火山石2、GC 金星温控布2；四级温控布：太阳能粉尘4、冰桶1、未知晶体2、三级温控布2。再按护甲形状合成各级四件套。配方见[主世界制造](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/planets/overworld/recipes/CraftingRecipesOverworld.java)。
- GS 土卫二裂谷实际放置晶体，普通掉落为未知晶体；木卫二两处同类生成段落被注释，不能作为备用矿源。保留并迁移土卫二可以保住原温控材料链。冰桶由空桶对普通冰使用取得，见[事件实现](../../GalaxySpace/src/main/java/galaxyspace/core/events/GSEventHandler.java)。
- GS 三级／四级温控强度为3／4；EP 三级至五级为50／100／150。EP 土星和海王星在高阶温控选项下使用量级明显不同的温度修正；EP 海卫一还直接返回极寒值。降低火箭门槛或关闭一项选项不等于完整环境适配。参数不是摄氏度，也不能直接用装备强度相减判定安全。
- 用户指定备份 `client-update-backups/20261102-061059-b1bb1581/config/ExtraPlanets.cfg` 已把 GS 普通与轻型宇航服列为 EP 一级防护兼容装备，但不代表免疫辐射或温度。EP 气压、辐射处理独立于 GS 事件；前述 GS 普通玩家事件取消异常不能当成统一防护规则。
- 改装台可给原 GS 四／五阶装离子引擎、六阶装亚光速引擎；拆卸会返还模块。离子与亚光速原来都进入 Io 的五级特殊奖励池；新方案仅留离子，引擎逐代制造升级。六阶离子改装与新准入尚未实现。
- 重型宇航服每件2个改装位，轻型每件1个。重型胸甲可装喷气背包；轻型列表没有该模块。头盔有夜视、探测、水下呼吸、自动进食等，部位和冲突受模块限制。氧气罐有 EPP 模块，气体提取器有升级电池；等离子工具可装列表为空。原模块不等于已完成 EP 环境兼容。

## 九、空间站与展示天体

### 1. 轨道空间站应作为独立建设玩法

| 内容 | 当前文件依据 | 适合调查的玩法 |
| --- | --- | --- |
| 地球轨道空间站 | GC 有注册实现 | 轨道基地、供能、供氧、建筑与地球往返 |
| 火星轨道空间站 | 首轮配置 GS 开启、EP 同类关闭；最终共存裁剪待定 | 火星阶段的轨道补给和建设 |
| 金星轨道空间站 | 首轮配置 GS 开启、EP 同类关闭；最终共存裁剪待定 | 金星阶段的轨道补给和建设 |
| EP 其他空间站 | 部分开关仍开启，但受母星与注册条件影响 | 先核对是否实际可创建，再决定内容 |

创建空间站需要的材料、权限、星图操作、实际维度和返回路径都应单独记录。Provider 类型 ID 不是某个存档中玩家创建的空间站维度 ID；不能把 `-27` 等类型号直接复制进 FTB 到达任务。

建设内容可以与星球采矿并行。要验收“建立可用空间站”，至少要验证供能、供氧、物资和往返；只进入一次维度不能证明这些条件。

依据：[GS 空间站注册](../../GalaxySpace/src/main/java/galaxyspace/systems/SolarSystem/SolarSystemBodies.java)、[GC 世界与空间站工具](../../Galacticraft-Legacy/src/main/java/micdoodle8/mods/galacticraft/core/util/WorldUtil.java)。

### 2. 展示天体用于理解星系，不安排登陆任务

以下是原 GS／历史配置的展示边界；2026-11-03 方案选择启用多颗 EP 同名地表，见物品线路线手稿，不据此改变原 GS 母星只有展示实现的事实。

- GS 木星、土星、天王星、海王星本体没有地表 Provider；首轮 EP 对应实现关闭。后续访问测试开启这些 EP 天体，不会使它们变成 GS 地表，也不代表正式保留。
- GS 火卫二、部分外行星卫星，以及部分系外天体仅展示；开启对应名称的开关不会自动增加地表。
- Barnarda C2 与测试星的地表受调试分支限制，本机 debug=false。
- EP Makemake、部分 Kepler 系统天体属于展示内容；EP Haumea 也不是第二套地表实现。

它们可以出现在星图介绍或已有目标的说明中。当前内容方向不增加只为阅读而勾选的任务，也不把展示天体描述为已经实现的远征目的地。

完整名单见[星图展示与未开放内容](planet-gameplay-audit.md#星图展示与未开放内容)。

## 十、把资源和制造串成实际进度

### 1. GC 前半段与 GS 后半段采用不同方式

GC T1～T3 的图纸、NASA 工作台、矿物与火箭材料组成基础发展。GS T4～T6 在当前 `enableAdvancedRocketCraft=false` 下，主要是材料加工、部件制造和火箭许可。

```text
地球后勤、GC T1
  → 月球：基础探索、地牢与 T2 图纸来源
  → GC T2、火星：戴斯、地牢与 T3 图纸来源
  → GC T3：小行星带材料；水星镁；谷神星白云石
  → GS T4 制造：HDP:0、部件、T4 许可、火箭装配
      ├─ Io：火山石；地牢与引擎模块
      ├─ Europa：冰地貌、硅铝等补给
      ├─ Ganymede：镁、钛铁矿替代来源
      └─ Callisto：洞穴、冰类与地形
  → GS T5 制造：HDP:1、部件、T5 许可、火箭装配
      ├─ Enceladus：冰地貌与煤
      ├─ Titan：宝石与常用资源
      └─ Miranda：多矿种与地层
  → GS T6 制造：HDP:2、部件、T6 许可、火箭装配
      ├─ 外太阳系探索
      └─ 系外目的地：另验引擎、星图和跨系往返

Kepler-22b：原 EP T10 与后期基础八阶接入规划分开，不能当成现有 GS T6 直接后继
```

这张图表示已查关系，不是强制完成顺序，也不是完整生存流程验收。

### 2. 核心材料链

| 结果 | 已查到的主要输入/工序 | 探索关联 |
| --- | --- | --- |
| SDHD120（`compressed_plates:4`） | 青铜板、煤板、铝板；装配机 | 基础材料加工 |
| 镁板 | 镁锭压缩配方 | 首次来源可查 T3 水星；T4 木卫三可补充 |
| HDP:0 | 镁板 + SDHD120 + GC T3 强化板 `basicItem:5` | GS T4 材料基础 |
| 白云石粉 | 白云石晶体经回收机加工；1 → 6 | T3 谷神星；T5 Miranda 可补充 |
| HDP:1 | 钴板 + HDP:0 + 镍板 + 白云石粉 | GS T5 材料基础；主世界钴镍生成开启 |
| GS 火山石 | Io 火山矿掉落 `gs_basic:12`；Io 普通宝箱表也列入该物品 | T4 Io，具体生成与随机掉落待验 |
| HDP:2 | SDHD120 + HDP:1 + GS 火山石 | GS T6 材料基础 |
| T3 温控布 | 白云石粉、液氮罐、GS 火山石、金星物品等 | 与外出装备相关，需单独验证完整制作和防护 |
| 空白图纸 | 装配机中使用压缩陨铁、染料与纸 | `enableDuplicateSchematic=true`，该项前置没有关闭 |

这部分已排除了几个明显的“把后期目的地当作首次升级唯一来源”的错误，但没有穷尽所有机器、物品和完整数量。镁、钴、镍的矿石冶炼与板材压缩、回收机供能、各部件制造都要以游戏内配方复核完成闭环。

### 3. 火箭许可与制造设备

| 目标 | 许可合成 | 制造流程 |
| --- | --- | --- |
| GS T4 | 空白图纸 + GC T3 完整火箭 → `rocket_modules:8` | HDP:0 部件 → 火箭装配机 |
| GS T5 | 空白图纸 + GS T4 完整火箭 → `rocket_modules:9` | HDP:1 部件 → 火箭装配机 |
| GS T6 | 空白图纸 + GS T5 完整火箭 → `rocket_modules:10` | HDP:2 部件 → 火箭装配机 |

普通零货舱配方使用头锥 ×1、箭体 ×2、引擎 ×1、助推器 ×2、稳定翼 ×2、对应许可 ×1；另有添加箱子的货舱版本。

许可将前一级完整火箭作为输入，源码未发现火箭作为容器返还的处理。现阶段应按会消耗来规划，并在安全基地办理升级。实际是否保留 NBT、燃料、货舱差异及旧火箭消耗，需要运行确认；不要写成免费复制或必定保留前级火箭。

装配机与火箭装配机是两种设备。火箭部件加工、最终组装、图纸页面解锁也不是同一个动作；向导中应明确告诉玩家当前需要打开哪个设备。

### 4. 地牢等级不等于火箭门槛

| 内容 | 登陆等级 | 特殊奖励/宝箱关联 | 应如何解释 |
| --- | --- | --- | --- |
| 月球 | T1 | GC 一级特殊池 | 图纸与初期探索 |
| 火星 | T2 | GC 二级特殊池 | T3 等图纸来源 |
| GS 谷神星 | T3 | GS 等级 4 池、`dungeon_tier_4` 普通表 | 工具与独立挑战，不是必须 T4 才能进入 |
| GS Io | T4 | GS 等级 5 池、`dungeon_tier_5` 普通表 | 引擎模块等，不是 T5 火箭许可领取 |
| GS Barnarda C1 | T6 | 月球式生成器与月球宝箱类型 | 待验复用内容，不代表独有 T7 奖励 |

Boss 的特殊奖励池与宝箱 JSON 普通池分别记录，不能将其中任意一项写成每箱保底。源码注释中的旧奖励注册也不能当作当前有效奖励。

## 十一、交给 FTB 与向导文本线程的使用规则

本节提供资料使用方式，不直接制定最终任务线，也不代写 RPG 文案。

### 1. 每颗目的地的内容可以分为不同目标

| 目标 | 适合说明的内容 | 可考虑的检测方式与限制 |
| --- | --- | --- |
| 出发准备 | 为什么带氧气、温控、返航物资 | 物品检测或说明；当前不另加阅读勾选任务，拥有物品不证明已穿戴、已充气或已加注 |
| 到达 | 当前具体维度和目标身份 | `DimensionTask`；同名天体必须不同 ID |
| 采样 | 矿物、地层、冰类、生态样本 | `ItemTask`；物品可从其他地方带入，不能自动证明当地采集 |
| 加工 | 从矿物到锭、板材、粉末和部件 | 物品/制作相关检测；机器加工事件是否覆盖需验证 |
| 战斗 | 经确认存在的 Boss | `KillTask`；核实实体注册 ID、计数和击杀归属 |
| 环境与景观观察 | 氧气差异、裂谷、生态和海洋 | 优先放在抵达目标的短说明中；群系或位置检测按需求核实，不为阅读增加勾选目标 |
| 建立据点 | 可供电、供氧、补给与返航的设施 | 单一到达或物品目标不足以证明据点可用，复杂运行状态可能需额外适配 |
| 返回 | 带回样本、安全结束远征 | 回到主世界和样本可分开检测；任务顺序与团队共享需专门设计 |

源码确认当前版本有物品、维度、击杀、地点、群系、勾选、观察、交互、进度等任务类型。类型存在不等于所有复杂玩法都能直接自动检测。

`DimensionTask` 按玩家当前 `dimension` 判断，约每 20 tick 尝试提交，并排除旁观模式；它不会检查玩家用了什么火箭，也不会证明完成返航、当地采矿或正常生存路线。

`ItemTask` 有是否消耗、元数据和 NBT 匹配等配置。GS 多个关键物品复用相同物品 ID、依靠元数据区分；随意忽略元数据可能把火山石、空白图纸或不同许可混在一起。火箭的货舱和 NBT 匹配也需按目标单独处理。

依据：[任务类型](../../FTB-Quests/src/main/java/com/feed_the_beast/ftbquests/quest/task/FTBQuestsTasks.java)、[维度任务](../../FTB-Quests/src/main/java/com/feed_the_beast/ftbquests/quest/task/DimensionTask.java)、[物品任务](../../FTB-Quests/src/main/java/com/feed_the_beast/ftbquests/quest/task/ItemTask.java)。

### 2. 区分必要制造、资源选择和自由探索

- **必要制造关系：** 已查的许可、板材与部件依赖，适合明确说明前置。
- **资源路线选择：** 同一种材料存在多个来源时，说明可选去处；不要把每处都设置为强制前置。
- **自由探索：** 生态、景观、遗迹、基地和非必要资源，让玩家知道值得做什么；当前不配置任务奖励，原模组的地牢战利品单独说明。
- **暂缓发布：** 未落实生存路线的 Kepler、未确认展示/入口的同名目标、未经验证的防护承诺和具体救援操作。

### 3. 文案需要避免的事实错误

1. 把所有星图天体都写成可以登陆。
2. 把 GS 和 EP 的同名地表、矿物、Boss、图纸混写。
3. 把每一颗星球都写成“打 Boss，获得下一阶火箭”。
4. 把注册等级当作完整到达能力，尤其 GS T6 系外飞行与 EP T10 Kepler。
5. 把含氧大气、绿色植物、海洋与可直接呼吸等同。
6. 用真实天文学替代游戏注册的温度、地形或资源。
7. 在火箭升级时承诺保留旧火箭、燃料或货物。
8. 把普通物品检测说成自动验收装备有效、基地工作或当地采集。
9. 把临时救援物品描述成任何情况下都能安全返航。
10. 把尚未运行验证的内容写成“已测试可用”。

## 十二、剩余验证及记录格式

### 1. 首次启动：先解决内容是否实际生效

- [ ] 核实本次启动实例、配置和模组版本；保留新日志的时间与注册证据。
- [ ] 检查维度注册、新错误和缺少依赖；旧日志不能作为本次结论。
- [ ] 逐项记录星图中的可见、可选、可登陆状态，特别是两套 Phobos 和 Kepler。
- [ ] 核对只展示的母星、卫星及调试天体，避免误算目的地。
- [ ] 验证当前星图、GS 引擎与跨星系选择的组合。

### 2. 关键流程：先抽查会卡住发展的节点

| 抽查 | 要证明什么 | 尚不能由此证明什么 |
| --- | --- | --- |
| 镁矿 → 镁板 → HDP:0 | T3 阶段有可用镁源和材料加工 | 整艘 T4 所有部件均可制成 |
| 谷神星晶体 → 回收机 → 白云石粉 → HDP:1 | T5 核心板材不会被错误后置 | 所有机器框架、电路和部件都已经验收 |
| Io 火山矿 → 正确火山石 → HDP:2 | T6 材料能在 T4 目的地取得 | 已经解决 T6 跨系与环境装备 |
| 空白图纸与三级许可 | 许可存在、输入匹配和旧火箭消耗 | 最终火箭能飞行并返航 |
| 装配机部件 → 火箭装配机 | 当前 JAR 中部件和整箭配方有效 | 所有目标维度的着陆适配都正常 |
| 普通生存玩家与防护对照 | 当前环境是否造成效果、装备是否改变它 | 其他星球、另一套环境系统也同样有效 |
| 地牢与奖励 | 结构可生成、Boss 与宝箱工作、奖池可用 | 随机奖励每次都保证出现 |

创造模式或管理员传送可以帮助确认世界和配方存在，但验证“正常生存可达”“装备有效”“完整制作链”需要相应的生存条件，分别记录。

### 3. 每次实测的最小记录

```text
目标 / 模组 / 维度：
实例和启动时间：
配置或 JAR 是否变化：
存档、区域是否新生成：
模式、火箭与引擎：
装备、氧气、燃料和能源：
执行步骤：
观察到的结果：
是否完成返航：
日志、截图或配方证据：
可确认的结论：
仍未证明的事项：
```

### 4. 当前交付状态

11 月 1 日已完成详细资料整理、关键代码复核和若干错误路线排除。11 月 2 日补充 EP 源码快照与新日志中的 Phobos 异常；此前“实际启动尚未完成”不再适用于最新状态。此次源码导入没有修改游戏配置、替换模组或修复生成器；正常飞行、配方交互、地形、生存与返航验收仍未完成。

后续实测结果应回填到对应目的地和流程条目，而不是把本次静态调查整体改名为“已验证”。

## 十三、2026-11-02 运行日志补记

下表保留此前只读核对的历史会话：当时 `logs/latest.log` 最后写入时间为 2026-11-02 02:29:37、大小 400707 字节。后续文件已被新启动覆盖，以下是固定的历史观察，不能继续称为当前最新日志，也不能据此推定当前任务数量、MixinBooter 版本或正式配置。

| 时间与证据 | 可以确认 | 尚不能确认 |
| --- | --- | --- |
| 01:54:04、02:14:11，FTB 日志两次读取 `config/ftbquests/normal`，均为 `Loaded 3 chapters, 3 quests, 3 tasks and 0 rewards` | 三节点小样被读入；两个原测试章节也仍在索引中 | 完整七章已部署，或物品、维度条件已正确完成 |
| 同两次记录 `Shared quest team: singleplayer` | 当前存档复用了该团队，未一律新建 `ftb_public` | 双人自动归队、进度同步、服务器重启后的保存全部通过 |
| 联机握手列表记录 FTB Quests 1202.9.3、FTB Library 5.4.7.2、MixinBooter 12.2 | 本机会话确实加载这些版本 | 历史使用 MixinBooter 10.7 的专项探针已在 12.2 上重跑 |
| 02:14:11 加载月球维度 -28，玩家登录该维度；02:29:37 保存并退出 | 该存档中的月球世界可以加载 | 从地球正常发射、月球着陆、装备有效、完整返航已验收 |
| 01:41:10 多个 `WorldProvider type` 查询失败，包括 -1012、-1023、-1234、-1032 及若干 EP ID | 本会话仍有 Provider 查询错误，需结合上下文逐项定位 | 所有报错都是 ID 冲突，或所有对应目的地均不可到达 |

上述是日志观察，不是亲自操作的玩法验收；已加载旧存档不能证明新区块生成与配方完整可用。此前 00:00:25 会话的 EP Phobos 空指针及源码依赖见[对应条目](#23-火卫一-phobosep先辨明入口再决定内容)，本轮没有修复，不因后续会话能进入世界就移除该已知问题。

### 当前实现与新地球规划

本文仍按实际注册的 GS 系外 T6 与 EP Kepler T10 说明现状。[NewEarth 物品线](../../NewEarth/items/README.md)规划为七阶复用 EP、常规系外最低 T7；基础八阶多人飞船造好后前往最低 T8 的开普勒，取得材料、制作并安装最终能源核心，才能前往[终章新维度](../../NewEarth/dimension/README.md)。均为未实现的目标规则，不能据此认定当前已可制造、航行或升级；终章 H01 暂不部署。


## 十四、补漏结论与已确定的开发方向

### 1. 这份 guide 提供什么

已有星球资源、取得限制与加工依据集中查本指南；NewEarth 文档只维护开发方向，不重复列另一套资源表或预定具体用法。以上补漏属于实现核对，未补造游戏内验证结论。

本次重点补齐：比邻星回收与赤铁矿关系、巴纳德 C 树汁、C1 缟玛瑙晶圆、鲸鱼座 τ f 珊瑚采集限制、冰层晶体掉落与回收，以及开普勒五色和铂的实际物品对应。

### 2. 以 GC / GS 为基础的深度整合

最新星球组合、图纸分配与制造路线见[物品线路线手稿](../../NewEarth/items/route-manuscript.md)。保留选定 EP 地表、独立资源和地牢，以 GS 制造与防护为基础重新设计配方；不用的 EP 装备移除生存获取并隐藏。双水星、双谷神星是有意保留，不一律去重。七阶旧材料链不再约束选材；其余未选地表不默认启用，临时全开配置不是发布方案。

裁剪还要核对隐含依赖：EP 七阶物品由 `URANUS` 分支初始化、注册；Kepler 材料组受 `KEPLER22B && KEPLER_SOLAR_SYSTEMS` 控制；Phobos 的化石引用又依赖部分主星。不能只关天体就假定火箭、材料与其他生成器仍完整。需通过后续适配保留所需内容，具体提取或兼容方案由物品线决定，本指南不代改配置或代码。最新方向已纳入 EP 天王星，但仍需审查其与七阶物品的绑定；开启水星、冥王星或 Eris 可满足化石初始化条件，不能据此推定全部裁剪依赖已经解决。

### 3. 已确定的后期路线

| 内容 | 设计要求，均待实现 |
| --- | --- |
| 七阶 | 复用 EP 火箭，制造条件接太阳系末段；常规系外目标最低 T7 |
| 八阶 | 独立装配台制造全新多人宇宙飞船；基础动力可前往开普勒；不依赖后置的开普勒材料 |
| 开普勒 | 基础八阶造好后访问，目标准入 T8；收集蓝、红、紫、黄、绿五种钻石与铂制品 |
| 最终能源核心 | 五色全部加铂制品组合制作；用量、铂制品形式与配方布局待定；安装后获得终章资格，飞船仍是八阶 |
| 终章 | 独立拟地球行星，要求实际八阶飞船与有效已安装核心；同船乘员共同到达和返航 |
| 结尾 | 在终章按主世界正常多人睡眠规则成功过夜后触发共同演出；不按个人首次躺床分别触发，不另设集合、参与名单或投票 |

终章按角色视角保留意外发现，朋友现实中知道计划不影响叙事。模型、特效、座位数、船内移动与演出文本等未定细节，不由资源调查自行推导。

### 4. 各系外目的地的价值与工具

每个可登陆的系外目的地都应有可利用的特色材料，并有相应专用工具或采集、处理能力支撑当地玩法。优先利用 guide 已查的原有资源；不要求每颗都新增一套矿石、工具或装备，也不在此提前规定具体用法和部件配方。

这是开发要求，不是已经满足的现状：尤其比邻星的特色选材与新用途仍需确认，鲸鱼座 τ f 的生存采集链需补核；巴纳德树汁的具体状态交互同样需要实测。资源存在、可取得、能加工和有实际用途需要分别成立，前置不能循环。

开发基调与分工见 [NewEarth 总览](../../NewEarth/README.md)、[物品线](../../NewEarth/items/README.md)及[维度线](../../NewEarth/dimension/README.md)。FTB 后续按实际资源、工具和成品补充轻量内容即可，不作为物品线前置；团队目标完成不代替核心安装、航行或成功过夜判定。
