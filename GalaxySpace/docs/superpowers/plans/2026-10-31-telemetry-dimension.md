# Telemetry dimension display

Approved behavior: players, living mobs and rockets show the tracked entity's
actual dimension. Replace living pulse display; keep health/hunger/oxygen and
rocket countdown/height/speed/fuel. Keep owner GUI and flight compatibility.
Text must fit both individual and joined screens.

Implementation:
1. Add additive telemetry network message in GSLaunchCompat with explicit
   client/server proxy separation; do not repurpose the five GC data values.
2. Preserve tracked UUID across save/reload and reacquire loaded entities in
   all loaded server worlds; report unavailable rather than invent stale data.
3. Display localized dimension in pulse row or rocket's existing blank row.
   Bound each text line and reserve a text column while preserving GC clipping.
4. Verify message round trip, cross-world lookup/recovery, unavailable state,
   existing data integrity and text bounds; load actual GC mixins client/server.
5. Package an updated standalone addon preserving earlier patch bytes, provide
   client/server installation instructions and regression evidence. Windows
   connector is currently unavailable; no claim of in-game visual validation.
