package galaxyspace.core.telemetry;

import java.util.UUID;
import micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class TelemetryTracking {
    private TelemetryTracking() { }
    /** Search already loaded entities only. Never create or force-load target worlds/chunks. */
    public static Entity find(UUID uuid, WorldServer[] worlds) {
        if (uuid == null) return null;
        for (WorldServer world : worlds) {
            if (world == null) continue;
            Entity entity = world.getEntityFromUuid(uuid);
            if (entity != null && !entity.isDead) return entity;
        }
        return null;
    }
    public static String dimensionKey(World world) {
        int dimension = world.provider.getDimension();
        if (dimension == 0) return "gslc.dimension.overworld";
        if (dimension == -1) return "gslc.dimension.nether";
        if (dimension == 1) return "gslc.dimension.end";
        if (world.provider instanceof IGalacticraftWorldProvider) {
            Object body = ((IGalacticraftWorldProvider) world.provider).getCelestialBody();
            if (body != null) {
                // GC4.0.6 renamed this API; support the previous name as well.
                for (String name : new String[] {"getTranslationKey", "getUnlocalizedName"}) {
                    try { return (String) body.getClass().getMethod(name).invoke(body); }
                    catch (ReflectiveOperationException ignored) { }
                }
            }
        }
        return "";
    }
}
