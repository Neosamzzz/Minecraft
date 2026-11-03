package galaxyspace.core.mixins;

import galaxyspace.core.util.LaunchControllerCompatibility;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityAutoRocket.class, remap = false)
public class MixinEntityAutoRocket {
    @Inject(method = "setTarget", at = @At("RETURN"), cancellable = true)
    private void galaxyspace$findAdvancedPad(boolean doSet, int destFrequency, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            cir.setReturnValue(LaunchControllerCompatibility.setTarget((EntityAutoRocket) (Object) this, doSet, destFrequency));
        }
    }
}
