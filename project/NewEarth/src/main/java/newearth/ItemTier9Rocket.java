package newearth;

import micdoodle8.mods.galacticraft.api.tile.IFuelDock;
import micdoodle8.mods.galacticraft.core.GCFluids;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

public class ItemTier9Rocket extends Item {
    public ItemTier9Rocket() {
        setMaxStackSize(1);
        setHasSubtypes(true);
        setCreativeTab(CreativeTabs.TRANSPORTATION);
    }
    protected EntityTier9Rocket createRocket(World world, double x, double y, double z) {
        return new EntityTier9Rocket(world, x, y, z);
    }
    public static boolean completePad(String id, int meta) {
        return "galaxyspace:advanced_landing_pad_full".equals(id)
                || ("galacticraftcore:landing_pad_full".equals(id) && meta == 0);
    }
    @Override public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
            EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return EnumActionResult.SUCCESS;
        ItemStack stack = player.getHeldItem(hand);
        if (!player.canPlayerEdit(pos, facing, stack)) return EnumActionResult.FAIL;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            BlockPos center = pos.add(x, 0, z);
            IBlockState state = world.getBlockState(center);
            if (!completePad(String.valueOf(state.getBlock().getRegistryName()), state.getBlock().getMetaFromState(state))) continue;
            TileEntity tile = world.getTileEntity(center);
            if (!(tile instanceof IFuelDock) || ((IFuelDock)tile).getDockedEntity() != null) continue;
            EntityTier9Rocket rocket = createRocket(world, center.getX() + .5D, center.getY() + .4D, center.getZ() + .5D);
            if (!rocket.isDockValid((IFuelDock)tile)) continue;
            rocket.setRocketType(stack.getMetadata());
            rocket.rotationYaw = player.rotationYaw + 45F;
            if (rocket.getType().getPreFueled()) rocket.fuelTank.fill(new FluidStack(GCFluids.fluidFuel, rocket.getMaxFuel()), true);
            else if (stack.hasTagCompound()) rocket.fuelTank.fill(new FluidStack(GCFluids.fluidFuel,
                    Math.max(0, stack.getTagCompound().getInteger("RocketFuel"))), true);
            if (!world.spawnEntity(rocket)) return EnumActionResult.FAIL;
            rocket.setPad((IFuelDock)tile);
            ((IFuelDock)tile).dockEntity(rocket);
            if (!player.capabilities.isCreativeMode) stack.shrink(1);
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.FAIL;
    }
}
