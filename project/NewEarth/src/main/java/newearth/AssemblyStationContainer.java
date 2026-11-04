package newearth;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** GS rocket assembler layout with a separate central propulsion slot. */
public class AssemblyStationContainer extends Container {
    private final AssemblyStationTile tile;
    private int lastProgress = -1, lastDuration = -1;

    public static final int[][] INPUT_POS = {
        {40, 25},  // nose cone
        {40, 50},  // body
        {40, 75},  // body
        {16, 100},  // booster left
        {64, 100},  // booster right
        {16, 130}, // fin left
        {64, 130}, // fin right
        {40, 130}, // structural propulsion unit
        {40, 100},  // independent propulsion module
        {100, 35}, // blueprint/control panel
    };
    public static final int[] OUTPUT_POS = {174, 87};

    public AssemblyStationContainer(InventoryPlayer playerInv, AssemblyStationTile tile) {
        this.tile = tile;

        for (int i = 0; i < AssemblyStationTile.INPUT_SLOTS; i++) {
            addSlotToContainer(new Slot(tile, i, INPUT_POS[i][0], INPUT_POS[i][1]) {
                @Override public boolean isItemValid(ItemStack stack) {
                    return tile.isItemValidForSlot(getSlotIndex(), stack);
                }
                @Override public int getSlotStackLimit() { return 1; }
            });
        }
        addSlotToContainer(new Slot(tile, AssemblyStationTile.OUTPUT_SLOT, OUTPUT_POS[0], OUTPUT_POS[1]) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
        });

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 24 + col * 18, 178 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 24 + col * 18, 235));
        }
    }

    @Override public boolean canInteractWith(EntityPlayer player) {
        return tile.isUsableByPlayer(player);
    }

    private void sendProgress(net.minecraft.inventory.IContainerListener listener) {
        for (int field = 0; field < 2; field++) {
            int value = tile.getField(field);
            listener.sendWindowProperty(this, field * 2, value & 0xFFFF);
            listener.sendWindowProperty(this, field * 2 + 1, value >>> 16);
        }
    }
    @Override public void addListener(net.minecraft.inventory.IContainerListener listener) {
        super.addListener(listener);
        sendProgress(listener);
    }
    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (lastProgress != tile.getField(0) || lastDuration != tile.getField(1)) {
            for (net.minecraft.inventory.IContainerListener listener : listeners) sendProgress(listener);
            lastProgress = tile.getField(0);
            lastDuration = tile.getField(1);
        }
    }
    @Override @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        int field = id / 2;
        if (id < 0 || id > 3) return;
        int old = tile.getField(field);
        tile.setField(field, id % 2 == 0 ? (old & 0xFFFF0000) | (value & 0xFFFF)
                : (old & 0xFFFF) | ((value & 0xFFFF) << 16));
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            copy = stack.copy();
            if (index < AssemblyStationTile.TOTAL_SLOTS) {
                // From station to player
                if (!mergeItemStack(stack, AssemblyStationTile.TOTAL_SLOTS, inventorySlots.size(), true)) return ItemStack.EMPTY;
            } else {
                // From player to station (try input slots only)
                if (!mergeItemStack(stack, 0, AssemblyStationTile.INPUT_SLOTS, false)) return ItemStack.EMPTY;
            }
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
            slot.onTake(player, stack);
        }
        return copy;
    }
}
