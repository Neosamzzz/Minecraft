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

/** Shared structural, propulsion and control inputs for T7/T9/T10 assembly. */
public class AssemblyStationTile extends TileEntity implements ISidedInventory, ITickable {
    public static final int INPUT_SLOTS = 10;
    public static final int OUTPUT_SLOT = 10;
    public static final int TOTAL_SLOTS = 11;

    private final ItemStack[] inventory = new ItemStack[TOTAL_SLOTS];
    private int progressTicks;
    private int requiredTicks = processingTime();
    private ItemStack activeResult = ItemStack.EMPTY;

    public static int processingTime() {
        // GS advances its 3600 * coefficient work budget by 5 per normal tick.
        return Math.max(1, (int) Math.ceil(720D * galaxyspace.core.configs.GSConfigEnergy.coefficientSpeedRocketAssembly));
    }

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
        return world != null && world.getTileEntity(pos) == this && player.getDistanceSq(pos.getX() + .5D, pos.getY() + .5D, pos.getZ() + .5D) <= 64D;
    }
    @Override public void openInventory(EntityPlayer player) {}
    @Override public void closeInventory(EntityPlayer player) {}
    @Override public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index < 0 || index >= INPUT_SLOTS || stack.isEmpty()) return false;
        for (int tier : new int[] {7, 9, 10})
            if (ItemStack.areItemsEqual(stack, inputs(tier).get(index))) return true;
        return false;
    }
    @Override public int getField(int id) { return id == 0 ? progressTicks : id == 1 ? requiredTicks : 0; }
    @Override public void setField(int id, int value) {
        if (id == 0) progressTicks = Math.max(0, value);
        if (id == 1) requiredTicks = Math.max(0, value);
    }
    @Override public int getFieldCount() { return 2; }
    @Override public void clear() {
        for (int i = 0; i < TOTAL_SLOTS; i++) inventory[i] = ItemStack.EMPTY;
        progressTicks = 0;
        activeResult = ItemStack.EMPTY;
    }
    @Override public String getName() { return "newearth.assembly_station"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() { return new net.minecraft.util.text.TextComponentTranslation("tile." + getName() + ".name"); }

    @Override public int[] getSlotsForFace(EnumFacing side) {
        int[] slots = new int[TOTAL_SLOTS];
        for (int i = 0; i < TOTAL_SLOTS; i++) slots[i] = i;
        return slots;
    }
    @Override public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction) {
        return isItemValidForSlot(index, stack);
    }
    @Override public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index == OUTPUT_SLOT;
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        process();
    }

    boolean process() {
        ItemStack result = findMatchingRecipe();
        if (result.isEmpty() || !inventory[OUTPUT_SLOT].isEmpty()) {
            if (progressTicks != 0) { progressTicks = 0; markDirty(); }
            activeResult = ItemStack.EMPTY;
            return false;
        }
        if (!ItemStack.areItemStacksEqual(activeResult, result)) {
            progressTicks = 0;
            requiredTicks = processingTime();
            activeResult = result.copy();
        }
        progressTicks++;
        markDirty();
        if (progressTicks >= requiredTicks) {
            for (int i = 0; i < INPUT_SLOTS; i++) {
                if (!inventory[i].isEmpty()) {
                    inventory[i].shrink(1);
                    if (inventory[i].getCount() <= 0) inventory[i] = ItemStack.EMPTY;
                }
            }
            inventory[OUTPUT_SLOT] = result.copy();
            progressTicks = 0;
            activeResult = ItemStack.EMPTY;
            markDirty();
            return true;
        }
        return false;
    }

    public static java.util.List<ItemStack> inputs(int tier) {
        String prefix = tier == 7 ? "" : "tier9_";
        return java.util.Arrays.asList(Manufacturing.own(prefix + "nose_cone"),
                Manufacturing.own(prefix + (tier == 7 ? "rocket_body" : "body")),
                Manufacturing.own(prefix + (tier == 7 ? "rocket_body" : "body")),
                Manufacturing.own(prefix + "booster"), Manufacturing.own(prefix + "booster"),
                Manufacturing.own(prefix + "fin"), Manufacturing.own(prefix + "fin"),
                Manufacturing.own(prefix + (tier == 7 ? "rocket_engine" : "engine")),
                ManufacturingHooks.modules(tier == 7 ? 6 : tier == 9 ? 5 : 7),
                Manufacturing.own(tier == 7 ? "blueprint_t7" : "control_panel"));
    }
    public static ItemStack output(int tier) {
        return Manufacturing.own("tier" + tier + "_rocket");
    }
    private ItemStack findMatchingRecipe() {
        for (int tier : new int[] {7, 9, 10}) {
            java.util.List<ItemStack> parts = inputs(tier);
            boolean matches = true;
            for (int slot = 0; slot < INPUT_SLOTS; slot++) {
                ItemStack expected = parts.get(slot), actual = inventory[slot];
                if (actual.isEmpty() || !ItemStack.areItemsEqual(actual, expected) || actual.getCount() < expected.getCount()) {
                    matches = false; break;
                }
            }
            if (matches) return output(tier);
        }
        return ItemStack.EMPTY;
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
        compound.setInteger("AssemblyProgress", progressTicks);
        compound.setInteger("AssemblyDuration", requiredTicks);
        if (!activeResult.isEmpty()) compound.setTag("AssemblyResult", activeResult.writeToNBT(new NBTTagCompound()));
        else compound.removeTag("AssemblyResult");
        return compound;
    }

    @Override public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        clear();
        NBTTagList list = compound.getTagList("Items", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int slot = tag.getByte("Slot") & 255;
            if (slot >= 0 && slot < TOTAL_SLOTS) {
                inventory[slot] = new ItemStack(tag);
            }
        }
        requiredTicks = compound.hasKey("AssemblyDuration") ? Math.max(1, compound.getInteger("AssemblyDuration")) : processingTime();
        ItemStack saved = new ItemStack(compound.getCompoundTag("AssemblyResult"));
        if (!saved.isEmpty() && inventory[OUTPUT_SLOT].isEmpty() && ItemStack.areItemStacksEqual(saved, findMatchingRecipe())) {
            activeResult = saved;
            progressTicks = Math.max(0, Math.min(requiredTicks - 1, compound.getInteger("AssemblyProgress")));
        }
    }
}
