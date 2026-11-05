package newearth.client;

import micdoodle8.mods.galacticraft.core.client.gui.container.GuiContainerGC;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import newearth.DisassemblyStationContainer;
import newearth.DisassemblyStationTile;

/** GS rocket assembly screen, with the rocket on the result slot and parts in the part slots. */
@SideOnly(Side.CLIENT)
public final class DisassemblyStationGui extends GuiContainerGC {
    private final DisassemblyStationTile tile;

    public DisassemblyStationGui(InventoryPlayer playerInventory, DisassemblyStationTile tile) {
        super(new DisassemblyStationContainer(playerInventory, tile));
        this.tile = tile;
        this.xSize = 204;
        this.ySize = 256;
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("tile.newearth.disassembly_station.name"), 19, 7, 0xFFFFFF);
        fontRenderer.drawString(I18n.format("container.inventory"), 32, ySize - 91, 0xFFFFFF);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(AssemblyStationGui.TEXTURE);
        GlStateManager.color(1F, 1F, 1F, 1F);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        for (Slot slot : inventorySlots.inventorySlots) {
            int x = guiLeft + slot.xPos, y = guiTop + slot.yPos;
            drawTexturedModalRect(x, y, 228, 206, 16, 16);
            if (slot.inventory instanceof InventoryPlayer) continue;
            if (slot.getSlotIndex() == 0) {
                drawTexturedModalRect(x - 9, y - 9, 215, 144, 34, 34);
            } else {
                GlStateManager.color(0F, 0.9F, 1F, 1F);
                drawTexturedModalRect(x - 2, y - 3, 207, 182, 20, 21);
                GlStateManager.color(1F, 1F, 1F, 1F);
            }
        }
    }
}
