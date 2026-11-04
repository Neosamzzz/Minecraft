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
        net.minecraftforge.client.model.obj.OBJLoader.INSTANCE.addDomain(NewEarth.MOD_ID);
        for(Item item:Manufacturing.ITEMS.values()) {
            int variants=item.getRegistryName().getPath().matches("tier(7|9|10)_rocket")?5:1;
            for(int meta=0;meta<variants;meta++) ModelLoader.setCustomModelResourceLocation(item,meta,new ModelResourceLocation(item.getRegistryName(),"inventory"));
        }
    }
    @SubscribeEvent public static void bake(net.minecraftforge.client.event.ModelBakeEvent event) {
        for (String name : new String[]{"tier7_rocket", "tier9_rocket", "tier10_rocket", "assembly_station", "disassembly_station"}) {
            boolean rocket = name.startsWith("tier");
            float scale = rocket ? .04F : .2F;
            net.minecraftforge.common.model.TRSRTransformation transform = new net.minecraftforge.common.model.TRSRTransformation(
                    new javax.vecmath.Vector3f(.5F, 0F, .5F),
                    net.minecraftforge.common.model.TRSRTransformation.quatFromXYZDegrees(new javax.vecmath.Vector3f(30F, 225F, 0F)),
                    new javax.vecmath.Vector3f(scale, scale, scale), null);
            micdoodle8.mods.galacticraft.core.util.ClientUtil.replaceModel(NewEarth.MOD_ID, event, name,
                    "models/" + (rocket ? name : "tier7_workbench") + ".obj",
                    com.google.common.collect.ImmutableList.of(net.minecraftforge.client.model.obj.OBJModel.Group.ALL),
                    null, transform);
        }
    }
    @SubscribeEvent public static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        net.minecraft.item.ItemStack stack=event.getItemStack();
        if("extraplanets:wafer".equals(String.valueOf(stack.getItem().getRegistryName())) && stack.getMetadata()==0) {
            String unused=net.minecraft.client.resources.I18n.format("currently.unused.name");
            event.getToolTip().removeIf(line -> unused.equals(net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes(line)));
        }
    }
}
