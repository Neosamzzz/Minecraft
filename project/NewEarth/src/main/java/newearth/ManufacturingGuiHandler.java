package newearth;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.*;

public final class ManufacturingGuiHandler implements IGuiHandler {
    public static final int GUI_ID = 1;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != GUI_ID) return null;
        return world.getTileEntity(new BlockPos(x, y, z)) instanceof ManufacturingTile
                ? new ManufacturingContainer(player.inventory, (ManufacturingTile) world.getTileEntity(new BlockPos(x, y, z)))
                : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id != GUI_ID) return null;
        return world.getTileEntity(new BlockPos(x, y, z)) instanceof ManufacturingTile
                ? new newearth.client.ManufacturingGui(player.inventory, (ManufacturingTile) world.getTileEntity(new BlockPos(x, y, z)))
                : null;
    }
}
