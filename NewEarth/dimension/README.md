# 终章新维度

维护分支：[main](https://github.com/Neosamzzz/Minecraft/tree/main)。核对日期：2026-11-02。

新维度、终章体验与正常过夜后的结尾触发独立推进；多人宇宙飞船、最终能源核心及现有系外目的地的专用工具由[物品线](../items/README.md)维护。整体关系见[支线总览](../README.md)。当前仅记录设计，维度与演出尚未实现。

## 体验方向

- 新增独立的拟地球行星，与现有 GS 系外行星和 EP 开普勒分开。
- 近处有熟悉的花草、河岸与地表细节，远处有更宏伟的森林、山脉和大陆，重视近景熟悉、远景辽阔的感受。
- 呼应大部分陆地被海洋吞没的地球开篇。角色视角按不知道最终目的地存在来写，不提前承担“寻找新地球”的使命；朋友实际知道终章计划不妨碍这个叙事视角。
- 抵达时先呈现景色和意外发现，之后由玩家选择探索、建设或定居。
- 保留现有生态星球的森林、海洋和价值，不将它们改写为寻找家园失败的荒地。

“巨型拟地球”描述地貌尺度与景观方向，不据此直接规定球体尺寸、地图边界、具体树种、呼吸条件或生成算法。

## 内容边界

景观优先复用原版、GC、GS、EP 已有的方块、生态和素材，不为终章配套扩展新的矿物、装备或通用工业体系。新维度仍需独立研究生成与呈现，但不承担新火箭物品和制造台的开发。

维度线可独立研究与验证地形和世界行为，不以七、九、十阶载具已经完成为起点。维度 ID、星图位置、环境参数与生成方式尚未确定。

## 地形、光影与远景路线

2026-11-02 调查结论：保留 Minecraft 1.12.2 与 GC / GS 玩法，光影是景观目标的重要组成部分，以 Complementary Reimagined r5.9.3 为优先验证对象，使用 DH 承担远景。优先保证终章星球的呈现，其他星球保留原有风格；是否能按维度切换光影与 DH、切换后能否正确恢复，尚待验证，不能据此假定其他维度不受客户端渲染器影响。

地形优先研究现有 1.12.2 模组或生成器的复用方式，包括能否用于独立维度；不将自行编写生成算法或移植 FreeTerraForged 作为当前前置。参考图用于确定景观尺度与气氛，不锁定具体树种、粉色森林或某个模组。

| 已查到的依据 | 判断与边界 |
| --- | --- |
| 用户日志中 DH 3.3.3 与 OptiFine 光影组合在渲染矩阵捕获处崩溃；移除 DH 后光影正常，换光影仍出现同类异常 | 不再以更换 OptiFine G5 / G6 或不断更换光影作为主要排障路线；[DH #1328](https://gitlab.com/distant-horizons-team/distant-horizons/-/work_items/1328#note_4127429824) 的维护者明确说明 1.12.2 不支持 OptiFine 光影，建议 Actinium / Demonica |
| [Actinium 的 DH 记录](https://github.com/Q-Engineering-Source/Actinium/blob/main/docs/compat/dh.md)包含 DH 3.3.0 与多款光影的实机回归；[兼容矩阵](https://github.com/Q-Engineering-Source/Actinium/blob/main/docs/compatibility-matrix.md)列有 Reimagined r5.9.3 与 DH LOD | Cleanroom + 现代 Java + Actinium + DH 是有实测依据的优先验证路线；记录涉及不同提交，不能视为任意版本组合都通过 |
| [第三方补丁项目](https://github.com/Marsietiss/celeritas-littletiles-bridge)记录官方 DH 3.3.3、修改过的 Demonica 0.7.3 与 Reimagined r5.9.3 HIGH 实测 | Demonica 可作为备选研究对象；该记录依赖补丁及指定 Celeritas，不代表官方 Demonica 任意版本直接可用，也不因此给本项目加入 LittleTiles 补丁 |
| [Actinium 的 GalaxySpace 记录](https://github.com/Q-Engineering-Source/Actinium/blob/main/docs/compat/galaxyspace.md)有 GC / GS / AsmodeusCore 共存与天空修复的用户确认 | 该次修复验证的是无光影天空，不能推导整套 GC / GS + 光影 + DH 已验证 |

优先使用 Actinium 文档推荐的 Cleanroom 0.6.13-alpha；Cleanroom 该版本的发布包要求 Java 25 / 26，不能只按 Actinium 字节码的 Java 21 下限选择运行时。Actinium 当前说明排除 Cleanroom 0.7.0 及以上。实际搭建前重新核实发布包、DH 接口和所需版本，固定一次测试组合；不把升级 Java 本身当作解决渲染冲突的措施。

区块预生成与 DH LOD 构建分别评估：已有区块仍需转换成远景数据，不能以预生成完成判定 DH 缓存就绪，也不能按高版本 DH SeedGen 的速度承诺 1.12.2 表现。此前工具名称曾混淆；用户日志里的工具是 Chunk-Pregenerator，不能将其问题直接归到 Chunky。

## 阶段与验证进度

| 阶段 | 范围与完成标志 | 当前状态 |
| --- | --- | --- |
| 一、独立版本验证 | 独立 1.12.2 实例、普通主世界；固定加载器、Java、Actinium、DH 与 Reimagined，验证近远景、水、天空、阴影及退出重进 | 已通过本轮普通世界画面与重进验收，保留已知提示 |
| 二、现有模组共存 | 在阶段一组合中加入 GC / GS 及整合包依赖，先在普通主世界确认启动、渲染和保存 | 本轮启动、光影画面、保存与完整重启重进通过；未验收跨星球及完整玩法 |
| 三、地形方案验证 | 用独立测试世界比较少量现有生成方案的尺度、景观、DH 表现与生成成本，选定能接入独立维度的一种 | 未开始 |
| 四、终章实现与对接 | 接入地形、环境、正常睡眠与演出，并与物品线对接飞船、有效核心、星图、发现流程和 FTB 抵达记录 | 未开始；可独立推进世界实现，不以飞船完成为前置 |
| 五、维度与完整旅程验收 | 最后实测星球往返、按维度呈现、缓存恢复、多人过夜、演出及重启后的状态 | 未开始 |

前期版本与地形验证使用普通主世界，不提前制作终章维度；阶段一通过不等于自定义维度或整合包通过。阶段结果持续更新在对应位置，被替换的版本与结论及时修订，历史由 Git 与 Issue #8 保留。生成文件沿用[项目目录规范](../../docs/project-layout.md)，不为每轮尝试新增报告或永久脚本。

### 阶段一实测记录

2026-11-02 开始，测试环境为 Windows 11、RTX 3060 Laptop、NVIDIA 610.88。独立实例放在 Windows 项目的 `.local/newearth-render/`，日志位于 `build/checks/newearth/render/`；复用现有 Java 25.0.1 运行时，仅由测试启动入口显式指定，原有 Java 8、启动器配置、正式客户端和服务器保持原样。可复用入口为该目录的 `launch.cmd`，测试存档为 `New WorldRenderTest`；首轮、重进日志与最近画面分别保留为检查目录下的 `actinium-console.log`、`reentry.log`、`reimagined.png`。

- 已完成：下载并校验 Cleanroom 0.6.13-alpha 的 100 项类库和 1305 项资源；无额外模组时成功进入、保存并退出普通主世界，日志确认 NVIDIA OpenGL 4.6。
- 本轮组合：Cleanroom 0.6.13-alpha、Java 25.0.1、Actinium alpha-0.0.13、官方 DH 3.3.3、Reimagined r5.9.3 HIGH。Actinium、DH 与光影包已按发布元数据校验哈希；本轮未加入 GC / GS、Fugue 或 Scalar，加载器的后两项缺失提示已明确记录，尚不能据此用于完整整合包。
- 已确认：Actinium 与 DH 共同加载并识别 Iris 兼容接口；同一普通主世界成功开启 Reimagined HIGH，近景、树木阴影和云层可正常显示，未复现原先花屏与矩阵空指针崩溃。F3 可见 DH 生成与渲染加载任务，LOD 数据库持续增长；已正常关闭、保存并完整重启，再次进入同一存档，光影设置保留且画面正常。用户随后实机确认近远景衔接可接受（轻微瑕疵不影响体验），并在检查水体后确认无问题。阶段一按本轮普通世界画面与重进范围通过，不代表长期稳定性、完整整合包或自定义维度通过。
- 已知限制：两次进入时各有一次 `Opaque + Translucent pass ran with shaders on` 错误，尚未确认对 LOD 绘制的影响；另有 DH 在线更新超时、生成时 cascading worldgen lag、G1 卡顿提示，以及 `BIOME_SULFUR_CAVES` / `endFlashIntensity` uniform 警告。用户已接受当前画面，不为轻微衔接瑕疵或清除日志提示继续调优；DH 开关定量对照未做。后续若出现可见问题，再结合这些提示定位，必要时对照上游实测过的 DH 3.3.0。现有模组共存结果见阶段二。

### 阶段二实测记录

2026-11-02，沿用阶段一的 Windows、Java 25、Cleanroom、Actinium、DH 和 Reimagined HIGH，另建 `.local/newearth-render/game-compat/`，由同目录上级的 `launch-compat.cmd` 启动。阶段一实例与存档保留；未更改正式客户端、服务器或 Java 8。

- 共存基线：从 Windows 正式客户端复制并逐个比对哈希的 12 个 JAR，包括 GC 4.0.6、GS 2.2.0 flight-fix、AsmodeusCore 文件版本 1.0.5、EP 0.8.0、MJRLegendsLib、GSLaunchCompat 1.4.8、GC Compatibility、FTBLib、FTBQuests、ItemFilters、JEI 及外置 MixinBooter 10.7；加入 Fugue 0.23.4 与 Scalar Legacy 1.0.1，合计 16 个外置 JAR，全部通过 ZIP CRC 检查。未加入物品线尚未部署的 NewEarth 开发包，不把该结果扩展为开发包验收。
- 配置：复制现有 GC / GS / EP、AsmodeusCore、FTB 与 JEI 配置，保留既有 `enableSkyOverworld=false`，本轮未修改天空开关；这只验证当前主世界配置，不代表自定义星球天空兼容。
- 已确认：28 个模组完成加载，创建普通创造世界 `New WorldCompatRenderTest`；Reimagined r5.9.3 HIGH 的岸边、水面反射与云层显示正常，JEI 可搜索并显示 GalaxySpace 物品。主世界 DH 数据库由约 41 MB 继续增长至约 42 MB。正常关闭时保存区块并关闭 DH，完整重启后再次进入同一存档，光影设置和画面保留。本轮普通主世界启动、渲染、保存与重进范围通过；实例留在世界中供用户继续体验。
- 已知提示与边界：仍有阶段一的 `Opaque + Translucent` 提示和 DH 在线更新超时；另有 missing MultiModel 以及 GC WorldProvider 类型 `-1234` / `-1032` 提示，尚未确定来源及跨维度影响，本轮不据此判定星球可通行。未验证机器与火箭完整玩法、FTB 任务行为、多人、长期稳定性或星球往返；这些不由主菜单加载成功替代。

首轮保存关闭日志、重进日志与最近画面分别保存在 Windows `build/checks/newearth/compat/startup.log`、`reentry.log`、`reimagined.png`，不提交本机实例和生成文件。后续进入阶段三地形方案验证，维度与完整旅程仍按阶段五验收。

## 发现与 FTB 呈现

发现前不展示终章名称、封面或目的地信息；章节、节点、通知和星图等需要共同考虑。具体揭示方式后续核实，不将某个隐藏选项当成已经实现的保证。

「似曾相识」与 H01 抵达占位由 [FTB 作者侧文稿](https://github.com/Neosamzzz/Minecraft/blob/main/docs/ftb-quest-manuscript.md)维护（`main` 主线），最终在维度接入后对接。基础 FTB 内容独立推进，不等待新维度完成。

## 与火箭线的接口

终章准入要求为使用黑洞引擎制造的十阶宇宙飞船及已安装、有效的最终能源核心。基础九阶先前往开普勒取得材料，再完成黑洞引擎及十阶制造／升级，满足最终能源核心条件；七阶及未升级的基础九阶不能进入终章。核心检查对应实际飞船，同船乘员随船航行，不能由 FTB 共享完成状态替代。

后续共同确定维度标识、星图归属、多人到达与返回，以及发现时机；天体等级、星图与实际通行检查应一致。此处是目标规则，不代表当前实例已改变。

两线仍可独立开发：维度线研究世界、地形与正常睡眠，物品线验证飞船、核心与多人航行，最后对接完整旅程。FTB 先完成文稿和目标说明，待注册项存在后补配置验证，不作为两线开发的前置。

## 正常过夜与结尾演出

抵达时先留出自由观察、探索和安顿的空间。在终章星球，大家按主世界正常的多人睡眠规则睡下、成功完成过夜后，触发共同的结尾演出；单个玩家右键床、尝试睡眠或刚躺下都不足以证明完成过夜。

维度实现应支持正常床睡眠与多人过夜，使用原版睡眠完成语义，不另设计集合观看、参与名单或投票机制。也不按每名玩家“第一次入睡”分别触发，不能由 H01 团队任务点亮直接启动。

演出参考击败末影龙后的终末之诗形式：渐暗、原创文字、音乐与背景承接共同旅程，结束后继续游玩。它不在抵达前预告，不作为新任务或强制定居要求。具体文字、背景、时长、重播和跳过方式后续确定。

成功过夜由服务端确认，客户端播放演出；事件时机、多人同步和重复事件防护需要实际联机验证。该流程由模组机制承担，FTB 不添加睡觉、放床或看完演出的勾选节点。

本页维护方向、调查结论与简要推进顺序；具体实现和地形算法根据验证结果确定。
