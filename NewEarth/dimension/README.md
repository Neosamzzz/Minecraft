# 终章新维度

维护分支：[main](https://github.com/Neosamzzz/Minecraft/tree/main)。核对日期：2026-11-03。

新维度、终章体验与正常过夜后的结尾触发独立推进；多人宇宙飞船、最终能源核心及现有系外目的地的专用工具由[物品线](../items/README.md)维护。整体关系见[支线总览](../README.md)。当前仅记录设计，维度与演出尚未实现。

## 体验方向

- 新增独立的拟地球行星，与现有 GS 系外行星和 EP 开普勒分开。
- 近处有熟悉的花草、河岸与地表细节，远处有更宏伟的森林、山脉和大陆，重视近景熟悉、远景辽阔的感受。
- 终章定位为适合欣赏、探索与生活的理想世界：以开阔草甸、河湖和缓坡留白，远处呈现有山脚、山腰与连续山脊的大尺度山体，森林疏密交替。避免主要依靠密集岩柱、零碎陡峰和装饰堆出丰富感；洞穴、地牢与遗迹不是必要内容，也不作为选型优势，是否保留按实际需要决定。
- 呼应大部分陆地被海洋吞没的地球开篇。角色视角按不知道最终目的地存在来写，不提前承担“寻找新地球”的使命；朋友实际知道终章计划不妨碍这个叙事视角。
- 抵达时先呈现景色和意外发现，之后由玩家选择探索、建设或定居。
- 保留现有生态星球的森林、海洋和价值，不将它们改写为寻找家园失败的荒地。

“巨型拟地球”描述地貌尺度与景观方向，不据此直接规定球体尺寸、地图边界、具体树种、呼吸条件或生成算法。

## 内容边界

景观优先复用原版、GC、GS、EP 已有的方块、生态和素材，不为终章配套扩展新的矿物、装备或通用工业体系。新维度仍需独立研究生成与呈现，但不承担新火箭物品和制造台的开发。

维度线可独立研究与验证地形和世界行为，不以九、十阶飞船已经完成为起点。维度 ID、星图位置、环境参数与生成方式尚未确定。

## 地形、光影与远景路线

2026-11-02 调查结论：保留 Minecraft 1.12.2 与 GC / GS 玩法，光影是景观目标的重要组成部分，以 Complementary Reimagined r5.9.3 为优先验证对象，使用 DH 承担远景。优先保证终章星球的呈现，其他星球保留原有风格；是否能按维度切换光影与 DH、切换后能否正确恢复，尚待验证，不能据此假定其他维度不受客户端渲染器影响。

地形优先研究现有 1.12.2 模组或生成器的复用方式，包括能否用于独立维度；不将自行编写生成算法或移植 FreeTerraForged 作为当前前置。参考图用于确定景观尺度与气氛，不锁定具体树种、粉色森林或某个模组。

呈现规则已确定：默认仅终章启用固定的 Reimagined 与预设参数；最多提供一个“全局启用终章光影”选项，开启后全局使用这套光影，并允许玩家正常手动调整参数。此规则尚未实现，按维度切换与恢复需实测。Galacticraft Compatibility 的“是否将会使用光影”实际写入 AsmodeusCore 的四个天空开关，不能代替外部光影包切换。

性能作为地形、光影与 DH 的共同约束：优先用山脉轮廓、河湖视野与森林分布形成尺度，控制密集植被、阴影质量、LOD 距离与后台任务负载。当前 HIGH 只是已测试组合，不预先定为最终默认档；比较首次生成、缓存完成后、移动探索与重进时的帧率稳定性、卡顿和内存，目标数值按实测确定。

终章目的地的后台准备从九阶飞船制造完成后即可启动，不必等到十阶出发或临近落地：提前异步生成终章落点及周边地形，并准备可用的 DH 远景缓存；优先复用缓存、准备第一眼可见范围，再逐步扩展，限制对正常游玩与航行性能的影响。提前生成不开放终章准入，实际进入仍要求十阶飞船及已安装、有效的最终能源核心，也不提前向玩家揭示目的地。地形区块生成、DH LOD 构建及客户端加载分别验证；尚未确认 1.12.2 DH 在进入目标维度前的数据准备接口与多人传输方式。此项仅记录规划，尚未实现。

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
| 三、地形方案验证 | 用独立测试世界比较少量现有生成方案的尺度、景观、DH 表现与生成成本，选定能接入独立维度的一种 | 进行中；RTG Plus 与 OTG / Biome Bundle 普通世界对照，尚未选型 |
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

### 阶段三地形验证

先比较自然地形基线与景观变化：首选测试 RTG 路线，再以 OTG + Biome Bundle 对照；参考图约束近景可生活、中距离有河湖留白、远处形成连续山脉轮廓，不照搬树种或配色。原 [RTG 项目](https://github.com/Team-RTG/Realistic-Terrain-Generation)已指向持续维护的 [RTG Plus](https://github.com/Zeno410/Realistic-Terrain-Generation-Plus)，本轮固定使用发布平台标为正式版的 [9.0.4](https://modrinth.com/mod/rtg-plus/version/9.0.4)，不使用旧 6.1 快照。

2026-11-03 首轮：Windows `.local/newearth-render/game-rtg/` 复制阶段二的模组、配置与渲染参数，新增 `RTG-1.12.2-9.0.4.jar`，按 Modrinth 发布元数据核对 SHA-512 并通过 ZIP CRC；未复制旧存档。入口为上级 `launch-rtg.cmd`，窗口名 `NewEarth RTG Test`；第二阶段实例仍保留。创建 RTG 世界类型的普通创造世界，种子 `20261103`，存档名 `新的世界RTG-Terrain`。原版视距 12、DH 半径 256、Reimagined HIGH 延续前轮，仅用于首轮观察，不作为最终性能参数。

- 已观察：29 个模组完成加载；出生点约 `(228, 78, 234)`，有草地、花草、河岸与高树森林，升至约 Y=152 可见山地和林地组合；光影及 DH 任务运行，远景数据库增长。
- 观察与限制：远景出现竖直断面与未补齐区域，根因未定，用户后续已接受为本轮显示瑕疵，不再专项排查（见下方共同结论）；日志有 RTG cascading worldgen lag 与此前已记录的渲染提示。单次 F3 观察约 61 / 111 FPS，两个游戏实例同时运行、视角及生成负载不同，不作平均帧率、稳定性或方案优劣结论。
- 对照排查：首个世界保存返回菜单后可重进，断面仍存在；保留首轮证据，改用 `INTERNAL_SERVER` 并重启客户端，在相同种子、相同 RTG 类型的新存档 `新的世界RTG-Internal` 中观察，随后仍出现相似断面，生成模式切换未解决。临时关闭 Reimagined 后断面仍可见，不能只归因于光影；已恢复光影，当前保留该对照世界供查看。本轮未完成严格 DH 开关对照、附近实际区块与 LOD 高度比对，根因未定；后续按用户决定停止专项排查。当前测试配置的 DH 生成路径为 `INTERNAL_SERVER`，原值 `FEATURES`，不改变正式实例。
- 接入依据：[RTG API](https://github.com/Zeno410/Realistic-Terrain-Generation-Plus/blob/main/src/main/java/rtg/api/RTGAPI.java)与世界类型实现提供额外维度允许列表及地形生成入口；已安装 JAR 的公开方法也已核对；这属于接口可行性依据，尚未完成 GC 自定义行星接入验证。默认配置另会向非 RTG 世界添加树木与池塘（`treesInNonRTGWorlds` / `lakesInNonRTGWorlds`），正式接入前必须限制影响范围，不能直接向现有实例安装默认配置。OTG + Biome Bundle 首轮对照见下文；代表性山脉/海岸探索和单实例性能比较待完成，不提前确定生成器。

OTG 对照同样使用独立普通世界：Windows `.local/newearth-render/game-otg/` 从阶段二复制模组、配置与渲染参数，不含 RTG；固定 CurseForge 官方文件 [OTG v9.9 / 9338964](https://www.curseforge.com/minecraft/mc-mods/open-terrain-generator/files/9338964) 与 [Biome Bundle v6.1 / 2505277](https://www.curseforge.com/minecraft/mc-mods/biome-bundle/files/2505277)，均通过 ZIP CRC，记录下载 SHA-256 / SHA-512；CurseForge 与 Modrinth 同名 OTG v9.9 的二进制不同，本轮只用前者。入口为上级 `launch-otg.cmd`，窗口名 `NewEarth OTG Test`。预设界面选择 Biome Bundle、种子 `20261103`、创造与允许命令，预生成半径为 0，未创建额外维度；存档名 `Biome Bundle`。首个区域为红色岩柱、草地与海岸，光影正常，但同样观察到远处竖直断面，不能据此认定 RTG 独有问题。OTG 的预设/独立维度 GUI 可用不等于已对接 GC 星球。已正常关闭保存、完整重启并重新进入同一世界，光影设置保留。远景断面仍在；远程 F6 操作没有确认切换成功，不作为对照证据。用户随后手动切换 DH，反馈与处理决定见下方共同结论。仅 OTG 测试实例开启了 `enableDebugKeybindings` 方便检查，不是交付默认配置。检查目录另保留 `otg-startup.log` 与 `otg-near.png`；三实例同时运行，不进行性能优劣判定。

**2026-11-03 用户复核与本轮决定：** 缺口有时呈近远景之间的一圈。用户曾关闭再开启 DH 后看到改善，但期间也移动到相关区域并等待加载，不能单独归因于开关；进一步确认走远再返回不一定恢复，而拉大原版视距再缩小可以恢复。该现象可能涉及原版区块与 DH 的数据准备、衔接或刷新，根因未证实，撤回“只是区块未加载”的确定判断，也不据此判定实际地形损坏或两个生成器不兼容。用户已明确接受为显示瑕疵，本轮到此停止专项排查，不再为它反复调整版本、配置或扩大测试。保留“拉大原版视距后再缩小”的临时恢复方式，不将其当作修复实现或自动恢复保证。

2026-11-03 配置与生成接口复核（只读，未调整预设）：

- RTG 已安装 JAR 提供河流大小、频率、弯曲及湖泊大小、频率等世界生成参数；当前 `rtg.cfg` 的 `treeDensityMultiplier`、`waterFeatureWidthMultiplier` 均为 1.0。这些提供水域留白与植被疏密的调整入口，不证明已能生成目标尺度的山体，也不代表 RTG 性能优于 OTG。
- 当前 Biome Bundle 解包内容有 445 份 `.bc` 和 2192 份 `.bo3`，包含草甸、阿尔卑斯山峰与森林山脉等配置；首个岩柱海岸不能代表整个预设。草甸、山地配置也引用城堡、地牢、遗迹和洞穴对象，不能只关闭原版建筑开关就假定这些内容均已取消。后续如采用，应筛选符合地表方向的配置并核对对象依赖；数量不直接代表实际生成负载。
- OTG 9.9 已安装 JAR 的调用链为 `ServerTickHandler.onServerTick` → `ForgeEngine.processPregeneratorTick` → `Pregenerator.processTick`，当前 `OTG.ini` 的 `PregeneratorMaxChunksPerTick` 为 5，预生成半径仍为 0。它有按 tick 分批的入口，但不是独立工作线程生成或低耗时保证；单个复杂区块仍可能造成卡顿。九阶后低负载后台准备仍只记录目标，实际需要评估时间预算、暂停恢复及 DH 缓存准备。

**接续工作：** 当前阶段三仍进行中，RTG Plus 和 OTG / Biome Bundle 均已有独立普通世界，尚未选型。按用户确认的理想世界方向，优先比较开阔地表与完整大山体的组合、森林疏密和探索建设空间，不以洞穴遗迹数量或局部岩柱密度评分，再在单实例条件下评估生成与游玩开销。落地前异步准备区块与 DH 远景仍是待实现目标，不承诺能消除上述显示瑕疵；终章维度与完整旅程继续最后验收。测试完成后保留游戏给用户体验，接手时先核对进程与窗口，不擅自关闭或控制用户正在玩的实例。

检查结果沿用 Windows `build/checks/newearth/terrain/`，按 `rtg-near.png`、`rtg-overview.png`、`rtg.log` 保存最近输出，`rtg-features.log` / `rtg-features.png` 和 `rtg-no-shaders.png` 保留本次必要的异常对照证据，实例和生成文件不提交。

## 发现与 FTB 呈现

发现前不展示终章名称、封面或目的地信息；章节、节点、通知和星图等需要共同考虑。具体揭示方式后续核实，不将某个隐藏选项当成已经实现的保证。

「似曾相识」与 H01 抵达占位由 [FTB 作者侧文稿](https://github.com/Neosamzzz/Minecraft/blob/main/docs/ftb-quest-manuscript.md)维护（`main` 主线），最终在维度接入后对接。基础 FTB 内容独立推进，不等待新维度完成。

## 与火箭线的接口

终章准入要求为十阶飞船及已安装、有效的最终能源核心。基础九阶先前往开普勒取得材料，再完成十阶与核心升级；七阶及基础九阶不能进入终章。九阶制造完成后可提前启动终章目的地的低负载后台准备，不能以此代替准入资格。核心检查对应实际飞船，同船乘员随船航行，不能由 FTB 共享完成状态替代。

后续共同确定维度标识、星图归属、多人到达与返回，以及发现时机；天体等级、星图与实际通行检查应一致。此处是目标规则，不代表当前实例已改变。

两线仍可独立开发：维度线研究世界、地形与正常睡眠，物品线验证飞船、核心与多人航行，最后对接完整旅程。FTB 先完成文稿和目标说明，待注册项存在后补配置验证，不作为两线开发的前置。

## 正常过夜与结尾演出

抵达时先留出自由观察、探索和安顿的空间。在终章星球，大家按主世界正常的多人睡眠规则睡下、成功完成过夜后，触发共同的结尾演出；单个玩家右键床、尝试睡眠或刚躺下都不足以证明完成过夜。

维度实现应支持正常床睡眠与多人过夜，使用原版睡眠完成语义，不另设计集合观看、参与名单或投票机制。也不按每名玩家“第一次入睡”分别触发，不能由 H01 团队任务点亮直接启动。

演出参考击败末影龙后的终末之诗形式：渐暗、原创文字、音乐与背景承接共同旅程，结束后继续游玩。它不在抵达前预告，不作为新任务或强制定居要求。具体文字、背景、时长、重播和跳过方式后续确定。

成功过夜由服务端确认，客户端播放演出；事件时机、多人同步和重复事件防护需要实际联机验证。该流程由模组机制承担，FTB 不添加睡觉、放床或看完演出的勾选节点。

本页维护方向、调查结论与简要推进顺序；具体实现和地形算法根据验证结果确定。
