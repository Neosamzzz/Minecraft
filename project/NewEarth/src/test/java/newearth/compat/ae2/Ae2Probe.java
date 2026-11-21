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
                    "COLOR_APPLICATOR","PAINT_BALLS","POWER_GEN","PANELS","STORAGE_MONITOR","PART_CONVERSION_MONITOR"}) {
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
            Set<String> originalKeys = keys(new String(read(jar, "assets/appliedenergistics2/lang/en_us.lang"), "UTF-8"));
            Path resource = Paths.get(System.getProperty("newearth.resourceRoot"));
            String chinese = new String(Files.readAllBytes(resource.resolve("assets/appliedenergistics2/lang/zh_cn.lang")), "UTF-8");
            Set<String> translated = keys(chinese);
            for (String key : translated) {
                if (key.endsWith(".newearth.desc")) {
                    check(originalKeys.contains(key.replace(".newearth.desc", ".name")), "Hover refers to an existing UEL item " + key);
                } else check(originalKeys.contains(key), "Name or GUI translation exists in UEL " + key);
            }
            checkOreDefinitions(jar);
            checkPairingPermissions(jar,transformer);
            checkReplacementRecipes(resource);
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
            com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(new String(Files.readAllBytes(resources.resolve("assets/newearth/recipes/ae2_"+name+".json")),"UTF-8")).getAsJsonObject();
            check(json.get("type").getAsString().equals("minecraft:crafting_shapeless"),"Replacement uses ordinary crafting: "+name);
            for (com.google.gson.JsonElement ingredient : json.getAsJsonArray("ingredients")) {
                String id = ingredient.getAsJsonObject().get("item").getAsString();
                check(id.startsWith("minecraft:")||id.equals("galacticraftcore:basic_item"),"Replacement source is available without AE2 mining: "+name);
            }
            check(json.has("conditions"),"Optional AE2 recipe safely skipped when mod absent");
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
