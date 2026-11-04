package newearth.client;

import com.google.common.collect.ImmutableList;
import newearth.EntityTier9Rocket;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import newearth.EntityTier10Rocket;
import newearth.NewEarth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import micdoodle8.mods.galacticraft.core.client.model.OBJLoaderGC;
import micdoodle8.mods.galacticraft.core.util.ClientUtil;

@SideOnly(Side.CLIENT)
public final class RocketRenderer extends Render<EntityAutoRocket> {
    private final String modelName;
    private IBakedModel baked;
    private Object textureAtlas;
    public static void register() {
        RenderingRegistry.registerEntityRenderingHandler(EntityTier9Rocket.class, manager -> new RocketRenderer(manager, "tier9_rocket"));
        RenderingRegistry.registerEntityRenderingHandler(EntityTier10Rocket.class, manager -> new RocketRenderer(manager, "tier10_rocket"));
    }
    private RocketRenderer(RenderManager manager, String name) { super(manager); modelName = name; shadowSize = 2F; }
    @Override protected ResourceLocation getEntityTexture(EntityAutoRocket rocket) { return TextureMap.LOCATION_BLOCKS_TEXTURE; }
    @Override public boolean shouldRender(EntityAutoRocket rocket, net.minecraft.client.renderer.culling.ICamera camera, double x, double y, double z) {
        return rocket.isInRangeToRender3d(x, y, z) && camera.isBoundingBoxInFrustum(rocket.getEntityBoundingBox().grow(3D, 1D, 3D));
    }
    @Override public void doRender(EntityAutoRocket rocket, double x, double y, double z, float yaw, float partial) {
        try {
            Object currentAtlas = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("minecraft:blocks/stone");
            if (baked == null || textureAtlas != currentAtlas) {
            textureAtlas = currentAtlas;
            IModel model = OBJLoaderGC.instance.loadModel(new ResourceLocation(NewEarth.MOD_ID, "models/" + modelName + ".obj"));
            baked = model.bake(new OBJModel.OBJState(ImmutableList.of(OBJModel.Group.ALL), false), DefaultVertexFormats.ITEM,
                    texture -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(texture.toString()));
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(180F - yaw, 0F, 1F, 0F);
            GlStateManager.rotate(rocket.prevRotationPitch + (rocket.rotationPitch - rocket.prevRotationPitch) * partial, 0F, 0F, 1F);
            GlStateManager.scale(.4F, .4F, .4F);
            bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.disableCull();
            ClientUtil.drawBakedModel(baked);
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        } catch (java.io.IOException error) { throw new IllegalStateException("Missing rocket hull " + modelName, error); }
    }
}
