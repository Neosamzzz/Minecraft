package newearth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.Moon;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.api.recipe.CompressorRecipes;
import micdoodle8.mods.galacticraft.api.recipe.INasaWorkbenchRecipe;
import micdoodle8.mods.galacticraft.core.recipe.NasaWorkbenchRecipe;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Exercises manufacturing retirement and retained resources using real recipe registries. */
public final class ProgressionProbe {
    private static int checks;
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        checks++;
    }
    private static Item item(String id) {
        Item value = new Item().setHasSubtypes(true).setRegistryName(new ResourceLocation(id));
        ForgeRegistries.ITEMS.register(value);
        return value;
    }
    private static IRecipe recipe(String id, ItemStack output) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.fromStacks(new ItemStack(Items.IRON_INGOT)));
        IRecipe recipe = new ShapelessRecipes("", output, ingredients).setRegistryName(id);
        ForgeRegistries.RECIPES.register(recipe);
        return recipe;
    }
    public static void main(String[] args) {
        net.minecraft.launchwrapper.Launch.blackboard = new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Bootstrap.register();
        List<INasaWorkbenchRecipe> nasa = new ArrayList<>();
        for (int tier = 4; tier <= 10; tier++) {
            Item rocket = item("extraplanets:item_tier" + tier + "_rocket");
            for (int cargo = 0; cargo <= 3; cargo++)
                nasa.add(new NasaWorkbenchRecipe(new ItemStack(rocket, 1, cargo), new HashMap<>()));
            Item parts = item("extraplanets:tier" + tier + "_items");
            for (int meta = 0; meta <= 3; meta++) {
                ItemStack output = new ItemStack(parts, 1, meta);
                check(ContentPruning.retiredOutput(output), "Retired rocket component " + tier + ":" + meta);
                recipe("newearth:retired_" + tier + "_" + meta, output);
            }
            for (int meta = 4; meta <= 7; meta++)
                check(ContentPruning.retiredOutput(new ItemStack(parts, 1, meta)) == (tier == 7 && meta == 6),
                        "Preserve resources except old reinforced plate intermediate " + tier + ":" + meta);
            Item cone = item("extraplanets:nose_cone_tier" + tier);
            check(ContentPruning.retiredOutput(new ItemStack(cone)), "Retired nose cone " + tier);
        }
        Item electric = item("extraplanets:item_tier10_electric_rocket");
        nasa.add(new NasaWorkbenchRecipe(new ItemStack(electric), new HashMap<>()));
        Item rover = item("extraplanets:mars_rover");
        nasa.add(new NasaWorkbenchRecipe(new ItemStack(rover), new HashMap<>()));
        check(ContentPruning.pruneNasa(nasa) == 29, "Remove every tier and cargo variant including electric rocket");
        check(nasa.size() == 1 && nasa.get(0).getRecipeOutput().getItem() == rover, "Preserve rover recipe");
        for (String color : new String[] {"blue", "red", "purple", "yellow", "green"})
            for (String part : new String[] {"pickaxe", "axe", "hoe", "shovel", "sword", "helmet", "chest", "legings", "boots"}) {
                Item tool = item("extraplanets:" + color + "_diamond_" + part);
                recipe("newearth:" + color + "_" + part, new ItemStack(tool));
            }
        Item raw = item("extraplanets:tier11_items");
        for (int meta = 0; meta <= 6; meta++)
            check(!ContentPruning.retiredOutput(new ItemStack(raw, 1, meta)), "Preserve five diamonds and platinum " + meta);
        Item other = item("anothermod:tier7_items");
        check(!ContentPruning.retiredOutput(new ItemStack(other)), "Do not match other mods by path alone");
        IRecipe retained = recipe("newearth:retained", new ItemStack(raw));
        check(ContentPruning.pruneCrafting(ForgeRegistries.RECIPES) == 73, "Remove 28 components and all 45 colored equipment recipes");
        check(ForgeRegistries.RECIPES.getValue(retained.getRegistryName()) == retained, "Preserve raw material crafting");
        check(ContentPruning.pruneCrafting(ForgeRegistries.RECIPES) == 0, "Crafting cleanup is idempotent");
        Item parts7 = ForgeRegistries.ITEMS.getValue(new ResourceLocation("extraplanets:tier7_items"));
        CompressorRecipes.addShapelessRecipe(new ItemStack(parts7, 2, 3), Items.IRON_INGOT);
        CompressorRecipes.addShapelessRecipe(new ItemStack(parts7, 1, 4), Items.GOLD_INGOT);
        CompressorRecipes.addShapelessRecipe(new ItemStack(parts7, 1, 6), Items.DIAMOND);
        check(ContentPruning.pruneCompressor() == 2, "Remove old heavy plate and reinforced intermediate regardless of output count");
        check(CompressorRecipes.getRecipeListAll().stream().noneMatch(r -> ContentPruning.retiredOutput(r.getRecipeOutput())), "No retired compressor output remains");
        check(CompressorRecipes.getRecipeListAll().stream().anyMatch(r -> r.getRecipeOutput().getItem() == parts7 && r.getRecipeOutput().getMetadata() == 4), "Preserve compressed resource recipe");
        check(ContentPruning.pruneCompressor() == 0, "Compressor cleanup is idempotent");
        Planet proxima = new Planet("proxima_b"); proxima.setTierRequired(6); GalaxyRegistry.registerPlanet(proxima);
        Planet barnarda = new Planet("barnarda_c"); barnarda.setTierRequired(6); GalaxyRegistry.registerPlanet(barnarda);
        Moon moon = new Moon("barnarda_c1").setParentPlanet(barnarda); moon.setTierRequired(6); GalaxyRegistry.registerMoon(moon);
        Planet kepler = new Planet("kepler22b"); kepler.setTierRequired(10); GalaxyRegistry.registerPlanet(kepler);
        SolarSystemBridge.install();
        check(proxima.getTierRequirement() == 7 && barnarda.getTierRequirement() == 7 && moon.getTierRequirement() == 7, "Preserve exoplanet T7 gates");
        check(kepler.getTierRequirement() == 10, "Keep Kepler gate pending custom T9");
        System.out.println("NewEarth progression: " + checks + " checks passed (isolated; no Forge server or flight)");
    }
}
