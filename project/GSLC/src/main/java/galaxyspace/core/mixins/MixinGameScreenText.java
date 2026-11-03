package galaxyspace.core.mixins;

import galaxyspace.core.telemetry.*;
import micdoodle8.mods.galacticraft.api.client.IScreenManager;
import micdoodle8.mods.galacticraft.core.client.screen.DrawGameScreen;
import micdoodle8.mods.galacticraft.core.client.screen.GameScreenText;
import micdoodle8.mods.galacticraft.core.tile.TileEntityTelemetry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import micdoodle8.mods.galacticraft.api.entity.ITelemetry;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameScreenText.class, remap = false)
public class MixinGameScreenText {
    @Unique private String galaxyspace$dimensionLine;
    @Unique private boolean galaxyspace$living;
    @Unique private int galaxyspace$line;
    @Shadow private float frameA;
    @Shadow private void drawText(String text, int colour) { throw new AssertionError(); }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lmicdoodle8/mods/galacticraft/core/client/screen/GameScreenText;drawBlackBackground(F)V", shift = At.Shift.AFTER), require = 1)
    private void galaxyspace$drawCornerClock(int type, float ticks, float sizeX, float sizeY, IScreenManager manager, CallbackInfo ci) {
        if (!(manager instanceof DrawGameScreen)) return;
        DrawGameScreen screen = (DrawGameScreen) manager;
        if (screen.driver == null || screen.driver.getWorld() == null) return;
        float usable = Math.min(sizeX, sizeY) - frameA * 2F;
        if (usable <= 0F) return;
        float scale = usable / 155F;
        float margin = usable * 0.02F;
        String text = ScreenClock.format(screen.driver.getWorld().getWorldTime());
        float x = sizeX - frameA - margin - Minecraft.getMinecraft().fontRenderer.getStringWidth(text) * scale;
        GL11.glPushMatrix();
        try {
            // Draw before GC applies the centred text/model transforms.
            GL11.glTranslatef(x, frameA + margin, 0F);
            GL11.glScalef(scale, scale, 1F);
            Minecraft.getMinecraft().fontRenderer.drawString(text, 0F, 0F, 0xfff0d8ff, false);
        } finally {
            GL11.glPopMatrix();
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lmicdoodle8/mods/galacticraft/core/client/screen/GameScreenText;makeTimeString(I)Ljava/lang/String;"), require = 1)
    private String galaxyspace$removeInlineClock(GameScreenText screen, int time) {
        return "";
    }

    @Redirect(method = "render(IFFFLmicdoodle8/mods/galacticraft/api/client/IScreenManager;)V", remap = false, at = @At(value = "INVOKE", target = "Lmicdoodle8/mods/galacticraft/api/entity/ITelemetry;adjustDisplay([I)V", remap = false), require = 1)
    private void galaxyspace$removeRocketLift(ITelemetry telemetry, int[] data) {
        telemetry.adjustDisplay(data);
        if (telemetry instanceof Entity) {
            Entity entity = (Entity) telemetry;
            float offset = TelemetryRocketLayout.verticalOffset(entity.getClass().getName(), entity.height);
            if (offset != 0F) {
                GL11.glTranslatef(0F, offset, 0F);
                // Text and clock were already drawn; only this rocket model is scaled.
                GL11.glScalef(0.7F, 0.7F, 0.7F);
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void galaxyspace$prepareDimension(int type, float ticks, float x, float y, IScreenManager manager, CallbackInfo ci) {
        galaxyspace$line = 0; galaxyspace$dimensionLine = null; galaxyspace$living = false;
        if (!(manager instanceof DrawGameScreen)) return;
        TileEntityTelemetry tile = TileEntityTelemetry.getNearest(((DrawGameScreen) manager).driver);
        if (tile == null) return;
        galaxyspace$living = tile.clientClass != null && EntityLivingBase.class.isAssignableFrom(tile.clientClass);
        TelemetryDimensionState state = (TelemetryDimensionState) tile;
        if (!state.galaxyspace$hasDimensionTarget()) return;
        String label = I18n.hasKey("gslc.dimension.unavailable") ? I18n.format("gslc.dimension.unavailable") : "目标未加载";
        if (state.galaxyspace$dimensionKnown()) {
            String key = state.galaxyspace$dimensionKey();
            label = !key.isEmpty() && I18n.hasKey(key) ? I18n.format(key) : state.galaxyspace$dimensionFallback();
            if (label == null || label.isEmpty()) label = "ID " + state.galaxyspace$targetDimension();
        }
        galaxyspace$dimensionLine = I18n.hasKey("gslc.dimension.line") ? I18n.format("gslc.dimension.line", label) : "所在维度：" + label;
    }
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lmicdoodle8/mods/galacticraft/core/client/screen/GameScreenText;drawText(Ljava/lang/String;I)V"), require = 6, allow = 6)
    private void galaxyspace$boundedTelemetryText(GameScreenText screen, String text, int colour) {
        int line = galaxyspace$line++;
        boolean dimensionRow = galaxyspace$living && line == 3
            || !galaxyspace$living && line == 1 && galaxyspace$dimensionLine != null;
        if (dimensionRow) {
            text = galaxyspace$dimensionLine == null ? "" : galaxyspace$dimensionLine;
            text = TelemetryTextLayout.fit(text, 155, s -> Minecraft.getMinecraft().fontRenderer.getStringWidth(s));
        }
        // Every other line, font scale, model size and screen layout stays vanilla.
        drawText(text, colour);
    }
}
