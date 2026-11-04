package newearth.dimension;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.world.AtmosphereInfo;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;

/** Uses OTG's own dimension lifecycle, generator and persistent dimension configuration. */
@Mod.EventBusSubscriber(modid = "newearth")
public final class EndDimension {
    private static final String PRESET = "Biome Bundle";
    private static int dimensionId = 1020;
    private static int configuredId = 1020;
    private static boolean enabled = true;
    private static DimensionType destinationType;
    private static Class<? extends WorldProvider> destinationProvider;
    public static final int REQUIRED_TIER = 10;
    public static final String BODY_NAME = "trars9";

    private EndDimension() { }

    public static void configure(File configDirectory) {
        Configuration config = new Configuration(new File(configDirectory, "newearth-dimension.cfg"));
        config.load();
        enabled = config.getBoolean("enabled", "dimension", true, "Create the NewEarth destination using the Biome Bundle preset.");
        config.getCategory("dimension").remove("tier6TestFlight");
        dimensionId = config.getInt("id", "dimension", 1020, 3, 1023, "Dimension ID for new saves. Existing saves retain their OTG dimension ID.");
        configuredId = dimensionId;
        if (config.hasChanged()) config.save();
        if (enabled && Loader.isModLoaded("openterraingenerator")) {
            try {
                Class<? extends WorldProvider> provider = Class.forName("com.pg85.otg.forge.dimensions.OTGWorldProvider")
                        .asSubclass(WorldProvider.class);
                destinationProvider = provider;
                for (DimensionType type : DimensionType.values()) {
                    if (type.getId() == configuredId)
                        throw new IllegalStateException("NewEarth dimension type ID is occupied: " + configuredId);
                }
                // Register enum types during mod loading, before the JVM optimizes enum lookup.
                destinationType = DimensionType.register(PRESET, "OTG", configuredId, provider, false);
                if (DimensionType.getById(configuredId) != destinationType)
                    throw new IllegalStateException("NewEarth dimension type registration was not visible.");
            } catch (ClassNotFoundException error) {
                throw new IllegalStateException("OTG 9.9 world provider is missing", error);
            }
        }
    }

    public static void bindCelestialBody() {
        if (!enabled || destinationProvider == null) return;
        CelestialBody body = GalaxyRegistry.getRegisteredPlanets().get(BODY_NAME);
        if (body == null) throw new IllegalStateException("ExtraPlanets destination trars9 is disabled");
        bindEarthProperties(body, GalacticraftCore.planetOverworld, destinationProvider, dimensionId,
                REQUIRED_TIER);
        GalaxyRegistry.refreshGalaxies();
    }

    public static void bindEarthProperties(CelestialBody body, CelestialBody earth,
            Class<? extends WorldProvider> provider, int id, int tier) {
        AtmosphereInfo atmosphere = earth.atmosphere;
        AtmosphereInfo copy = new AtmosphereInfo(atmosphere.isBreathable(), atmosphere.hasPrecipitation(),
                atmosphere.isCorrosive(), atmosphere.thermalLevel(), atmosphere.windLevel(), atmosphere.relativeDensity());
        copy.composition.addAll(atmosphere.composition);
        body.setAtmosphere(copy);
        body.setBodyIcon(earth.getBodyIcon());
        body.setRelativeSize(earth.getRelativeSize());
        body.setDimensionInfo(id, provider, false).setTierRequired(tier);
        body.setForceStaticLoad(false);
    }

    public static boolean isDestinationId(int id) {
        return enabled && destinationProvider != null && id == dimensionId;
    }

    public static boolean canTravel(Object player) {
        return player instanceof EntityPlayerMP
                && GCPlayerStats.get((EntityPlayerMP) player).getSpaceshipTier() >= REQUIRED_TIER;
    }

