package galaxyspace.core.telemetry;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = TelemetryAddon.MODID, name = "GS Launch Compatibility Telemetry", version = "1.4",
    dependencies = "required-after:galacticraftcore;required-after:galaxyspace")
public class TelemetryAddon {
    public static final String MODID = "gslaunchtelemetry";
    @SidedProxy(clientSide = "galaxyspace.core.telemetry.TelemetryClient", serverSide = "galaxyspace.core.telemetry.TelemetryProxy")
    public static TelemetryProxy proxy;
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        TelemetryNetwork.init();
        event.getModLog().info("[GSLC] dimension telemetry 1.4 loaded; update both client and server");
    }
}
