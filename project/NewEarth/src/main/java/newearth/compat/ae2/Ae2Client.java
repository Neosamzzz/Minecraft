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
    private static final ClassValue<Optional<Method>> SLOT_TYPE = new ClassValue<Optional<Method>>() {
        @Override protected Optional<Method> computeValue(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (!current.getName().equals("appeng.container.slot.SlotRestrictedInput")) continue;
                try {
                    return Optional.of(current.getMethod("getPlaceableItemType"));
                } catch (NoSuchMethodException error) {
                    throw new IllegalStateException("Unsupported AE2 slot API", error);
                }
            }
            return Optional.empty();
        }
    };

    @SubscribeEvent public static void open(GuiOpenEvent event) {
        if (event.getGui() != null && event.getGui().getClass().getName().equals("appeng.client.gui.implementations.GuiSecurityStation"))
            event.setGui(new GuiWirelessPairing(((GuiContainer) event.getGui()).inventorySlots));
    }
    @SubscribeEvent public static void itemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack(); ResourceLocation id = stack.getItem().getRegistryName();
        if (id == null || !"appliedenergistics2".equals(id.getNamespace())) return;
        if (Ae2Integration.retired(id.getPath(), stack.getMetadata()) && !stack.isEmpty()) {
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
        if (!supportsGuiHints(gui.getClass().getName())) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !mc.player.inventory.getItemStack().isEmpty()) return;
        String screen = gui.getClass().getSimpleName();
        Slot slot = gui.getSlotUnderMouse();
        String key = null;
        List<String> lines = Collections.emptyList();
        try {
            if (slot == null) {
                key = FluidWidgets.hoverKey(gui, screen, event.getMouseX(), event.getMouseY());
            } else if (!slot.getHasStack() && slot.inventory != mc.player.inventory) {
                key = slotHint(screen, slot.getClass().getSimpleName());
                Method slotType = key == null ? SLOT_TYPE.get(slot.getClass()).orElse(null) : null;
                if (slotType != null) {
                    String kind = String.valueOf(slotType.invoke(slot));
                    if (kind.equals("UPGRADES")) {
                        lines = upgradeHints(gui, slot);
                    } else {
                        if (kind.equals("ENCODED_PATTERN") && screen.contains("PatternTerm")) kind = "EDITED_PATTERN";
                        key = "newearth.ae2.slot." + kind;
                    }
                }
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unsupported AE2 slot API", error);
        }
        if (key != null && I18n.hasKey(key)) lines = Collections.singletonList(I18n.format(key));
        if (!lines.isEmpty()) GuiUtils.drawHoveringText(lines,
                event.getMouseX(), event.getMouseY(), gui.width, gui.height, 260, mc.fontRenderer);
    }

    static boolean supportsGuiHints(String className) {
        return className.startsWith("appeng.client.gui.") || className.startsWith("appeng.fluids.client.gui.");
    }

    private static final class FluidWidgets {
        private static final Method SLOTS, X, Y, WIDTH, HEIGHT, ENABLED, MESSAGE;
        private static final Class<?> FILTER_SLOT, TANK_SLOT;

        static {
            try {
                Class<?> slot = Class.forName("appeng.client.gui.widgets.GuiCustomSlot");
                SLOTS = Class.forName("appeng.client.gui.AEBaseGui").getMethod("getGuiSlots");
                X = slot.getMethod("xPos");
                Y = slot.getMethod("yPos");
                WIDTH = slot.getMethod("getWidth");
                HEIGHT = slot.getMethod("getHeight");
                ENABLED = slot.getMethod("isSlotEnabled");
                MESSAGE = slot.getMethod("getMessage");
                FILTER_SLOT = Class.forName("appeng.fluids.client.gui.widgets.GuiFluidSlot");
                TANK_SLOT = Class.forName("appeng.fluids.client.gui.widgets.GuiFluidTank");
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unsupported AE2 fluid widget API", error);
            }
        }

        private static String hoverKey(GuiContainer gui, String screen, int mouseX, int mouseY) throws ReflectiveOperationException {
            if (!screen.equals("GuiFluidIO") && !screen.equals("GuiFluidStorageBus") && !screen.equals("GuiFluidInterface")) return null;
            int x = mouseX - gui.getGuiLeft();
            int y = mouseY - gui.getGuiTop();
            for (Object widget : (Iterable<?>) SLOTS.invoke(gui)) {
                boolean filter = FILTER_SLOT.isInstance(widget);
                String key = fluidSlotHint(screen, filter, TANK_SLOT.isInstance(widget));
                if (key == null || !(Boolean) ENABLED.invoke(widget)) continue;
                int left = (Integer) X.invoke(widget);
                int top = (Integer) Y.invoke(widget);
                if (x >= left && y >= top && x < left + (Integer) WIDTH.invoke(widget)
                        && y < top + (Integer) HEIGHT.invoke(widget) && MESSAGE.invoke(widget) == null) return key;
            }
            return null;
        }
    }

    static String fluidSlotHint(String screen, boolean filter, boolean tank) {
        if (screen.equals("GuiFluidInterface")) {
            if (filter) return "newearth.ae2.slot.fluid_stock_config";
            if (tank) return "newearth.ae2.slot.fluid_stock_tank";
        }
        if (filter && (screen.equals("GuiFluidIO") || screen.equals("GuiFluidStorageBus")))
            return "newearth.ae2.slot.fluid_filter";
        return null;
    }

    private static List<String> upgradeHints(GuiContainer gui, Slot slot) throws ReflectiveOperationException {
        Object inventory = gui.getClass().getSimpleName().equals("GuiCellWorkbench")
                ? UpgradeCards.CELL_INVENTORY.invoke(gui.inventorySlots) : UpgradeCards.SLOT_INVENTORY.invoke(slot);
        if (!UpgradeCards.INVENTORY.isInstance(inventory)) return Collections.emptyList();
        int slots = ((IItemHandler) inventory).getSlots();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<ItemStack, Object> card : UpgradeCards.TYPES.entrySet()) {
            int limit = Math.min((Integer) UpgradeCards.MAX_INSTALLED.invoke(inventory, card.getValue()), slots);
            if (limit > 0) lines.add(card.getKey().getDisplayName() + " ×" + limit);
        }
        if (!lines.isEmpty()) {
            lines.add(0, I18n.format("newearth.ae2.slot.accepts_upgrades"));
            if (gui.getClass().getSimpleName().equals("GuiFluidInterface"))
                lines.add(I18n.format("newearth.ae2.slot.fluid_capacity_upgrade"));
        }
        return lines;
    }

    private static final class UpgradeCards {
        static final Class<?> INVENTORY;
        static final Method SLOT_INVENTORY, CELL_INVENTORY, MAX_INSTALLED;
        static final Map<ItemStack, Object> TYPES = new LinkedHashMap<>();

        static {
            try {
                INVENTORY = Class.forName("appeng.parts.automation.UpgradeInventory");
                SLOT_INVENTORY = Class.forName("appeng.container.slot.AppEngSlot").getMethod("getItemHandler");
                CELL_INVENTORY = Class.forName("appeng.container.implementations.ContainerCellWorkbench")
                        .getMethod("getCellUpgradeInventory");
                MAX_INSTALLED = INVENTORY.getMethod("getMaxInstalled", Class.forName("appeng.api.config.Upgrades"));
                Item material = Item.getByNameOrId("appliedenergistics2:material");
                if (material != null) {
                    NonNullList<ItemStack> candidates = NonNullList.create();
                    material.getSubItems(CreativeTabs.SEARCH, candidates);
                    Method getType = material.getClass().getMethod("getType", ItemStack.class);
                    for (ItemStack card : candidates) {
                        Object type = getType.invoke(material, card);
                        if (type != null) TYPES.put(card, type);
                    }
                }
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unsupported AE2 upgrade API", error);
            }
        }
    }

    static String slotHint(String gui, String slot) {
        if (gui.equals("GuiInterface")) {
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
