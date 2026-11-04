package newearth.dimension;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.IMob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
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

    private EndDimension() { }

    public static void configure(File configDirectory) {
        Configuration config = new Configuration(new File(configDirectory, "newearth-dimension.cfg"));
        config.load();
        enabled = config.getBoolean("enabled", "dimension", true, "Create the NewEarth destination using the Biome Bundle preset.");
        dimensionId = config.getInt("id", "dimension", 1020, 3, 1023, "Dimension ID for new saves. Existing saves retain their OTG dimension ID.");
        configuredId = dimensionId;
        if (config.hasChanged()) config.save();
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
                return;
            }
            if (DimensionManager.isDimensionRegistered(dimensionId))
                throw new IllegalStateException("NewEarth dimension ID is occupied: " + dimensionId + ". Change config/newearth-dimension.cfg before creating the destination.");
            Class<?> configType = Class.forName("com.pg85.otg.configuration.dimensions.DimensionConfig");
            Object config = configType.getConstructor(String.class, int.class, boolean.class)
                    .newInstance(PRESET, dimensionId, false);
            set(config, "Seed", Long.toString(server.getWorld(0).getSeed()));
            set(config, "ShowInWorldCreationGUI", false);
            Object settings = value(config, "Settings");
            set(settings, "DimensionPortalMaterials", new String[0]);
            set(settings, "HasSkyLight", true);
            set(settings, "IsSurfaceWorld", true);
            set(settings, "CanRespawnHere", true);
            set(settings, "CanDoRainSnowIce", true);
            set(settings, "CanDoLightning", true);
            set(settings, "RespawnDimension", dimensionId);
            Class<?> manager = Class.forName("com.pg85.otg.forge.dimensions.OTGDimensionManager");
            boolean created = (Boolean) manager.getMethod("createDimension", configType, long.class, boolean.class)
                    .invoke(null, config, server.getWorld(0).getSeed(), true);
            if (!created) throw new IllegalStateException("OTG rejected the NewEarth dimension registration.");
            dimensions.getClass().getMethod("save").invoke(dimensions);
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
