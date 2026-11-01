# Dimension telemetry addon

当前 GSLC 版本为 1.4.8，日常构建从仓库根目录执行 `py -3 tools/build.py --target gslc`；Windows 之外使用 `python3`。统一入口负责重编译、重混淆及打包，最终产物位于根目录 `build/libs/`。以下英文技术说明保留 1.4.1 基线，不能视为最新全部显示行为；1.4.2～1.4.8 的火箭变换、地图、时钟和缓存变化见[开发记录](../../../changelog/gslc.md)。

历史专项验证使用 MixinBooter 10.7。本机当前文件为 12.2，尚未据此重跑全部探针；日志进入世界不代表 GPU 画面或多人行为全部验收。当前实例基线见[整合包基线](../../../changelog/modpack.md)。

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

## 版本历史

1.4.2 起各版本的改动、影响面与验证状态统一记录在 [changelog/gslc.md](../../../changelog/gslc.md)。
本文件只保留构建方式、测试手段与 1.4.1 基线的技术要点。
