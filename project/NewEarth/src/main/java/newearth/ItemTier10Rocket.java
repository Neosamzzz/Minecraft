package newearth;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Tier 10 rocket item (6-seat). Places EntityTier10Rocket.
 */
public class ItemTier10Rocket extends Item {
    public ItemTier10Rocket() {
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return EnumActionResult.PASS;
        BlockPos placePos = pos.up();
        EntityTier10Rocket rocket = new EntityTier10Rocket(world, placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5);
        world.spawnEntity(rocket);
        ItemStack stack = player.getHeldItem(hand);
        stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }
}
