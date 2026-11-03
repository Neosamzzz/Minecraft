package galaxyspace.core.telemetry;

/** Model-space correction, used only by the telemetry screen. */
public final class TelemetryRocketLayout {
    private TelemetryRocketLayout() { }

    public static float verticalOffset(String entityClass, float height) {
        switch (entityClass) {
            case "galaxyspace.core.prefab.entities.EntityTier4Rocket":
            case "galaxyspace.core.prefab.entities.EntityTier5Rocket":
            case "galaxyspace.core.prefab.entities.EntityTier6Rocket": return -height / 4F;
            default: return 0F;
        }
    }
}
