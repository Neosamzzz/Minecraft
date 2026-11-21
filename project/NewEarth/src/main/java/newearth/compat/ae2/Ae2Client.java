package newearth.compat.ae2;

import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.IItemHandler;
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
            event.getToolTip().addAll(Minecraft.getMinecraft().fontRenderer
                    .listFormattedStringToWidth(TextFormatting.GRAY + I18n.format(key), 260));
        }
    }

    @SubscribeEvent public static void guiTooltip(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiContainer)) return;
        GuiContainer gui = (GuiContainer) event.getGui();
        if (!gui.getClass().getName().startsWith("appeng.client.gui.")) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !mc.player.inventory.getItemStack().isEmpty()) return;
        Slot slot = gui.getSlotUnderMouse();
        if (slot == null || slot.getHasStack() || slot.inventory == mc.player.inventory) return;
        String screen = gui.getClass().getSimpleName();
        String key = slotHint(screen, slot.getClass().getSimpleName());
        List<String> lines = Collections.emptyList();
        if (key == null && restricted(slot.getClass())) {
            try {
                String kind = String.valueOf(slot.getClass().getMethod("getPlaceableItemType").invoke(slot));
                if (kind.equals("UPGRADES")) {
                    lines = upgradeHints(gui, slot);
                } else {
                    if (kind.equals("ENCODED_PATTERN") && screen.contains("PatternTerm")) kind = "EDITED_PATTERN";
                    key = "newearth.ae2.slot." + kind;
                }
            } catch (ReflectiveOperationException error) { throw new IllegalStateException("Unsupported AE2 slot API", error); }
        }
        if (key != null && I18n.hasKey(key)) lines = Collections.singletonList(I18n.format(key));
        if (!lines.isEmpty()) GuiUtils.drawHoveringText(lines,
                event.getMouseX(), event.getMouseY(), gui.width, gui.height, 260, mc.fontRenderer);
    }

    private static List<String> upgradeHints(GuiContainer gui, Slot slot) throws ReflectiveOperationException {
        Object inventory = slot.getClass().getMethod("getItemHandler").invoke(slot);
        Class<?> upgrades = Class.forName("appeng.parts.automation.UpgradeInventory");
        if (gui.getClass().getSimpleName().equals("GuiCellWorkbench")) {
            Object container = gui.inventorySlots;
            inventory = container.getClass().getMethod("getCellUpgradeInventory").invoke(container);
        }
        if (!upgrades.isInstance(inventory)) return Collections.emptyList();

        Item material = Item.getByNameOrId("appliedenergistics2:material");
        if (material == null) return Collections.emptyList();
        NonNullList<ItemStack> candidates = NonNullList.create();
        material.getSubItems(CreativeTabs.SEARCH, candidates);
        Method getType = material.getClass().getMethod("getType", ItemStack.class);
        Method getMax = upgrades.getMethod("getMaxInstalled", getType.getReturnType());
        List<String> lines = new ArrayList<>();
        for (ItemStack card : candidates) {
            Object type = getType.invoke(material, card);
            if (type == null) continue;
            int limit = Math.min((Integer) getMax.invoke(inventory, type), ((IItemHandler) inventory).getSlots());
            if (limit > 0) lines.add(card.getDisplayName() + " ×" + limit);
        }
        if (!lines.isEmpty()) lines.add(0, I18n.format("newearth.ae2.slot.accepts_upgrades"));
        return lines;
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
        return null;
    }
}
