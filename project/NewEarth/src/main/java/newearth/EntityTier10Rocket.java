package newearth;

import java.util.ArrayList;
import java.util.List;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Tier 10 multi-seat rocket (6 seats).
 * Same hull as T9, black hole engine.
 * Exhaust is the shared EP four-nozzle flame.
 */
public class EntityTier10Rocket extends EntityTier9Rocket {
    private boolean arrivalHold;
    private double holdX, holdY, holdZ;

    public EntityTier10Rocket(World world) { super(world); }
    public EntityTier10Rocket(World world, double x, double y, double z) { super(world, x, y, z); }
    @Override public int getRocketTier() { return 10; }
    @Override public float getRotateOffset() { return 5.8F; }
    @Override public double getOnPadYOffset() { return 1.0D; }
    @Override protected String itemId() { return "tier10_rocket"; }

    /** Pin the hull, with whoever is still aboard, until the arrival session moves or releases it. */
    public void holdAbove(BlockPos at, double y) {
        arrivalHold = true;
        holdX = at.getX() + 0.5D;
        holdY = y;
        holdZ = at.getZ() + 0.5D;
        motionX = motionY = motionZ = 0;
        setPosition(holdX, holdY, holdZ);
    }

    public void releaseHold() {
        arrivalHold = false;
        motionX = motionY = motionZ = 0;
        setLaunchPhase(EntitySpaceshipBase.EnumLaunchPhase.UNIGNITED);
    }

    @Override public boolean shouldIgnoreShiftExit() { return arrivalHold || super.shouldIgnoreShiftExit(); }

    @Override public void onUpdate() {
        if (arrivalHold) motionX = motionY = motionZ = 0;
        super.onUpdate();
        if (!arrivalHold) return;
        motionX = motionY = motionZ = 0;
        setPosition(holdX, holdY, holdZ);
        fallDistance = 0;
    }

    @Override protected void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setBoolean("NewEarthArrivalHold", arrivalHold);
        nbt.setDouble("NewEarthHoldX", holdX);
        nbt.setDouble("NewEarthHoldY", holdY);
        nbt.setDouble("NewEarthHoldZ", holdZ);
    }

    @Override protected void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        arrivalHold = nbt.getBoolean("NewEarthArrivalHold");
        holdX = nbt.getDouble("NewEarthHoldX");
        holdY = nbt.getDouble("NewEarthHoldY");
        holdZ = nbt.getDouble("NewEarthHoldZ");
    }

    @Override public void onReachAtmosphere() {
        if (arrivalHold) return;
        if (newearth.dimension.EndDimension.isDestination(world)) { super.onReachAtmosphere(); return; }
        if (world.isRemote) { stopRocketSound(); return; }
        int target = newearth.dimension.EndDimension.destinationId();
        if (target == 0 || !net.minecraftforge.common.DimensionManager.isDimensionRegistered(target)) return;
        WorldServer destination = world.getMinecraftServer().getWorld(target);
        if (destination == null) return;
        List<Entity> crew = new ArrayList<>(getPassengers());
        List<EntityPlayerMP> players = new ArrayList<>();
        for (Entity passenger : crew) if (passenger instanceof EntityPlayerMP) players.add((EntityPlayerMP) passenger);
        if (players.isEmpty()) return;
        newearth.dimension.ArrivalSession.prepareCrew(destination, players);
        EntityPlayerMP pilot = getDriver() instanceof EntityPlayerMP ? (EntityPlayerMP) getDriver() : players.get(0);
        for (EntityPlayerMP player : players) onTeleport(player);
        Entity moved = micdoodle8.mods.galacticraft.core.util.WorldUtil.transferEntityToDimension(pilot, target, destination, false, this);
        if (moved == null || !(pilot.getRidingEntity() instanceof EntityTier10Rocket)) return;
        EntityTier10Rocket ship = (EntityTier10Rocket) pilot.getRidingEntity();
        for (Entity member : crew) if (member instanceof EntityPlayerMP && member != pilot) {
            Entity transferred = micdoodle8.mods.galacticraft.core.util.WorldUtil.transferEntityToDimension(member, target, destination, false, null);
            if (transferred != null) member.startRiding(ship, true);
        }
        pilot.dismountRidingEntity();
        pilot.startRiding(ship, true);
        ship.setWaitForPlayer(false);
        ship.holdAbove(destination.getSpawnPoint(), destination.getActualHeight() - 4);
    }
}
