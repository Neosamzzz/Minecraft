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
        Planet earthReference = new Planet("earth_reference");
        earthReference.setBodyIcon(new net.minecraft.util.ResourceLocation("galacticraftcore", "textures/gui/celestialbodies/earth.png"));
        earthReference.setAtmosphere(new micdoodle8.mods.galacticraft.api.world.AtmosphereInfo(true, true, false, 0, 0, 1));
        earthReference.atmosphere.composition.add(micdoodle8.mods.galacticraft.api.world.EnumAtmosphericGas.OXYGEN);
        SolarSystem targetSystem = new SolarSystem("target_system", "probe");
        Planet destination = new Planet("trars9").setParentSolarSystem(targetSystem);
        newearth.dimension.EndDimension.bindEarthProperties(destination, earthReference, net.minecraft.world.WorldProviderSurface.class, 1020, 6);
        check(destination.getReachable() && destination.getDimensionID() == 1020 && !destination.shouldAutoRegister(), "Destination points to OTG without duplicate registration");
        check(destination.getParentSolarSystem() == targetSystem, "Destination remains in its original star system");
        check(destination.getBodyIcon().equals(earthReference.getBodyIcon()) && destination.getTierRequirement() == 6, "Earth icon and test tier are applied");
        check(destination.atmosphere != earthReference.atmosphere && destination.atmosphere.composition.equals(earthReference.atmosphere.composition), "Earth atmosphere is copied without sharing mutable gas lists");
        check(destination.atmosphere.isBreathable() && destination.atmosphere.hasPrecipitation() && !destination.atmosphere.isCorrosive() && destination.atmosphere.thermalLevel() == 0 && destination.atmosphere.relativeDensity() == 1, "Earth environment properties are retained");
        java.lang.reflect.Field providerField = newearth.dimension.EndDimension.class.getDeclaredField("destinationProvider");
        providerField.setAccessible(true); Object previousProvider = providerField.get(null);
        try {
            providerField.set(null, net.minecraft.world.WorldProviderSurface.class);
            net.minecraft.world.WorldProviderSurface unbound = new net.minecraft.world.WorldProviderSurface();
            unbound.setDimension(1020);
            check(newearth.dimension.EndDimension.mapDimensionName(unbound).equals("trars9"), "Map names an unbound destination provider without querying its World");
            unbound.setDimension(0);
            check(newearth.dimension.EndDimension.mapDimensionName(unbound).equals(micdoodle8.mods.galacticraft.core.util.WorldUtil.getDimensionName(unbound)), "Other dimensions retain GC dimension names");
        } finally { providerField.set(null, previousProvider); }
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
        check(ep.get(2).getTierRequirement() == 5, "Uranus main gate agrees with its T5 exploration stage");
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
                "micdoodle8.mods.galacticraft.api.GalacticraftRegistry",
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
        File epFile = null;
        Set<String> targets = new HashSet<>(required);
        Set<String> teleportTypes = new HashSet<>();
        int untouched = 0;
        for (File file : jars) try (JarFile jar = new JarFile(file)) {
            if (jar.getJarEntry("com/mjr/extraplanets/ExtraPlanets.class") != null) epFile = file;
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
                if (name.endsWith("WorldUtil")) {
                    int mapHooks = 0;
                    for (MethodNode method : node.methods) if (method.name.equals("getArrayOfPossibleDimensions"))
                        for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction instanceof MethodInsnNode) {
                            MethodInsnNode call = (MethodInsnNode) instruction;
                            if (call.owner.equals("newearth/PlanetHooks") && call.name.equals("addEndDestination")) mapHooks++;
                        }
                    check(mapHooks == 1, "Allowed OTG destination survives GC client-map conversion");
                }
                if (name.endsWith("GuiCelestialSelection") || name.endsWith("CustomCelestialSelection")) checkMapPatch(node);
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
        check(epFile != null, "Installed EP resource pack found");
        checkLanguageLoading(epFile);
    }

    private static void checkLanguageLoading(File epFile) throws Exception {
        net.minecraft.client.resources.data.MetadataSerializer serializer = new net.minecraft.client.resources.data.MetadataSerializer();
        serializer.registerMetadataSectionType(new net.minecraft.client.resources.data.PackMetadataSectionSerializer(),
                net.minecraft.client.resources.data.PackMetadataSection.class);
        net.minecraft.client.resources.IResourcePack addon = new net.minecraft.client.resources.FolderResourcePack(
                new File(System.getProperty("newearth.resourceRoot")));
        net.minecraft.client.resources.data.PackMetadataSection metadata = addon.getPackMetadata(serializer, "pack");
        check(metadata != null && metadata.getPackFormat() == 3, "Forge selects modern lowercase language paths");
        net.minecraft.client.resources.IResourcePack legacyAddon = new net.minecraft.client.resources.LegacyV2Adapter(addon);
        net.minecraft.client.resources.IResourcePack ep = new net.minecraft.client.resources.LegacyV2Adapter(
                new net.minecraft.client.resources.FileResourcePack(epFile));
        net.minecraft.client.resources.SimpleReloadableResourceManager manager = new net.minecraft.client.resources.SimpleReloadableResourceManager(serializer);
        net.minecraft.client.resources.Locale locale = new net.minecraft.client.resources.Locale();
        manager.reloadResources(Arrays.asList(ep, legacyAddon));
        locale.loadLocaleDataFiles(manager, Arrays.asList("en_us", "zh_cn"));
        check(locale.formatMessage("planet.JupiterEP", new Object[0]).contains("2.0"), "Control reproduces old-format addon ignoring lowercase translations");
        manager.reloadResources(Arrays.asList(ep, addon));
        for (String language : new String[] {"en_us", "zh_cn"}) {
            locale.loadLocaleDataFiles(manager, language.equals("zh_cn") ? Arrays.asList("en_us", "zh_cn") : Arrays.asList("en_us"));
            java.util.Properties expected = new java.util.Properties();
            try (java.io.Reader reader = new java.io.InputStreamReader(SolarSystemProbe.class.getResourceAsStream(
                    "/assets/extraplanets/lang/" + language + ".lang"), java.nio.charset.StandardCharsets.UTF_8)) { expected.load(reader); }
            for (String key : expected.stringPropertyNames())
                check(locale.formatMessage(key, new Object[0]).equals(expected.getProperty(key)), "Actual Locale override: " + language + " / " + key);
            checkMapFacts(locale, language);
            System.out.println("Actual MC Locale " + language + ": " + locale.formatMessage("planet.JupiterEP", new Object[0])
                    + ", " + locale.formatMessage("planet.MercuryEP", new Object[0]));
        }
    }

    private static void checkMapPatch(ClassNode node) {
        int hints = 0, renders = 0, legacy = 0;
        for (MethodNode method : node.methods) for (AbstractInsnNode i : method.instructions.toArray()) {
            if (i instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) i;
                if (call.owner.equals("newearth/client/CatalogueHooks") && call.name.equals("drawFacts")) renders++;
                if (call.owner.equals("newearth/client/CatalogueHooks") && call.name.equals("legacyDetails")) legacy++;
            }
            if (method.name.equals("drawButtons") && i.getOpcode() == Opcodes.POP2 && i.getNext().getOpcode() == Opcodes.ICONST_0
                    && i.getNext().getNext().getOpcode() == Opcodes.IFEQ) hints++;
        }
        check(hints == 1, "Zoom hint disabled without altering selection input: " + node.name);
        check(renders > 0, "Right-panel facts drawn after map buttons: " + node.name);
        if (node.name.contains("CustomCelestialSelection")) check(legacy == 1, "Legacy details gated; galaxy controls retained");
    }

    private static void checkMapFacts(net.minecraft.client.resources.Locale locale, String language) throws Exception {
        for (java.lang.reflect.Field field : net.minecraft.client.resources.I18n.class.getDeclaredFields())
            if (field.getType() == net.minecraft.client.resources.Locale.class) { field.setAccessible(true); field.set(null, locale); }
        java.util.Properties expected = new java.util.Properties();
        try (java.io.Reader reader = new java.io.InputStreamReader(SolarSystemProbe.class.getResourceAsStream(
                "/assets/newearth/lang/" + language + ".lang"), java.nio.charset.StandardCharsets.UTF_8)) { expected.load(reader); }
        for (String key : expected.stringPropertyNames())
            check(locale.formatMessage(key, new Object[0]).equals(expected.getProperty(key)), "Actual map translation: " + key);
        Planet body = new Planet("mercuryep");
        body.setDimensionInfo(0, net.minecraft.world.WorldProviderSurface.class);
        body.setTierRequired(3);
        body.setAtmosphere(new micdoodle8.mods.galacticraft.api.world.AtmosphereInfo(false, false, false, -5, 0, 0));
        List<String> rows = newearth.client.PlanetFacts.describe(body);
        check(rows.stream().anyMatch(row -> row.endsWith("T3")), "Map uses registered tier");
        check(rows.stream().anyMatch(row -> row.endsWith(expected.getProperty("newearth.map.freezing"))), "Cold classification from game parameter");
        check(rows.stream().anyMatch(row -> row.endsWith(expected.getProperty("newearth.map.resources.mercuryep"))), "EP resource identity retained");
        check(!expected.getProperty("newearth.map.resources.mercury").equals(expected.getProperty("newearth.map.resources.mercuryep")), "GS and EP resources differ");
        body.setUnreachable();
        check(newearth.client.PlanetFacts.describe(body).size() == 2, "Display-only bodies do not claim a landable environment");
        check(newearth.client.PlanetFacts.thermalClass(0).equals("mild") && newearth.client.PlanetFacts.thermalClass(4).equals("scorching"), "Thermal classification boundaries");
    }

    private static void checkNames() throws Exception {
        for (String language : new String[] {"zh_cn", "en_us"}) {
            java.util.Properties names = new java.util.Properties();
            try (java.io.Reader reader = new java.io.InputStreamReader(SolarSystemProbe.class.getResourceAsStream(
                    "/assets/extraplanets/lang/" + language + ".lang"), java.nio.charset.StandardCharsets.UTF_8)) { names.load(reader); }
            check(names.stringPropertyNames().stream().filter(key -> key.startsWith("planet.") || key.startsWith("moon.")).count() == 21, "All EP compatibility celestial names supplied: " + language);
            check(names.getProperty("item.wafer.diamond_wafer.name") != null, "Repurposed wafer has a localized name: " + language);
            check(names.values().stream().noneMatch(value -> value.toString().contains("2.0")), "No 2.0 label remains: " + language);
            check(names.getProperty("planet.MercuryEP").contains("EP"), "Dual Mercury destinations remain distinguishable: " + language);
            check(names.values().stream().filter(value -> value.toString().contains("EP")).count() == 2,
                    "Only the two retained dual planets need source labels: " + language);
        }
    }

    private static void checkCatalogue() {
        check(CatalogueScroll.zoomWheel(1, 1) == CatalogueScroll.zoomWheel(120, 120), "Cleanroom and LWJGL 2 notches produce equal zoom input");
        check(CatalogueScroll.zoomWheel(-1, 1) == -600 && CatalogueScroll.zoomWheel(0, 1) == 0, "Zoom normalization preserves direction and idle input");
        check(CatalogueScroll.zoomWheel(3, 1) == 1800 && CatalogueScroll.zoomWheel(360, 120) == 1800, "Multiple wheel notches retain their full zoom distance");
        CatalogueScroll scroll = new CatalogueScroll();
        scroll.contentHeight = 700; scroll.viewportHeight = 200;
        check(scroll.maximum() == 500, "Expanded catalogue overflow measured");
        scroll.offset = 500;
        check(!scroll.wheel(-120) && scroll.offset == 500, "Bottom boundary releases wheel input for map zoom");
        check(scroll.wheel(120) && scroll.offset == 472, "Reversing at bottom scrolls the catalogue");
        scroll.offset = 0;
        check(!scroll.wheel(120) && scroll.offset == 0, "Top boundary releases wheel input for map zoom");
        check(scroll.wheel(-120) && scroll.offset == 28, "Scrollable catalogue consumes wheel and moves two rows");
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
        for (String name : new String[] {"enableJupiter", "enableSaturn", "enableUranus", "enableNeptune", "enablePluto", "enableTriton"})
            configFlag(gs, "general", name, false);
        for (String name : new String[] {"enableMercury", "enableCeres", "enableIo", "enableEnceladus", "enableMiranda",
                "enablePhobos", "enableEuropa", "enableGanymede", "enableCallisto", "enableTitan",
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
        for (String name : new String[] {"Phobos", "IO", "Europa", "Ganymede", "Callisto", "Titan"}) {
            String category = null;
            for (String candidate : ep.getCategoryNames()) if (ep.hasKey(candidate, name)) category = candidate;
            check(category != null, "Excluded EP moon key exists: " + name);
            configFlag(ep, category, name, false);
        }
        int duplicateSurfaces = 0;
        String[][] pairs = {{"enableMercury", "Mercury & Tier 4 Rocket"}, {"enableCeres", "Ceres"},
                {"enablePhobos", "Phobos"}, {"enableIo", "IO"}, {"enableEuropa", "Europa"},
                {"enableGanymede", "Ganymede"}, {"enableCallisto", "Callisto"}, {"enableTitan", "Titan"}, {"enableTriton", "Triton"}};
        for (String[] pair : pairs) {
            boolean epEnabled = false;
            for (String category : ep.getCategoryNames()) if (ep.hasKey(category, pair[1])) epEnabled = ep.getCategory(category).get(pair[1]).getBoolean();
            if (gs.getCategory("general").get(pair[0]).getBoolean() && epEnabled) duplicateSurfaces++;
        }
        check(duplicateSurfaces == 2, "Only Mercury and Ceres retain two landable implementations");
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
