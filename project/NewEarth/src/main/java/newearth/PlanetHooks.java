package newearth;

import java.util.ArrayList;
import java.util.List;
import micdoodle8.mods.galacticraft.api.galaxies.*;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import micdoodle8.mods.galacticraft.core.util.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Runtime rules for the fixed modpack config; never rewrites config values. */
public final class PlanetHooks {
    private static Integer erisDimension;
    private PlanetHooks() { }

    public static boolean isReplacedDummy(String name) {
        return "jupiter".equals(name) || "saturn".equals(name) || "uranus".equals(name) || "neptune".equals(name);
    }

    public static boolean hasReachableMoonCounterpart(String name) {
        String counterpart = ("rheya".equals(name) ? "rhea" : name) + "ep";
        for (Moon moon : GalaxyRegistry.getRegisteredMoons().values())
            if (moon.getName().equalsIgnoreCase(counterpart) && moon.getReachable()) return true;
        return false;
    }

    static void validateConfiguration() {
        try {
            Class<?> ep = Class.forName("com.mjr.extraplanets.Config");
            Class<?> gs = Class.forName("galaxyspace.core.configs.GSConfigDimensions");
            for (String name : new String[] {"GALAXY_SPACE_COMPATIBILITY", "MERCURY", "CERES", "JUPITER", "SATURN", "URANUS", "NEPTUNE", "PLUTO", "ERIS", "TRITON"})
                expectFlag(ep, name, true, "config/ExtraPlanets.cfg");
            for (String name : new String[] {"PHOBOS", "IO", "EUROPA", "GANYMEDE", "CALLISTO", "TITAN"})
                expectFlag(ep, name, false, "config/ExtraPlanets.cfg");
            for (String name : new String[] {"enableMercury", "enableCeres", "enableIo", "enableEnceladus", "enableMiranda",
                    "enablePhobos", "enableEuropa", "enableGanymede", "enableCallisto", "enableTitan",
                    "enableJupiterMoons", "enableSaturnMoons", "enableUranusMoons", "enableNeptuneMoons"})
                expectFlag(gs, name, true, "config/GalaxySpace/dimensions.conf");
            for (String name : new String[] {"enableJupiter", "enableSaturn", "enableUranus", "enableNeptune", "enablePluto", "enableTriton"})
                expectFlag(gs, name, false, "config/GalaxySpace/dimensions.conf");
        } catch (ReflectiveOperationException e) { throw unsupported(e); }
    }

    private static void expectFlag(Class<?> config, String field, boolean expected, String path) throws ReflectiveOperationException {
        if (config.getField(field).getBoolean(null) != expected)
            throw new IllegalStateException("NewEarth modpack configuration mismatch: " + path + " / " + field
                    + " must be " + expected + ". Install the repository config together with NewEarth; no value was overridden.");
    }

