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

        // Rocket goes in the GS result slot. Parts come out in the part slots.
        addSlotToContainer(new Slot(tile, 0, 174, 87));

        int[][] partSlots = {
            {40, 25}, {40, 55}, {40, 85}, {40, 110},
            {16, 90}, {64, 90}, {16, 120}, {64, 120},
            {100, 35}, {130, 35}
        };
        for (int i = 0; i < partSlots.length; i++) {
            int x = partSlots[i][0], y = partSlots[i][1];
            addSlotToContainer(new Slot(tile, 1 + i, x, y) {
                @Override public boolean isItemValid(ItemStack stack) { return false; }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 24 + col * 18, 178 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(new Slot(playerInv, col, 24 + col * 18, 235));
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
