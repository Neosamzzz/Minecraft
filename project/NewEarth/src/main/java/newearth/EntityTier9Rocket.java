package newearth;

import java.util.List;
import galaxyspace.core.prefab.entities.EntityMultiSeatRocket;
import micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/** Shared six-seat hull, inventory and recovery for the two propulsion tiers. */
public class EntityTier9Rocket extends EntityMultiSeatRocket {
    public EntityTier9Rocket(World world) {
        super(world);
        setSize(2F, 10F);
        rocketType = EnumRocketType.DEFAULT;
        stacks = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
    }
    public EntityTier9Rocket(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }
    public void setRocketType(int meta) {
        rocketType = EnumRocketType.values()[Math.max(0, Math.min(meta, EnumRocketType.values().length - 1))];
        stacks = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
    }
    @Override public boolean isDockValid(micdoodle8.mods.galacticraft.api.tile.IFuelDock dock) {
        return super.isDockValid(dock) || dock instanceof galaxyspace.systems.SolarSystem.planets.overworld.tile.TileEntityAdvLandingPad;
    }
    @Override public int getMaxSeats() { return 6; }
    @Override public int getRocketTier() { return 9; }
    @Override public double getYOffset() { return 1.5D; }
    @Override public float getRotateOffset() { return 4.0F; }
    @Override public double getOnPadYOffset() { return 0.8D; }
    @Override public float getRenderOffsetY() { return 1.1F; }
    @Override public float getCameraZoom() { return 15F; }
    @Override public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (launchPhase >= micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase.EnumLaunchPhase.LAUNCHED.ordinal()) return false;
        if (!world.isRemote && isPassenger(player)) {
            // Same height trick as the GS tier-6 rocket, so the rider is not dropped from the nose.
            float heightBefore = height;
            height = height / 2.0F;
            player.dismountRidingEntity();
            height = heightBefore;
            return true;
        }
        return super.processInitialInteract(player, hand);
    }
    @Override public boolean defaultThirdPerson() { return true; }
    @Override public int getFuelTankCapacity() { return 1500; }
    @Override public int getPreLaunchWait() { return 400; }
    protected String itemId() { return "tier9_rocket"; }
    @Override public ItemStack getPickedResult(RayTraceResult target) { return recoveredItem(); }
    public ItemStack recoveredItem() {
        ItemStack item = Manufacturing.own(itemId());
        item.setItemDamage(rocketType.getIndex());
        item.setTagCompound(new NBTTagCompound());
        item.getTagCompound().setInteger("RocketFuel", fuelTank.getFluidAmount());
        return item;
    }
    @Override public List<ItemStack> getItemsDropped(List<ItemStack> items) {
        super.getItemsDropped(items);
        items.add(recoveredItem());
        return items;
    }
    @Override public void onUpdate() {
        // The upstream single-rider remount loop removes every other seat.
        if (getWaitForPlayer() && !getPassengers().isEmpty()) setWaitForPlayer(false);
        super.onUpdate();
    }
    @Override public void onReachAtmosphere() {
        if (destinationFrequency == -1 || world.isRemote) { super.onReachAtmosphere(); return; }
        setTarget(true, destinationFrequency);
        if (targetVec == null || targetDimension == world.provider.getDimension()) { super.onReachAtmosphere(); return; }
        net.minecraft.world.WorldProvider provider = micdoodle8.mods.galacticraft.core.util.WorldUtil.getProviderForDimensionServer(targetDimension);
        if (provider == null) return;
        if (provider instanceof micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider
                && !((micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider)provider).canSpaceshipTierPass(getRocketTier())) return;
        if (!PlanetHooks.allowTransfer(this, targetDimension, this)) return;
        net.minecraft.world.WorldServer destination = world.getMinecraftServer().getWorld(targetDimension);
        if (destination == null) return;
        java.util.List<Entity> crew = new java.util.ArrayList<>(getPassengers());
        EntityPlayerMP pilot = getDriver() instanceof EntityPlayerMP ? (EntityPlayerMP)getDriver() : null;
        if (pilot == null) {
            micdoodle8.mods.galacticraft.core.util.WorldUtil.transferEntityToDimension(this, targetDimension, destination, false, null);
            return;
        }
        for (Entity member : crew) if (member instanceof EntityPlayerMP) onTeleport((EntityPlayerMP)member);
        Entity moved = micdoodle8.mods.galacticraft.core.util.WorldUtil.transferEntityToDimension(pilot, targetDimension, destination, false, this);
        if (moved == null || !(pilot.getRidingEntity() instanceof EntityTier9Rocket)) return;
        EntityTier9Rocket ship = (EntityTier9Rocket)pilot.getRidingEntity();
        for (Entity member : crew) if (member instanceof EntityPlayerMP && member != pilot) {
            Entity transferred = micdoodle8.mods.galacticraft.core.util.WorldUtil.transferEntityToDimension(member, targetDimension, destination, false, null);
            if (transferred != null) member.startRiding(ship, true);
        }
        // Driver is the last occupied seat in the shared GS interface.
        pilot.dismountRidingEntity();
        pilot.startRiding(ship, true);
        ship.setWaitForPlayer(false);
    }
    @Override public void onTeleport(EntityPlayerMP player) {
        super.onTeleport(player);
        GCPlayerStats stats = GCPlayerStats.get(player);
        player.getEntityData().setString("newearthFlightRocket", "newearth:" + itemId());
        stats.setSpaceshipTier(getRocketTier());
        stats.setRocketItem(Manufacturing.ITEMS.get(itemId()));
        if (player == getDriver()) {
            NonNullList<ItemStack> recovery = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
            for (int i = 0; i < stacks.size(); i++) recovery.set(i, stacks.get(i).copy());
            recovery.set(recovery.size() - 1, recoveredItem());
            stats.setRocketStacks(recovery);
        } else {
            // Only the pilot carries the hull and cargo into the recovery chest.
            stats.setRocketStacks(NonNullList.create());
        }
    }

    /** EP tier 9/10 four nozzles. The rider excluded from the flame is the GS pilot. */
    @Override protected void spawnParticles(boolean launched) {
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
        Object[] rider = new Object[] { getDriver() };
        makeFlame(x2 + d1.x, y2 + d1.y, z2 + d1.z, motionVec.clone().translate(d1), launched, rider);
        makeFlame(x2 + d2.x, y2 + d2.y, z2 + d2.z, motionVec.clone().translate(d2), launched, rider);
        makeFlame(x2 + d3.x, y2 + d3.y, z2 + d3.z, motionVec.clone().translate(d3), launched, rider);
        makeFlame(x2 + d4.x, y2 + d4.y, z2 + d4.z, motionVec.clone().translate(d4), launched, rider);
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
