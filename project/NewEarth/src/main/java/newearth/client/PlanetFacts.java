package newearth.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import micdoodle8.mods.galacticraft.api.galaxies.*;
import micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;

/** Short gameplay properties for the selected, registered destination. Never loads a world. */
public final class PlanetFacts {
    private PlanetFacts() { }

    public static List<String> describe(CelestialBody body) {
        List<String> rows = new ArrayList<>();
        add(rows, "type", text(body instanceof Star ? "star" : body instanceof Moon ? "moon" : "planet"));
        if (!body.getReachable() || body instanceof Star) {
            add(rows, "landing", text("display"));
            return rows;
        }
        if (body.getTierRequirement() > 0) add(rows, "tier", "T" + body.getTierRequirement());
        boolean destination = body.getName().equals(newearth.dimension.EndDimension.BODY_NAME);
        if (destination) add(rows, "landing", text("landable"));
        boolean earth = body.getName().equalsIgnoreCase("overworld") || destination;
        WorldProvider provider = null;
        // A provider is metadata only: do not get/init a server World or attach a fake World.
        try { provider = DimensionManager.createProviderFor(body.getDimensionID()); }
        catch (RuntimeException ignored) { /* Unregistered destinations still have registry facts. */ }
        IGalacticraftWorldProvider space = provider instanceof IGalacticraftWorldProvider
                ? (IGalacticraftWorldProvider) provider : null;
        boolean breathable = earth || body.atmosphere.isBreathable();
        if (space != null) try { breathable = space.hasBreathableAtmosphere(); } catch (RuntimeException ignored) { }
        add(rows, "air", text(breathable ? "breathable" : "unbreathable"));
        float thermal = body.atmosphere.thermalLevel();
        if (space != null) try { thermal = space.getThermalLevelModifier(); } catch (RuntimeException ignored) { }
        add(rows, "temperature", text(thermalClass(thermal)));
        if (!breathable) add(rows, "oxygen", text("required"));
        if (thermal != 0) add(rows, "thermal", text("required"));
        if (space != null) {
            try { add(rows, "gravity", text(space.getGravity() > 0.01F ? "low" : space.getGravity() < -0.01F ? "high" : "normal")); }
            catch (RuntimeException ignored) { }
            try { add(rows, "dungeon", text(space.getDungeonSpacing() > 0 ? "present" : "no_dungeon")); }
            catch (RuntimeException ignored) { }
        }
        if (earth) {
            add(rows, "gravity", text("normal"));
            add(rows, "day", "24 h");
        } else if (provider != null) {
            try {
                long ticks = ((Number) provider.getClass().getMethod("getDayLength").invoke(provider)).longValue();
                if (ticks > 0) add(rows, "day", number(ticks / 1000.0) + " h");
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
        if (provider != null) try {
            double solar = ((Number) provider.getClass().getMethod("getSolarEnergyMultiplier").invoke(provider)).doubleValue();
            if (Double.isFinite(solar) && solar >= 0) add(rows, "solar", number(solar) + "x");
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
        String resourceKey = "newearth.map.resources." + body.getName().toLowerCase(Locale.ROOT);
        if (I18n.hasKey(resourceKey)) add(rows, "resources", I18n.format(resourceKey));
        return rows;
    }

    public static String thermalClass(float thermal) {
        return thermal <= -4 ? "freezing" : thermal < 0 ? "cold" : thermal >= 4 ? "scorching" : thermal > 0 ? "hot" : "mild";
    }

    private static String number(double value) { return String.format(Locale.ROOT, "%.1f", value); }
    private static String text(String key) { return I18n.format("newearth.map." + key); }
    private static void add(List<String> rows, String key, String value) { rows.add(text(key) + ": " + value); }
}
