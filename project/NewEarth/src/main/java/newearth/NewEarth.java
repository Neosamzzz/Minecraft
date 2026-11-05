package newearth;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import newearth.dimension.EndDimension;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = NewEarth.MOD_ID, name = "NewEarth", version = NewEarth.VERSION,
        dependencies = "required-after:galacticraftcore;required-after:galaxyspace;required-after:extraplanets",
        acceptableRemoteVersions = NewEarth.VERSION)
public final class NewEarth {
    public static final String MOD_ID = "newearth";
    public static final String VERSION = "0.3.46";
    static final Logger LOG = LogManager.getLogger("NewEarth");

    @Mod.Instance(MOD_ID)
    public static NewEarth instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EndDimension.configure(event.getModConfigurationDirectory());
        newearth.dimension.ArrivalNetwork.init();
        if (event.getSide().isClient()) newearth.client.RocketRenderer.register();
        // Register T9/T10 multi-seat rocket entities
        EntityRegistry.registerModEntity(new net.minecraft.util.ResourceLocation(MOD_ID, "tier7_rocket"),
            EntityTier7Rocket.class, "tier7_rocket", 2, this, 256, 1, true);
        EntityRegistry.registerModEntity(new net.minecraft.util.ResourceLocation(MOD_ID, "tier9_rocket"),
            EntityTier9Rocket.class, "tier9_rocket", 0, this, 256, 1, true);
        EntityRegistry.registerModEntity(new net.minecraft.util.ResourceLocation(MOD_ID, "tier10_rocket"),
            EntityTier10Rocket.class, "tier10_rocket", 1, this, 256, 1, true);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ManufacturingGuiHandler());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new newearth.dimension.ArrivalSession.ReplayCommand());
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        EndDimension.start(FMLCommonHandler.instance().getMinecraftServerInstance());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        PlanetHooks.validateConfiguration();
        ContentPruning.install();
        MaterialUnification.install();
        SolarSystemBridge.install();
        PlanetHooks.integrate();
        EnvironmentHooks.adaptMetadata();
        EndDimension.bindCelestialBody();
        Manufacturing.install();
    }
}
