package newearth.client;

import micdoodle8.mods.galacticraft.core.client.gui.container.GuiContainerGC;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.*;
import newearth.AssemblyStationContainer;
import newearth.AssemblyStationTile;

/** GS rocket assembly texture, slot frames and processing bar, without power. */
@SideOnly(Side.CLIENT)
public final class AssemblyStationGui extends GuiContainerGC {
    public static final ResourceLocation TEXTURE = new ResourceLocation("galaxyspace", "textures/gui/rocket_assembly.png");
    private final AssemblyStationTile tile;

    public AssemblyStationGui(InventoryPlayer playerInventory, AssemblyStationTile tile) {
        super(new AssemblyStationContainer(playerInventory, tile));
        this.tile = tile;
        this.xSize = 204;
        this.ySize = 256;
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("tile.newearth.assembly_station.name"), 19, 7, 0xFFFFFF);
        fontRenderer.drawString(I18n.format("container.inventory"), 32, ySize - 91, 0xFFFFFF);
        String status = I18n.format(tile.getField(0) > 0 ? "newearth.gui.status.working" : "newearth.gui.status.idle");
        fontRenderer.drawString(status, 140 - fontRenderer.getStringWidth(status) / 2, 137, 0xFFFFFF);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        mc.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color(1F, 1F, 1F, 1F);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        drawTexturedModalRect(guiLeft + 100, guiTop + 91, 204, 96, 52, 9);
        if (tile.getField(0) > 0) {
            int fill = (int) Math.min(50L, (long) tile.getField(0) * 50 / Math.max(1, tile.getField(1)));
            GlStateManager.color(0F, 1F, 0F, 1F);
            drawTexturedModalRect(guiLeft + 101, guiTop + 92, 206, 230, fill, 7);
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
        for (Slot slot : inventorySlots.inventorySlots) {
            int x = guiLeft + slot.xPos, y = guiTop + slot.yPos;
            GlStateManager.color(1F, 1F, 1F, 1F);
            drawTexturedModalRect(x, y, 228, 206, 16, 16);
            if (slot.slotNumber == AssemblyStationTile.OUTPUT_SLOT) {
                drawTexturedModalRect(x - 9, y - 9, 215, 144, 34, 34);
            } else {
                boolean function = slot.slotNumber == 9;
                if (!(slot.inventory instanceof InventoryPlayer))
                    GlStateManager.color(function ? 1F : 0F, function ? 0F : .9F, function ? 0F : 1F, 1F);
                drawTexturedModalRect(x - 2, y - 3, 207, function ? 203 : 182, 20, 21);
            }
        }
        GlStateManager.color(1F, 1F, 1F, 1F);
    }
}
