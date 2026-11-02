package newearth;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/** Early hooks are needed before EP registers its config-dependent items and worlds. */
@IFMLLoadingPlugin.Name("NewEarthSolarSystem")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"newearth.PlanetTransformer", "newearth.ContentTransformer", "newearth.PlanetLoadingPlugin"})
public final class PlanetLoadingPlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() { return new String[] {"newearth.PlanetTransformer", "newearth.ContentTransformer"}; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) { }
    public String getAccessTransformerClass() { return null; }
}
