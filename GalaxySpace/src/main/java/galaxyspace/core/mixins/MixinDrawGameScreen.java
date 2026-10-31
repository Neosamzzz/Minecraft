package galaxyspace.core.mixins;

import galaxyspace.core.telemetry.SurfaceMapClient;
import micdoodle8.mods.galacticraft.core.client.screen.DrawGameScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DrawGameScreen.class, remap = false)
public class MixinDrawGameScreen {
    @Inject(method = "makeMap", at = @At("HEAD"), cancellable = true, require = 1)
    private void galaxyspace$skipPredictedMap(CallbackInfo ci) {
        // The old generator predicts Overworld terrain and its cache has no dimension key.
        ci.cancel();
    }

    @Inject(method = "doDraw", at = @At("HEAD"), require = 1)
    private void galaxyspace$prepareSurfaceMap(int type, float ticks, CallbackInfo ci) {
        if (type != 1) return;
        SurfaceMapClient.prepare((DrawGameScreen) (Object) this);
    }
}
