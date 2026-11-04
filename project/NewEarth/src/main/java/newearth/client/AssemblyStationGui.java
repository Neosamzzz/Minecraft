package newearth.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;
import newearth.AssemblyStationContainer;
import newearth.AssemblyStationTile;
import newearth.NewEarth;

/**
 * GUI for Tier 7/9 Assembly Station.
 * 9 input slots in assembly pattern + 1 output slot.
 * No power/progress bars (manual assembly, instant on valid recipe).
 */
@SideOnly(Side.CLIENT)
public final class AssemblyStationGui extends GuiContainer {
    private static final ResourceLocation BG = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private final AssemblyStationTile tile;

    public AssemblyStationGui(InventoryPlayer playerInventory, AssemblyStationTile tile) {
        super(new AssemblyStationContainer(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 202;
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = I18n.format("tile.newearth.assembly_station.name");
        fontRenderer.drawString(name, xSize / 2 - fontRenderer.getStringWidth(name) / 2, 6, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        drawRect(x, y, x + xSize, y + ySize, 0xFFC6C6C6);
        for (net.minecraft.inventory.Slot slot : inventorySlots.inventorySlots) {
            drawRect(x + slot.xPos - 1, y + slot.yPos - 1, x + slot.xPos + 17, y + slot.yPos + 17, 0xFF373737);
            drawRect(x + slot.xPos, y + slot.yPos, x + slot.xPos + 16, y + slot.yPos + 16, 0xFF8B8B8B);
        }
    }
}
