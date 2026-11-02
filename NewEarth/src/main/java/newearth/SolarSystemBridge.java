package newearth;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.recipe.INasaWorkbenchRecipe;
import micdoodle8.mods.galacticraft.core.recipe.NasaWorkbenchRecipe;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Adapts existing registered content; does not enable EP planets or change world IDs. */
final class SolarSystemBridge {
    private SolarSystemBridge() { }

    @SuppressWarnings("unchecked")
    static void install() {
        try {
            List<INasaWorkbenchRecipe> recipes = (List<INasaWorkbenchRecipe>) Class.forName(
                    "com.mjr.extraplanets.recipes.Tier7RocketRecipes")
                    .getMethod("getTier7RocketRecipes").invoke(null);
            install(recipes);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("NewEarth requires the EP 0.8.0 T7 workbench API", e);
        }
    }

    static void install(List<INasaWorkbenchRecipe> nasaRecipes) {
        // Validate every input and the integration method before changing recipes or tiers.
        Item rocket = requireItem("extraplanets:item_tier7_rocket");
        Item schematic = requireItem("extraplanets:schematic_tier7");
        Item parts = requireItem("galaxyspace:rocket_parts");
        Item modules = requireItem("galaxyspace:rocket_modules");
        Item plates = requireItem("galaxyspace:hdp");
        Method addRecipe;
        try {
            addRecipe = Class.forName("galaxyspace.systems.SolarSystem.planets.overworld.recipes.AssemblyRecipes")
                    .getMethod("addShapelessRecipe", ItemStack.class, Object[].class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("NewEarth requires the GS 2.2.0 assembler API", e);
        }

        // The GS rocket GUI has fixed filtered slots. Its generic 3x3 assembler makes
        // the schematic; the existing EP NASA page accepts inputs from its recipe list.
        Object[] schematicInput = {new ItemStack(Items.PAPER), new ItemStack(modules, 1, 10),
                new ItemStack(plates, 1, 2), new ItemStack(plates, 1, 2),
                new ItemStack(plates, 1, 2), new ItemStack(plates, 1, 2)};
        try {
            addRecipe.invoke(null, new ItemStack(schematic), schematicInput);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("NewEarth could not register the T7 schematic recipe", e);
        }
        for (int cargoMask = 0; cargoMask < 8; cargoMask++) {
            HashMap<Integer, ItemStack> input = new HashMap<>();
            input.put(1, new ItemStack(parts, 1, 15));
            for (int slot = 2; slot <= 11; slot++) input.put(slot, new ItemStack(plates, 1, 2));
            input.put(12, new ItemStack(parts, 1, 18));
            input.put(16, new ItemStack(parts, 1, 18));
            input.put(15, new ItemStack(parts, 1, 17));
            for (int slot : new int[] {13, 14, 17, 18}) input.put(slot, new ItemStack(parts, 1, 19));
            for (int i = 0; i < 3; i++) {
                input.put(19 + i, (cargoMask & (1 << i)) == 0 ? ItemStack.EMPTY : new ItemStack(Blocks.CHEST));
            }
            nasaRecipes.add(new NasaWorkbenchRecipe(new ItemStack(rocket, 1, Integer.bitCount(cargoMask)), input));
        }
        NewEarth.LOG.info("Registered solar-system T7 schematic and eight NASA recipes (cargo 0-3)");

        setTier(GalaxyRegistry.getRegisteredPlanets(), "proxima_b", 7);
        setTier(GalaxyRegistry.getRegisteredPlanets(), "barnarda_c", 7);
        setTier(GalaxyRegistry.getRegisteredMoons(), "barnarda_c1", 7);
        setTier(GalaxyRegistry.getRegisteredPlanets(), "tauceti_f", 7);
        // Kepler remains at its original gate until the custom T8 ship is implemented.
    }

    private static Item requireItem(String name) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(name));
        if (item == null || item == Item.getItemFromBlock(Blocks.AIR)) {
            throw new IllegalStateException("NewEarth missing required item " + name
                    + (name.startsWith("extraplanets:")
                    ? "; keep EP Uranus enabled for T7 registration. NewEarth does not edit EP config."
                    : "; check that GS 2.2.0 is installed and its item registration completed."));
        }
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
