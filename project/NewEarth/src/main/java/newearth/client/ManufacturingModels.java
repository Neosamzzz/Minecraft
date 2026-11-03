package newearth.client;

import newearth.Manufacturing;
import newearth.NewEarth;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid=NewEarth.MOD_ID,value=Side.CLIENT)
public final class ManufacturingModels {
    @SubscribeEvent public static void models(ModelRegistryEvent event) {
        for(Item item:Manufacturing.ITEMS.values()) {
            int variants="tier7_rocket".equals(item.getRegistryName().getPath())?5:1;
            for(int meta=0;meta<variants;meta++) ModelLoader.setCustomModelResourceLocation(item,meta,new ModelResourceLocation(item.getRegistryName(),"inventory"));
        }
    }
}
