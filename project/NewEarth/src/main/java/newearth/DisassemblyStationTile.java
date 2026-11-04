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
 * Disassembly Station Tile: 1 input (rocket) -> 9 outputs (parts).
 * Instant disassembly when recipe matches and outputs are empty.
 */
public class DisassemblyStationTile extends TileEntity implements ISidedInventory, ITickable {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_START = 1;
    private static final int TOTAL_SLOTS = 10;
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    @Override public void update() {
        if (world.isRemote) return;
        if (inventory[INPUT_SLOT].isEmpty()) return;
        // Check outputs are empty
        for (int i = OUTPUT_START; i < TOTAL_SLOTS; i++) {
            if (!inventory[i].isEmpty()) return;
        }
        ItemStack[] parts = findDisassembly(inventory[INPUT_SLOT]);
        if (parts == null) return;
        // Consume input, produce outputs
        inventory[INPUT_SLOT].shrink(1);
        if (inventory[INPUT_SLOT].getCount() <= 0) inventory[INPUT_SLOT] = ItemStack.EMPTY;
        for (int i = 0; i < parts.length && i < 9; i++) {
            inventory[OUTPUT_START + i] = parts[i].copy();
        }
        markDirty();
    }

    /**
     * Returns the 9 parts for a rocket, or null if not disassemblable.
     */
    private ItemStack[] findDisassembly(ItemStack rocket) {
        if (isItem(rocket, "tier7_rocket")) {
            return new ItemStack[] {
                Manufacturing.own("nose_cone"),
                Manufacturing.own("rocket_body"), Manufacturing.own("rocket_body"),
                Manufacturing.own("booster"), Manufacturing.own("booster"),
                Manufacturing.own("fin"), Manufacturing.own("fin"),
                Manufacturing.own("rocket_engine"),
                Manufacturing.own("blueprint_t7"),
            };
        }
        if (isItem(rocket, "tier9_rocket")) {
            return new ItemStack[] {
                Manufacturing.own("tier9_nose_cone"),
                Manufacturing.own("tier9_body"), Manufacturing.own("tier9_body"),
                Manufacturing.own("tier9_booster"), Manufacturing.own("tier9_booster"),
                Manufacturing.own("tier9_fin"), Manufacturing.own("tier9_fin"),
                Manufacturing.own("tier9_engine"),
                gsModule(5), // plasma engine
            };
        }
        if (isItem(rocket, "tier10_rocket")) {
            return new ItemStack[] {
                Manufacturing.own("tier9_nose_cone"),
                Manufacturing.own("tier9_body"), Manufacturing.own("tier9_body"),
                Manufacturing.own("tier9_booster"), Manufacturing.own("tier9_booster"),
                Manufacturing.own("tier9_fin"), Manufacturing.own("tier9_fin"),
                Manufacturing.own("tier9_engine"),
                gsModule(7), // black hole engine
            };
        }
        return null;
    }

    private ItemStack gsModule(int meta) {
        try {
            Class<?> cls = Class.forName("galaxyspace.core.GSItems");
            Object item = cls.getField("ROCKET_MODULES").get(null);
            return new ItemStack((net.minecraft.item.Item) item, 1, meta);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private boolean isItem(ItemStack stack, String id) {
        if (stack.isEmpty()) return false;
        ItemStack expected = Manufacturing.own(id);
        return !expected.isEmpty() && stack.getItem() == expected.getItem();
    }

    // ISidedInventory boilerplate
    @Override public int getSizeInventory() { return TOTAL_SLOTS; }
    @Override public boolean isEmpty() {
        for (ItemStack s : inventory) if (!s.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getStackInSlot(int i) { return inventory.get(i); }
    @Override public ItemStack decrStackSize(int i, int n) { return ItemStackHelper.getAndSplit(inventory, i, n); }
    @Override public ItemStack removeStackFromSlot(int i) { return ItemStackHelper.getAndRemove(inventory, i); }
    @Override public void setInventorySlotContents(int i, ItemStack s) { inventory.set(i, s); }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer p) { return true; }
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
        NBTTagList list = c.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getInteger("Slot");
            if (slot >= 0 && slot < TOTAL_SLOTS) inventory.set(slot, new ItemStack(tag));
        }
    }
}
