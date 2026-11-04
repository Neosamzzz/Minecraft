package newearth.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;
import newearth.DisassemblyStationContainer;
import newearth.DisassemblyStationTile;
import newearth.NewEarth;

/**
 * GUI for Disassembly Station.
 * 1 input (rocket) + 9 outputs (parts).
 */
@SideOnly(Side.CLIENT)
public final class DisassemblyStationGui extends GuiContainer {
    private static final ResourceLocation BG = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private final DisassemblyStationTile tile;

    public DisassemblyStationGui(InventoryPlayer playerInventory, DisassemblyStationTile tile) {
        super(new DisassemblyStationContainer(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 166;
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = I18n.format("newearth.disassembly_station");
        this.fontRenderer.drawString(name, 8, 6, 0x404040);
        this.fontRenderer.drawString(I18n.format("container.inventory"), 8, 72, 0x404040);
        this.fontRenderer.drawString("→", 60, 39, 0x555555);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        this.mc.getTextureManager().bindTexture(BG);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
    }
}
