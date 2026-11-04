package newearth;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.relauncher.*;

/** Slot layout mirrors ManufacturingGui: 3x3 input, output, battery, player inventory. */
public final class ManufacturingContainer extends Container {
    private final ManufacturingTile tile;
    private int lastEnergy = -1, lastProgress = -1, lastStatus = -1;

    public ManufacturingContainer(InventoryPlayer playerInventory, ManufacturingTile tile) {
        this.tile = tile;
        if (tile.engine()) {
            addSlotToContainer(new Slot(tile, 0, 44, 36));
            addSlotToContainer(new Slot(tile, 1, 80, 36));
        } else {
            for (int row = 0; row < 3; row++)
                for (int col = 0; col < 3; col++)
                    addSlotToContainer(new Slot(tile, row * 3 + col, 19 + col * 18, 18 + row * 18));
        }
        addSlotToContainer(new Slot(tile, ManufacturingTile.OUTPUT_SLOT, 138, 36) {
            @Override public boolean isItemValid(ItemStack stack) { return false; }
        });
        if (!tile.engine()) {
            addSlotToContainer(new Slot(tile, ManufacturingTile.BATTERY_SLOT, 77, 83) {
                @Override public boolean isItemValid(ItemStack stack) { return stack.hasCapability(CapabilityEnergy.ENERGY, null); }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 129 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 187));
    }

    @Override public void detectAndSendChanges() {
        super.detectAndSendChanges();
        int energy = tile.getField(0), progress = tile.getField(1), status = tile.getField(2);
        for (IContainerListener listener : listeners) {
            if (lastEnergy != energy) listener.sendWindowProperty(this, 0, energy);
            if (lastProgress != progress) listener.sendWindowProperty(this, 1, progress);
            if (lastStatus != status) listener.sendWindowProperty(this, 2, status);
        }
        lastEnergy = energy; lastProgress = progress; lastStatus = status;
    }

    @Override @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data) { tile.setField(id, id == 0 ? data & 0xFFFF : data); }

    @Override public boolean canInteractWith(EntityPlayer player) { return tile.isUsableByPlayer(player); }

    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return result;
        ItemStack stack = slot.getStack();
        result = stack.copy();
        int tileSlots = tile.engine() ? 3 : ManufacturingTile.SIZE;
        if (index < tileSlots) {
            if (!mergeItemStack(stack, tileSlots, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else if (!tile.engine() && stack.hasCapability(CapabilityEnergy.ENERGY, null)) {
            if (!mergeItemStack(stack, ManufacturingTile.INPUT_COUNT + 1, ManufacturingTile.INPUT_COUNT + 2, false)) return ItemStack.EMPTY;
        } else if (!mergeItemStack(stack, 0, tile.inputLimit(), false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return result;
    }
}
