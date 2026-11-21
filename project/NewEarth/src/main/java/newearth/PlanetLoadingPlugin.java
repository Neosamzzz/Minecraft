package newearth;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/** Early hooks are needed before EP registers its config-dependent items and worlds. */
@IFMLLoadingPlugin.Name("NewEarthSolarSystem")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"newearth.PlanetTransformer", "newearth.ContentTransformer", "newearth.EnvironmentTransformer", "newearth.ManufacturingTransformer", "newearth.AstroMiningTransformer", "newearth.worldgen.IslandTransformer", "newearth.DrawerJeiTransformer", "newearth.DrawerReturnTransformer", "newearth.DrawerManualTakeTransformer", "newearth.InventoryTweaksTransformer", "newearth.DisenchanterTransformer", "newearth.PlanetLoadingPlugin"})
public final class PlanetLoadingPlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() { return new String[] {"newearth.DrawerJeiTransformer", "newearth.DrawerReturnTransformer", "newearth.DrawerManualTakeTransformer", "newearth.InventoryTweaksTransformer", "newearth.DisenchanterTransformer", "newearth.dimension.HeightTransformer", "newearth.worldgen.IslandTransformer", "newearth.PlanetTransformer", "newearth.ContentTransformer", "newearth.EnvironmentTransformer", "newearth.ManufacturingTransformer", "newearth.AstroMiningTransformer"}; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) { }
    public String getAccessTransformerClass() { return null; }
}
