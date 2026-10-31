# Public launch controller GUI addon

The two client-only mixins replace PlayerUtil.getName only inside the selected
GUI permission checks. The real tile owner and foreground title rendering are
unchanged; cooldown branches and non-owner-independent logic are preserved.
No server packet permission patch is required for GC 4.0.6.

Build GalaxySpace with the existing Gradle jar/reobfJar tasks, then run:

    python3 tests/public_controller/build_addon.py BASE_ADDON REOBF_ALL_JAR OUTPUT_ADDON

This retains the original flight addon entries byte for byte and registers the
new GUI classes exclusively in the client list. Replace the old GSLaunchCompat
addon on every client; do not load two versions together. The original modified
GalaxySpace flight-fix JAR is still required and remains unchanged.

Runtime test:

    python3 tests/public_controller/run_probe.py RUNTIME_PROBE_DIR OUTPUT_ADDON WORK_DIR

The runtime probe uses actual GC 4.0.6 classes, LaunchWrapper and Mixin, asserting
both GUIs reject a different owner before patching and allow it after patching.
It also checks the advanced dropdown and unchanged owner data. This is not an
OpenGL UI or multiplayer packet round-trip test; verify persisted settings with
another player in game. The existing server BindingProbe passed 105 checks with
the new addon, confirming client-side isolation and preserved flight behavior.