    public static void start(MinecraftServer server) {
        if (!enabled || !Loader.isModLoaded("openterraingenerator")) return;
        dimensionId = configuredId;
        try {
            Class<?> otg = Class.forName("com.pg85.otg.OTG");
            Object engine = otg.getMethod("getEngine").invoke(null);
            File presets = (File) engine.getClass().getMethod("getWorldsDirectory").invoke(engine);
            if (!new File(presets, PRESET + "/WorldConfig.ini").isFile())
                throw new IllegalStateException("Install Biome Bundle 6.1 and the NewEarth preset overlay before creating this world.");
            Object dimensions = otg.getMethod("getDimensionsConfig").invoke(null);
            if (dimensions == null) throw new IllegalStateException("OTG dimension configuration is not initialized.");
            Object overworld = value(dimensions, "Overworld");
            if (PRESET.equals(value(overworld, "PresetName"))) {
                LogManager.getLogger("NewEarth").info("Biome Bundle overworld is a terrain preview; no duplicate destination is created.");
                return;
            }
            for (Object existing : (List<?>) value(dimensions, "Dimensions")) {
                if (!PRESET.equals(value(existing, "PresetName"))) continue;
                dimensionId = (Integer) value(existing, "DimensionId");
                if (!DimensionManager.isDimensionRegistered(dimensionId))
                    throw new IllegalStateException("Saved NewEarth dimension was not registered by OTG: " + dimensionId);
                bindCelestialBody();
                return;
            }
            if (DimensionManager.isDimensionRegistered(dimensionId))
                throw new IllegalStateException("NewEarth dimension ID is occupied: " + dimensionId + ". Change config/newearth-dimension.cfg before creating the destination.");
            Class<?> configType = Class.forName("com.pg85.otg.configuration.dimensions.DimensionConfig");
            // OTG's three-argument constructor tries to load a WorldConfig before the world exists.
            Object config = configType.getConstructor().newInstance();
            set(config, "PresetName", PRESET);
            set(config, "DimensionId", dimensionId);
            set(config, "Seed", Long.toString(server.getWorld(0).getSeed()));
            set(config, "ShowInWorldCreationGUI", false);
            Object settings = value(config, "Settings");
            set(settings, "DimensionPortalMaterials", new String[0]);
            set(settings, "HasSkyLight", true);
            set(settings, "IsSurfaceWorld", true);
            set(settings, "CanRespawnHere", true);
            set(settings, "CanDoRainSnowIce", true);
            set(settings, "CanDoLightning", true);
            set(settings, "DoesWaterVaporize", false);
            set(settings, "DoesXZShowFog", false);
            set(settings, "UseCustomFogColor", false);
            set(settings, "IsSkyColored", true);
            set(settings, "IsNightWorld", false);
            set(settings, "GravityFactor", 0.08D);
            set(settings, "MovementFactor", 1);
            set(settings, "CloudHeight", 128);
            set(settings, "RespawnDimension", dimensionId);
            Class<?> manager = Class.forName("com.pg85.otg.forge.dimensions.OTGDimensionManager");
            @SuppressWarnings("unchecked")
            List<Object> configuredDimensions = (List<Object>) value(dimensions, "Dimensions");
            configuredDimensions.add(config);
            manager.getMethod("registerDimension", int.class, DimensionType.class)
                    .invoke(null, dimensionId, destinationType);
            manager.getMethod("initDimension", int.class).invoke(null, dimensionId);
            manager.getMethod("SaveDimensionData").invoke(null);
            dimensions.getClass().getMethod("save").invoke(dimensions);
            bindCelestialBody();
            LogManager.getLogger("NewEarth").info("Registered NewEarth destination: {} (dimension {})", PRESET, dimensionId);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot initialize the NewEarth destination with OTG 9.9", error);
        }
    }

    private static Object value(Object object, String name) throws ReflectiveOperationException {
        return object.getClass().getField(name).get(object);
    }

    private static void set(Object object, String name, Object value) throws ReflectiveOperationException {
        Field field = object.getClass().getField(name);
        field.set(object, value);
    }

    public static boolean isDestination(World world) {
        return enabled && matches(world.provider.getDimensionType().getName(), world.provider.getDimension());
    }

    public static boolean matches(String preset, int dimension) {
        return dimension != 0 && PRESET.equals(preset);
    }

    public static BlockPos landing(WorldServer world) {
        if (!isDestination(world)) throw new IllegalArgumentException("Not the NewEarth destination");
        return LandingSite.get(world);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void arrived(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP && isDestination(event.player.world))
            ArrivalSession.enter((EntityPlayerMP) event.player, true);
        else if (event.player instanceof EntityPlayerMP) ArrivalSession.restore((EntityPlayerMP) event.player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || !isDestination(event.player.world)) return;
        ArrivalSession.enter((EntityPlayerMP) event.player, false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.player instanceof EntityPlayerMP && isDestination(event.player.world)
                && event.player.getBedLocation(event.player.dimension) == null)
            placeAtLanding((EntityPlayerMP) event.player);
    }

    private static void placeAtLanding(EntityPlayerMP player) {
        BlockPos at = landing(player.getServerWorld());
        player.setPositionAndUpdate(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.fallDistance = 0;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void spawn(LivingSpawnEvent.CheckSpawn event) {
        if (isDestination(event.getWorld()) && (event.getEntityLiving() instanceof IMob
                || event.getEntityLiving().isCreatureType(EnumCreatureType.MONSTER, false)))
            event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinWorldEvent event) {
        if (!event.getWorld().isRemote && isDestination(event.getWorld())
                && (event.getEntity() instanceof IMob
                || event.getEntity().isCreatureType(EnumCreatureType.MONSTER, false)))
            event.setCanceled(true);
    }
}
