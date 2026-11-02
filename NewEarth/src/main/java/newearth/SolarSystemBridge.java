package newearth;

import java.util.Map;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Adapts exoplanet tiers; solar-system registration lives in PlanetHooks. */
final class SolarSystemBridge {
    private SolarSystemBridge() { }

    static void install() {
        // The original T7 bridge recipes are intentionally retired with the EP chain.
        // New T7 manufacturing will be selected and implemented separately.
        setTier(GalaxyRegistry.getRegisteredPlanets(), "proxima_b", 7);
        setTier(GalaxyRegistry.getRegisteredPlanets(), "barnarda_c", 7);
        setTier(GalaxyRegistry.getRegisteredMoons(), "barnarda_c1", 7);
        setTier(GalaxyRegistry.getRegisteredPlanets(), "tauceti_f", 7);
        // Kepler remains at its original gate until the custom T9 ship is implemented.
    }

    static Item requireItem(String name) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
        if (item == null || item == Item.getItemFromBlock(Blocks.AIR))
            throw new IllegalStateException("NewEarth missing required item " + name);
        return item;
    }

    private static void setTier(Map<String, ? extends CelestialBody> bodies, String name, int tier) {
        CelestialBody body = bodies.get(name);
        if (body == null) {
            NewEarth.LOG.info("Skipping disabled destination {}", name);
            return;
        }
        int previous = body.getTierRequirement();
        body.setTierRequired(tier);
        NewEarth.LOG.info("Destination {} (dimension {}): tier {} -> {}", name,
                body.getDimensionID(), previous, tier);
    }
}
