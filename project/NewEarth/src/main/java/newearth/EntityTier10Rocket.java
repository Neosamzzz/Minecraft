package newearth;

import net.minecraft.world.World;

/**
 * Tier 10 multi-seat rocket (6 seats).
 * Same hull as T9, black hole engine.
 * Custom exhaust flame positions for the T10_new model.
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
                // 大火焰：更多粒子，更快下喷
                for (int i = 0; i < 8; i++) {
                    double ox = (rand.nextDouble() - 0.5) * 1.0;
                    double oz = (rand.nextDouble() - 0.5) * 1.0;
                    double oy = rand.nextDouble() * 0.3;
                    // 蓝紫色火星
                    world.spawnParticle(net.minecraft.util.EnumParticleTypes.REDSTONE,
                        x + ox, y + oy, z + oz, r, g, b);
                    // 高速下喷的烟（速度-2.0）
                    world.spawnParticle(net.minecraft.util.EnumParticleTypes.SMOKE_LARGE,
                        x + ox * 0.5, y + oy, z + oz * 0.5, 0, -2.0, 0);
                    // 火焰粒子（有速度）
                    if (rand.nextFloat() < 0.5) {
                        world.spawnParticle(net.minecraft.util.EnumParticleTypes.FLAME,
                            x + ox, y + oy, z + oz, 0, -1.5, 0);
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
