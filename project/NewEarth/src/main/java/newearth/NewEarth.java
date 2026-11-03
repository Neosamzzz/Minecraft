package newearth;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = NewEarth.MOD_ID, name = "NewEarth", version = "0.2.13",
        dependencies = "required-after:galacticraftcore;required-after:galaxyspace;required-after:extraplanets",
        acceptableRemoteVersions = "0.2.13")
public final class NewEarth {
    public static final String MOD_ID = "newearth";
    static final Logger LOG = LogManager.getLogger("NewEarth");

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        PlanetHooks.validateConfiguration();
        ContentPruning.install();
        SolarSystemBridge.install();
        PlanetHooks.integrate();
        EnvironmentHooks.adaptMetadata();
    }
}
