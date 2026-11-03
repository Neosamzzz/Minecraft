package galaxyspace.core.mixins;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import galaxyspace.core.util.LaunchControllerCompatibility;
import galaxyspace.systems.SolarSystem.planets.overworld.tile.TileEntityAdvLandingPad;
import micdoodle8.mods.galacticraft.core.world.ChunkLoadingCallback;
import micdoodle8.mods.galacticraft.planets.mars.ConfigManagerMars;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.ForgeChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityLaunchController.class, remap = false)
public class MixinTileEntityLaunchConroller {
    @Shadow private ForgeChunkManager.Ticket chunkLoadTicket;
    @Shadow private List<BlockPos> connectedPads;
    @Unique private final List<BlockPos> galaxyspace$connectedPads = new ArrayList<>();

    @Inject(method = "canAttachToLandingPad", at = @At("RETURN"), cancellable = true)
    private void galaxyspace$canAttachToLandingPad(IBlockAccess world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && LaunchControllerCompatibility.canAttachToPad(world, pos)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "updateRocketOnDockSettings", at = @At("HEAD"), cancellable = true)
    private void galaxyspace$updateRocketOnDockSettings(CallbackInfo ci) {
        TileEntityLaunchController controller = (TileEntityLaunchController) (Object) this;
        if (LaunchControllerCompatibility.updateRocketOnDockSettings(controller.attachedDock)) {
            ci.cancel();
        }
    }

    @Inject(method = "onTicketLoaded", at = @At("RETURN"))
    private void galaxyspace$loadPadChunks(ForgeChunkManager.Ticket ticket, boolean placed, CallbackInfo ci) {
        TileEntityLaunchController controller = (TileEntityLaunchController) (Object) this;
        if (ticket == null || controller.getWorld().isRemote || !ConfigManagerMars.launchControllerChunkLoad) {
            return;
        }
        this.galaxyspace$trackPadChunks(controller, ticket, placed);
    }

    @Unique
    private void galaxyspace$trackPadChunks(TileEntityLaunchController controller, ForgeChunkManager.Ticket ticket, boolean placed) {
        ChunkPos controllerChunk = new ChunkPos(controller.getPos());
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                BlockPos pos = controller.getPos().add(x, 0, z);
                if (new ChunkPos(pos).equals(controllerChunk)
                        // Restored tickets must also discover pads across unloaded chunk borders.
                        || (placed && !controller.getWorld().isBlockLoaded(pos))
                        || !LaunchControllerCompatibility.canAttachToPad(controller.getWorld(), pos)) {
                    continue;
                }
                TileEntityAdvLandingPad pad = (TileEntityAdvLandingPad) controller.getWorld().getTileEntity(pos);
                if (!pad.getConnectedTiles().contains(controller) || this.galaxyspace$connectedPads.contains(pos)) {
                    continue;
                }
                this.galaxyspace$connectedPads.add(pos);
                if (placed) {
                    ChunkLoadingCallback.forceChunk(ticket, controller.getWorld(), pos.getX(), pos.getY(), pos.getZ(), controller.getOwnerName());
                } else {
                    ChunkLoadingCallback.addToList(controller.getWorld(), controller.getPos().getX(), controller.getPos().getY(), controller.getPos().getZ(), controller.getOwnerName());
                }
            }
        }
    }

    @Inject(method = {"update", "func_73660_a"}, at = @At("RETURN"), require = 1)
    private void galaxyspace$diagnoseBinding(CallbackInfo ci) {
        TileEntityLaunchController controller = (TileEntityLaunchController) (Object) this;
        if (!controller.getWorld().isRemote && controller.ticks % 40 == 0) {
            LaunchControllerCompatibility.logController(controller);
        }
    }

    @Inject(method = {"update", "func_73660_a"}, at = @At("RETURN"), require = 1)
    private void galaxyspace$releaseRemovedPadChunks(CallbackInfo ci) {
        TileEntityLaunchController controller = (TileEntityLaunchController) (Object) this;
        if (controller.getWorld().isRemote || this.chunkLoadTicket == null || controller.ticks % 20 != 0) {
            return;
        }
        // Pads can be built after their controller has received its ticket.
        this.galaxyspace$trackPadChunks(controller, this.chunkLoadTicket, true);
        List<ChunkPos> removedChunks = new ArrayList<>();
        Iterator<BlockPos> iterator = this.galaxyspace$connectedPads.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!LaunchControllerCompatibility.canAttachToPad(controller.getWorld(), pos)) {
                iterator.remove();
                removedChunks.add(new ChunkPos(pos));
            } else {
                // GC may have just unforced this chunk after a GC pad was removed.
                ForgeChunkManager.forceChunk(this.chunkLoadTicket, new ChunkPos(pos));
            }
        }
        // A ticket may cover several pads in the same chunk; keep it while any pad remains.
        for (ChunkPos chunk : removedChunks) {
            boolean stillNeeded = false;
            for (BlockPos pos : this.galaxyspace$connectedPads) {
                stillNeeded |= new ChunkPos(pos).equals(chunk);
            }
            for (BlockPos pos : this.connectedPads) {
                stillNeeded |= new ChunkPos(pos).equals(chunk);
            }
            if (!stillNeeded && !new ChunkPos(controller.getPos()).equals(chunk)) {
                ForgeChunkManager.unforceChunk(this.chunkLoadTicket, chunk);
            }
        }
    }
}
