package newearth;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.*;

public final class ManufacturingGuiHandler implements IGuiHandler {
    public static final int MANUFACTURING_GUI_ID = 1;
    public static final int ASSEMBLY_GUI_ID = 2;
    public static final int DISASSEMBLY_GUI_ID = 3;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == MANUFACTURING_GUI_ID && te instanceof ManufacturingTile) {
            return new ManufacturingContainer(player.inventory, (ManufacturingTile) te);
        }
        if (id == ASSEMBLY_GUI_ID && te instanceof AssemblyStationTile) {
            return new AssemblyStationContainer(player.inventory, (AssemblyStationTile) te);
        }
        if (id == DISASSEMBLY_GUI_ID && te instanceof DisassemblyStationTile) {
            return new DisassemblyStationContainer(player.inventory, (DisassemblyStationTile) te);
        }
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (id == MANUFACTURING_GUI_ID && te instanceof ManufacturingTile) {
            ManufacturingTile station = (ManufacturingTile) te;
            return station.engine()
                    ? new newearth.client.ManufacturingGui(player.inventory, station)
                    : new newearth.client.MaterialStationGui(player.inventory, station);
        }
        if (id == ASSEMBLY_GUI_ID && te instanceof AssemblyStationTile) {
            return new newearth.client.AssemblyStationGui(player.inventory, (AssemblyStationTile) te);
        }
        if (id == DISASSEMBLY_GUI_ID && te instanceof DisassemblyStationTile) {
            return new newearth.client.DisassemblyStationGui(player.inventory, (DisassemblyStationTile) te);
        }
        return null;
    }
}
