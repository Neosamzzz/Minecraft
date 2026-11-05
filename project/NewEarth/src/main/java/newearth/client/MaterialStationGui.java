package newearth.client;

import java.util.Arrays;
import galaxyspace.core.client.gui.tile.GuiTileBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import newearth.ManufacturingContainer;
import newearth.ManufacturingTile;

/** GS component-assembler screen for the material station. No upgrade column. */
@SideOnly(Side.CLIENT)
public final class MaterialStationGui extends GuiTileBase {
    private final ManufacturingTile tile;

    public MaterialStationGui(InventoryPlayer playerInventory, ManufacturingTile tile) {
        super(new ManufacturingContainer(playerInventory, tile), 2, 1);
        this.tile = tile;
        this.ySize = 199;
    }

    @Override protected boolean isModuleSupport() { return false; }

    @Override protected String getName() { return tile.getDisplayName().getUnformattedText(); }

    @Override protected Slot getBatterySlot() {
        return inventorySlots.getSlotFromInventory(tile, ManufacturingTile.BATTERY_SLOT);
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        int status = tile.getField(2);
        String key;
        int color;
        switch (status) {
            case ManufacturingTile.STATUS_WORKING: key = "newearth.gui.status.working"; color = 0x5aff8a; break;
            case ManufacturingTile.STATUS_NO_POWER: key = "newearth.gui.status.nopower"; color = 0xff5a5a; break;
            default: key = "newearth.gui.status.idle"; color = 0xffb347; break;
        }
        String text = I18n.format("newearth.gui.status.label") + I18n.format(key);
        fontRenderer.drawString(text, 135 - fontRenderer.getStringWidth(text) / 2, 85, color);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        int progress = tile.getField(1);
        renderProgressArray(x + 86, y + 38, 35, progress, ManufacturingTile.PROCESS_TIME);
        mc.getTextureManager().bindTexture(getTexture());
        GlStateManager.color(1F, 1F, 1F, 1F);
        int stored = tile.getField(0);
        drawTexturedModalRect(x + 5, y + 85, 180, 56, 9, 8);
        drawTexturedModalRect(x + 16, y + 85, 180, 47, 56, 9);
        if (stored > 0) {
            int scale = stored * 54 / ManufacturingTile.ENERGY_CAPACITY;
            drawTexturedModalRect(x + 4, y + 85, 180, 7, 11, 10);
            if (scale > 0) drawTexturedModalRect(x + 17, y + 86, 180, 0, scale, 7);
        }
        drawTexturedModalRect(x + 94, y + 29, 200, 19, 17, 13);
        drawTexturedModalRect(x + 98, y + 50, 197, 40, 16, 6);
        if (progress > ManufacturingTile.PROCESS_TIME / 2) {
            drawTexturedModalRect(x + 94, y + 29, 182, 19, 17, 13);
            drawTexturedModalRect(x + 98, y + 50, 180, 40, 16, 6);
        }
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int x = (width - xSize) / 2 + 5, y = (height - ySize) / 2 + 85;
        if (mouseX >= x && mouseX < x + 68 && mouseY >= y && mouseY < y + 10) {
            drawHoveringText(Arrays.asList(tile.getField(0) + " / " + ManufacturingTile.ENERGY_CAPACITY + " FE"), mouseX, mouseY);
        }
    }
}
