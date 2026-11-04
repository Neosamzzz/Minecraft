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
 * Disassembly Station: breaks rockets back into component parts.
 * 1 input (rocket) + 9 outputs (parts).
 */
public class DisassemblyStationBlock extends Block implements ITileEntityProvider {
    public static final int GUI_ID = 3;

    public DisassemblyStationBlock() {
        super(Material.IRON);
        setRegistryName(NewEarth.MOD_ID, "disassembly_station");
        setTranslationKey("newearth.disassembly_station");
        setCreativeTab(CreativeTabs.MISC);
        setHardness(3.0F);
        setResistance(10.0F);
    }

    @Override public TileEntity createNewTileEntity(World world, int meta) {
        return new DisassemblyStationTile();
    }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(NewEarth.instance, GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
