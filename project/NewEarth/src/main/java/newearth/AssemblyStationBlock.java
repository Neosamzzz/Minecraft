package newearth;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Tier 7/9 Rocket Assembly Station block.
 * Uses GC NASA Workbench model (tier7_workbench.obj).
 * Opens GUI on right-click.
 */
public class AssemblyStationBlock extends Block implements ITileEntityProvider {
    public static final int GUI_ID = 2;

    public AssemblyStationBlock() {
        super(Material.IRON);
        setRegistryName(NewEarth.MOD_ID, "assembly_station");
        setTranslationKey("newearth.assembly_station");
        setCreativeTab(CreativeTabs.MISC);
        setHardness(3.0F);
        setResistance(10.0F);
    }

    @Override public TileEntity createNewTileEntity(World world, int meta) {
        return new AssemblyStationTile();
    }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(NewEarth.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof net.minecraft.inventory.IInventory)
            net.minecraft.inventory.InventoryHelper.dropInventoryItems(world, pos, (net.minecraft.inventory.IInventory)tile);
        super.breakBlock(world, pos, state);
    }
}
