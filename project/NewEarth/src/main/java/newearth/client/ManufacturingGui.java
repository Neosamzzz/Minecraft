package newearth.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;
import newearth.ManufacturingContainer;
import newearth.ManufacturingTile;
import newearth.NewEarth;

/** Imitates the reference component-assembler GUI: 3x3 input, arrow, output,
 *  energy bar + battery slot, status line, player inventory. No right-side slots. */
@SideOnly(Side.CLIENT)
public final class ManufacturingGui extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(NewEarth.MOD_ID, "textures/gui/manufacturing.png");
    private final ManufacturingTile tile;

    public ManufacturingGui(InventoryPlayer playerInventory, ManufacturingTile tile) {
        super(new ManufacturingContainer(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 202;
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(TEXTURE);
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
        // energy fill inside the bar frame (19,83)-(100,88)
        int energy = tile.getField(0);
        int w = energy * 82 / ManufacturingTile.ENERGY_CAPACITY;
        if (w > 0) {
            int color = 0xFF2bd98a;
            drawRect(x + 19, y + 83, x + 19 + w, y + 88, color);
        }
        // progress arrow overlay: fill the arrow proportionally
        int progress = tile.getField(1);
        if (progress > 0) {
            int aw = progress * 20 / ManufacturingTile.PROCESS_TIME;
            drawRect(x + 80, y + 41, x + 80 + aw, y + 44, 0xFFffd24a);
        }
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = tile.getDisplayName().getUnformattedText();
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0x9fe8ff);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 114, 0x9fe8ff);
        int status = tile.getField(2);
        String key, text;
        int color;
        switch (status) {
            case ManufacturingTile.STATUS_WORKING: key = "newearth.gui.status.working"; color = 0x5aff8a; break;
            case ManufacturingTile.STATUS_NO_POWER: key = "newearth.gui.status.nopower"; color = 0xff5a5a; break;
            default: key = "newearth.gui.status.idle"; color = 0xffb347; break;
        }
        text = I18n.format("newearth.gui.status.label") + I18n.format(key);
        fontRenderer.drawString(text, 18, 102, color);
    }
}
