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
    public static void main(String[] args) throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard = new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Bootstrap.register();
        check(!ContentPruning.retiredOutput(null), "Null unlock stack is not a retired item");
        check(ContentPruning.allowSchematic(new micdoodle8.mods.galacticraft.core.schematic.SchematicRocketT1()),
                "GC built-in T1 page with null required item must register");
        check(!ContentPruning.allowRegistration(new Item(), "item_tier7_rocket"), "Rocket registration is rejected");
        check(!ContentPruning.allowRegistration(new Item(), "schematic_tier7"), "Old schematic registration is rejected");
        check(!ContentPruning.allowRegistration(new Item(), "blue_diamond_helmet"), "Colored armor registration is rejected");
        check(ContentPruning.allowRegistration(new Item(), "tier7_items"), "Mixed resource container remains registered");
        com.google.gson.JsonParser parser = new com.google.gson.JsonParser();
        check(ContentPruning.skipRecipe(parser.parse("{\"result\":{\"item\":\"blue_diamond_helmet\"}}").getAsJsonObject(), "extraplanets"), "Skip deleted output before parsing items");
        check(ContentPruning.skipRecipe(parser.parse("{\"result\":{\"item\":\"minecraft:paper\"},\"key\":{\"X\":{\"item\":\"extraplanets:nose_cone_tier7\"}}}").getAsJsonObject(), "anothermod"), "Skip recipes referencing deleted inputs");
        check(!ContentPruning.skipRecipe(parser.parse("{\"result\":{\"item\":\"anothermod:blue_diamond_helmet\"}}").getAsJsonObject(), "extraplanets"), "Do not skip another mod's similarly named output");
        check(!ContentPruning.skipRecipe(parser.parse("{\"result\":{\"item\":\"tier7_items\",\"data\":5}}").getAsJsonObject(), "extraplanets"), "Preserve crystal recipes");
        check(!ContentPruning.allowRegistration(new Item(), "oxygen_tank_light_full_black"), "Colored oxygen tank is not submitted");
        check(ContentPruning.allowRegistration(new Item(), "oxygen_tank_very_heavy_full"), "Preserve larger oxygen tanks");
        check(ContentPruning.allowRegistration(new Item(), "oxygen_tank_extremely_heavy_full"), "Preserve largest oxygen tanks");
        check(ContentPruning.skipRecipe(parser.parse("{\"result\":{\"item\":\"nuclear_bomb\"}}").getAsJsonObject(), "extraplanets"), "Skip disabled nuclear bomb recipe");
        check(ContentPruning.skipRecipe(parser.parse("{\"result\":{\"item\":\"fire_bomb\"}}").getAsJsonObject(), "extraplanets"), "Skip disabled fire bomb recipe");
        verifyContentTransforms();
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
        List<INasaWorkbenchRecipe> roverRecipes = new ArrayList<>();
        roverRecipes.add(new NasaWorkbenchRecipe(new ItemStack(rover), new HashMap<>()));
        nasa.add(new NasaWorkbenchRecipe(new ItemStack(new Item()), new HashMap<>()));
        check(ContentPruning.pruneNasa(nasa) == 30 && nasa.isEmpty(), "Clear dedicated rocket table including outputs without registered IDs");
        check(roverRecipes.size() == 1, "Separate rover recipe table remains intact");
        for (String color : new String[] {"blue", "red", "purple", "yellow", "green"})
            for (String part : new String[] {"pickaxe", "axe", "hoe", "shovel", "sword", "helmet", "chest", "legings", "boots"}) {
                Item tool = item("extraplanets:" + color + "_diamond_" + part);
                recipe("newearth:" + color + "_" + part, new ItemStack(tool));
            }
        Item raw = item("extraplanets:tier11_items");
        for (int meta = 0; meta <= 6; meta++)
            check(!ContentPruning.retiredOutput(new ItemStack(raw, 1, meta)), "Preserve five diamonds and platinum " + meta);
        Item other = item("anothermod:tier7_items");
        List<ItemStack> subItems = new ArrayList<>();
        Item container7 = ForgeRegistries.ITEMS.getValue(new ResourceLocation("extraplanets:tier7_items"));
        for (int meta = 0; meta <= 7; meta++) subItems.add(new ItemStack(container7, 1, meta));
        ContentPruning.filterSubItems(subItems);
        check(subItems.size() == 3 && subItems.get(0).getMetadata() == 4 && subItems.get(2).getMetadata() == 7,
                "Mixed container only enumerates retained crystal and gem materials");
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
        verifyMaterials();
        Planet proxima = new Planet("proxima_b"); proxima.setTierRequired(6); GalaxyRegistry.registerPlanet(proxima);
        Planet barnarda = new Planet("barnarda_c"); barnarda.setTierRequired(6); GalaxyRegistry.registerPlanet(barnarda);
        Moon moon = new Moon("barnarda_c1").setParentPlanet(barnarda); moon.setTierRequired(6); GalaxyRegistry.registerMoon(moon);
        Planet kepler = new Planet("kepler22b"); kepler.setTierRequired(10); GalaxyRegistry.registerPlanet(kepler);
        SolarSystemBridge.install();
        check(proxima.getTierRequirement() == 7 && barnarda.getTierRequirement() == 7 && moon.getTierRequirement() == 7, "Preserve exoplanet T7 gates");
        check(kepler.getTierRequirement() == 10, "Keep Kepler gate pending custom T9");
        System.out.println("NewEarth progression: " + checks + " checks passed (isolated; no Forge server or flight)");
    }

    public static final class PendingFixture {
        public static int submitted;
        public static void registerItem(Item item, String name) { submitted++; }
    }

    public static final class MaterialFixture {
        public static ItemStack make(Item item, int count, int meta) { return new ItemStack(item, count, meta); }
    }

    private static void verifyMaterials() throws Exception {
        item("galaxyspace:ingots"); item("galaxyspace:compressed_plates");
        item("galacticraftplanets:basic_item_venus"); item("extraplanets:ingot_lead");
        String[][] rows = {
            {"tier6_items", "5", "galaxyspace:ingots", "1"},
            {"tier6_items", "4", "galaxyspace:compressed_plates", "2"},
            {"tier5_items", "7", "galaxyspace:ingots", "2"},
            {"tier5_items", "6", "galaxyspace:compressed_plates", "3"},
            {"ingot_lead", "0", "galacticraftplanets:basic_item_venus", "1"}
        };
        String name = "newearth.ProgressionProbe$MaterialFixture";
        byte[] original;
        try (java.io.InputStream input = ProgressionProbe.class.getResourceAsStream("/" + name.replace('.', '/') + ".class")) { original = read(input); }
        byte[] patched = new ContentTransformer().transform(name, "com.mjr.extraplanets.recipes.ExtraPlanets_Recipes", original);
        Class<?> fixture = new ClassLoader(ProgressionProbe.class.getClassLoader()) {
            Class<?> loadFixture() { return defineClass(name, patched, 0, patched.length); }
        }.loadFixture();
        for (String[] row : rows) {
            Item old = Item.REGISTRY.getObject(new ResourceLocation("extraplanets:" + row[0]));
            int meta = Integer.parseInt(row[1]), targetMeta = Integer.parseInt(row[3]);
            ItemStack result = (ItemStack) fixture.getMethod("make", Item.class, int.class, int.class).invoke(null, old, 9, meta);
            check(result.getCount() == 9 && result.getMetadata() == targetMeta
                    && result.getItem().getRegistryName().toString().equals(row[2]), "EP bytecode emits canonical metal " + row[0] + ":" + meta);
            ItemStack legacy = new ItemStack(old, 7, meta);
            net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound(); tag.setString("test", "preserve"); legacy.setTagCompound(tag);
            ItemStack converted = MaterialUnification.canonical(legacy);
            check(converted.getCount() == 7 && converted.getTagCompound().equals(tag) && converted.getTagCompound() != tag,
                    "Stack conversion copies count and NBT " + row[0] + ":" + meta);
            com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse("{\"result\":{\"item\":\"" + row[0]
                    + "\",\"data\":" + meta + ",\"count\":9},\"ingredients\":[{\"item\":\"extraplanets:" + row[0] + "\",\"data\":" + meta + "}]}").getAsJsonObject();
            check(!ContentPruning.skipRecipe(json, "extraplanets"), "Metal recipe survives pruning");
            check(json.getAsJsonObject("result").get("item").getAsString().equals(row[2])
                    && json.getAsJsonObject("result").get("data").getAsInt() == targetMeta
                    && json.getAsJsonObject("result").get("count").getAsInt() == 9
                    && json.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString().equals(row[2]),
                    "Both recipe inputs and storage-block unpacking outputs are canonical");
            java.util.List<ItemStack> creative = new ArrayList<>(); creative.add(legacy); ContentPruning.filterSubItems(creative);
            check(creative.isEmpty(), "Legacy metal is hidden from meta enumeration");
        }
        MaterialUnification.conversions(ForgeRegistries.RECIPES);
        for (String[] row : rows) {
            IRecipe conversion = ForgeRegistries.RECIPES.getValue(new ResourceLocation("newearth:convert_" + row[0] + "_" + row[1]));
            check(conversion != null && conversion.getIngredients().size() == 1
                    && conversion.getIngredients().get(0).apply(new ItemStack(Item.REGISTRY.getObject(new ResourceLocation("extraplanets:" + row[0])), 1, Integer.parseInt(row[1])))
                    && conversion.getRecipeOutput().getCount() == 1 && conversion.getRecipeOutput().getItem().getRegistryName().toString().equals(row[2]),
                    "Old stock has exact one-for-one conversion");
        }
        ItemStack crystal = new ItemStack(Item.REGISTRY.getObject(new ResourceLocation("extraplanets:tier7_items")), 1, 5);
        check(MaterialUnification.canonical(crystal) == crystal, "Distinct crystal remains untouched");
        int before = CompressorRecipes.getRecipeListAll().size();
        MaterialUnification.addCompressor(new ItemStack(Item.REGISTRY.getObject(new ResourceLocation("galaxyspace:compressed_plates")), 1, 2), new Object[] {Items.IRON_INGOT});
        check(CompressorRecipes.getRecipeListAll().size() == before, "EP duplicate magnesium compression is omitted");
        Item oldLead = Item.REGISTRY.getObject(new ResourceLocation("extraplanets:ingot_lead"));
        oldLead.setCreativeTab(net.minecraft.creativetab.CreativeTabs.MATERIALS);
        MaterialUnification.install();
        NonNullList<ItemStack> search = NonNullList.create();
        oldLead.getSubItems(net.minecraft.creativetab.CreativeTabs.SEARCH, search);
        check(search.isEmpty(), "Standalone legacy lead leaves creative/search enumeration");
    }

    public static final class GearFixture {
        public static int submitted;
        public static void addGearObject(int id, micdoodle8.mods.galacticraft.api.item.EnumExtendedInventorySlot slot, Object gear) {
            submitted++;
        }
    }

    public static final class ModelFixture {
        public static int variants, models;
        public static void addVariants(String domain, String name, String... variants) { ModelFixture.variants++; }
        public static void registerModel(Item item, int meta,
                net.minecraft.client.renderer.block.model.ModelResourceLocation model) { models++; }
    }

    private static void verifyContentTransforms() throws Exception {
        ContentTransformer transformer = new ContentTransformer();
        String modelName = "newearth.ProgressionProbe$ModelFixture";
        final byte[] modelBytes;
        try (java.io.InputStream input = ProgressionProbe.class.getResourceAsStream("/" + modelName.replace('.', '/') + ".class")) {
            modelBytes = transformer.transform(modelName, "com.mjr.mjrlegendslib.util.ClientUtilities", read(input));
        }
        Class<?> modelType = new ClassLoader(ProgressionProbe.class.getClassLoader()) {
            Class<?> loadFixture() { return defineClass(modelName, modelBytes, 0, modelBytes.length); }
        }.loadFixture();
        java.lang.reflect.Method variants = modelType.getMethod("addVariants", String.class, String.class, String[].class);
        for (String id : new String[] {"thermal_cloth", "tier3_thermal_padding", "tier4_thermal_padding", "tier5_thermal_padding",
                "tier1_kit", "tier5_kit", "module_items"}) variants.invoke(null, "extraplanets", id, new String[] {"variant"});
        check(modelType.getField("variants").getInt(null) == 0, "Retired model variants never resolve absent registry entries");
        variants.invoke(null, "extraplanets", "tools", new String[] {"filter"});
        variants.invoke(null, "anothermod", "thermal_cloth", new String[] {"cloth"});
        check(modelType.getField("variants").getInt(null) == 2, "Retained and other-mod variants still register");
        java.lang.reflect.Method models = modelType.getMethod("registerModel", Item.class, int.class,
                net.minecraft.client.renderer.block.model.ModelResourceLocation.class);
        models.invoke(null, new Item().setRegistryName("extraplanets:thermal_cloth"), 0, null);
        models.invoke(null, new Item().setRegistryName("extraplanets:tier1_space_suit_helmet"), 0, null);
        check(modelType.getField("models").getInt(null) == 0, "Retired item objects never reach model loader");
        models.invoke(null, new Item().setRegistryName("extraplanets:tools"), 3, null);
        models.invoke(null, new Item().setRegistryName("anothermod:thermal_cloth"), 0, null);
        check(modelType.getField("models").getInt(null) == 2, "Retained and other-mod item models still register");
        String fixtureName = "newearth.ProgressionProbe$PendingFixture";
        byte[] original;
        try (java.io.InputStream input = ProgressionProbe.class.getResourceAsStream("/" + fixtureName.replace('.', '/') + ".class")) {
            original = read(input);
        }
        final byte[] fixture = transformer.transform(fixtureName, "com.mjr.extraplanets.items.ExtraPlanets_Items", original);
        Class<?> executed = new ClassLoader(ProgressionProbe.class.getClassLoader()) {
            Class<?> loadFixture() { return defineClass(fixtureName, fixture, 0, fixture.length); }
        }.loadFixture();
        java.lang.reflect.Method submit = executed.getMethod("registerItem", Item.class, String.class);
        submit.invoke(null, new Item(), "item_tier7_rocket");
        submit.invoke(null, new Item(), "blue_diamond_helmet");
        check(executed.getField("submitted").getInt(null) == 0, "Transformed registration never submits retired IDs");
        submit.invoke(null, new Item(), "tier7_items");
        check(executed.getField("submitted").getInt(null) == 1, "Transformed registration submits retained material container");
        String gearFixtureName = "newearth.ProgressionProbe$GearFixture";
        byte[] gearOriginal;
        try (java.io.InputStream input = ProgressionProbe.class.getResourceAsStream("/" + gearFixtureName.replace('.', '/') + ".class")) {
            gearOriginal = read(input);
        }
        final byte[] gearFixture = transformer.transform(gearFixtureName, "micdoodle8.mods.galacticraft.api.GalacticraftRegistry", gearOriginal);
        Class<?> gearExecuted = new ClassLoader(ProgressionProbe.class.getClassLoader()) {
            Class<?> loadFixture() { return defineClass(gearFixtureName, gearFixture, 0, gearFixture.length); }
        }.loadFixture();
        java.lang.reflect.Method gearSubmit = gearExecuted.getMethod("addGearObject", int.class,
                micdoodle8.mods.galacticraft.api.item.EnumExtendedInventorySlot.class, Object.class);
        Item retiredTank = new Item().setRegistryName("extraplanets:oxygen_tank_med_full_black");
        Object tankSlot = micdoodle8.mods.galacticraft.api.item.EnumExtendedInventorySlot.LEFT_TANK;
        gearSubmit.invoke(null, 9001, tankSlot, retiredTank);
        gearSubmit.invoke(null, 9002, tankSlot, new ItemStack(retiredTank));
        check(gearExecuted.getField("submitted").getInt(null) == 0, "Both Item and ItemStack retired gear registrations are blocked");
        gearSubmit.invoke(null, 9003, tankSlot, new Item().setRegistryName("anothermod:oxygen_tank_med_full_black"));
        gearSubmit.invoke(null, 9004, tankSlot, new Item().setRegistryName("extraplanets:oxygen_tank_very_heavy_full"));
        check(gearExecuted.getField("submitted").getInt(null) == 2, "Other mod and capacity-upgrade gear remains registered");
        String helper = "net/minecraftforge/common/crafting/CraftingHelper";
        try (java.io.InputStream input = ProgressionProbe.class.getResourceAsStream("/" + helper + ".class")) {
            verifyPatched(transformer, helper.replace('/', '.'), read(input));
        }
        String schematicRegistry = "micdoodle8/mods/galacticraft/api/recipe/SchematicRegistry";
        try (java.io.InputStream input = ProgressionProbe.class.getResourceAsStream("/" + schematicRegistry + ".class")) {
            verifyPatched(transformer, schematicRegistry.replace('/', '.'), read(input));
        }
        String runtime = System.getenv("NEWEARTH_RUNTIME_MODS");
        if (runtime != null && !runtime.isEmpty()) {
            java.util.Set<String> required = new java.util.HashSet<>(java.util.Arrays.asList(
                    "com.mjr.extraplanets.items.ExtraPlanets_Items", "com.mjr.mjrlegendslib.item.ItemBasicMeta",
                    "com.mjr.extraplanets.recipes.ExtraPlanets_Recipes", "com.mjr.extraplanets.ExtraPlanets",
                    "com.mjr.mjrlegendslib.util.RegisterUtilities", "micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry",
                    "micdoodle8.mods.galacticraft.api.GalacticraftRegistry", "com.mjr.mjrlegendslib.util.ClientUtilities"));
            int coloredRecipes = 0, metalRecipes = 0;
            for (java.io.File file : new java.io.File(runtime).listFiles()) if (file.getName().endsWith(".jar")) {
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(file)) {
                    if (jar.getJarEntry("com/mjr/extraplanets/ExtraPlanets.class") != null) {
                        java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            java.util.jar.JarEntry entry = entries.nextElement();
                            if (entry.getName().startsWith("assets/extraplanets/recipes/") && entry.getName().endsWith(".json")) {
                                try (java.io.InputStream input = jar.getInputStream(entry)) {
                                    com.google.gson.JsonElement json = new com.google.gson.JsonParser().parse(
                                            new String(read(input), java.nio.charset.StandardCharsets.UTF_8));
                                    String before = json.toString();
                                    MaterialUnification.rewrite(json, "extraplanets");
                                    if (!before.equals(json.toString())) {
                                        metalRecipes++;
                                        String after = json.toString(); MaterialUnification.rewrite(json, "extraplanets");
                                        check(after.equals(json.toString()), "Installed metal recipe rewrite is stable " + entry.getName());
                                    }
                                }
                            }
                            if (!entry.getName().matches("assets/extraplanets/recipes/oxygen_tank_(light|med|heavy)_full_.*\\.json")) continue;
                            try (java.io.InputStream input = jar.getInputStream(entry)) {
                                com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(
                                        new String(read(input), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                                check(ContentPruning.skipRecipe(json, "extraplanets"), "Installed colored tank recipe filtered " + entry.getName());
                                String output = json.getAsJsonObject("result").get("item").getAsString();
                                check(ContentPruning.retiredItem(output.substring("extraplanets:".length())), "Installed tank ID is retired " + output);
                                coloredRecipes++;
                            }
                        }
                    }
                    for (String name : new ArrayList<>(required)) {
                        java.util.jar.JarEntry entry = jar.getJarEntry(name.replace('.', '/') + ".class");
                        if (entry == null) continue;
                        try (java.io.InputStream input = jar.getInputStream(entry)) { verifyPatched(transformer, name, read(input)); }
                        required.remove(name);
                    }
                }
            }
            check(required.isEmpty(), "Installed EP and MJR content targets were verified");
            check(coloredRecipes == 45, "All 45 installed colored oxygen tank recipes are covered");
            check(metalRecipes >= 20, "Installed EP metal recipes, machines and storage recipes are covered: " + metalRecipes);
        }
    }

    private static byte[] read(java.io.InputStream input) throws java.io.IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    private static void verifyPatched(ContentTransformer transformer, String name, byte[] original) throws Exception {
        byte[] patched = transformer.transform(name, name, original);
        check(!java.util.Arrays.equals(patched, original), "Content patch applied " + name);
        org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        new org.objectweb.asm.ClassReader(patched).accept(node, org.objectweb.asm.ClassReader.EXPAND_FRAMES);
        for (org.objectweb.asm.tree.MethodNode method : node.methods)
            if ((method.access & (org.objectweb.asm.Opcodes.ACC_ABSTRACT | org.objectweb.asm.Opcodes.ACC_NATIVE)) == 0)
                new org.objectweb.asm.tree.analysis.Analyzer<>(new org.objectweb.asm.tree.analysis.BasicVerifier()).analyze(node.name, method);
        check(true, "Content bytecode verified " + name);
    }
}
