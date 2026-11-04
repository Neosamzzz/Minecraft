package newearth;

import galaxyspace.core.prefab.entities.EntityMultiSeatRocket;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityTieredRocket.EnumRocketType;
import micdoodle8.mods.galacticraft.api.vector.Vector3;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * Tier 10 multi-seat rocket (6 seats).
 * Same hull as T9, black hole engine.
 * Custom exhaust flame positions for the T10_new model.
 */
public class EntityTier10Rocket extends EntityMultiSeatRocket {
    public EntityTier10Rocket(World world) {
        super(world);
        this.setSize(2.0F, 10.0F);
    }

    public EntityTier10Rocket(World world, double x, double y, double z) {
        super(world, x, y, z);
        this.rocketType = EnumRocketType.INVENTORY35;
        this.stacks = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
    }

    @Override public int getMaxSeats() { return 6; }

    @Override public int getRocketTier() { return 10; }

    @Override public double getYOffset() { return 1.5F; }

    @Override public float getRotateOffset() { return 1.5F; }

    @Override public double getOnPadYOffset() { return 0.0D; }

    @Override public ItemStack getPickedResult(RayTraceResult target) {
        return Manufacturing.own("tier10_rocket");
    }

    /**
     * Custom flame positions for T10_new model.
     * Main nozzle at center + 4 side engines.
     * Based on EP electric T10 effect, adapted for new model.
     */
    @Override
    protected void spawnParticles(boolean launched) {
        if (this.isDead) return;
        // T10_new nozzle offsets (relative to rocket center, in blocks)
        // Main center nozzle + 4 side engines
        double[][] nozzles = {
            {0.0, 0.0},    // 主喷口
            {1.2, 0.0},    // 右侧引擎
            {-1.2, 0.0},   // 左侧引擎
            {0.0, 1.2},    // 前侧引擎
            {0.0, -1.2},   // 后侧引擎
        };
        double y = this.posY - 0.5; // 火焰在火箭下方
        EntityLivingBase driver = getDriver();
        Object[] rider = new Object[] { driver };
        Vector3 motionVec = new Vector3(0, -2.0, 0); // 向下喷
        for (double[] n : nozzles) {
            double x = this.posX + n[0];
            double z = this.posZ + n[1];
            if (launched) {
                // 发射时：大火焰
                for (int i = 0; i < 3; i++) {
                    GalacticraftCore.proxy.spawnParticle("launchFlameLaunched",
                        new Vector3(x + (rand.nextDouble() - 0.5) * 0.6, y, z + (rand.nextDouble() - 0.5) * 0.6),
                        motionVec, rider);
                }
            } else if (this.ticksExisted % 2 == 0) {
                // 待发射时：小火焰
                GalacticraftCore.proxy.spawnParticle("launchFlameIdle",
                    new Vector3(x, y, z), motionVec, rider);
            }
        }
    }
}
