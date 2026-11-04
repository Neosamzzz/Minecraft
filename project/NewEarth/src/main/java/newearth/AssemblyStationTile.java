package newearth;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * Tier 7/9 Rocket Assembly Station.
 * 9 input slots + 1 output slot. No power required (manual assembly).
 * Layout matches recipe-studio drafts:
 *   T7: nose_cone x1, rocket_body x2, booster x2, fin x2, rocket_engine x1, blueprint_t7 x1 -> tier7_rocket
 *   T9: tier9 parts + plasma engine -> tier9 rocket (reuses same station)
 */
public class AssemblyStationTile extends TileEntity implements ISidedInventory, ITickable {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int TOTAL_SLOTS = 10;

    private final ItemStack[] inventory = new ItemStack[TOTAL_SLOTS];

    public AssemblyStationTile() {
        for (int i = 0; i < TOTAL_SLOTS; i++) inventory[i] = ItemStack.EMPTY;
    }

    @Override public int getSizeInventory() { return TOTAL_SLOTS; }
    @Override public boolean isEmpty() {
        for (ItemStack s : inventory) if (!s.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getStackInSlot(int index) { return inventory[index]; }
    @Override public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = inventory[index];
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (stack.getCount() <= count) {
            inventory[index] = ItemStack.EMPTY;
            markDirty();
            return stack;
        }
        ItemStack split = stack.splitStack(count);
        if (stack.getCount() == 0) inventory[index] = ItemStack.EMPTY;
        markDirty();
        return split;
    }
    @Override public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = inventory[index];
        inventory[index] = ItemStack.EMPTY;
        return stack;
    }
    @Override public void setInventorySlotContents(int index, ItemStack stack) {
        inventory[index] = stack;
        if (!stack.isEmpty() && stack.getCount() > getInventoryStackLimit())
            stack.setCount(getInventoryStackLimit());
        markDirty();
    }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer player) {
        return player.getDistanceSq(pos) <= 64.0D;
    }
    @Override public void openInventory(EntityPlayer player) {}
    @Override public void closeInventory(EntityPlayer player) {}
    @Override public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index != OUTPUT_SLOT; // Output slot is extract-only
    }
    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) {}
    @Override public int getFieldCount() { return 0; }
    @Override public void clear() {
        for (int i = 0; i < TOTAL_SLOTS; i++) inventory[i] = ItemStack.EMPTY;
    }
    @Override public String getName() { return "newearth.assembly_station"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new TextComponentString(getName()); }

    @Override public int[] getSlotsForFace(EnumFacing side) {
        int[] slots = new int[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) slots[i] = i;
        return slots;
    }
    @Override public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction) {
        return index != OUTPUT_SLOT;
    }
    @Override public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index == OUTPUT_SLOT;
    }

    @Override public void update() {
        if (world.isRemote) return;
        // Check for valid recipe match
        ItemStack result = findMatchingRecipe();
        if (!result.isEmpty() && inventory[OUTPUT_SLOT].isEmpty()) {
            // Consume inputs and produce output
            for (int i = 0; i < INPUT_SLOTS; i++) {
                if (!inventory[i].isEmpty()) {
                    inventory[i].shrink(1);
                    if (inventory[i].getCount() <= 0) inventory[i] = ItemStack.EMPTY;
                }
            }
            inventory[OUTPUT_SLOT] = result.copy();
            markDirty();
        }
    }

    /**
     * Matches inputs against T7/T9 assembly recipes.
     * Returns the output ItemStack if matched, else EMPTY.
     */
    private ItemStack findMatchingRecipe() {
        // T7: nose_cone(1) + rocket_body(2) + booster(2) + fin(2) + rocket_engine(1) + blueprint_t7(1)
        // Slot layout (from studio draft):
        //   0: cone, 1: body, 2: body, 3: booster, 4: booster, 5: fin, 6: fin, 7: engine, 8: blueprint
        if (matchesT7()) {
            return Manufacturing.own("tier7_rocket");
        }
        // T9: tier9 parts + plasma engine (same station, different recipe)
        // TODO: Implement when tier9 items are finalized
        return ItemStack.EMPTY;
    }

    private boolean matchesT7() {
        return isItem(inventory[0], "nose_cone")
            && isItem(inventory[1], "rocket_body")
            && isItem(inventory[2], "rocket_body")
            && isItem(inventory[3], "booster")
            && isItem(inventory[4], "booster")
            && isItem(inventory[5], "fin")
            && isItem(inventory[6], "fin")
            && isItem(inventory[7], "rocket_engine")
            && isItem(inventory[8], "blueprint_t7");
    }

    private boolean isItem(ItemStack stack, String id) {
        if (stack.isEmpty()) return false;
        ItemStack expected = Manufacturing.own(id);
        return !expected.isEmpty() && stack.getItem() == expected.getItem();
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (!inventory[i].isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte("Slot", (byte) i);
                inventory[i].writeToNBT(tag);
                list.appendTag(tag);
            }
        }
        compound.setTag("Items", list);
        return compound;
    }

    @Override public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        NBTTagList list = compound.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 255;
            if (slot >= 0 && slot < TOTAL_SLOTS) {
                inventory[slot] = new ItemStack(tag);
            }
        }
    }
}
