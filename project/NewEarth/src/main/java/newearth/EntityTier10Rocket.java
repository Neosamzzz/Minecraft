package newearth;

import java.util.ArrayList;
import java.util.List;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Tier 10 multi-seat rocket (6 seats).
 * Same hull as T9, black hole engine.
 * Exhaust uses ExtraPlanets' tier-10 launch flames.
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

    @Override
    protected void spawnParticles(boolean launched) {
        if (this.isDead) return;
        double sinPitch = Math.sin(Math.toRadians(this.rotationPitch));
        double x1 = 3.2 * Math.cos(Math.toRadians(this.rotationYaw)) * sinPitch;
        double z1 = 3.2 * Math.sin(Math.toRadians(this.rotationYaw)) * sinPitch;
        double y1 = 3.2 * Math.cos(Math.toRadians(this.rotationPitch - 180));
        if (this.launchPhase == micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase.EnumLaunchPhase.LANDING.ordinal() && this.targetVec != null) {
            double modifier = Math.max(this.posY - this.targetVec.getY(), 180.0);
            x1 *= modifier / 200.0D;
            y1 *= Math.min(modifier / 200.0D, 2.5D);
            z1 *= modifier / 200.0D;
        }
        double y2 = this.prevPosY + (this.posY - this.prevPosY) + y1 - 0.75 * this.motionY - 0.3 + 1.2D;
        double x2 = this.posX + x1 + this.motionX;
        double z2 = this.posZ + z1 + this.motionZ;
        Vector3 motionVec = new Vector3(x1 + this.motionX, y1 + this.motionY, z1 + this.motionZ);
        Vector3 d1 = new Vector3(y1 * 0.1D, -x1 * 0.1D, z1 * 0.1D).rotate(315 - this.rotationYaw, motionVec);
        Vector3 d2 = new Vector3(x1 * 0.1D, -z1 * 0.1D, y1 * 0.1D).rotate(315 - this.rotationYaw, motionVec);
        Vector3 d3 = new Vector3(-y1 * 0.1D, x1 * 0.1D, z1 * 0.1D).rotate(315 - this.rotationYaw, motionVec);
        Vector3 d4 = new Vector3(x1 * 0.1D, z1 * 0.1D, -y1 * 0.1D).rotate(315 - this.rotationYaw, motionVec);
        EntityLivingBase rider = this.getPassengers().isEmpty() || !(this.getPassengers().get(0) instanceof EntityLivingBase)
                ? null : (EntityLivingBase) this.getPassengers().get(0);
        Object[] riderArg = new Object[] { rider };
        makeFlame(x2 + d1.x, y2 + d1.y, z2 + d1.z, motionVec.clone().translate(d1), launched, riderArg);
        makeFlame(x2 + d2.x, y2 + d2.y, z2 + d2.z, motionVec.clone().translate(d2), launched, riderArg);
        makeFlame(x2 + d3.x, y2 + d3.y, z2 + d3.z, motionVec.clone().translate(d3), launched, riderArg);
        makeFlame(x2 + d4.x, y2 + d4.y, z2 + d4.z, motionVec.clone().translate(d4), launched, riderArg);
    }

    private void makeFlame(double x2, double y2, double z2, Vector3 motionVec, boolean launched, Object[] rider) {
        if (launched) {
            GalacticraftCore.proxy.spawnParticle("launchFlameLaunched", new Vector3(x2 + 0.4 - this.rand.nextDouble() / 10, y2, z2 + 0.4 - this.rand.nextDouble() / 10), motionVec, rider);
            GalacticraftCore.proxy.spawnParticle("launchFlameLaunched", new Vector3(x2 - 0.4 + this.rand.nextDouble() / 10, y2, z2 + 0.4 - this.rand.nextDouble() / 10), motionVec, rider);
            GalacticraftCore.proxy.spawnParticle("launchFlameLaunched", new Vector3(x2 - 0.4 + this.rand.nextDouble() / 10, y2, z2 - 0.4 + this.rand.nextDouble() / 10), motionVec, rider);
            GalacticraftCore.proxy.spawnParticle("launchFlameLaunched", new Vector3(x2 + 0.4 - this.rand.nextDouble() / 10, y2, z2 - 0.4 + this.rand.nextDouble() / 10), motionVec, rider);
            GalacticraftCore.proxy.spawnParticle("launchFlameLaunched", new Vector3(x2, y2, z2), motionVec, rider);
            GalacticraftCore.proxy.spawnParticle("launchFlameLaunched", new Vector3(x2 + 0.4, y2, z2), motionVec, rider);
            GalacticraftCore.proxy.spawnParticle("launchFlameLaunched", new Vector3(x2 - 0.4, y2, z2), motionVec, rider);
            GalacticraftCore.proxy.spawnParticle("launchFlameLaunched", new Vector3(x2, y2, z2 + 0.4D), motionVec, rider);
            GalacticraftCore.proxy.spawnParticle("launchFlameLaunched", new Vector3(x2, y2, z2 - 0.4D), motionVec, rider);
            return;
        }
        if (this.ticksExisted % 2 == 0) return;
        y2 += 1.2D;
        double x1 = motionVec.x, y1 = motionVec.y, z1 = motionVec.z;
        GalacticraftCore.proxy.spawnParticle("launchFlameIdle", new Vector3(x2 + 0.4 - this.rand.nextDouble() / 10, y2, z2 + 0.4 - this.rand.nextDouble() / 10), new Vector3(x1 + 0.5D, y1 - 0.3D, z1 + 0.5D), rider);
        GalacticraftCore.proxy.spawnParticle("launchFlameIdle", new Vector3(x2 - 0.4 + this.rand.nextDouble() / 10, y2, z2 + 0.4 - this.rand.nextDouble() / 10), new Vector3(x1 - 0.5D, y1 - 0.3D, z1 + 0.5D), rider);
        GalacticraftCore.proxy.spawnParticle("launchFlameIdle", new Vector3(x2 - 0.4 + this.rand.nextDouble() / 10, y2, z2 - 0.4 + this.rand.nextDouble() / 10), new Vector3(x1 - 0.5D, y1 - 0.3D, z1 - 0.5D), rider);
        GalacticraftCore.proxy.spawnParticle("launchFlameIdle", new Vector3(x2 + 0.4 - this.rand.nextDouble() / 10, y2, z2 - 0.4 + this.rand.nextDouble() / 10), new Vector3(x1 + 0.5D, y1 - 0.3D, z1 - 0.5D), rider);
        GalacticraftCore.proxy.spawnParticle("launchFlameIdle", new Vector3(x2 + 0.4, y2, z2), new Vector3(x1 + 0.8D, y1 - 0.3D, z1), rider);
        GalacticraftCore.proxy.spawnParticle("launchFlameIdle", new Vector3(x2 - 0.4, y2, z2), new Vector3(x1 - 0.8D, y1 - 0.3D, z1), rider);
        GalacticraftCore.proxy.spawnParticle("launchFlameIdle", new Vector3(x2, y2, z2 + 0.4D), new Vector3(x1, y1 - 0.3D, z1 + 0.8D), rider);
        GalacticraftCore.proxy.spawnParticle("launchFlameIdle", new Vector3(x2, y2, z2 - 0.4D), new Vector3(x1, y1 - 0.3D, z1 - 0.8D), rider);
    }
}
