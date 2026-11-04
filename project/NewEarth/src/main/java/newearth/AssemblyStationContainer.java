package newearth;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Container for Tier 7/9 Assembly Station.
 * Slot layout from recipe-studio draft (scaled to GUI coordinates):
 *   Input slots 0-8 in assembly pattern, Output slot 9.
 */
public class AssemblyStationContainer extends Container {
    private final AssemblyStationTile tile;

    // Slot positions from studio draft (x,y in draft coords, scaled for 176x166 GUI)
    // Draft was 480x340, GUI is 176x166. Scale factor ~0.36
    // Simplified layout: 3x3 grid for parts + special slots
    private static final int[][] INPUT_POS = {
        {62, 17},  // 0: nose cone (top center)
        {62, 35},  // 1: body
        {62, 53},  // 2: body
        {30, 71},  // 3: booster left
        {94, 71},  // 4: booster right
        {30, 89},  // 5: fin left
        {94, 89},  // 6: fin right
        {62, 89},  // 7: engine (center bottom)
        {134, 17}, // 8: blueprint/license
    };
    private static final int[] OUTPUT_POS = {134, 53};

    public AssemblyStationContainer(InventoryPlayer playerInv, AssemblyStationTile tile) {
        this.tile = tile;

        // Input slots 0-8
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(tile, i, INPUT_POS[i][0], INPUT_POS[i][1]));
        }
        // Output slot 9 (extract-only handled by TileEntity.isItemValidForSlot)
        addSlotToContainer(new Slot(tile, 9, OUTPUT_POS[0], OUTPUT_POS[1]) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
        });

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
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            copy = stack.copy();
            if (index < 10) {
                // From station to player
                if (!mergeItemStack(stack, 10, inventorySlots.size(), true)) return ItemStack.EMPTY;
            } else {
                // From player to station (try input slots only)
                if (!mergeItemStack(stack, 0, 9, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY);
            else slot.onSlotChanged();
        }
        return copy;
    }
}
