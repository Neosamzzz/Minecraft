package galaxyspace.core.mixins;

import java.util.UUID;
import galaxyspace.core.telemetry.*;
import micdoodle8.mods.galacticraft.core.tile.TileEntityTelemetry;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = TileEntityTelemetry.class, remap = false)
public class MixinTileEntityTelemetry implements TelemetryDimensionState {
    @Shadow public Entity linkedEntity;
    @Shadow private UUID toUpdate;
    @Unique private UUID galaxyspace$targetUUID;
    @Unique private int galaxyspace$ticks;
    @Unique private String galaxyspace$lastState;
    @Unique private boolean galaxyspace$clientTracked, galaxyspace$clientKnown;
    @Unique private int galaxyspace$clientDimension;
    @Unique private String galaxyspace$clientKey = "", galaxyspace$clientFallback = "";
    @Unique private long galaxyspace$lastReceived;

    @Inject(method = "addTrackedEntity(Ljava/util/UUID;)V", at = @At("HEAD"))
    private void galaxyspace$trackUUID(UUID uuid, CallbackInfo ci) {
        galaxyspace$targetUUID = uuid != null && (uuid.getMostSignificantBits() != 0 || uuid.getLeastSignificantBits() != 0) ? uuid : null;
    }
    @Inject(method = "addTrackedEntity(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    private void galaxyspace$trackEntity(Entity entity, CallbackInfo ci) {
        galaxyspace$targetUUID = entity == null ? null : entity.getUniqueID();
    }
    @Inject(method = "removeTrackedEntity", at = @At("RETURN"))
    private void galaxyspace$unlink(CallbackInfo ci) { galaxyspace$targetUUID = null; }

    @Inject(method = {"readFromNBT", "func_145839_a"}, at = @At("RETURN"))
    private void galaxyspace$readLink(NBTTagCompound nbt, CallbackInfo ci) {
        galaxyspace$targetUUID = null;
        if (nbt.hasKey("gslcTrackedUUID")) {
            try { galaxyspace$targetUUID = UUID.fromString(nbt.getString("gslcTrackedUUID")); }
            catch (IllegalArgumentException ignored) { }
        } else if (nbt.hasKey("entityUUIDMost") && nbt.hasKey("entityUUIDLeast")) {
            UUID uuid = new UUID(nbt.getLong("entityUUIDMost"), nbt.getLong("entityUUIDLeast"));
            if (uuid.getMostSignificantBits() != 0 || uuid.getLeastSignificantBits() != 0) galaxyspace$targetUUID = uuid;
        }
        if (galaxyspace$targetUUID != null && galaxyspace$targetUUID.getMostSignificantBits() == 0
                && galaxyspace$targetUUID.getLeastSignificantBits() == 0) galaxyspace$targetUUID = null;
        // Reacquire across all loaded dimensions instead of GC's same-world-only lookup.
        toUpdate = null;
    }
    @Inject(method = {"writeToNBT", "func_189515_b"}, at = @At("RETURN"))
    private void galaxyspace$saveLink(NBTTagCompound nbt, CallbackInfoReturnable<NBTTagCompound> cir) {
        if (galaxyspace$targetUUID != null) nbt.setString("gslcTrackedUUID", galaxyspace$targetUUID.toString());
        else {
            nbt.removeTag("gslcTrackedUUID");
            nbt.removeTag("entityUUIDMost");
            nbt.removeTag("entityUUIDLeast");
        }
    }
    @Inject(method = {"update", "func_73660_a"}, at = @At("HEAD"))
    private void galaxyspace$recoverLink(CallbackInfo ci) {
        TileEntityTelemetry tile = (TileEntityTelemetry) (Object) this;
        if (tile.getWorld() == null || tile.getWorld().isRemote) return;
        if (galaxyspace$targetUUID == null && linkedEntity != null) galaxyspace$targetUUID = linkedEntity.getUniqueID();
        if (galaxyspace$ticks++ % 20 == 0 && galaxyspace$targetUUID != null) {
            Entity entity = TelemetryTracking.find(galaxyspace$targetUUID, GCCoreUtil.getWorldServerList(tile.getWorld()));
            if (entity != linkedEntity) {
                if (entity != null) tile.addTrackedEntity(entity);
                else linkedEntity = null;
            }
        }
    }
    @Inject(method = {"update", "func_73660_a"}, at = @At("RETURN"))
    private void galaxyspace$sendDimension(CallbackInfo ci) {
        TileEntityTelemetry tile = (TileEntityTelemetry) (Object) this;
        if (tile.getWorld() == null || tile.getWorld().isRemote) return;
        boolean known = linkedEntity != null && !linkedEntity.isDead && linkedEntity.world != null;
        String state = galaxyspace$targetUUID + ":" + (known ? linkedEntity.world.provider.getDimension() : "unavailable");
        if (!state.equals(galaxyspace$lastState) || galaxyspace$ticks % 20 == 0) {
            TelemetryNetwork.send(tile, known ? linkedEntity : null, galaxyspace$targetUUID != null);
            if (!state.equals(galaxyspace$lastState)) {
                org.apache.logging.log4j.LogManager.getLogger("GSLaunchControl").info("[GSLC] telemetry={} target={}", tile.getPos(), state);
                galaxyspace$lastState = state;
            }
        }
    }
    @Override public void galaxyspace$receiveDimension(boolean tracked, boolean known, int dimension, String key, String fallback) {
        galaxyspace$clientTracked = tracked; galaxyspace$clientKnown = known; galaxyspace$clientDimension = dimension;
        galaxyspace$clientKey = key; galaxyspace$clientFallback = fallback; galaxyspace$lastReceived = System.nanoTime();
    }
    @Override public boolean galaxyspace$hasDimensionTarget() { return galaxyspace$clientTracked; }
    @Override public boolean galaxyspace$dimensionKnown() { return galaxyspace$clientKnown && System.nanoTime() - galaxyspace$lastReceived < 5_000_000_000L; }
    @Override public int galaxyspace$targetDimension() { return galaxyspace$clientDimension; }
    @Override public String galaxyspace$dimensionKey() { return galaxyspace$clientKey; }
    @Override public String galaxyspace$dimensionFallback() { return galaxyspace$clientFallback; }
}
