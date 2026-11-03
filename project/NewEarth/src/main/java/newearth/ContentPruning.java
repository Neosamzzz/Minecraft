package newearth;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
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

/** Fixed pack item/recipe policy; retained resource IDs stay stable. */
@Mod.EventBusSubscriber(modid = NewEarth.MOD_ID)
public final class ContentPruning {
    private ContentPruning() { }

    public static boolean retiredItem(String name) {
        if (name.equals("ingot_lead") || name.equals("tier6_items")) return true;
        if (name.matches("tier[0-4]_(space_suit|un_prepared_space_suit)_.*")
                || name.matches("tier[1-4]_(pressure|radiation|armor)_layer")
                || name.matches("tier[3-5]_thermal_padding") || name.matches("tier[1-5]_kit")
                || name.equals("thermal_cloth") || name.equals("cloth") || name.equals("gravity_controller")
                || name.equals("module_items") || name.equals("enviro_emergency_kit")) return true;
        if (name.equals("nuclear_bomb") || name.equals("fire_bomb") || name.equals("debugworldfilegen")) return true;
        if (name.startsWith("oxygen_tank_light_full_") || name.startsWith("oxygen_tank_med_full_")
                || name.startsWith("oxygen_tank_heavy_full_")) return true;
        for (int tier = 4; tier <= 10; tier++)
            if (name.equals("item_tier" + tier + "_rocket") || name.equals("nose_cone_tier" + tier)
                    || name.equals("schematic_tier" + tier)) return true;
        if (name.equals("item_tier10_electric_rocket") || name.equals("schematic_tier10_electric_rocket")) return true;
        for (String material : new String[] {"mercury", "carbon", "uranium", "palladium", "magnesium", "crystal",
                "tungsten", "red_gem", "blue_gem", "white_gem", "zinc", "blue_diamond", "red_diamond",
                "purple_diamond", "yellow_diamond", "green_diamond"})
            for (String part : new String[] {"pickaxe", "axe", "hoe", "shovel", "sword", "helmet", "chest", "legings", "boots"})
                if (name.equals(material + "_" + part)) return true;
        return false;
    }

    // Called before EP appends to its pending item registry list. Retain Java
    // objects for EP's internal initialization, but never submit these IDs.
    public static boolean allowRegistration(Item item, String name) {
        if (!retiredItem(name)) return true;
        if (item.getRegistryName() == null) item.setRegistryName(new ResourceLocation("extraplanets", name));
        return false;
    }

    public static boolean skipRecipe(JsonObject json, String domain) {
        if (MaterialUnification.storageRecipe(json, domain)) return true;
        MaterialUnification.rewrite(json, domain);
        // The retained machine filter originally consumed EP spacesuit cloth.
        if ("extraplanets".equals(domain) && json.has("result") && json.get("result").isJsonObject()) {
            JsonObject result = json.getAsJsonObject("result");
            if (result.has("item") && "extraplanets:tools".equals(result.get("item").getAsString())
                    && result.has("data") && result.get("data").getAsInt() == 3 && json.has("key")) {
                JsonObject cloth = json.getAsJsonObject("key").getAsJsonObject("C");
                if (cloth != null && cloth.has("item") && "extraplanets:cloth".equals(cloth.get("item").getAsString())) {
                    cloth.addProperty("item", "minecraft:wool");
                    cloth.addProperty("data", 32767);
                }
            }
        }
        return referencesRetired(json, domain);
    }

    private static boolean referencesRetired(JsonElement element, String domain) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("item") && object.get("item").isJsonPrimitive()) {
                String id = object.get("item").getAsString();
                if (id.indexOf(':') < 0) id = domain + ":" + id;
                if (id.startsWith("extraplanets:")) {
                    String name = id.substring("extraplanets:".length());
                    if (retiredItem(name)) return true;
                    int meta = object.has("data") ? object.get("data").getAsInt() : 0;
                    if (retiredMeta(name, meta)) return true;
                }
            }
            for (java.util.Map.Entry<String, JsonElement> entry : object.entrySet())
                if (referencesRetired(entry.getValue(), domain)) return true;
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray())
                if (referencesRetired(child, domain)) return true;
        }
        return false;
    }

    private static boolean retiredMeta(String name, int meta) {
        for (int tier = 4; tier <= 10; tier++)
            if (name.equals("tier" + tier + "_items"))
                return (meta >= 0 && meta <= 3) || (tier == 7 && meta == 6);
        return false;
    }

    public static void filterSubItems(List<ItemStack> stacks) {
        stacks.removeIf(ContentPruning::retiredOutput);
        stacks.removeIf(MaterialUnification::removed);
    }

    public static boolean allowGear(Object gear) {
        if (gear instanceof Item) {
            ResourceLocation name = ((Item) gear).getRegistryName();
            return name == null || !"extraplanets".equals(name.getNamespace()) || !retiredItem(name.getPath());
        }
        return !(gear instanceof ItemStack) || !retiredOutput((ItemStack) gear);
    }

    public static boolean allowVariants(String domain, String name) {
        return !"extraplanets".equals(domain) || !retiredItem(name);
    }

    public static boolean allowEntityRegistration(Class<?> type) {
        if (!type.getName().startsWith("com.mjr.extraplanets.entities.")) return true;
        String name = type.getSimpleName();
        if (name.equals("EntityElectricRocket")) return false;
        for (int tier = 4; tier <= 10; tier++) if (name.equals("EntityTier" + tier + "Rocket")) return false;
        return true;
    }

    public static boolean allowSchematic(micdoodle8.mods.galacticraft.api.recipe.ISchematicPage page) {
        // GC's built-in T1 page intentionally has no unlock item. Only EP's
        // own pages are in scope, so never query unrelated pages for an item.
        if (!page.getClass().getName().startsWith("com.mjr.extraplanets.schematicPages.")) return true;
        return !retiredOutput(page.getRequiredItem());
    }

    static boolean retiredOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null || !"extraplanets".equals(id.getNamespace())) return false;
        String name = id.getPath();
        return retiredItem(name) || retiredMeta(name, stack.getMetadata()) || MaterialUnification.removed(stack);
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
        // Only called on EP's eight dedicated rocket tables, never rover tables.
        // Do not depend on an output ID: retired/unregistered objects may lack one.
        recipes.clear();
        return previous;
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
