package newearth;

import java.util.ArrayList;
import java.util.List;
import micdoodle8.mods.galacticraft.api.recipe.CompressorRecipes;
import micdoodle8.mods.galacticraft.api.recipe.INasaWorkbenchRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;

/** Fixed pack manufacturing policy; item registrations and dungeon loot stay intact. */
@Mod.EventBusSubscriber(modid = NewEarth.MOD_ID)
public final class ContentPruning {
    private ContentPruning() { }

    static boolean retiredOutput(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null || !"extraplanets".equals(id.getNamespace())) return false;
        String name = id.getPath();
        for (int tier = 4; tier <= 10; tier++) {
            if (name.equals("item_tier" + tier + "_rocket")
                    || name.equals("nose_cone_tier" + tier)) return true;
            if (name.equals("tier" + tier + "_items"))
                return (stack.getMetadata() >= 0 && stack.getMetadata() <= 3)
                        || (tier == 7 && stack.getMetadata() == 6); // old plate-only reinforced crystal
        }
        if (name.equals("item_tier10_electric_rocket")) return true;
        // Keep the five diamond raw materials and their item registrations. EP's
        // recipe conditions do not honor its equipment-only config switch.
        for (String color : new String[] {"blue", "red", "purple", "yellow", "green"})
            for (String part : new String[] {"pickaxe", "axe", "hoe", "shovel", "sword",
                    "helmet", "chest", "legings", "boots"})
                if (name.equals(color + "_diamond_" + part)) return true;
        return false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void recipes(RegistryEvent.Register<IRecipe> event) {
        pruneCrafting(event.getRegistry());
    }

    static int pruneCrafting(IForgeRegistry<IRecipe> registry) {
        IForgeRegistryModifiable<IRecipe> mutable = (IForgeRegistryModifiable<IRecipe>) registry;
        List<ResourceLocation> remove = new ArrayList<>();
        for (IRecipe recipe : registry.getValuesCollection())
            if (retiredOutput(recipe.getRecipeOutput())) remove.add(recipe.getRegistryName());
        for (ResourceLocation name : remove) mutable.remove(name);
        NewEarth.LOG.info("Removed {} retired EP crafting recipes", remove.size());
        return remove.size();
    }

    static void install() {
        // Resolve every EP API before mutating lists, and leave rover recipes alone.
        List<List<INasaWorkbenchRecipe>> lists = new ArrayList<>();
        try {
            for (String tier : new String[] {"4", "5", "6", "7", "8", "9", "10", "10Electric"}) {
                Class<?> api = Class.forName("com.mjr.extraplanets.recipes.Tier" + tier + "RocketRecipes");
                @SuppressWarnings("unchecked")
                List<INasaWorkbenchRecipe> recipes = (List<INasaWorkbenchRecipe>) api
                        .getMethod("getTier" + tier + "RocketRecipes").invoke(null);
                lists.add(recipes);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("NewEarth requires the EP 0.8.0 rocket recipe APIs", e);
        }
        int removed = 0;
        for (List<INasaWorkbenchRecipe> recipes : lists) removed += pruneNasa(recipes);
        int compressed = pruneCompressor();
        NewEarth.LOG.info("Removed {} retired EP NASA recipes and {} compressor recipes", removed, compressed);
    }

    static int pruneNasa(List<INasaWorkbenchRecipe> recipes) {
        int previous = recipes.size();
        recipes.removeIf(recipe -> retiredOutput(recipe.getRecipeOutput()));
        return previous - recipes.size();
    }

    static int pruneCompressor() {
        int removed = 0;
        for (IRecipe recipe : new ArrayList<>(CompressorRecipes.getRecipeListAll())) {
            if (retiredOutput(recipe.getRecipeOutput())) {
                CompressorRecipes.removeRecipe(recipe.getRecipeOutput());
                removed++;
            }
        }
        return removed;
    }
}
