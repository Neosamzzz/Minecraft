package newearth;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Container for Disassembly Station.
 * 1 input slot (rocket) + 9 output slots (parts in 3x3 grid).
 */
public class DisassemblyStationContainer extends Container {
    private final DisassemblyStationTile tile;

    public DisassemblyStationContainer(InventoryPlayer playerInv, DisassemblyStationTile tile) {
        this.tile = tile;

        // Input slot 0 (rocket)
        addSlotToContainer(new Slot(tile, 0, 26, 35));

        // Output slots 1-9 in 3x3 grid
        for (int i = 0; i < 10; i++) {
            int x = 80 + (i % 5) * 18;
            int y = 17 + (i / 5) * 18;
            addSlotToContainer(new Slot(tile, 1 + i, x, y) {
                @Override public boolean isItemValid(ItemStack stack) { return false; }
            });
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override public boolean canInteractWith(EntityPlayer player) {
        return tile.isUsableByPlayer(player);
    }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (!slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), copy = stack.copy();
        if (index < 11) {
            if (!mergeItemStack(stack, 11, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else if (!mergeItemStack(stack, 0, 1, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        slot.onTake(player, stack);
        return copy;
    }
}
