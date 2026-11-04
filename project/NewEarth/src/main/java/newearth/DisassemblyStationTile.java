package newearth;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;

/**
 * Disassembly Station Tile: 1 input (rocket) -> 10 outputs (parts).
 * Instant disassembly when recipe matches and outputs are empty.
 */
public class DisassemblyStationTile extends TileEntity implements ISidedInventory, ITickable {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_START = 1;
    private static final int TOTAL_SLOTS = 11;
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    @Override public void update() {
        if (world == null || world.isRemote) return;
        process();
    }

    void process() {
        if (inventory.get(INPUT_SLOT).isEmpty()) return;
        // Check outputs are empty
        for (int i = OUTPUT_START; i < TOTAL_SLOTS; i++) {
            if (!inventory.get(i).isEmpty()) return;
        }
        ItemStack[] parts = findDisassembly(inventory.get(INPUT_SLOT));
        if (parts == null) return;
        // Consume input, produce outputs
        inventory.get(INPUT_SLOT).shrink(1);
        if (inventory.get(INPUT_SLOT).getCount() <= 0) inventory.set(INPUT_SLOT, ItemStack.EMPTY);
        for (int i = 0; i < parts.length && i < TOTAL_SLOTS - 1; i++) {
            inventory.set(OUTPUT_START + i, parts[i].copy());
        }
        markDirty();
    }

    public static java.util.List<ItemStack> parts(ItemStack rocket) {
        String id = String.valueOf(rocket.getItem().getRegistryName());
        int tier = id.equals("newearth:tier7_rocket") ? 7 : id.equals("newearth:tier9_rocket") ? 9
                : id.equals("newearth:tier10_rocket") ? 10 : 0;
        if (tier == 0) return java.util.Collections.emptyList();
        java.util.List<ItemStack> result = new java.util.ArrayList<>();
        for (ItemStack part : AssemblyStationTile.inputs(tier)) {
            if (tier == 7 && part.getItem() == ManufacturingHooks.modules(6).getItem()
                    && part.getMetadata() == 6 && !ManufacturingHooks.readyStack(rocket)) continue;
            result.add(part.copy());
        }
        // Cargo variants retain their installed chests; merge duplicate body parts to make room.
        if (rocket.getMetadata() >= 1 && rocket.getMetadata() <= 3) {
            java.util.List<ItemStack> compact = new java.util.ArrayList<>();
            for (ItemStack part : result) {
                ItemStack existing = null;
                for (ItemStack candidate : compact) if (ItemStack.areItemsEqual(candidate, part)) { existing = candidate; break; }
                if (existing == null) compact.add(part); else existing.grow(part.getCount());
            }
            compact.add(new ItemStack(net.minecraft.init.Blocks.CHEST, rocket.getMetadata()));
            return compact;
        }
        return result;
    }
    private ItemStack[] findDisassembly(ItemStack rocket) {
        java.util.List<ItemStack> parts = parts(rocket);
        return parts.isEmpty() ? null : parts.toArray(new ItemStack[0]);
    }

    // ISidedInventory boilerplate
    @Override public int getSizeInventory() { return TOTAL_SLOTS; }
    @Override public boolean isEmpty() {
        for (ItemStack s : inventory) if (!s.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getStackInSlot(int i) { return inventory.get(i); }
    @Override public ItemStack decrStackSize(int i, int n) { ItemStack result = ItemStackHelper.getAndSplit(inventory, i, n); markDirty(); return result; }
    @Override public ItemStack removeStackFromSlot(int i) { ItemStack result = ItemStackHelper.getAndRemove(inventory, i); markDirty(); return result; }
    @Override public void setInventorySlotContents(int i, ItemStack s) { inventory.set(i, s); markDirty(); }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer p) { return world != null && world.getTileEntity(pos) == this && p.getDistanceSq(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D) <= 64D; }
    @Override public void openInventory(EntityPlayer p) {}
    @Override public void closeInventory(EntityPlayer p) {}
    @Override public boolean isItemValidForSlot(int i, ItemStack s) { return i == INPUT_SLOT; }
    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) {}
    @Override public int getFieldCount() { return 0; }
    @Override public void clear() { inventory.clear(); }
    @Override public String getName() { return "newearth.disassembly_station"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public int[] getSlotsForFace(EnumFacing side) {
        int[] slots = new int[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) slots[i] = i;
        return slots;
    }
    @Override public boolean canInsertItem(int i, ItemStack s, EnumFacing d) { return i == INPUT_SLOT; }
    @Override public boolean canExtractItem(int i, ItemStack s, EnumFacing d) { return i >= OUTPUT_START; }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound c) {
        super.writeToNBT(c);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (!inventory.get(i).isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger("Slot", i);
                inventory.get(i).writeToNBT(tag);
                list.appendTag(tag);
            }
        }
        c.setTag("Items", list);
        return c;
    }
    @Override public void readFromNBT(NBTTagCompound c) {
        super.readFromNBT(c);
        inventory.clear();
        NBTTagList list = c.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getInteger("Slot");
            if (slot >= 0 && slot < TOTAL_SLOTS) inventory.set(slot, new ItemStack(tag));
        }
    }
}
