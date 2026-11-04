package newearth;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * Tier 9 rocket item (6-seat). Places EntityTier9Rocket on launch pad.
 */
public class ItemTier9Rocket extends Item {
    public ItemTier9Rocket() {
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TRANSPORTATION);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return EnumActionResult.PASS;

        // Find launch pad in 3x3 area (GS Advanced Landing Pad or GC Landing Pad)
        boolean padFound = false;
        float centerX = 0, centerY = 0, centerZ = 0;
        for (int i = -1; i <= 1 && !padFound; i++) {
            for (int j = -1; j <= 1 && !padFound; j++) {
                BlockPos p = pos.add(i, 0, j);
                IBlockState state = world.getBlockState(p);
                Block block = state.getBlock();
                String name = String.valueOf(block.getRegistryName());
                if (name.equals("galaxyspace:advanced_landing_pad") || name.equals("galacticraftcore:landing_pad")) {
                    padFound = true;
                    centerX = p.getX() + 0.5F;
                    centerY = p.getY() + 0.4F;
                    centerZ = p.getZ() + 0.5F;
                }
            }
        }

        if (!padFound) {
            player.sendStatusMessage(new TextComponentString("需要在发射台上放置火箭"), true);
            return EnumActionResult.FAIL;
        }

        EntityTier9Rocket rocket = new EntityTier9Rocket(world, centerX, centerY, centerZ);
        world.spawnEntity(rocket);
        ItemStack stack = player.getHeldItem(hand);
        stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }
}
