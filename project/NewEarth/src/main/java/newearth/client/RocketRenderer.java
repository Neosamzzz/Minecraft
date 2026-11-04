package newearth.client;

import newearth.EntityTier10Rocket;
import newearth.EntityTier9Rocket;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** Draws the placed hull with ExtraPlanets' own rocket model and transform. */
@SideOnly(Side.CLIENT)
public final class RocketRenderer extends Render<EntityAutoRocket> {
    private final String registryName;
    private final boolean tier10;
    private Object model;

    public static void register() {
        RenderingRegistry.registerEntityRenderingHandler(EntityTier9Rocket.class, manager -> new RocketRenderer(manager, "rocket_t9", false));
        RenderingRegistry.registerEntityRenderingHandler(EntityTier10Rocket.class, manager -> new RocketRenderer(manager, "rocket_t10_new", true));
    }

    private RocketRenderer(RenderManager manager, String registryName, boolean tier10) {
        super(manager);
        this.registryName = registryName;
        this.tier10 = tier10;
        shadowSize = 2F;
    }

    private void updateModel() throws ReflectiveOperationException {
        if (model != null) return;
        Class<?> utilities = Class.forName("com.mjr.mjrlegendslib.util.ModelUtilities");
        model = utilities.getMethod("getModelFromRegistry", String.class, String.class)
                .invoke(null, "extraplanets:", registryName);
    }

    private void drawModel() throws ReflectiveOperationException {
        Class<?> utilities = Class.forName("com.mjr.mjrlegendslib.util.ModelUtilities");
        utilities.getMethod("drawBakedModel", net.minecraft.client.renderer.block.model.IBakedModel.class)
                .invoke(null, model);
    }

    @Override protected ResourceLocation getEntityTexture(EntityAutoRocket entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }

    @Override public void doRender(EntityAutoRocket entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.disableRescaleNormal();
        GlStateManager.pushMatrix();
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks + 180;
        if (tier10) GlStateManager.translate((float) x, (float) y - 0.2F, (float) z);
        else GlStateManager.translate((float) x, (float) y, (float) z + 0.6F);
        GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-pitch, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(0.0F, entity.getRenderOffsetY(), 0.0F);
        float roll = entity.rollAmplitude / 3 - partialTicks;
        if (roll > 0.0F) {
            float wobble = entity.getLaunched() ? (5 - MathHelper.floor(entity.timeUntilLaunch / 85)) / 10F : 0.3F;
            GlStateManager.rotate(MathHelper.sin(roll) * roll * wobble * partialTicks, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(MathHelper.sin(roll) * roll * wobble * partialTicks, 1.0F, 0.0F, 1.0F);
        }
        try {
            updateModel();
            bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
            float scale = tier10 ? 0.7F : 1.1F;
            GlStateManager.scale(scale, scale, scale);
            drawModel();
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Missing ExtraPlanets rocket model " + registryName, error);
        }
        GlStateManager.popMatrix();
        RenderHelper.enableStandardItemLighting();
    }

    @Override public boolean shouldRender(EntityAutoRocket rocket, ICamera camera, double camX, double camY, double camZ) {
        AxisAlignedBB box = rocket.getEntityBoundingBox().grow(0.6D, 0, 0.6D);
        return rocket.isInRangeToRender3d(camX, camY, camZ) && camera.isBoundingBoxInFrustum(box);
    }
}
