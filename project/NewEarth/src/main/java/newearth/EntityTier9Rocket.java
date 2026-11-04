package newearth;

import galaxyspace.core.prefab.entities.EntityMultiSeatRocket;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityTieredRocket.EnumRocketType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * Tier 9 multi-seat rocket (6 seats).
 * Uses GS multi-seat rocket base.
 */
public class EntityTier9Rocket extends EntityMultiSeatRocket {
    public EntityTier9Rocket(World world) {
        super(world);
        this.setSize(2.0F, 10.0F);
    }

    public EntityTier9Rocket(World world, double x, double y, double z) {
        super(world, x, y, z);
        this.rocketType = EnumRocketType.INVENTORY35; // placeholder, tier set below
        this.stacks = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
    }

    @Override public int getMaxSeats() { return 6; }

    @Override public int getRocketTier() { return 9; }

    @Override public double getYOffset() { return 1.5F; }

    @Override public float getRotateOffset() { return 1.5F; }

    @Override public double getOnPadYOffset() { return 0.0D; }

    @Override public ItemStack getPickedResult(RayTraceResult target) {
        return Manufacturing.own("tier9_rocket");
    }
}
