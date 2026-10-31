# Dimension telemetry addon (1.4.1)

Requirements: MC1.12.2, GC4.0.6, existing GS flight-fix and MixinBooter.
Replace the earlier GSLaunchCompat addon on both client and server. The new
`gslaunchtelemetry` FML container registers an independent dimension message;
previous GC telemetry payloads and flight/controller class bytes remain intact.
A server restart is required. The manifest must contain
`FMLCorePluginContainsFMLMod: true` so FML discovers the network container.

Living-entity pulse row becomes dimension. Rockets use their previously blank
first data row, preserving countdown, height, speed and fuel. Original GC screen
scaling, text offsets, model size and existing lines remain unchanged. Only the
dimension line is width-limited with an ellipsis. Target dimension comes from the tracked entity's world; the source
unit's dimension is only used for packet routing.

UUID recovery only searches already loaded server worlds/entities, on the server
thread once per second. Missing targets retain their UUID and report unavailable;
manual unlink clears persistence. Old GC links are migrated. Empty/malformed/zero
UUID data cannot create a phantom target. Client updates are scheduled on its main
thread and dimensions expire after five seconds without metadata updates.

Build with the existing Gradle jar/reobfJar tasks, then:

    python3 tests/telemetry_dimension/build_addon.py BASE_1_3_ADDON REOBF_ALL_JAR OUTPUT_ADDON

Validation:

- `TextLayoutTest`: bounded CJK/long dimension strings; no global layout modification.
- `run_probe.py RUNTIME_PROBE_DIR OUTPUT_ADDON WORK_DIR`: actual GC4.0.6/Mixin
  client and server, packet round trip, player/mob/rocket lookup, cross-world saved
  UUID recovery, stale state, explicit unlink, legacy and malformed NBT.
- `ForgeIntegrationProbe`: temporary test-only mod, compiled against the existing
  SRG runtime and installed only into an isolated server. Tests initialized
  networking, real target deletion, UUID-preserving dimension transfer and reload.
- Existing public-controller probe and 105 flight-binding checks passed.

No OpenGL screenshot or interactive multiplayer acceptance is claimed. Physical
flight, landing and player/mob lifecycle behavior still require game acceptance.
The temporary Forge integration mod must never be included in the release addon.

The FML/network version remains 1.4. Existing 1.4 servers can use the 1.4.1 client
layout fix without restarting. Upgrading from 1.3 still requires both sides.

The addon includes pack.mcmeta with pack_format=3. Without it Forge synthesizes
format 2; Minecraft's legacy adapter changes language filenames to zh_CN/en_US,
which makes the packaged lowercase 1.12 locale paths unavailable. A label fallback
prevents a raw translation key from appearing if resources are absent.

## 1.4.2：GS 四、五、六阶火箭显示位置

仅客户端显示器的非生物模型渲染调用增加 Y 平移；不改文字、缩放或实际世界渲染。
补偿值来自已安装 GS 2.2.0 的 OBJ 顶点 Y 范围中点、RenderRockets 的 0.8 缩放和渲染偏移，
并抵消 EntityAutoRocket.adjustDisplay 的 height/4 抬升。其他实体偏移为零。
网络版本仍为 1.4，服务端可继续使用 1.4.1，无需重启。

验证：RocketLayoutTest 读取真实 OBJ，上下边界居中，原始零补偿可复现失败；
客户端/服务端 TelemetryProbe 均通过，包含实际渲染调用重定向。
与 1.4.1 比较，仅显示器 Mixin、其映射和实现版本变化，新增无客户端依赖的布局计算类；
服务端与通信类逐字节相同。实际游戏内画面待确认。

## 1.4.3：所有维度的真实地表俯视图

保留 GC 第 2 页（内部 type=1）、352×352 纹理和原有绘制/合屏逻辑。
客户端 MixinDrawGameScreen 阻止原来的主世界预测地图入口，只在该页绘制前准备实际地表像素。
ScreenSurfaceMap 逐列读取 getLoadedChunk 返回的区块，按地表方块的原版地图颜色绘制；
跳过无地图颜色的方块，依北侧高度作简单明暗。未知/空区块为深色留空，不加载或生成区块。
范围为显示器中心附近 352×352 方块。每两个世界 tick 更新 8 行，完整扫描约 4.4 秒。

每台显示器有独立像素缓冲，切换世界、维度或位置会清空；无跨服务器磁盘缓存。
复用 GC 的共享纹理时，按屏幕缓冲和版本上传，避免多显示器串图。
只支持客户端已收到的区块，覆盖范围取决于视距；不是远程任意地点地图。
网络版本仍为 1.4，服务端保持 1.4.1，不需要重启。

