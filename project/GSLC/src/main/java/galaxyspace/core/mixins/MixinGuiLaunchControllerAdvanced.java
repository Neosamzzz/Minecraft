package galaxyspace.core.mixins;

import micdoodle8.mods.galacticraft.planets.mars.client.gui.GuiLaunchControllerAdvanced;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Public editing within this GUI only; preserve the stored owner and title. */
@Mixin(value = GuiLaunchControllerAdvanced.class, remap = false)
public class MixinGuiLaunchControllerAdvanced {
    @Shadow private TileEntityLaunchController launchController;
    @Unique private boolean galaxyspace$publicAccessLogged;

    @Redirect(method = {"drawScreen", "func_73863_a", "actionPerformed", "func_146284_a", "canBeClickedBy", "canPlayerEdit"},
        at = @At(value = "INVOKE", target = "Lmicdoodle8/mods/galacticraft/core/util/PlayerUtil;getName(Lnet/minecraft/entity/player/EntityPlayer;)Ljava/lang/String;"),
        require = 4, allow = 4)
    private String galaxyspace$allowPublicEditing(EntityPlayer player) {
        if (!this.galaxyspace$publicAccessLogged) {
            org.apache.logging.log4j.LogManager.getLogger("GSLaunchControl").info("[GSLC] public controller editing active: GuiLaunchControllerAdvanced");
            this.galaxyspace$publicAccessLogged = true;
        }
        return this.launchController.getOwnerName();
    }
}
