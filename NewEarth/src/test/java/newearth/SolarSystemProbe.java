package newearth;

import java.io.File;
import java.util.*;
import java.util.jar.*;
import micdoodle8.mods.galacticraft.api.galaxies.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Exercises the real registry and transforms the supplied installed jars without running a world. */
public final class SolarSystemProbe {
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard = new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        java.lang.reflect.Field home = net.minecraftforge.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true); home.set(null, new File(".").getAbsoluteFile());
        List<Planet> gs = new ArrayList<>(), ep = new ArrayList<>();
        for (String name : new String[] {"jupiter", "saturn", "uranus", "neptune", "pluto"}) {
            gs.add(new Planet(name));
            Planet target = new Planet(name + "ep");
            target.setTierRequired(9); GalaxyRegistry.registerPlanet(target); ep.add(target);
        }
        Moon io = moon("io", gs.get(0), 4);
        Moon enceladus = moon("enceladus", gs.get(1), 5);
        Moon titan = moon("titan", gs.get(1), 5);
        Moon miranda = moon("miranda", gs.get(2), 5);
        Moon triton = moon("tritonep", ep.get(3), 7);
        Satellite station = new Satellite("probe_station").setParentBody(gs.get(4));
        GalaxyRegistry.registerSatellite(station);
        Planet eris = new Planet("eris"); eris.setTierRequired(9); GalaxyRegistry.registerPlanet(eris);
        GalaxyRegistry.refreshGalaxies();
        check(GalaxyRegistry.getMoonsForPlanet(gs.get(1)).contains(enceladus), "Precondition: old parent indexed");
        PlanetHooks.integrate(gs, ep, eris);
        check(io.getParentPlanet() == ep.get(0) && io.getTierRequirement() == 4, "GS Io retained at T4 under EP Jupiter");
        check(enceladus.getParentPlanet() == ep.get(1) && enceladus.getTierRequirement() == 5, "GS Enceladus retained at T5");
        check(titan.getParentPlanet() == ep.get(1), "Pending Titan choice is preserved");
        check(miranda.getParentPlanet() == ep.get(2) && miranda.getTierRequirement() == 5, "Miranda stays T5 under pending Uranus gate");
        check(ep.get(2).getTierRequirement() == 9, "Uranus main gate is not redesigned");
        check(triton.getTierRequirement() == 6 && ep.get(3).getTierRequirement() == 6, "Neptune and EP Triton agree at T6");
        check(station.getParentPlanet() == ep.get(4) && station.getTierRequirement() == 6, "Pluto station parent and stage agree");
        check(GalaxyRegistry.getMoonsForPlanet(gs.get(1)).isEmpty(), "Old moon index removed");
        check(GalaxyRegistry.getMoonsForPlanet(ep.get(1)).contains(enceladus), "New moon index rebuilt");
        check(eris.getTierRequirement() == 6, "Eris tier is six");
        check(!PlanetHooks.allowTransfer(null, eris.getDimensionID(), null), "Missing actual rocket fails Eris authorization");
        check(PlanetHooks.filterDestinations(Arrays.asList(eris.getDimensionID()), null).isEmpty(), "Eris removed from unauthorized selection");
        PlanetHooks.integrate(gs, ep, eris);
        check(GalaxyRegistry.getMoonsForPlanet(ep.get(1)).size() == 2, "Repeated integration does not duplicate moons");
        gs.set(4, null);
        Moon orphan = moon("probe_orphan", null, 1);
        PlanetHooks.integrate(gs, ep, eris);
        check(orphan.getParentPlanet() == null, "Disabled GS Pluto does not capture unrelated null-parent moons");
        check(ep.get(4).getTierRequirement() == 6, "EP Pluto remains valid without a GS Pluto object");
        checkConfig();
        checkCatalogue();
        for (String name : new String[] {"jupiter", "saturn", "uranus", "neptune"})
            check(PlanetHooks.isReplacedDummy(name), "GC fallback suppressed: " + name);
        for (String name : new String[] {"mercury", "ceres", "mars", "haumea"})
            check(!PlanetHooks.isReplacedDummy(name), "Other GC fallback untouched: " + name);
        Moon titania = moon("titaniaep", ep.get(2), 6);
        check(!PlanetHooks.hasReachableMoonCounterpart("titania"), "Display-only counterpart does not hide GS dummy");
        titania.setDimensionInfo(-998, micdoodle8.mods.galacticraft.core.dimension.WorldProviderMoon.class);
        check(PlanetHooks.hasReachableMoonCounterpart("titania"), "Reachable EP Titania replaces GS display moon");
        Moon rhea = moon("rheaep", ep.get(1), 5);
        rhea.setDimensionInfo(-997, micdoodle8.mods.galacticraft.core.dimension.WorldProviderMoon.class);
        check(PlanetHooks.hasReachableMoonCounterpart("rheya"), "GS Rheya alias matches reachable EP Rhea");
        check(!PlanetHooks.hasReachableMoonCounterpart("charon"), "Charon remains when no reachable replacement exists");
        checkNames();

        String runtime = System.getenv("NEWEARTH_RUNTIME_MODS");
        if (runtime != null && !runtime.isEmpty()) transformRuntime(new File(runtime));
        else System.out.println("Installed-jar bytecode checks skipped; set NEWEARTH_RUNTIME_MODS to a test mods directory");
        String summary = "NewEarth solar system: " + checks + " checks passed (isolated; no world or flight)";
        System.out.println(summary);
        java.nio.file.Files.write(java.nio.file.Paths.get("result.txt"), (summary + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static Moon moon(String name, Planet parent, int tier) {
        Moon moon = new Moon(name).setParentPlanet(parent); moon.setTierRequired(tier); GalaxyRegistry.registerMoon(moon); return moon;
    }

    private static void transformRuntime(File directory) throws Exception {
        Set<String> required = new HashSet<>(Arrays.asList(
                "galaxyspace.systems.SolarSystem.SolarSystemBodies",
                "com.mjr.extraplanets.client.gui.screen.CustomCelestialSelection",
                "micdoodle8.mods.galacticraft.core.GalacticraftCore",
                "micdoodle8.mods.galacticraft.core.client.gui.screen.GuiCelestialSelection",
                "micdoodle8.mods.galacticraft.core.util.WorldUtil",
                "galaxyspace.core.prefab.items.rockets.ItemTier6Rocket", "galaxyspace.core.prefab.entities.EntityTier6Rocket",
                "com.mjr.extraplanets.planets.Neptune.WorldProviderNeptune", "com.mjr.extraplanets.planets.Pluto.WorldProviderPluto",
                "com.mjr.extraplanets.planets.Eris.WorldProviderEris",
                "com.mjr.extraplanets.planets.Saturn.spacestation.WorldProviderSaturnOrbit",
                "com.mjr.extraplanets.planets.Neptune.spacestation.WorldProviderNeptuneOrbit",
                "com.mjr.extraplanets.planets.Pluto.spacestation.WorldProviderPlutoOrbit",
                "com.mjr.extraplanets.planets.Eris.spacestation.WorldProviderErisOrbit"));
        File[] jars = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) throw new IllegalArgumentException("Missing runtime mods directory " + directory);
        PlanetTransformer transformer = new PlanetTransformer();
        Set<String> targets = new HashSet<>(required);
        Set<String> teleportTypes = new HashSet<>();
        int untouched = 0;
        for (File file : jars) try (JarFile jar = new JarFile(file)) {
            // A target-only test missed TeleportTypeSaturnOrbit being intercepted.
            // Sweep every non-target class, including all four sibling teleport types.
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                String name = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                if (targets.contains(name)) continue;
                byte[] original = readEntry(jar, entry);
                if (transformer.transform(name, name, original) != original)
                    throw new AssertionError("Non-target class was modified: " + name);
                untouched++;
                for (String planet : new String[] {"Saturn", "Neptune", "Pluto", "Eris"})
                    if (name.equals("com.mjr.extraplanets.planets." + planet + ".spacestation.TeleportType" + planet + "Orbit"))
                        teleportTypes.add(name);
            }
            for (String name : new ArrayList<>(required)) {
                JarEntry entry = jar.getJarEntry(name.replace('.', '/') + ".class");
                if (entry == null) continue;
                byte[] original;
                try (java.io.InputStream input = jar.getInputStream(entry)) {
                    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[8192]; int count;
                    while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
                    original = output.toByteArray();
                }
                byte[] patched = transformer.transform(name, name, original);
                check(!Arrays.equals(original, patched), "Installed patch applied: " + name);
                ClassNode node = new ClassNode(); new ClassReader(patched).accept(node, ClassReader.EXPAND_FRAMES);
                for (MethodNode method : node.methods) {
                    if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0)
                        new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
                }
                check(true, "Installed bytecode dataflow valid: " + name);
                if (name.endsWith("WorldProviderNeptune") || name.endsWith("WorldProviderPluto") || name.endsWith("WorldProviderEris"))
                    executeGate(node);
                required.remove(name);
                System.out.println("Verified installed target: " + name + " in " + file.getName());
            }
        }
        check(required.isEmpty(), "Every required runtime target found: " + required);
        check(untouched > 0, "Non-target installed classes are unchanged");
        check(teleportTypes.size() == 4, "All four orbit teleport types load without interception");
        System.out.println("Verified " + untouched + " non-target classes unchanged, including four orbit teleport types");
    }

    private static void checkNames() throws Exception {
        for (String language : new String[] {"zh_cn", "en_us"}) {
            java.util.Properties names = new java.util.Properties();
            try (java.io.Reader reader = new java.io.InputStreamReader(SolarSystemProbe.class.getResourceAsStream(
                    "/assets/extraplanets/lang/" + language + ".lang"), java.nio.charset.StandardCharsets.UTF_8)) { names.load(reader); }
            check(names.size() == 21, "All EP compatibility names supplied: " + language);
            check(names.values().stream().noneMatch(value -> value.toString().contains("2.0")), "No 2.0 label remains: " + language);
            check(names.getProperty("planet.MercuryEP").contains("EP"), "Dual Mercury destinations remain distinguishable: " + language);
        }
    }

    private static void checkCatalogue() {
        CatalogueScroll scroll = new CatalogueScroll();
        scroll.contentHeight = 700; scroll.viewportHeight = 200;
        check(scroll.maximum() == 500, "Expanded catalogue overflow measured");
        scroll.wheel(120); check(scroll.offset == 0, "Wheel clamps at first row");
        scroll.wheel(-120); check(scroll.offset == 28, "Wheel moves two rows");
        check(scroll.rowY(78) == 50 && scroll.rowY(92) == 64, "Drawing and hit tests use the same row offset");
        scroll.drag(10000); check(scroll.offset == 500, "Dragging reaches last row");
        check(scroll.thumbTop() + scroll.thumbHeight() == 200, "Scrollbar reaches bottom of track");
        scroll.drag(-100); check(scroll.offset == 0, "Dragging above track clamps to start");
        scroll.contentHeight = 100; scroll.clamp();
        check(scroll.maximum() == 0 && scroll.thumbHeight() == 200, "Short catalogue needs no scrolling");
        for (int height : new int[] {120, 240, 480}) {
            scroll.viewportHeight = height; scroll.contentHeight = height + 140;
            scroll.drag(height); check(scroll.offset == 140, "Bottom reachable across GUI scales: " + height);
            scroll.drag(0); check(scroll.offset == 0, "Top reachable across GUI scales: " + height);
        }
    }

    private static void checkConfig() throws Exception {
        File root = new File(System.getProperty("newearth.configRoot"));
        net.minecraftforge.common.config.Configuration gs = config(new File(root, "GalaxySpace/dimensions.conf"));
        for (String name : new String[] {"enableJupiter", "enableSaturn", "enableUranus", "enableNeptune", "enablePluto"})
            configFlag(gs, "general", name, false);
        for (String name : new String[] {"enableMercury", "enableCeres", "enableIo", "enableEnceladus", "enableMiranda",
                "enableJupiterMoons", "enableSaturnMoons", "enableUranusMoons", "enableNeptuneMoons"})
            configFlag(gs, "general", name, true);
        net.minecraftforge.common.config.Configuration ep = config(new File(root, "ExtraPlanets.cfg"));
        for (String name : new String[] {"Mercury & Tier 4 Rocket", "Jupiter & Tier 5 Rocket", "Saturn & Tier 6 Rocket",
                "Uranus & Tier 7 Rocket", "Neptune & Tier 8 Rocket", "Pluto & Tier 9 Rocket", "Eris & Tier 10 Rocket",
                "Ceres", "Triton", "Enable Galaxy Space Compatibility"}) {
            String category = null;
            for (String candidate : ep.getCategoryNames()) if (ep.hasKey(candidate, name)) category = candidate;
            check(category != null, "EP fixed config key exists: " + name);
            configFlag(ep, category, name, true);
        }
    }

    private static net.minecraftforge.common.config.Configuration config(File file) {
        check(file.isFile(), "Fixed config exists: " + file);
        File input = new File("config", file.getName());
        try {
            input.getParentFile().mkdirs();
            java.nio.file.Files.copy(file.toPath(), input.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot prepare isolated config input", e); }
        net.minecraftforge.common.config.Configuration config = new net.minecraftforge.common.config.Configuration(input);
        config.load(); return config;
    }

    private static void configFlag(net.minecraftforge.common.config.Configuration config, String category, String key, boolean value) {
        check(config.hasKey(category, key) && config.getCategory(category).get(key).getBoolean() == value,
                "Fixed config " + key + "=" + value);
    }

    private static byte[] readEntry(JarFile jar, JarEntry entry) throws Exception {
        try (java.io.InputStream input = jar.getInputStream(entry)) {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static void executeGate(ClassNode original) throws Exception {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String name = "newearth/probe/" + original.name.substring(original.name.lastIndexOf('/') + 1);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode(); init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN); init.visitMaxs(1, 1); init.visitEnd();
        for (MethodNode method : original.methods) if (method.name.equals("canSpaceshipTierPass")) {
            method.localVariables = null;
            method.accept(writer.visitMethod(method.access, method.name, method.desc, null, null));
        }
        writer.visitEnd(); byte[] bytes = writer.toByteArray();
        Class<?> type = new ClassLoader(SolarSystemProbe.class.getClassLoader()) {
            Class<?> define() { return defineClass(null, bytes, 0, bytes.length); }
        }.define();
        Object instance = type.newInstance();
        for (int tier = 1; tier <= 10; tier++) check((boolean) type.getMethod("canSpaceshipTierPass", int.class).invoke(instance, tier) == (tier >= 6),
                "Actual EP provider gate tier " + tier + " in " + original.name);
    }
}