验证：run_surface_probe.py 在实际 GC/Minecraft 类加载器下通过 22 项检查，
模拟主世界/月球/火星，使用真实区块/方块 API，调用实际注入的 GC 绘制入口；
覆盖负坐标、建筑变化、透明方块跳过、区块卸载、换世界、移动显示器、多显示器纹理隔离和分批刷新。
测试以拒绝加载/生成区块的 provider 运行。纹理上传使用替身，未进行游戏内 GPU 画面验收。
原客户端/服务端遥感探针通过；与 1.4.2 比较，全部已有 class 逐字节不变，仅新增地图类和客户端 Mixin。
完整构建请加 --rerun-tasks，避免增量注解处理遗漏之前的 Mixin refmap。

时钟位置与日期未在本次地图补丁中实现。

## 1.4.4：合并地图与右上角时钟

保留 1.4.3 的真实地表地图。遥感信息页在背景之后、正文/模型坐标变换之前，
用独立矩阵在内框右上角绘制 HH:mm；字号随屏幕短边缩放，留内边距。
有无连接都读取显示器所在世界的 getWorldTime，沿用 GC 的固定 24000 tick 一天和 6000 tick 偏移，
不做星球昼夜适配，不增加现实日期或游戏天数。隐藏原来无连接时正文中的带秒时钟。
未改服务端和网络，服务端 1.4.1 可继续使用，无需重启。

验证：8 个格式换算用例（正午/午夜/跨天/负时间等）、实际 GameScreenText 的时钟注入和旧时钟隐藏，
客户端/服务端遥感探针以及 22 项地图检查通过。尚未实机验收文字位置、地图和模型的 GPU 显示效果。

## 1.4.5：火箭居中注入定位试修

针对实际 Forge 2864 / MixinBooter 10.7 启动报告 centreRocket 注入失败，
明确 GC render 的完整签名及 Render.func_76986_a 运行期方法，关闭这处注入的自动映射。
保留严格 require=1，不以忽略注入失败掩盖问题。其余功能不变。
使用 MixinBooter 10.7 中的实际 Mixin 引擎通过客户端/服务端遥感探针和 22 项地图检查；
无完整 Forge 启动的探针环境中，屏蔽了 MixinBooter 自身依赖启动状态的两项附加注入。
同一探针对旧包也通过，未完整复现用户启动环境，需用户实机验证本修正版。

## 1.4.6：仅取消 GS 火箭四分之一高度抬升

按用户确认，四/五/六阶火箭在显示器中调用 GC ITelemetry.adjustDisplay 后，
抵消该调用的 height/4 平移。移除旧的按 OBJ 中心计算的额外补偿及 Render.doRender 重定向，
不保证视觉中心严格居中。模型大小、实际世界中的火箭、地图与时钟不变。
使用 GC API 调用作为注入点，无需 Minecraft 渲染方法映射。
精确抵消/非目标实体零偏移测试，以及 MixinBooter 10.7 引擎下的客户端/服务端探针通过。
仅显示器 Mixin、位置计算类和包版本有变化。待实际启动/画面验收，服务端无需更换或重启。

## 1.4.7：仅缩小显示器内 GS 四/五/六阶火箭

按用户确认，在取消 height/4 后，仅对这三类火箭的模型追加 0.7 倍缩放。
文字和时钟此前已经绘制，不受模型变换影响；地图及实际世界渲染不变。
构建和 MixinBooter 10.7 引擎下客户端/服务端加载检查通过，GPU 画面待确认。

## 1.4.8 / NEO-91：降低刷新频率，保留已绘制地形

已确认「不变黑」指已显示过的地形在走远或区块卸载后继续保留；从未采样过的区域仍可留空。
每 600 世界 tick（正常速度 30 秒）开始一轮扫描，仍每两 tick 处理 8 行以分摊计算。
首次完整画面约 4.3 秒生成。扫描使用独立缓冲，整轮完成且像素发生变化时才发布新版本；
等待期间不采样，单一地图不重复上传未改变的纹理。GC 多屏共用纹理时仍需按绘制对象切换上传。

未加载/不可用区块不再擦黑旧像素；真正已加载但被清空的地表仍会更新。
客户端按当前世界及显示器位置保留最近 32 处地图的内存缓存，显示器卸载重建后可复用；
更换世界对象（含重新进入存档）、维度变化或缓存淘汰后不会沿用旧地图。无磁盘持久化。
仍不强制加载/生成区块。文字、时钟、70% 火箭模型和网络版本均不变。

验证：旧版在「卸载区块擦除已绘地形」测试中失败；修正版通过 32 项实际 Minecraft/GC 类加载器检查，
包括 30 秒采样间隔、等待期无地表读取、整帧提交、未变化不重复上传、卸载保留、显示器重建恢复、
已加载虚空清除、新世界隔离、负坐标与多显示器隔离。使用 MixinBooter 10.7 引擎的客户端/服务端遥感探针通过。
游戏内 GPU 画面与性能待用户验收。
