package newearth.compat.ae2;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import newearth.NewEarth;

@Mod.EventBusSubscriber(modid = NewEarth.MOD_ID, value = Side.CLIENT)
public final class Ae2Client {
    @SubscribeEvent public static void open(GuiOpenEvent event) {
        if (event.getGui() != null && event.getGui().getClass().getName().equals("appeng.client.gui.implementations.GuiSecurityStation"))
            event.setGui(new GuiWirelessPairing(((GuiContainer) event.getGui()).inventorySlots));
    }
    @SubscribeEvent public static void itemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack(); ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null || !"appliedenergistics2".equals(id.getNamespace())) return;
        if (Ae2Integration.retired(stack)) {
            event.getToolTip().add(TextFormatting.DARK_GRAY + I18n.format("newearth.ae2.retired"));
            return;
        }
        String key = stack.getItem().getTranslationKey(stack) + ".newearth.desc";
        if (I18n.hasKey(key)) {
            Minecraft client = Minecraft.getMinecraft();
            String[] lines = I18n.format(key).split("\\|");
            boolean details = GameSettings.isKeyDown(client.gameSettings.keyBindSneak);
            for (int index = 0; index < lines.length && (index == 0 || details); index++)
                event.getToolTip().addAll(client.fontRenderer.listFormattedStringToWidth(TextFormatting.GRAY + lines[index], 260));
            if (lines.length > 1 && !details) event.getToolTip().add(TextFormatting.DARK_GRAY + I18n.format("newearth.ae2.more") + " "
                    + GameSettings.getKeyDisplayString(client.gameSettings.keyBindSneak.getKeyCode()));
        }
    }

    @SubscribeEvent public static void guiTooltip(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) event.getGui();
        if (!gui.getClass().getName().startsWith("appeng.client.gui.")) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !mc.player.inventory.getItemStack().isEmpty()) return;
        Slot slot = gui.getSlotUnderMouse();
        // Occupied slots already render the item description; annotate empty slots without covering it.
        String key;
        if (slot == null) {
            key = "newearth.ae2.gui." + gui.getClass().getSimpleName();
            if (event.getMouseX() < gui.getGuiLeft() || event.getMouseX() >= gui.getGuiLeft() + 90
                    || event.getMouseY() < gui.getGuiTop() || event.getMouseY() >= gui.getGuiTop() + 18
                    || !I18n.hasKey(key)) return;
        } else {
            if (slot.getHasStack() || slot.inventory == mc.player.inventory) return;
            key = slotHint(gui.getClass().getSimpleName(), slot.getClass().getSimpleName());
        }
        if (key == null && slot != null && restricted(slot.getClass())) {
            try {
                String kind = String.valueOf(slot.getClass().getMethod("getPlaceableItemType").invoke(slot));
                String candidate = "newearth.ae2.slot." + kind;
                if (I18n.hasKey(candidate)) key = candidate;
            } catch (ReflectiveOperationException error) { throw new IllegalStateException("Unsupported AE2 slot API", error); }
        }
        if (key != null && I18n.hasKey(key)) GuiUtils.drawHoveringText(Arrays.asList(I18n.format(key).split("\\|")),
                event.getMouseX(), event.getMouseY(), gui.width, gui.height, 260, mc.fontRenderer);
    }

    private static boolean restricted(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass())
            if (current.getName().equals("appeng.container.slot.SlotRestrictedInput")) return true;
        return false;
    }

    static String slotHint(String gui, String slot) {
        if (gui.equals("GuiInterface") || gui.equals("GuiFluidInterface")) {
            if (slot.equals("SlotFake")) return "newearth.ae2.slot.stock_config";
            if (slot.equals("SlotOversized")) return "newearth.ae2.slot.stock_inventory";
        }
        if (gui.equals("GuiInterfaceTerminal") && slot.equals("SlotDisconnected")) return "newearth.ae2.slot.remote_pattern";
        if (gui.contains("PatternTerm")) {
            if (slot.equals("SlotFakeCraftingMatrix")) return "newearth.ae2.slot.recipe_input";
            if (slot.equals("OptionalSlotFake") || slot.equals("SlotPatternOutputs") || slot.equals("SlotPatternTerm")) return "newearth.ae2.slot.recipe_output";
        }
        if (gui.equals("GuiCraftingTerm") && slot.equals("SlotCraftingTerm")) return "newearth.ae2.slot.manual_output";
        return null;
    }
}