    static void integrate() {
        try {
            Class<?> ep = Class.forName("com.mjr.extraplanets.planets.ExtraPlanets_Planets");
            Class<?> gs = Class.forName("galaxyspace.systems.SolarSystem.SolarSystemBodies");
            // Resolve all required bodies before mutating any parent or tier.
            List<Planet> sources = new ArrayList<>(), targets = new ArrayList<>();
            for (String name : new String[] {"Jupiter", "Saturn", "Uranus", "Neptune", "Pluto"}) {
                sources.add((Planet) gs.getField("planet" + name).get(null));
                Planet target = (Planet) ep.getField(name.toUpperCase(java.util.Locale.ROOT)).get(null);
                if (target == null || !GalaxyRegistry.getRegisteredPlanets().containsValue(target))
                    throw new IllegalStateException("Required EP planet is not registered: " + name);
                targets.add(target);
            }
            Planet eris = (Planet) ep.getField("ERIS").get(null);
            if (eris == null) throw new IllegalStateException("Required EP Eris is disabled");
            preserveCharon(gs, targets.get(4));
            integrate(sources, targets, eris);
        } catch (ReflectiveOperationException e) { throw unsupported(e); }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void preserveCharon(Class<?> gs, Planet pluto) throws ReflectiveOperationException {
        if (!Class.forName("galaxyspace.core.configs.GSConfigCore").getField("enableUnreachable").getBoolean(null)) return;
        // Disabling GS Pluto also disables its dummy Charon. Preserve that existing
        // display moon under EP Pluto without enabling EP's whole dummy-moon group.
        for (Moon moon : GalaxyRegistry.getRegisteredMoons().values())
            if (moon.getName().equals("charon")) return;
        java.lang.reflect.Method register = gs.getDeclaredMethod("registerDummyMoon", String.class, Planet.class, float.class);
        register.setAccessible(true);
        Moon charon = (Moon) register.invoke(gs.getDeclaredConstructor().newInstance(), "charon", pluto, 15F);
        gs.getField("charonPluto").set(null, charon);
        Class<?> registry = Class.forName("asmodeuscore.core.astronomy.BodiesRegistry");
        registry.getMethod("setOrbitData", CelestialBody.class, float.class, float.class, float.class)
                .invoke(null, charon, (float) Math.PI, 0.0017F, 50F);
        Class type = Class.forName("asmodeuscore.api.dimension.IAdvancedSpace$TypeBody");
        Class<?> data = Class.forName("asmodeuscore.core.astronomy.BodiesData");
        Object moonData = data.getConstructor(type).newInstance(Enum.valueOf(type, "MOON"));
        registry.getMethod("registerBodyData", CelestialBody.class, data).invoke(null, charon, moonData);
    }

    static void integrate(List<Planet> sources, List<Planet> targets, Planet eris) {
        for (int i = 0; i < sources.size(); i++) {
            Planet source = sources.get(i), target = targets.get(i);
            for (Moon moon : GalaxyRegistry.getRegisteredMoons().values()) {
                if (source != null && moon.getParentPlanet() == source) moon.setParentPlanet(target);
            }
            for (Satellite satellite : GalaxyRegistry.getRegisteredSatellites().values()) {
                if (source != null && satellite.getParentPlanet() == source) satellite.setParentBody(target);
            }
            // Preserve pending satellite choices, but apply the confirmed stage to
            // every retained child of Saturn / Neptune rather than just the main body.
            int tier = i == 1 ? 5 : (i == 3 || i == 4) ? 6 : -1;
            if (tier > 0) {
                target.setTierRequired(tier);
                for (Moon moon : GalaxyRegistry.getRegisteredMoons().values())
                    if (moon.getParentPlanet() == target) moon.setTierRequired(tier);
                for (Satellite satellite : GalaxyRegistry.getRegisteredSatellites().values())
                    if (satellite.getParentPlanet() == target) satellite.setTierRequired(tier);
            }
        }
        eris.setTierRequired(6);
        erisDimension = eris.getDimensionID();
        for (Satellite satellite : GalaxyRegistry.getRegisteredSatellites().values())
            if (satellite.getParentPlanet() == eris) satellite.setTierRequired(6);
        GalaxyRegistry.refreshGalaxies();
        NewEarth.LOG.info("Integrated EP main planets; GS moons reparented, Saturn T5, Neptune/Pluto T6, Eris T6 + ion");
    }

    public static List<Integer> filterDestinations(List<Integer> dimensions, Object player) {
        List<Integer> result = new ArrayList<>(dimensions);
        if (erisDimension != null && !hasIonRocket(player, null)) result.removeIf(PlanetHooks::isErisDestination);
        return result;
    }

    public static boolean allowTransfer(Object entity, int destination, Object rocket) {
        return erisDimension == null || !isErisDestination(destination) || hasIonRocket(entity, rocket);
    }

    private static boolean isErisDestination(int destination) {
        if (erisDimension == null) return false;
        if (destination == erisDimension) return true;
        net.minecraft.world.WorldProvider provider = WorldUtil.getProviderForDimensionServer(destination);
        return provider != null && provider.getClass().getName().equals("com.mjr.extraplanets.planets.Eris.spacestation.WorldProviderErisOrbit");
    }

    private static boolean hasIonRocket(Object entity, Object rocket) {
        if (rocket == null && entity instanceof Entity) rocket = ((Entity) entity).getRidingEntity();
        if (rocket != null && rocket.getClass().getName().equals("galaxyspace.core.prefab.entities.EntityTier6Rocket")) {
            return "ION_ENGINE".equals(engine(rocket).toString());
        }
        if (rocket instanceof EntityAutoRocket) return false;
        if (!(entity instanceof EntityPlayerMP)) return false;
        GCPlayerStats stats = GCPlayerStats.get((EntityPlayerMP) entity);
        if (stats.getSpaceshipTier() != 6) return false;
        List<ItemStack> stacks = stats.getRocketStacks();
        if (stacks == null || stacks.isEmpty()) return false;
        ItemStack stack = stacks.get(stacks.size() - 1);
        return !stack.isEmpty() && stack.getItem().getClass().getName().equals("galaxyspace.core.prefab.items.rockets.ItemTier6Rocket")
                && stack.hasTagCompound() && stack.getTagCompound().getBoolean(ionKey());
    }

    public static void preserveEngine(Object rocket, Object player) {
        if (!(player instanceof EntityPlayerMP)) return;
        Object engine = engine(rocket);
        if (!"ION_ENGINE".equals(engine.toString())) return;
        GCPlayerStats stats = GCPlayerStats.get((EntityPlayerMP) player);
        List<ItemStack> stacks = stats.getRocketStacks();
        if (stacks == null || stacks.isEmpty()) throw new IllegalStateException("Missing T6 recovery inventory");
        ItemStack stack = new ItemStack(SolarSystemBridge.requireItem("galaxyspace:rocket_tier_6"), 1, stats.getRocketType());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean(ionKey(), true);
        try {
            String count = (String) Class.forName("galaxyspace.systems.SolarSystem.planets.overworld.items.armor.ItemSpaceSuit")
                    .getField("mod_count").get(null);
            tag.setInteger(count, 0);
        } catch (ReflectiveOperationException e) { throw unsupported(e); }
        stack.setTagCompound(tag);
        stacks.set(stacks.size() - 1, stack);
    }

    private static Object engine(Object rocket) {
        try { return rocket.getClass().getMethod("getEngine").invoke(rocket); }
        catch (ReflectiveOperationException e) { throw unsupported(e); }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String ionKey() {
        try {
            Class type = Class.forName("asmodeuscore.core.astronomy.SpaceData$Engine_Type");
            Object ion = Enum.valueOf(type, "ION_ENGINE");
            return (String) type.getMethod("getName").invoke(ion);
        } catch (ReflectiveOperationException e) { throw unsupported(e); }
    }

    private static IllegalStateException unsupported(Exception e) {
        return new IllegalStateException("NewEarth requires GS 2.2.0 / EP 0.8.0 solar-system APIs", e);
    }
}
