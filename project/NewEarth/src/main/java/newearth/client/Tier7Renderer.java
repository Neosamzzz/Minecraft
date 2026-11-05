package newearth.client;

import com.google.common.collect.ImmutableList;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import micdoodle8.mods.galacticraft.core.client.model.OBJLoaderGC;
import micdoodle8.mods.galacticraft.core.util.ClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import newearth.NewEarth;
import org.lwjgl.opengl.GL11;

/** GS tier-6 placement and flight, drawn with the tier-7 hull. */
@SideOnly(Side.CLIENT)
final class Tier7Renderer extends Render<EntityAutoRocket> {
    private net.minecraft.client.renderer.block.model.IBakedModel baked;

    Tier7Renderer(RenderManager manager) {
        super(manager);
        shadowSize = 2F;
    }

    @Override protected ResourceLocation getEntityTexture(EntityAutoRocket entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    @Override public void doRender(EntityAutoRocket entity, double x, double y, double z, float yaw, float partial) {
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partial + 180;
        GlStateManager.disableRescaleNormal();
        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(pitch, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(0.0F, entity.getRenderOffsetY(), 0.0F);
        float roll = entity.rollAmplitude / 3 - partial;
        if (roll > 0.0F) {
            float wobble = entity.getLaunched() ? (5 - MathHelper.floor(entity.timeUntilLaunch / 85)) / 10F : 0.3F;
            GlStateManager.rotate(MathHelper.sin(roll) * roll * wobble * partial, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(MathHelper.sin(roll) * roll * wobble * partial, 1.0F, 0.0F, 1.0F);
        }
        try {
            if (baked == null) {
                IModel model = OBJLoaderGC.instance.loadModel(new ResourceLocation(NewEarth.MOD_ID, "models/tier7_rocket.obj"));
                baked = model.bake(new OBJModel.OBJState(ImmutableList.of(OBJModel.Group.ALL), false), DefaultVertexFormats.ITEM,
                        location -> Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(location.toString()));
            }
            bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            // The stored hull is the enlarged EP tier-7 mesh; 0.04 matches its item display.
            GlStateManager.scale(0.04F, 0.04F, 0.04F);
            ClientUtil.drawBakedModel(baked);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Missing tier-7 hull", error);
        }
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
        RenderHelper.enableStandardItemLighting();
    }
}
