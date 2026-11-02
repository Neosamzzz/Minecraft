package newearth;

import java.util.ArrayList;
import java.util.List;
import galaxyspace.systems.SolarSystem.planets.overworld.recipes.AssemblyRecipes;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.Moon;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.api.recipe.INasaWorkbenchRecipe;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Real MC inventory, GS assembler matcher and GC NASA recipes; test items stand in for GS/EP registration. */
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
    public static void main(String[] args) {
        net.minecraft.launchwrapper.Launch.blackboard = new java.util.HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Bootstrap.register();
        Planet proxima = new Planet("proxima_b"); proxima.setTierRequired(6); GalaxyRegistry.registerPlanet(proxima);
        Planet barnarda = new Planet("barnarda_c"); barnarda.setTierRequired(6); GalaxyRegistry.registerPlanet(barnarda);
        Moon moon = new Moon("barnarda_c1").setParentPlanet(barnarda); moon.setTierRequired(6); GalaxyRegistry.registerMoon(moon);
        Planet mars = new Planet("probe_mars"); mars.setTierRequired(2); GalaxyRegistry.registerPlanet(mars);
        Planet kepler = new Planet("kepler22b"); kepler.setTierRequired(10); GalaxyRegistry.registerPlanet(kepler);
        List<INasaWorkbenchRecipe> nasa = new ArrayList<>();
        int initialRecipes = AssemblyRecipes.getRecipeList().size();
        try {
            SolarSystemBridge.install(nasa);
            throw new AssertionError("Missing EP registration must fail explicitly");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("item_tier7_rocket"), "Missing item is named");
        }
        check(proxima.getTierRequirement() == 6, "Invalid setup does not change tiers");
        check(nasa.isEmpty() && AssemblyRecipes.getRecipeList().size() == initialRecipes, "Invalid setup adds no recipes");
        Item rocket = item("extraplanets:item_tier7_rocket");
        Item schematic = item("extraplanets:schematic_tier7");
        Item parts = item("galaxyspace:rocket_parts");
        Item modules = item("galaxyspace:rocket_modules");
        Item plates = item("galaxyspace:hdp");
        SolarSystemBridge.install(nasa);
        check(proxima.getTierRequirement() == 7 && barnarda.getTierRequirement() == 7 && moon.getTierRequirement() == 7, "Loaded exoplanets and moon require T7");
        check(mars.getTierRequirement() == 2, "Solar-system tier is unchanged");
        check(kepler.getTierRequirement() == 10, "Kepler remains gated until custom T9 exists");
        check(!GalaxyRegistry.getRegisteredPlanets().containsKey("tauceti_f"), "Disabled destination stays absent");
        Container container = new Container() {
            @Override public boolean canInteractWith(EntityPlayer player) { return true; }
        };
        InventoryCrafting crafting = new InventoryCrafting(container, 3, 3);
        // Deliberately reorder inputs to exercise the real GS shapeless matcher.
        crafting.setInventorySlotContents(0, new ItemStack(plates, 1, 2));
        crafting.setInventorySlotContents(1, new ItemStack(Items.PAPER));
        crafting.setInventorySlotContents(3, new ItemStack(plates, 1, 2));
        crafting.setInventorySlotContents(5, new ItemStack(plates, 1, 2));
        crafting.setInventorySlotContents(7, new ItemStack(modules, 1, 10));
        crafting.setInventorySlotContents(8, new ItemStack(plates, 1, 2));
        check(AssemblyRecipes.findMatchingRecipe(crafting, null).getItem() == schematic, "Schematic is craftable in GS generic assembler");
        crafting.setInventorySlotContents(7, ItemStack.EMPTY);
        check(AssemblyRecipes.findMatchingRecipe(crafting, null).isEmpty(), "Schematic requires T6 licence");
        crafting.setInventorySlotContents(7, new ItemStack(modules, 1, 10));
        crafting.setInventorySlotContents(0, new ItemStack(plates, 1, 1));
        check(AssemblyRecipes.findMatchingRecipe(crafting, null).isEmpty(), "Schematic rejects T5 plate");
        check(nasa.size() == 8, "All eight cargo placements are registered");
        for (int mask = 0; mask < 8; mask++) {
            InventoryBasic inventory = new InventoryBasic("probe", false, 22);
            inventory.setInventorySlotContents(1, new ItemStack(parts, 1, 15));
            for (int slot = 2; slot <= 11; slot++) inventory.setInventorySlotContents(slot, new ItemStack(plates, 1, 2));
            inventory.setInventorySlotContents(12, new ItemStack(parts, 1, 18));
            inventory.setInventorySlotContents(16, new ItemStack(parts, 1, 18));
            inventory.setInventorySlotContents(15, new ItemStack(parts, 1, 17));
            for (int slot : new int[] {13, 14, 17, 18}) inventory.setInventorySlotContents(slot, new ItemStack(parts, 1, 19));
            for (int i = 0; i < 3; i++) if ((mask & (1 << i)) != 0) inventory.setInventorySlotContents(19 + i, new ItemStack(Blocks.CHEST));
            INasaWorkbenchRecipe matched = null;
            for (INasaWorkbenchRecipe recipe : nasa) if (recipe.matches(inventory)) {
                check(matched == null, "No ambiguous cargo recipes " + mask);
                matched = recipe;
            }
            check(matched != null && matched.getRecipeOutput().getItem() == rocket && matched.getRecipeOutput().getMetadata() == Integer.bitCount(mask), "Correct cargo variant " + mask);
            check(!matched.getRecipeOutput().hasTagCompound(), "No fabricated fuel/inventory NBT");
            inventory.setInventorySlotContents(2, new ItemStack(plates, 1, 1));
            check(nasa.stream().noneMatch(r -> r.matches(inventory)), "T5 plate rejected " + mask);
            inventory.setInventorySlotContents(2, new ItemStack(plates, 1, 2));
            inventory.setInventorySlotContents(15, ItemStack.EMPTY);
            check(nasa.stream().noneMatch(r -> r.matches(inventory)), "Engine required " + mask);
        }
        System.out.println("NewEarth progression: " + checks + " checks passed (isolated; no Forge server or flight)");
    }
}
