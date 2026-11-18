package newearth.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Associates a disappearing held stack with a preceding drawer deposit, including delayed server updates. */
@SideOnly(Side.CLIENT)
public final class DrawerAutoRefill {
    private static final PendingDeposit pending = new PendingDeposit();

    public static void register() { MinecraftForge.EVENT_BUS.register(new DrawerAutoRefill()); }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void interact(PlayerInteractEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getEntityPlayer() != mc.player || !event.getWorld().isRemote) return;
        // A second offhand event must not erase the preceding main-hand interaction.
        if (event.getHand() != EnumHand.MAIN_HAND) return;
        pending.clear();
        if (!(event instanceof PlayerInteractEvent.RightClickBlock) || event.isCanceled()) return;
        PlayerInteractEvent.RightClickBlock click = (PlayerInteractEvent.RightClickBlock) event;
        if (click.getUseBlock() == Event.Result.DENY || !isDrawerFront(click)) return;
        ItemStack held = mc.player.getHeldItemMainhand();
        if (!held.isEmpty()) pending.record(mc.player, mc.world, mc.player.inventory.currentItem,
                held.getItem().getRegistryName().toString(), held.getItemDamage(), System.nanoTime());
    }

    private static boolean isDrawerFront(PlayerInteractEvent.RightClickBlock event) {
        IBlockState state = event.getWorld().getBlockState(event.getPos());
        boolean drawer = false, controller = false;
        for (Class<?> type = state.getBlock().getClass(); type != null; type = type.getSuperclass()) {
            drawer |= type.getName().equals("com.jaquadro.minecraft.storagedrawers.block.BlockDrawers");
            controller |= type.getName().equals("com.jaquadro.minecraft.storagedrawers.block.BlockController");
        }
        if (controller) {
            for (IProperty<?> property : state.getPropertyKeys())
                if ("facing".equals(property.getName())) return state.getValue(property) == event.getFace();
        }
        if (drawer) {
            TileEntity tile = event.getWorld().getTileEntity(event.getPos());
            if (tile != null) try {
                return ((Number) tile.getClass().getMethod("getDirection").invoke(tile)).intValue()
                        == event.getFace().ordinal();
            } catch (ReflectiveOperationException ignored) { }
        }
        return false;
    }

    @SubscribeEvent
    public void gui(GuiOpenEvent event) { pending.clear(); }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) { pending.clear(); return; }
        ItemStack held = mc.player.getHeldItemMainhand();
        pending.observe(mc.player, mc.world, mc.player.inventory.currentItem, held.isEmpty() ? null
                : held.getItem().getRegistryName().toString(), held.isEmpty() ? 0 : held.getItemDamage(), System.nanoTime());
    }

    public static boolean suppress(int slot, String item, int damage) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.world == null || mc.currentScreen != null) { pending.clear(); return false; }
        ItemStack held = player.getHeldItemMainhand();
        pending.observe(player, mc.world, player.inventory.currentItem, held.isEmpty() ? null
                : held.getItem().getRegistryName().toString(), held.isEmpty() ? 0 : held.getItemDamage(), System.nanoTime());
        return pending.consume(slot, item, damage, held.isEmpty());
    }

    /** Return true when the queued action was completed or is no longer valid. */
    public static boolean refillEmptyHand(Object queuedContainer, int source, int target, String expectedItem, boolean beforeBreak) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.playerController == null || mc.currentScreen != null || player.isSpectator()
                || player.openContainer != queuedContainer || player.openContainer != player.inventoryContainer
                || player.openContainer.getClass() != ContainerPlayer.class) return true;
        int hotbar = target - 27;
        if (hotbar < 0 || hotbar >= 9 || hotbar != player.inventory.currentItem || !player.inventory.getItemStack().isEmpty()) return true;
        ItemStack held = player.getHeldItemMainhand();
        if (!held.isEmpty()) {
            // Soup and the opt-in tool replacement can legitimately target an occupied slot.
            return !(beforeBreak || (held.getItem() == Items.BOWL && "minecraft:mushroom_stew".equals(expectedItem)));
        }
        Container container = player.inventoryContainer;
        int sourceSlot = refillSourceSlot(container, player.inventory, source, target, expectedItem);
        if (sourceSlot >= 0) mc.playerController.windowClick(container.windowId, sourceSlot, hotbar, ClickType.SWAP, player);
        return true;
    }

    static int refillSourceSlot(Container container, net.minecraft.entity.player.InventoryPlayer inventory,
            int source, int target, String expectedItem) {
        if (source < 0 || source >= 36 || source == target || expectedItem == null) return -1;
        // Inventory Tweaks' inventory section is vanilla slots 9 through 44.
        int sourceSlot = source + 9;
        Slot slot = container.getSlot(sourceSlot);
        ItemStack stack = slot.getStack();
        if (slot.inventory != inventory || stack.isEmpty()
                || !expectedItem.equals(String.valueOf(stack.getItem().getRegistryName()))) return -1;
        return sourceSlot;
    }

    static final class PendingDeposit {
        private static final long MAX_DELAY = 5_000_000_000L;
        private Object player, world;
        private int slot, damage;
        private String item;
        private long time;

        void record(Object player, Object world, int slot, String item, int damage, long time) {
            this.player = player; this.world = world; this.slot = slot; this.item = item;
            this.damage = damage; this.time = time;
        }
        void clear() { player = null; world = null; item = null; }
        void observe(Object player, Object world, int slot, String heldItem, int damage, long time) {
            if (this.player != player || this.world != world || this.slot != slot || time - this.time > MAX_DELAY
                    || (heldItem != null && (!heldItem.equals(item) || damage != this.damage))) clear();
        }
        boolean consume(int refillSlot, String item, int damage, boolean empty) {
            if (this.item == null || !empty || refillSlot != slot + 27 || !this.item.equals(item)
                    || this.damage != damage) return false;
            clear();
            return true;
        }
    }
}
