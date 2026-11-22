package newearth.compat.ae2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistryModifiable;
import newearth.ContentTransformer;
import newearth.NewEarth;

/** Optional AE2 UEL configuration and acquisition rules; early feature filtering lives in ContentTransformer. */
@Mod.EventBusSubscriber(modid = NewEarth.MOD_ID)
public final class Ae2Integration {
    private static final String[] RECIPE_INPUT_FIELDS = {"ore", "part", "material", "item"};

    private Ae2Integration() { }

    public static void configure() {
        if (!Loader.isModLoaded("appliedenergistics2")) return;
        try {
            Class<?> type = Class.forName("appeng.core.AEConfig");
            Configuration config = (Configuration) type.getMethod("instance").invoke(null);
            // Channel behavior is filtered earlier by the existing ContentTransformer hook.
            applySettings(config);
            Class<?> featureType = Class.forName("appeng.core.features.AEFeature");
            java.lang.reflect.Method categoryName = featureType.getMethod("category");
            java.lang.reflect.Method configKey = featureType.getMethod("key");
            for (Object feature : featureType.getEnumConstants())
                if (ContentTransformer.retiredAe2Feature(((Enum<?>) feature).name())) {
                    String category = (String) categoryName.invoke(feature);
                    String key = (String) configKey.invoke(feature);
                    config.get("features." + category, key, true).set(false);
                }
            Field preview = type.getDeclaredField("showPlacementPreview"); preview.setAccessible(true);
            preview.setBoolean(config, false);
            config.save();
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("NewEarth requires AE2 UEL v0.56.6 configuration API", error);
        }
    }

    static void applySettings(Configuration config) {
        config.get("features.networkfeatures", "Channels", true).set(false);
        config.get("client", "showPlacementPreview", true).set(false);
    }

