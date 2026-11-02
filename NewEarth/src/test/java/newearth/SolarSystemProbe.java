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
                "com.mjr.extraplanets.Config", "galaxyspace.core.configs.GSConfigDimensions",
                "micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry", "micdoodle8.mods.galacticraft.core.util.WorldUtil",
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
