package newearth.compat.ae2;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

/** Keep AE2's existing two-slot wireless encoder; expose none of the player permission controls. */
public final class GuiWirelessPairing extends GuiContainer {
    private Slot input, output;
    public GuiWirelessPairing(Container container) {
        super(container); xSize = 176; ySize = 166;
        for (Slot slot : container.inventorySlots) {
            if (slot.inventory == Minecraft.getMinecraft().player.inventory) {
                int index = slot.getSlotIndex();
                slot.xPos = 8 + index % 9 * 18;
                slot.yPos = index < 9 ? 142 : 84 + (index / 9 - 1) * 18;
            } else if (restrictedType(slot).equals("ENCODABLE_ITEM")) {
                input = slot; slot.xPos = 53; slot.yPos = 35;
            } else if (slot.getClass().getSimpleName().equals("SlotOutput")) {
                output = slot; slot.xPos = 107; slot.yPos = 35;
            } else { slot.xPos = -10000; slot.yPos = -10000; }
        }
        if (input == null || output == null) throw new IllegalStateException("Unsupported AE2 wireless pairing slots");
    }
    private static String restrictedType(Slot slot) {
        try { return String.valueOf(slot.getClass().getMethod("getPlaceableItemType").invoke(slot)); }
        catch (NoSuchMethodException notRestricted) { return ""; }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    @Override protected void drawGuiContainerBackgroundLayer(float partial, int mouseX, int mouseY) {
        GlStateManager.color(1,1,1,1);
        mc.getTextureManager().bindTexture(new ResourceLocation("minecraft:textures/gui/container/dispenser.png"));
        drawTexturedModalRect(guiLeft,guiTop,0,0,xSize,ySize);
        drawRect(guiLeft + 7,guiTop + 16,guiLeft + 169,guiTop + 75,0xFFC6C6C6);
        drawSlot(input); drawSlot(output);
    }
    private void drawSlot(Slot slot) {
        int x = guiLeft + slot.xPos - 1, y = guiTop + slot.yPos - 1;
        drawRect(x,y,x+18,y+18,0xFF373737);
        drawRect(x+1,y+1,x+18,y+18,0xFFFFFFFF);
        drawRect(x+1,y+1,x+17,y+17,0xFF8B8B8B);
    }
    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("newearth.ae2.pair.title"),8,6,0x404040);
        fontRenderer.drawString(I18n.format("newearth.ae2.pair.input"),45,22,0x404040);
        fontRenderer.drawString(I18n.format("newearth.ae2.pair.output"),99,22,0x404040);
        fontRenderer.drawString("→",82,39,0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"),8,72,0x404040);
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partial) {
        drawDefaultBackground(); super.drawScreen(mouseX,mouseY,partial); renderHoveredToolTip(mouseX,mouseY);
        Slot hovered = getSlotUnderMouse();
        if (hovered != null && !hovered.getHasStack() && mc.player.inventory.getItemStack().isEmpty()) {
            String key = hovered == input ? "newearth.ae2.pair.input_tip" : hovered == output ? "newearth.ae2.pair.output_tip" : null;
            if (key != null) drawHoveringText(fontRenderer.listFormattedStringToWidth(I18n.format(key),240),mouseX,mouseY);
        }
    }
}