    public static void rewriteRecipe(JsonElement json, String domain) {
        if (!"appliedenergistics2".equals(domain)) return;
        if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();
            if (object.has("name") && object.has("ingredient") && object.get("ingredient").isJsonArray()) {
                String name = object.get("name").getAsString();
                if (name.equals("appliedenergistics2:knife")) {
                    removeConstantOption(object, "item", "appliedenergistics2:certus_quartz_cutting_knife");
                } else if (name.equals("appliedenergistics2:cable")) {
                    removeConstantOption(object, "part", "cable_smart");
                }
            }
        }
        if (json.isJsonObject()) useSiliconPlateInMonitorRecipe(json.getAsJsonObject());
        rewriteInputs(json);
    }

    private static void useSiliconPlateInMonitorRecipe(JsonObject recipe) {
        JsonElement result = recipe.get("result");
        if (result == null || !result.isJsonObject() || !result.getAsJsonObject().has("part")
                || !"part.storage_monitor".equals(result.getAsJsonObject().get("part").getAsString())) return;
        for (JsonElement ingredient : recipe.getAsJsonArray("ingredients")) {
            if (ingredient.isJsonObject() && ingredient.getAsJsonObject().has("ore")
                    && "itemIlluminatedPanel".equals(ingredient.getAsJsonObject().get("ore").getAsString())) {
                JsonObject panel = ingredient.getAsJsonObject();
                panel.remove("ore");
                panel.addProperty("type", "appliedenergistics2:part");
                panel.addProperty("part", "material.silicon_print");
                return;
            }
        }
    }

    private static void removeConstantOption(JsonObject object, String key, String value) {
        Iterator<JsonElement> options = object.getAsJsonArray("ingredient").iterator();
        while (options.hasNext()) {
            JsonElement option = options.next();
            if (option.isJsonObject() && option.getAsJsonObject().has(key)
                    && value.equals(option.getAsJsonObject().get(key).getAsString())) options.remove();
        }
    }

    private static void rewriteInputs(JsonElement json) {
        if (json.isJsonArray()) {
            for (JsonElement entry : json.getAsJsonArray()) rewriteInputs(entry);
            return;
        }
        if (!json.isJsonObject()) return;
        JsonObject object = json.getAsJsonObject();
        for (String key : RECIPE_INPUT_FIELDS) {
            if (!object.has(key) || !object.get(key).isJsonPrimitive()) continue;
            String replacement = replacement(object.get(key).getAsString());
            if (replacement == null) continue;
            boolean condition = object.has("type") && object.get("type").getAsString().endsWith("_exists");
            object.remove(key);
            object.remove("type");
            if (condition) object.addProperty("type", "minecraft:item_exists");
            if (replacement.startsWith("gc/")) {
                object.addProperty("item", "galacticraftcore:basic_item");
                if (!condition) object.addProperty("data", Integer.parseInt(replacement.substring(3)));
            } else object.addProperty("item", "minecraft:" + replacement);
            return;
        }
        for (Map.Entry<String,JsonElement> entry : object.entrySet())
            if (!entry.getKey().equals("result")) rewriteInputs(entry.getValue());
    }

    private static String replacement(String name) {
        switch (name) {
            case "itemIlluminatedPanel":
            case "appliedenergistics2:quartz_glass":
                return "glass";
            case "appliedenergistics2:quartz_vibrant_glass":
                return "glowstone";
            case "#appliedenergistics2:certusCrystal":
            case "#appliedenergistics2:crystalQuartz":
            case "#appliedenergistics2:dustQuartz":
            case "#appliedenergistics2:netherCrystal":
            case "crystalCertusQuartz":
            case "dustCertusQuartz":
            case "dustNetherQuartz":
            case "material.certus_quartz_crystal":
            case "material.certus_quartz_crystal_charged":
            case "material.purified_certus_quartz_crystal":
            case "material.purified_nether_quartz_crystal":
                return "quartz";
            case "#appliedenergistics2:fluixCrystal":
            case "crystalFluix":
            case "material.fluix_crystal":
            case "material.purified_fluix_crystal":
                return "gc/8";
            case "dustFluix":
                return "redstone";
            case "#appliedenergistics2:dustEnder":
            case "dustEnder":
            case "dustEnderPearl":
                return "ender_pearl";
            case "material.basic_card":
            case "material.formation_core":
            case "material.cell1k_part":
                return "gc/13";
            case "material.advanced_card":
            case "material.fluix_pearl":
            case "material.annihilation_core":
            case "material.cell4k_part":
                return "gc/14";
            case "material.cell16k_part":
                return "diamond";
            case "material.cell64k_part":
                return "diamond_block";
            case "material.empty_storage_cell":
                return "gc/9";
            case "material.wireless":
                return "gc/19";
            case "material.wooden_gear":
            case "gearWood":
                return "planks";
            case "appliedenergistics2:sky_stone_block":
            case "appliedenergistics2:smooth_sky_stone_block":
                return "stonebrick";
            case "appliedenergistics2:fluix_block":
                return "iron_block";
            case "material.fluid_cell1k_part":
                return "bucket";
            case "material.fluid_cell4k_part":
                return "gold_block";
            case "material.fluid_cell16k_part":
                return "emerald";
            case "material.fluid_cell64k_part":
                return "emerald_block";
            default: return null;
        }
    }

    public static boolean removedMaterialOutput(JsonObject recipe, String domain) {
        if (!"appliedenergistics2".equals(domain) || !recipe.has("result") || !recipe.get("result").isJsonObject()) return false;
        JsonObject result = recipe.getAsJsonObject("result");
        if (result.has("item")) {
            String id = result.get("item").getAsString();
            if (replacement(id) != null) return true;
            String prefix = "appliedenergistics2:";
            if (id.startsWith(prefix)) {
                String path = id.substring(prefix.length());
                if (path.equals("crank") || retired(path, result.has("data") ? result.get("data").getAsInt() : 0))
                    return true;
            }
        }
        if (result.has("part")) {
            String part = result.get("part").getAsString();
            if (part.startsWith("paint_ball.") || part.startsWith("lumen_paint_ball.")) return true;
            if (part.startsWith("material.")) {
                String name = part.substring("material.".length()).toUpperCase(Locale.ROOT);
                return !ContentTransformer.allowAe2MaterialName(name);
            }
        }
        return false;
    }

    public static boolean retired(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && "appliedenergistics2".equals(id.getNamespace())
                && retired(id.getPath(), stack.getMetadata());
    }

    public static boolean retired(String path, int meta) {
        if (path.equals("part")) return meta >= 40 && meta <= 76 || meta >= 500 && meta <= 516
                || meta == 160 || meta == 180 || meta == 200;
        if (path.equals("material")) return meta == 59 || meta >= 32 && meta <= 34;
        if (path.startsWith("spatial_")) return true;
        switch (path) {
            case "controller":
            case "portable_cell":
            case "matter_cannon":
            case "charged_staff":
            case "biometric_card":
            case "entropy_manipulator":
            case "color_applicator":
            case "paint_ball":
            case "quantum_ring":
            case "quantum_link":
            case "vibration_chamber":
                return true;
            default:
                return false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void recipes(RegistryEvent.Register<IRecipe> event) {
        if (!Loader.isModLoaded("appliedenergistics2")) return;
        IForgeRegistryModifiable<IRecipe> registry = (IForgeRegistryModifiable<IRecipe>) event.getRegistry();
        List<ResourceLocation> removed = new ArrayList<>();
        for (IRecipe recipe : registry.getValuesCollection())
            if (retired(recipe.getRecipeOutput())) removed.add(recipe.getRegistryName());
        for (ResourceLocation id : removed) registry.remove(id);
        registerPressing();
    }

    private static void registerPressing() {
        try {
            Class<?> configType = Class.forName("appeng.core.AEConfig");
            Class<?> featureType = Class.forName("appeng.core.features.AEFeature");
            Object config = configType.getMethod("instance").invoke(null);
            java.lang.reflect.Method enabled = configType.getMethod("isFeatureEnabled", featureType);
            if (!(Boolean) enabled.invoke(config, featureType.getField("INSCRIBER").get(null))
                    || !(Boolean) enabled.invoke(config, featureType.getField("PRINTED_CIRCUITS").get(null))) return;
            boolean processors = (Boolean) enabled.invoke(config, featureType.getField("PROCESSORS").get(null));
            Item gc = Item.getByNameOrId("galacticraftcore:basic_item");
            Item material = Item.getByNameOrId("appliedenergistics2:material");
            if (gc == null || material == null) throw new IllegalStateException("Missing inscriber ingredients");
            Object api = Class.forName("appeng.api.AEApi").getMethod("instance").invoke(null);
            Object registries = Class.forName("appeng.api.IAppEngApi").getMethod("registries").invoke(api);
            Object inscriber = Class.forName("appeng.api.features.IRegistryContainer").getMethod("inscriber").invoke(registries);
            Class<?> recipeType = Class.forName("appeng.api.features.IInscriberRecipe");
            java.lang.reflect.Method addRecipe = Class.forName("appeng.api.features.IInscriberRegistry")
                    .getMethod("addRecipe", recipeType);
            for (Object recipe : pressingRecipes(Ae2Integration.class.getClassLoader(), new ItemStack(gc, 1, 2), material, processors))
                addRecipe.invoke(inscriber, recipe);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot register the NewEarth inscriber recipes", error);
        }
    }

    static List<Object> pressingRecipes(ClassLoader loader, ItemStack gcSilicon, Item material, boolean processors)
            throws ReflectiveOperationException {
        Class<?> modeType = Class.forName("appeng.api.features.InscriberProcessType", true, loader);
        Object press = modeType.getField("PRESS").get(null);
        Object inscribe = modeType.getField("INSCRIBE").get(null);
        // The upstream builder requires an occupied plate slot. The machine can match empty plate lists.
        Constructor<?> recipe = Class.forName("appeng.core.features.registries.inscriber.InscriberRecipe", true, loader)
                .getDeclaredConstructor(Collection.class, ItemStack.class, List.class, List.class, modeType);
        recipe.setAccessible(true);
        ItemStack siliconPlate = new ItemStack(material, 1, 20);
        List<Object> recipes = new ArrayList<>();
        List<ItemStack> empty = Collections.emptyList();
        recipes.add(recipe.newInstance(Collections.singletonList(gcSilicon), siliconPlate, empty, empty, inscribe));
        Item[] boardInputs = {Items.GOLD_INGOT, Items.QUARTZ, Items.DIAMOND};
        int[] boardTypes = {18, 16, 17};
        for (int i = 0; i < boardInputs.length; i++) {
            ItemStack board = new ItemStack(material, 1, boardTypes[i]);
            recipes.add(recipe.newInstance(Collections.singletonList(new ItemStack(boardInputs[i])), board, empty, empty, inscribe));
            if (processors) {
                recipes.add(recipe.newInstance(Collections.singletonList(new ItemStack(Items.REDSTONE)),
                        new ItemStack(material, 1, 22 + i), Collections.singletonList(board.copy()),
                        Collections.singletonList(siliconPlate.copy()), press));
            }
        }
        return recipes;
    }
}
