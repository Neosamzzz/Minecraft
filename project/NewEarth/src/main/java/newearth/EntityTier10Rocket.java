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
     * Black hole theme: blue/purple flames instead of orange.
     */
    @Override
    protected void spawnParticles(boolean launched) {
        if (this.isDead) return;
        // T10_new nozzle offsets (relative to rocket center, in blocks)
        double[][] nozzles = {
            {0.0, 0.0},    // 主喷口
            {1.2, 0.0},    // 右侧引擎
            {-1.2, 0.0},   // 左侧引擎
            {0.0, 1.2},    // 前侧引擎
            {0.0, -1.2},   // 后侧引擎
        };
        double y = this.posY - 0.5;
        // 黑洞主题：蓝紫色火焰 (R:0.3, G:0.5, B:1.0)
        float r = 0.3F, g = 0.5F, b = 1.0F;
        for (double[] n : nozzles) {
            double x = this.posX + n[0];
            double z = this.posZ + n[1];
            if (launched) {
                // 大火焰：多个蓝紫色粒子
                for (int i = 0; i < 5; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 0.8;
                    double oz = (rand.nextDouble() - 0.5) * 0.8;
                    double oy = rand.nextDouble() * 0.5;
                    world.spawnParticle(net.minecraft.util.EnumParticleTypes.REDSTONE,
                        x + ox, y + oy, z + oz, r, g, b);
                    // 加一点烟
                    if (rand.nextFloat() < 0.3) {
                        world.spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_LARGE,
                            x + ox, y + oy, z + oz, 0, -0.5, 0);
                    }
                }
            } else if (this.ticksExisted % 4 == 0) {
                // 待命：偶尔冒个小火星
                world.spawnParticle(net.minecraft.util.EnumParticleTypes.REDSTONE,
                    x, y, z, r, g, b);
            }
        }
    }
}
