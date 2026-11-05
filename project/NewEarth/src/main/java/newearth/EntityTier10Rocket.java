package newearth;

import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

/**
 * Tier 10 multi-seat rocket (6 seats).
 * Same hull as T9, black hole engine.
 * Exhaust uses ExtraPlanets' tier-10 launch flames.
 */
public class EntityTier10Rocket extends EntityTier9Rocket {
    public EntityTier10Rocket(World world) { super(world); }
    public EntityTier10Rocket(World world, double x, double y, double z) { super(world, x, y, z); }
    @Override public int getRocketTier() { return 10; }
    @Override protected String itemId() { return "tier10_rocket"; }

    @Override public void onReachAtmosphere() {
        if (newearth.dimension.EndDimension.isDestination(world)) { super.onReachAtmosphere(); return; }
        if (world.isRemote) { stopRocketSound(); return; }
        int target = newearth.dimension.EndDimension.destinationId();
        if (target == 0 || !net.minecraftforge.common.DimensionManager.isDimensionRegistered(target)) return;
        net.minecraft.world.WorldServer destination = world.getMinecraftServer().getWorld(target);
        if (destination == null) return;
        java.util.List<net.minecraft.entity.player.EntityPlayerMP> crew = new java.util.ArrayList<>();
        for (net.minecraft.entity.Entity passenger : getPassengers())
            if (passenger instanceof net.minecraft.entity.player.EntityPlayerMP)
                crew.add((net.minecraft.entity.player.EntityPlayerMP)passenger);
        if (crew.isEmpty()) return;
        newearth.dimension.ArrivalSession.prepareCrew(destination, crew);
        for (net.minecraft.entity.player.EntityPlayerMP player : crew) onTeleport(player);
        for (net.minecraft.entity.player.EntityPlayerMP player : crew) {
            player.dismountRidingEntity();
            micdoodle8.mods.galacticraft.core.util.WorldUtil.transferEntityToDimension(player, target, destination, true, null);
        }
        setDead();
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
