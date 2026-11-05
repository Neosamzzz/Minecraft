package newearth.client;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;
import newearth.ManufacturingContainer;
import newearth.ManufacturingTile;
import newearth.NewEarth;

/** Hand-drawn screen for the engine station. The material station uses MaterialStationGui. */
@SideOnly(Side.CLIENT)
public final class ManufacturingGui extends GuiContainer {
    private static final ResourceLocation BG_ENGINE = new ResourceLocation(NewEarth.MOD_ID, "textures/gui/engine_station.png");
    private static final ResourceLocation ATLAS = new ResourceLocation(NewEarth.MOD_ID, "textures/gui/gs_machine.png");
    private static final int ARROW_X = 86, ARROW_Y = 38, ARROW_W = 35;
    private final ManufacturingTile tile;

    public ManufacturingGui(InventoryPlayer playerInventory, ManufacturingTile tile) {
        super(new ManufacturingContainer(playerInventory, tile));
        this.tile = tile;
        this.xSize = 176;
        this.ySize = 212;
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        mc.getTextureManager().bindTexture(BG_ENGINE);
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
        mc.getTextureManager().bindTexture(ATLAS);
        int progress = tile.getField(1);
        if (progress > 0) {
            int scale = progress * (ARROW_W + 8) / ManufacturingTile.PROCESS_TIME;
            if (scale > 0)
                drawScaledCustomSizeModalRect(x + ARROW_X, y + ARROW_Y, 181, 125, 6, 15, scale, 15, 256, 256);
            if (scale > ARROW_W - 8)
                drawScaledCustomSizeModalRect(x + ARROW_X + ARROW_W, y + ARROW_Y, 209, 125, 8, 15, scale - ARROW_W, 15, 256, 256);
        }
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = tile.getDisplayName().getUnformattedText();
        fontRenderer.drawString(title, (xSize - fontRenderer.getStringWidth(title)) / 2, 6, 0x9fe8ff);
    }
}
