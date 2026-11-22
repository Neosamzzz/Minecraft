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
        if (!gui.getClass().getName().startsWith("appeng.client.gui.")) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || !mc.player.inventory.getItemStack().isEmpty()) return;
        Slot slot = gui.getSlotUnderMouse();
        if (slot == null || slot.getHasStack() || slot.inventory == mc.player.inventory) return;
        String screen = gui.getClass().getSimpleName();
        String key = slotHint(screen, slot.getClass().getSimpleName());
        List<String> lines = Collections.emptyList();
        Method slotType = key == null ? SLOT_TYPE.get(slot.getClass()).orElse(null) : null;
        if (slotType != null) {
            try {
                String kind = String.valueOf(slotType.invoke(slot));
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
        Object inventory = gui.getClass().getSimpleName().equals("GuiCellWorkbench")
                ? UpgradeCards.CELL_INVENTORY.invoke(gui.inventorySlots) : UpgradeCards.SLOT_INVENTORY.invoke(slot);
        if (!UpgradeCards.INVENTORY.isInstance(inventory)) return Collections.emptyList();
        int slots = ((IItemHandler) inventory).getSlots();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<ItemStack, Object> card : UpgradeCards.TYPES.entrySet()) {
            int limit = Math.min((Integer) UpgradeCards.MAX_INSTALLED.invoke(inventory, card.getValue()), slots);
            if (limit > 0) lines.add(card.getKey().getDisplayName() + " ×" + limit);
        }
        if (!lines.isEmpty()) lines.add(0, I18n.format("newearth.ae2.slot.accepts_upgrades"));
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
