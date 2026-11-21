package newearth.compat.ae2;

import java.lang.reflect.Field;
import java.util.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import newearth.NewEarth;

/** Optional AE2 UEL configuration and acquisition rules; early feature filtering lives in ContentTransformer. */
@Mod.EventBusSubscriber(modid = NewEarth.MOD_ID)
public final class Ae2Integration {
    private Ae2Integration() { }

    public static void configure() {
        if (!Loader.isModLoaded("appliedenergistics2")) return;
        try {
            Class<?> type = Class.forName("appeng.core.AEConfig");
            Configuration config = (Configuration) type.getMethod("instance").invoke(null);
            // Channel behavior is filtered earlier by the existing ContentTransformer hook.
            applySettings(config);
            Class<?> featureType = Class.forName("appeng.core.features.AEFeature");
            for (Object feature : featureType.getEnumConstants())
                if (newearth.ContentTransformer.retiredAe2Feature(((Enum<?>) feature).name())) {
                    String category = (String) featureType.getMethod("category").invoke(feature);
                    String key = (String) featureType.getMethod("key").invoke(feature);
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
            if (object.has("ore") && "itemIlluminatedPanel".equals(object.get("ore").getAsString())) {
                object.remove("ore"); object.remove("type"); object.addProperty("item", "minecraft:glass");
            }
            for (Map.Entry<String,JsonElement> entry : object.entrySet()) rewriteRecipe(entry.getValue(), domain);
        } else if (json.isJsonArray()) for (JsonElement entry : json.getAsJsonArray()) rewriteRecipe(entry,domain);
    }

    public static boolean retired(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && "appliedenergistics2".equals(id.getNamespace())
                && retired(id.getPath(), stack.getMetadata());
    }

    public static boolean retired(String path, int meta) {
        if (path.equals("part")) return meta >= 40 && meta <= 76 || meta >= 500 && meta <= 516
                || meta == 160 || meta == 180 || meta == 200 || meta == 400 || meta == 420;
        if (path.equals("material")) return meta == 59 || meta >= 32 && meta <= 34;
        if (path.startsWith("spatial_")) return true;
        return Arrays.asList("controller", "portable_cell", "matter_cannon", "charged_staff",
                "biometric_card",
                "entropy_manipulator", "color_applicator", "paint_ball", "quantum_ring", "quantum_link",
                "vibration_chamber", "crafting_monitor").contains(path);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void recipes(RegistryEvent.Register<IRecipe> event) {
        if (!Loader.isModLoaded("appliedenergistics2")) return;
        IForgeRegistryModifiable<IRecipe> registry = (IForgeRegistryModifiable<IRecipe>) event.getRegistry();
        List<ResourceLocation> removed = new ArrayList<>();
        for (IRecipe recipe : registry.getValuesCollection())
            if (retired(recipe.getRecipeOutput())) removed.add(recipe.getRegistryName());
        for (ResourceLocation id : removed) registry.remove(id);
    }
}
