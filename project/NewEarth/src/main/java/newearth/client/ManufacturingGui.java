package newearth.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;
import newearth.ManufacturingContainer;
import newearth.ManufacturingTile;
import newearth.NewEarth;

/** Background is pre-composited from GS's modern_gui.png sprite atlas
 *  (see textures/gui/gs_machine.png); dynamic bars reuse the same atlas. */
@SideOnly(Side.CLIENT)
public final class ManufacturingGui extends GuiContainer {
    private static final ResourceLocation BG = new ResourceLocation(NewEarth.MOD_ID, "textures/gui/manufacturing.png");
    private static final ResourceLocation BG_ENGINE = new ResourceLocation(NewEarth.MOD_ID, "textures/gui/engine_station.png");
    private static final ResourceLocation ATLAS = new ResourceLocation(NewEarth.MOD_ID, "textures/gui/gs_machine.png");
    private static final int ARROW_X = 86, ARROW_Y = 38, ARROW_W = 35;
    private static final int ENERGY_X = 5, ENERGY_Y = 85, ENERGY_FILL_W = 52;
    private final ManufacturingTile tile;

    public ManufacturingGui(InventoryPlayer playerInventory, ManufacturingTile tile) {
        super(new ManufacturingContainer(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 212;
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        // 引擎台用用户手绘界面，材料台用原版
        boolean isEngine = false;
        try {
            String blockId = String.valueOf(tile.getWorld().getBlockState(tile.getPos()).getBlock().getRegistryName());
            isEngine = blockId.equals("newearth:engine_station");
        } catch (Exception e) { }
        mc.getTextureManager().bindTexture(isEngine ? BG_ENGINE : BG);
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
        mc.getTextureManager().bindTexture(ATLAS);
        boolean engine = tile.engine();
        // progress arrow fill, same math as GS renderProgressArray
        int progress = tile.getField(1);
        if (progress > 0) {
            int scale = progress * (ARROW_W + 8) / ManufacturingTile.PROCESS_TIME;
            if (scale > 0)
                drawScaledCustomSizeModalRect(x + ARROW_X, y + ARROW_Y, 181, 125, 6, 15, scale, 15, 256, 256);
            if (scale > ARROW_W - 8)
                drawScaledCustomSizeModalRect(x + ARROW_X + ARROW_W, y + ARROW_Y, 209, 125, 8, 15, scale - ARROW_W, 15, 256, 256);
        }
        // energy fill; the engine station has no power interface
        int energy = engine ? 0 : tile.getField(0);
        if (energy > 0) {
            int w = energy * ENERGY_FILL_W / ManufacturingTile.ENERGY_CAPACITY;
            if (w > 0)
                drawScaledCustomSizeModalRect(x + ENERGY_X + 9, y + ENERGY_Y + 1, 180, 7, 11, 10, w, 7, 256, 256);
        }
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = tile.getDisplayName().getUnformattedText();
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0x9fe8ff);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, 117, 0x9fe8ff);
        if (tile.engine()) return;
        int status = tile.getField(2);
        String key;
        int color;
        switch (status) {
            case ManufacturingTile.STATUS_WORKING: key = "newearth.gui.status.working"; color = 0x5aff8a; break;
            case ManufacturingTile.STATUS_NO_POWER: key = "newearth.gui.status.nopower"; color = 0xff5a5a; break;
            default: key = "newearth.gui.status.idle"; color = 0xffb347; break;
        }
        String text = I18n.format("newearth.gui.status.label") + I18n.format(key);
        fontRenderer.drawString(text, 135 - fontRenderer.getStringWidth(text) / 2, 88, color);
    }
}
