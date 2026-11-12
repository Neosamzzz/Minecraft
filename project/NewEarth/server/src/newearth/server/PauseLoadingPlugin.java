package newearth.server;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("NewEarthServerPause")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"newearth.server.PauseLoadingPlugin", "newearth.server.PauseTransformer"})
public final class PauseLoadingPlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() { return new String[] {"newearth.server.PauseTransformer"}; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) { }
    public String getAccessTransformerClass() { return null; }
}
