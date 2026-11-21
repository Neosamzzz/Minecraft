package newearth.compat.ae2;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import net.minecraftforge.common.config.Configuration;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Configuration merging and pinned UEL bytecode regression; no game or save is opened. */
public final class Ae2Probe {
    private static int checks;
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label); checks++;
    }
    public static void main(String[] args) throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard = new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        java.lang.reflect.Field home = net.minecraftforge.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true); home.set(null, new File(".").getAbsoluteFile());
        Configuration config = new Configuration(new File("merge.cfg"));
        config.get("features.networkfeatures", "Channels", true).set(true);
        config.get("client", "showPlacementPreview", true).set(true);
        config.get("client", "SEARCH_MODE", "MANUAL_SEARCH").set("JEI_MANUAL_SEARCH");
        config.get("battery", "wirelessTerminal", 1600000).set(123456);
        Ae2Integration.applySettings(config); config.save();
        Configuration reread = new Configuration(new File("merge.cfg")); reread.load();
        check(!reread.get("features.networkfeatures", "Channels", true).getBoolean(), "Channel setting persists");
        check(!reread.get("client", "showPlacementPreview", true).getBoolean(), "Preview setting persists");
        check(reread.get("client", "SEARCH_MODE", "").getString().equals("JEI_MANUAL_SEARCH"), "Existing user search setting preserved");
        check(reread.get("battery", "wirelessTerminal", 0).getInt() == 123456, "Existing battery settings preserved");
        Ae2Integration.applySettings(config);
        check(!config.get("features.networkfeatures", "Channels", true).getBoolean(), "Applying policy twice is harmless");
        Files.delete(Paths.get("merge.cfg"));
        for (int color = 0; color < 17; color++) {
            check(!Ae2Integration.retired("part", color), "Basic cable retained " + color);
            check(!Ae2Integration.retired("part", 20 + color), "Covered cable retained " + color);
            check(Ae2Integration.retired("part", 40 + color), "Smart cable retired " + color);
            check(Ae2Integration.retired("part", 60 + color), "Dense smart cable retired " + color);
            check(Ae2Integration.retired("part", 500 + color), "Dense covered cable retired " + color);
        }
        for (int meta : new int[]{220,240,260,340,341,360,380,440,480})
            check(!Ae2Integration.retired("part", meta), "Core storage and crafting part retained " + meta);
        check(Ae2Integration.retired("material",59), "Quantum wireless card retired");
        check(!Ae2Integration.retired("material",53), "Crafting card retained until replacement exists");
        check(Ae2Integration.retired("biometric_card",0), "Identity card removed from acquisition");
        byte[] absent = {1,2,3};
        newearth.ContentTransformer transformer = new newearth.ContentTransformer();
        check(transformer.transform("unrelated","unrelated",absent) == absent, "Optional mod absence leaves unrelated classes unchanged");
        check(transformer.transform("unrelated","unrelated",null) == null, "Absent class safe");
        String directory = System.getenv("NEWEARTH_RUNTIME_MODS");
        String explicitJar = System.getenv("NEWEARTH_AE2_JAR");
        if (directory == null && explicitJar == null) throw new AssertionError("Set NEWEARTH_AE2_JAR or NEWEARTH_RUNTIME_MODS");
        File runtimeJar = explicitJar != null ? new File(explicitJar) : Paths.get(directory,"ae2-uel-v0.56.6.jar").toFile();
        try (ZipFile jar = new ZipFile(runtimeJar)) {
            byte[] original = read(jar,"appeng/core/AEConfig.class");
            byte[] patched = transformer.transform("appeng.core.AEConfig","appeng.core.AEConfig",original);
            check(!Arrays.equals(original,patched), "Existing early feature hook patches pinned UEL");
            check(transformer.transform("appeng.core.AEConfig","appeng.core.AEConfig",patched) == patched, "Early feature hook is idempotent");
            ClassNode settings = node(patched);
            for (MethodNode method : settings.methods) if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0)
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(settings.name,method);
            check(settings.fields.stream().anyMatch(f -> f.name.equals("showPlacementPreview") && f.desc.equals("Z")), "Installed preview field matches configuration hook");
            Set<String> features = new HashSet<>();
            for (FieldNode field : node(read(jar,"appeng/core/features/AEFeature.class")).fields) features.add(field.name);
            for (String feature : new String[]{"CHANNELS","QUANTUM_NETWORK_BRIDGE","QUANTUM_LINKING_CARD","SPATIAL_IO",
                    "CERTUS_ORE","CHARGED_CERTUS_ORE","CERTUS_QUARTZ_WORLD_GEN","METEORITE_WORLD_GEN","SPAWN_PRESSES_IN_METEORITES","METEORITE_COMPASS",
                    "SMART_CABLES","DENSE_CABLES","PORTABLE_CELL","MATTER_CANNON","CHARGED_STAFF","ENTROPY_MANIPULATOR",
                    "COLOR_APPLICATOR","PAINT_BALLS","POWER_GEN","PANELS"}) {
                check(features.contains(feature), "Retired feature exists in pinned version: " + feature);
                check(newearth.ContentTransformer.retiredAe2Feature(feature), "Selected feature disabled: " + feature);
            }
            for (String feature : new String[]{"STORAGE_CELLS","CRAFTING_CPU","PATTERNS","ENERGY_ACCEPTOR","WIRELESS_ACCESS_TERMINAL"})
                check(!newearth.ContentTransformer.retiredAe2Feature(feature), "Essential feature retained: " + feature);
            for (String recipe : new String[]{"tools/network_tool", "network/parts/terminals", "network/parts/terminals_interface"}) {
                String originalJson = new String(read(jar,"assets/appliedenergistics2/recipes/" + recipe + ".json"),"UTF-8");
                com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(originalJson).getAsJsonObject();
                String output = json.get("result").toString();
                Ae2Integration.rewriteRecipe(json,"appliedenergistics2");
                check(!json.toString().contains("itemIlluminatedPanel") && json.toString().contains("minecraft:glass"), "Actual core recipe survives panel removal: " + recipe);
                check(json.get("result").toString().equals(output), "Core recipe output unchanged: " + recipe);
                com.google.gson.JsonObject other = new com.google.gson.JsonParser().parse(originalJson).getAsJsonObject();
                Ae2Integration.rewriteRecipe(other,"other_mod");
                check(other.toString().contains("itemIlluminatedPanel"), "Unrelated mod recipes not rewritten");
            }
            for (String name : new String[]{"network/blocks/controller", "misc/grindstone_crank", "tools/paintballs_lumen_pink"}) {
                com.google.gson.JsonObject recipe = new com.google.gson.JsonParser().parse(new String(
                        read(jar, "assets/appliedenergistics2/recipes/" + name + ".json"), "UTF-8")).getAsJsonObject();
                check(Ae2Integration.removedMaterialOutput(recipe, "appliedenergistics2"),
                        "Retired recipe is skipped before ingredient lookup: " + name);
            }
            com.google.gson.JsonArray constants = new com.google.gson.JsonParser().parse(new String(
                    read(jar, "assets/appliedenergistics2/recipes/_constants.json"), "UTF-8")).getAsJsonArray();
            for (com.google.gson.JsonElement constant : constants) {
                com.google.gson.JsonObject definition = constant.getAsJsonObject();
                Ae2Integration.rewriteRecipe(definition, "appliedenergistics2");
                if ("appliedenergistics2:knife".equals(definition.get("name").getAsString())) {
                    com.google.gson.JsonArray knives = definition.getAsJsonArray("ingredient");
                    check(knives.size() == 1 && "appliedenergistics2:nether_quartz_cutting_knife".equals(
                            knives.get(0).getAsJsonObject().get("item").getAsString()),
                            "Constants only resolve the registered cutting knife");
                }
            }
            Set<String> originalKeys = keys(new String(read(jar, "assets/appliedenergistics2/lang/en_us.lang"), "UTF-8"));
            Path resource = Paths.get(System.getProperty("newearth.resourceRoot"));
            String chinese = new String(Files.readAllBytes(resource.resolve("assets/appliedenergistics2/lang/zh_cn.lang")), "UTF-8");
            Set<String> translated = keys(chinese);
            for (String key : translated) {
                if (key.endsWith(".newearth.desc")) {
                    check(originalKeys.contains(key.replace(".newearth.desc", ".name")), "Hover refers to an existing UEL item " + key);
                } else check(originalKeys.contains(key), "Name or GUI translation exists in UEL " + key);
            }
            for (String feature : new String[]{"INSCRIBER","PRINTED_CIRCUITS","STORAGE_MONITOR","PART_CONVERSION_MONITOR"})
                check(features.contains(feature) && !newearth.ContentTransformer.retiredAe2Feature(feature),
                        "Restored feature: " + feature);
            check(!Ae2Integration.retired("part", 400) && !Ae2Integration.retired("part", 420)
                    && !Ae2Integration.retired("crafting_monitor", 0), "Three monitors remain obtainable");
            com.google.gson.JsonObject monitor = new com.google.gson.JsonParser().parse(new String(read(jar,
                    "assets/appliedenergistics2/recipes/network/parts/monitors_storage.json"), "UTF-8")).getAsJsonObject();
            Ae2Integration.rewriteRecipe(monitor, "appliedenergistics2");
            check(monitor.get("ingredients").toString().contains("material.silicon_print"), "Silicon plate has a monitor recipe");
            net.minecraft.init.Bootstrap.register();
            try (java.net.URLClassLoader upstream = new java.net.URLClassLoader(
                    new java.net.URL[]{runtimeJar.toURI().toURL()}, Ae2Probe.class.getClassLoader())) {
                net.minecraft.item.ItemStack input = new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_INGOT);
                net.minecraft.item.ItemStack output = new net.minecraft.item.ItemStack(net.minecraft.init.Items.GOLD_INGOT);
                Object recipe = Ae2Integration.siliconPressingRecipe(upstream, input, output);
                Class<?> recipeType = recipe.getClass();
                check(((List<?>) recipeType.getMethod("getInputs").invoke(recipe)).equals(Collections.singletonList(input)),
                        "Inscriber receives one ingredient");
                check(((List<?>) recipeType.getMethod("getTopInputs").invoke(recipe)).isEmpty()
                        && ((List<?>) recipeType.getMethod("getBottomInputs").invoke(recipe)).isEmpty(),
                        "Inscriber leaves both mold slots empty");
                check(recipeType.getMethod("getOutput").invoke(recipe) == output, "Inscriber recipe records its output");
            }
            checkOreDefinitions(jar);
            checkPairingPermissions(jar,transformer);
            checkReplacementRecipes(resource);
            String loader = "appeng.recipes.AERecipeLoader";
            byte[] processing = read(jar, loader.replace('.', '/') + ".class");
            byte[] disabled = transformer.transform(loader, loader, processing);
            MethodNode entry = node(disabled).methods.stream()
                    .filter(m -> m.name.equals("loadProcessingRecipes") && m.desc.equals("()Z"))
                    .findFirst().get();
            check(entry.instructions.size() == 2 && entry.instructions.getFirst().getOpcode() == Opcodes.ICONST_0
                    && entry.instructions.getLast().getOpcode() == Opcodes.IRETURN,
                    "AE2's separate processing loader cannot register removed ingredients");
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(loader, entry);
            String materialType = "appeng.items.materials.ItemMaterial";
            byte[] patchedMaterials = transformer.transform(materialType, materialType, read(jar, materialType.replace('.', '/') + ".class"));
            check(transformer.transform(materialType, materialType, patchedMaterials) == patchedMaterials, "Subtype filter installed once");
            for (MethodNode method : node(patchedMaterials).methods) if (method.name.equals("createMaterial"))
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(materialType, method);

            actualLocale(runtimeJar, resource);
        }
        System.out.println("AE2: " + checks + " checks passed (actual UEL v0.56.6; no game or save modified)");
    }
    private static void checkOreDefinitions(ZipFile jar) throws Exception {
        ClassNode blocks = node(read(jar,"appeng/core/api/definitions/ApiBlocks.class"));
        Map<String,Set<String>> guards = new HashMap<>(); String current = null;
        for (MethodNode method : blocks.methods) if (method.name.equals("<init>"))
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof LdcInsnNode && ((LdcInsnNode)instruction).cst instanceof String) {
                    current = (String)((LdcInsnNode)instruction).cst;
                    if (current.equals("quartz_ore") || current.equals("charged_quartz_ore")) guards.put(current,new HashSet<>());
                }
                if (instruction instanceof FieldInsnNode && guards.containsKey(current)) {
                    FieldInsnNode field = (FieldInsnNode)instruction;
                    if (field.owner.equals("appeng/core/features/AEFeature")) guards.get(current).add(field.name);
                }
            }
        check(guards.containsKey("quartz_ore") && guards.get("quartz_ore").contains("CERTUS_ORE"), "Actual quartz ore definition is guarded by the disabled registration feature");
        check(guards.containsKey("charged_quartz_ore") && guards.get("charged_quartz_ore").contains("CHARGED_CERTUS_ORE"), "Actual charged ore definition is guarded before block/item registration");
    }
    private static void checkPairingPermissions(ZipFile jar, newearth.ContentTransformer transformer) throws Exception {
        String type = "appeng.tile.misc.TileSecurityStation";
        byte[] original = read(jar,type.replace('.','/')+".class");
        byte[] patched = transformer.transform(type,type,original);
        check(!Arrays.equals(original,patched), "Actual permission provider disabled");
        check(transformer.transform(type,type,patched) == patched, "Permission hook is idempotent");
        ClassNode after = node(patched); boolean found = false;
        for (MethodNode method : after.methods) {
            if ((method.access & (Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE)) == 0)
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,method);
            if (method.name.equals("isSecurityEnabled")) {
                found = true; check(method.instructions.size()==2 && method.instructions.getFirst().getOpcode()==Opcodes.ICONST_0,"Pairing station cannot enable player permission enforcement");
            }
        }
        check(found,"Expected permission-provider method found");
        check(after.methods.stream().anyMatch(m->m.name.equals("getSecurityKey")),"Wireless binding identifier retained");
        boolean cacheUsesProvider = false;
        for (MethodNode method : node(read(jar,"appeng/me/cache/SecurityCache.class")).methods)
            if (method.name.equals("isAvailable")) for (AbstractInsnNode i : method.instructions.toArray())
                if (i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("isSecurityEnabled")) cacheUsesProvider=true;
        check(cacheUsesProvider,"Actual security grid consults the disabled provider");
    }
    private static void checkReplacementRecipes(Path resources) throws Exception {
        for (String name : new String[]{"certus_crystal","fluix_crystal","logic_processor","calculation_processor","engineering_processor"}) {
            check(!Files.exists(resources.resolve("assets/newearth/recipes/ae2_"+name+".json")), "Obsolete material recipe removed: "+name);
        }
    }
    private static void actualLocale(File ae2, Path resources) throws Exception {
        net.minecraft.client.resources.data.MetadataSerializer serializer = new net.minecraft.client.resources.data.MetadataSerializer();
        serializer.registerMetadataSectionType(new net.minecraft.client.resources.data.PackMetadataSectionSerializer(),
                net.minecraft.client.resources.data.PackMetadataSection.class);
        net.minecraft.client.resources.IResourcePack upstream = new net.minecraft.client.resources.FileResourcePack(ae2);
        net.minecraft.client.resources.data.PackMetadataSection metadata = upstream.getPackMetadata(serializer,"pack");
        if (metadata == null || metadata.getPackFormat() < 3) upstream = new net.minecraft.client.resources.LegacyV2Adapter(upstream);
        net.minecraft.client.resources.SimpleReloadableResourceManager manager = new net.minecraft.client.resources.SimpleReloadableResourceManager(serializer);
        manager.reloadResources(Arrays.asList(upstream,new net.minecraft.client.resources.FolderResourcePack(resources.toFile())));
        net.minecraft.client.resources.Locale locale = new net.minecraft.client.resources.Locale();
        locale.loadLocaleDataFiles(manager,Arrays.asList("en_us","zh_cn"));
        Properties expected = new Properties();
        try (Reader reader = Files.newBufferedReader(resources.resolve("assets/appliedenergistics2/lang/zh_cn.lang"),java.nio.charset.StandardCharsets.UTF_8)) { expected.load(reader); }
        for (String key : expected.stringPropertyNames())
            check(locale.formatMessage(key,new Object[0]).equals(expected.getProperty(key)), "Actual Minecraft Locale loads AE2 override: " + key);
        Set<String> ae2Keys = new HashSet<>(expected.stringPropertyNames());
        try (ZipFile jar = new ZipFile(ae2)) {
            for (String language : Arrays.asList("en_us", "zh_cn")) {
                ae2Keys.addAll(keys(new String(read(jar, "assets/appliedenergistics2/lang/" + language + ".lang"), "UTF-8")));
            }
        }
        try (java.util.stream.Stream<String> lines = Files.lines(resources.resolve("assets/newearth/lang/zh_cn.lang"))) {
            lines.filter(line -> line.startsWith("newearth.ae2.") && line.contains("="))
                    .map(line -> line.substring(0, line.indexOf('='))).forEach(ae2Keys::add);
        }
        for (String key : ae2Keys)
            check(locale.formatMessage(key, new Object[0]).indexOf(0x4e0d) < 0,
                    "Merged Chinese AE2 text contains no disallowed wording: " + key);
    }
    private static Set<String> keys(String text) {
        Set<String> keys = new HashSet<>();
        for (String line : text.split("\\r?\\n")) if (!line.startsWith("#") && !line.startsWith("//") && line.contains("=")) {
            String key = line.substring(0,line.indexOf('='));
            check(keys.add(key), "No duplicate localization key " + key);
        }
        return keys;
    }
    private static byte[] read(ZipFile jar, String path) throws IOException {
        try (InputStream input = jar.getInputStream(jar.getEntry(path))) { return com.google.common.io.ByteStreams.toByteArray(input); }
    }
    private static ClassNode node(byte[] bytes) { ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node,0); return node; }
}
