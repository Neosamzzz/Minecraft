package newearth;

import java.util.ArrayList;
import java.util.Collection;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.world.AtmosphereInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class EnvironmentHooks {
    private EnvironmentHooks() { }

    public static boolean protectedByGS(EntityPlayer player) {
        if (player == null) return false;
        return gsSuit(player.inventory.armorInventory.get(3), player.inventory.armorInventory.get(2),
                player.inventory.armorInventory.get(1), player.inventory.armorInventory.get(0));
    }

    public static boolean gsSuit(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        return gsPiece(head, "head") && gsPiece(chest, "chest") && gsPiece(legs, "legs") && gsPiece(feet, "feet");
    }

    private static boolean gsPiece(ItemStack stack, String slot) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && "galaxyspace".equals(id.getNamespace())
                && (id.getPath().equals("space_suit_" + slot) || id.getPath().equals("space_suit_light_" + slot));
    }

    public static float thermal(float value) {
        // EP's high-tier padding uses 50/100/150 strength; GS uses 3/4.
        // Ordinary GC-scale values retain their existing environmental behavior.
        if (Math.abs(value) <= 16) return value;
        return Math.copySign(Math.min(Math.abs(value) / 10.0F, 16.0F), value);
    }

    static void adaptMetadata() {
        Collection<CelestialBody> bodies = new ArrayList<>();
        bodies.addAll(GalaxyRegistry.getRegisteredPlanets().values());
        bodies.addAll(GalaxyRegistry.getRegisteredMoons().values());
        for (CelestialBody body : bodies) {
            if (!body.getReachable() || !body.getName().toLowerCase(java.util.Locale.ROOT).endsWith("ep")) continue;
            // GC registers runtime dimensions later; body metadata already owns the provider type.
            if (body.getWorldProvider() == null || !body.getWorldProvider().getName()
                    .startsWith("com.mjr.extraplanets.")) continue;
            adaptAtmosphere(body);
        }
    }

    static void adaptAtmosphere(CelestialBody body) {
        AtmosphereInfo original = body.atmosphere;
        float adjusted = thermal(original.thermalLevel());
        if (adjusted == original.thermalLevel()) return;
        AtmosphereInfo replacement = new AtmosphereInfo(original.isBreathable(), original.hasPrecipitation(),
                original.isCorrosive(), adjusted, original.windLevel(), original.relativeDensity());
        replacement.composition.addAll(original.composition);
        body.setAtmosphere(replacement);
    }
}
