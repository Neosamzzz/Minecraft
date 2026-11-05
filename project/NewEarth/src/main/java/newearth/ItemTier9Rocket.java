package newearth;

import java.util.List;
import micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType;
import micdoodle8.mods.galacticraft.api.item.IHoldableItem;
import micdoodle8.mods.galacticraft.api.tile.IFuelDock;
import micdoodle8.mods.galacticraft.core.GCFluids;
import micdoodle8.mods.galacticraft.core.util.EnumColor;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemTier9Rocket extends Item implements IHoldableItem {
    public ItemTier9Rocket() {
        setMaxStackSize(1);
        setHasSubtypes(true);
        setCreativeTab(CreativeTabs.TRANSPORTATION);
    }
    protected EntityTier9Rocket createRocket(World world, double x, double y, double z) {
        return new EntityTier9Rocket(world, x, y, z);
    }
    /** Same pad seat as EP and GC: the 0.4 block base, then the rocket's own pad offset. */
    static double placedY(EntityTier9Rocket rocket, double padY) {
        return padY + 0.4D + rocket.getOnPadYOffset();
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
            rocket.setPosition(rocket.posX, placedY(rocket, center.getY()), rocket.posZ);
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

    @Override public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (tab != getCreativeTab() && tab != CreativeTabs.SEARCH) return;
        for (int i = 0; i < EnumRocketType.values().length; i++) items.add(new ItemStack(this, 1, i));
    }

    @Override @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        EnumRocketType[] types = EnumRocketType.values();
        EnumRocketType type = types[Math.max(0, Math.min(stack.getMetadata(), types.length - 1))];
        if (!type.getTooltip().isEmpty()) tooltip.add(type.getTooltip());
        if (type.getPreFueled()) tooltip.add(EnumColor.RED + "\u00a7o" + GCCoreUtil.translate("gui.creative_only.desc"));
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("RocketFuel"))
            tooltip.add(GCCoreUtil.translate("gui.message.fuel.name") + ": " + stack.getTagCompound().getInteger("RocketFuel") + " / " + 1500);
    }

    @Override public boolean shouldHoldLeftHandUp(EntityPlayer player) { return true; }
    @Override public boolean shouldHoldRightHandUp(EntityPlayer player) { return true; }
    @Override public boolean shouldCrouch(EntityPlayer player) { return true; }
}
