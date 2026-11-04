package newearth;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import newearth.dimension.EndDimension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = NewEarth.MOD_ID, name = "NewEarth", version = NewEarth.VERSION,
        dependencies = "required-after:galacticraftcore;required-after:galaxyspace;required-after:extraplanets",
        acceptableRemoteVersions = NewEarth.VERSION)
public final class NewEarth {
    public static final String MOD_ID = "newearth";
    public static final String VERSION = "0.3.0-terrain.7";
    static final Logger LOG = LogManager.getLogger("NewEarth");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EndDimension.configure(event.getModConfigurationDirectory());
        newearth.dimension.ArrivalNetwork.init();
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
    }
}
