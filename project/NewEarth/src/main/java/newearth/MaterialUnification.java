package newearth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import micdoodle8.mods.galacticraft.api.recipe.CompressorRecipes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

/** Canonical pack metals and removal of superseded EP material variants. */
public final class MaterialUnification {
    private MaterialUnification() { }

    // source ID, source metadata, target ID, target metadata
    private static final String[][] METALS = {
        {"extraplanets:tier6_items", "5", "galaxyspace:ingots", "1"},
        {"extraplanets:tier6_items", "4", "galaxyspace:compressed_plates", "2"},
        {"extraplanets:tier5_items", "7", "galaxyspace:ingots", "2"},
        {"extraplanets:tier5_items", "6", "galaxyspace:compressed_plates", "3"},
        {"extraplanets:ingot_lead", "0", "galacticraftplanets:basic_item_venus", "1"},
        {"extraplanets:saturn", "7", "galaxyspace:blocksmetals", "2"},
        {"extraplanets:jupiter", "13", "galaxyspace:blocksmetals", "1"}
    };

    private static String[] replacement(String id, int meta) {
        for (String[] row : METALS)
            if (row[0].equals(id) && Integer.parseInt(row[1]) == meta) return row;
        return null;
    }

    public static boolean removed(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem().getRegistryName() != null
                && replacement(stack.getItem().getRegistryName().toString(), stack.getMetadata()) != null;
    }

    private static ItemStack stack(String id, int count, int meta) {
        Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
        if (item == null || item == net.minecraft.init.Items.AIR)
            throw new IllegalStateException("Missing canonical metal: " + id);
        return new ItemStack(item, count, meta);
    }

    /** Called only on stacks built by EP's recipe and salvage registration. */
    public static ItemStack canonical(ItemStack original) {
        if (!removed(original)) return original;
        String[] row = replacement(original.getItem().getRegistryName().toString(), original.getMetadata());
        ItemStack result = stack(row[2], original.getCount(), Integer.parseInt(row[3]));
        if (original.hasTagCompound()) result.setTagCompound(original.getTagCompound().copy());
        return result;
    }

    public static void rewrite(JsonElement element, String domain) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("item") && object.get("item").isJsonPrimitive()) {
                String id = object.get("item").getAsString();
                if (id.indexOf(':') < 0) id = domain + ":" + id;
                int meta = object.has("data") ? object.get("data").getAsInt() : 0;
                String[] row = replacement(id, meta);
                if (row != null) {
                    object.addProperty("item", row[2]);
                    object.addProperty("data", Integer.parseInt(row[3]));
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) rewrite(entry.getValue(), domain);
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) rewrite(child, domain);
        }
    }

    public static boolean storageRecipe(JsonElement element, String domain) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("item") && object.get("item").isJsonPrimitive()) {
                String id = object.get("item").getAsString();
                if (id.indexOf(':') < 0) id = domain + ":" + id;
                int meta = object.has("data") ? object.get("data").getAsInt() : 0;
                if ((id.equals("extraplanets:saturn") && meta == 7)
                        || (id.equals("extraplanets:jupiter") && meta == 13)) return true;
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet())
                if (storageRecipe(entry.getValue(), domain)) return true;
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) if (storageRecipe(child, domain)) return true;
        }
        return false;
    }

    public static void registerOre(String name, ItemStack value) {
        // GS/GC register the canonical metals themselves. Do not put superseded
        // stacks (including unregistered standalone items) in the ore dictionary.
        if (!removed(value)) OreDictionary.registerOre(name, value);
    }

    public static boolean visible(ItemStack value) {
        return !removed(value) && !ContentPruning.retiredOutput(value);
    }

    public static void addCompressor(ItemStack output, Object[] inputs) {
        // GS already registers two ingots -> one plate using ore dictionary inputs.
        ResourceLocation id = output.getItem().getRegistryName();
        if (new ResourceLocation("galaxyspace:compressed_plates").equals(id)
                && (output.getMetadata() == 2 || output.getMetadata() == 3)) return;
        CompressorRecipes.addShapelessRecipe(output, inputs);
    }

    static void install() {
        for (String[] row : METALS) stack(row[2], 1, Integer.parseInt(row[3]));
    }
}
