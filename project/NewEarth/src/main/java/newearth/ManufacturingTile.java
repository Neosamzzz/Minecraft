package newearth;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.*;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Power-driven assembler behind material_station and engine_station.
 * Slots: 0-8 input (3x3), 9 output, 10 battery. Imitates the GS Assembly
 * Machine: timed processing, energy per tick, idle/working/no-power status.
 */
public final class ManufacturingTile extends TileEntityLockable implements ITickable, ISidedInventory {
    public static final int INPUT_COUNT = 9;
    public static final int OUTPUT_SLOT = 9;
    public static final int BATTERY_SLOT = 10;
    public static final int SIZE = 11;

    public static final int ENERGY_CAPACITY = 50000;
    public static final int ENERGY_MAX_RECEIVE = 200;
    public static final int ENERGY_PER_TICK = 40;
    public static final int PROCESS_TIME = 200;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_WORKING = 1;
    public static final int STATUS_NO_POWER = 2;

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final MachineEnergy energy = new MachineEnergy(ENERGY_CAPACITY, ENERGY_MAX_RECEIVE);
    private int processTicks;
    private int status = STATUS_IDLE;

    /** Drains an energy item in the battery slot into internal storage. */
    private void chargeFromBattery() {
        ItemStack battery = inventory.get(BATTERY_SLOT);
        if (battery.isEmpty() || !battery.hasCapability(CapabilityEnergy.ENERGY, null)) return;
        IEnergyStorage cap = battery.getCapability(CapabilityEnergy.ENERGY, null);
        if (cap == null || !cap.canExtract()) return;
        int moved = energy.receiveEnergy(cap.extractEnergy(ENERGY_MAX_RECEIVE, true), false);
        if (moved > 0) {
            cap.extractEnergy(moved, false);
            markDirty();
        }
    }

    private boolean outputFits(ItemStack result) {
        ItemStack existing = inventory.get(OUTPUT_SLOT);
        if (existing.isEmpty()) return true;
        return ItemStack.areItemsEqual(existing, result)
                && ItemStack.areItemStackTagsEqual(existing, result)
                && existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        chargeFromBattery();
        String id = world.getBlockState(pos).getBlock().getRegistryName().getPath();
        Manufacturing.Batch match = null;
        for (Manufacturing.Batch batch : Manufacturing.BATCHES) {
            if (!batch.machine.equals(id)) continue;
            int[] plan = batch.debit(this, INPUT_COUNT);
            if (plan != null && outputFits(batch.output)) { match = batch; break; }
        }
        int oldStatus = status;
        if (match == null) {
            processTicks = 0;
            status = STATUS_IDLE;
        } else if (energy.getEnergyStored() < ENERGY_PER_TICK) {
            status = STATUS_NO_POWER;
        } else {
            energy.extractEnergy(ENERGY_PER_TICK, false);
            processTicks++;
            status = STATUS_WORKING;
            if (processTicks >= PROCESS_TIME) {
                // Re-validate inputs at completion time; abort quietly if they changed.
                int[] finalDebit = match.debit(this, INPUT_COUNT);
                if (finalDebit != null && outputFits(match.output)) {
                    for (int slot = 0; slot < INPUT_COUNT; slot++)
                        if (finalDebit[slot] > 0) decrStackSize(slot, finalDebit[slot]);
                    ItemStack existing = inventory.get(OUTPUT_SLOT);
                    if (existing.isEmpty()) inventory.set(OUTPUT_SLOT, match.output.copy());
                    else existing.grow(match.output.getCount());
                }
                processTicks = 0;
            }
        }
        if (status == STATUS_WORKING || status != oldStatus) markDirty();
    }

    // ---- energy capability: this is what GC aluminium wire detects ----
    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap, EnumFacing side) {
        return cap == CapabilityEnergy.ENERGY || super.hasCapability(cap, side);
    }
    @Override @SuppressWarnings("unchecked")
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, EnumFacing side) {
        if (cap == CapabilityEnergy.ENERGY) return (T) energy;
        return super.getCapability(cap, side);
    }

    // ---- synced fields for the GUI: 0 energy, 1 progress, 2 status ----
    @Override public int getField(int i) {
        switch (i) {
            case 0: return energy.getEnergyStored();
            case 1: return processTicks;
            case 2: return status;
            default: return 0;
        }
    }
    @Override public void setField(int i, int n) {
        switch (i) {
            case 0: energy.setEnergy(n); break;
            case 1: processTicks = n; break;
            case 2: status = n; break;
            default: break;
        }
    }
    @Override public int getFieldCount() { return 3; }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        ItemStackHelper.saveAllItems(tag, inventory);
        tag.setInteger("Energy", energy.getEnergyStored());
        tag.setInteger("ProcessTicks", processTicks);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        inventory.clear();
        for (int i = 0; i < SIZE; i++) inventory.set(i, ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, inventory);
        energy.setEnergy(tag.getInteger("Energy"));
        processTicks = tag.getInteger("ProcessTicks");
    }

    @Override public int getSizeInventory() { return SIZE; }
    @Override public boolean isEmpty() { for (ItemStack s : inventory) if (!s.isEmpty()) return false; return true; }
    @Override public ItemStack getStackInSlot(int i) { return inventory.get(i); }
    @Override public ItemStack decrStackSize(int i, int n) { ItemStack s = ItemStackHelper.getAndSplit(inventory, i, n); if (!s.isEmpty()) markDirty(); return s; }
    @Override public ItemStack removeStackFromSlot(int i) { ItemStack s = ItemStackHelper.getAndRemove(inventory, i); markDirty(); return s; }
    @Override public void setInventorySlotContents(int i, ItemStack s) { inventory.set(i, s); markDirty(); }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer player) { return world.getTileEntity(pos) == this && player.getDistanceSq(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64; }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    @Override public boolean isItemValidForSlot(int i, ItemStack s) {
        if (i < INPUT_COUNT) return true;
        return i == BATTERY_SLOT && s.hasCapability(CapabilityEnergy.ENERGY, null);
    }
    @Override public void clear() { inventory.clear(); markDirty(); }
    @Override public String getName() { return "tile.newearth." + (world == null ? "material_station" : world.getBlockState(pos).getBlock().getRegistryName().getPath()) + ".name"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public String getGuiID() { return NewEarth.MOD_ID + ":manufacturing"; }
    @Override public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player) { return new ManufacturingContainer(playerInventory, this); }
    @Override public int[] getSlotsForFace(EnumFacing face) {
        if (face == EnumFacing.DOWN) return new int[] { OUTPUT_SLOT };
        int[] in = new int[INPUT_COUNT];
        for (int i = 0; i < INPUT_COUNT; i++) in[i] = i;
        return in;
    }
    @Override public boolean canInsertItem(int i, ItemStack s, EnumFacing face) { return i < INPUT_COUNT; }
    @Override public boolean canExtractItem(int i, ItemStack s, EnumFacing face) { return i == OUTPUT_SLOT; }

    /** EnergyStorage with a setter so NBT and GUI sync can write back. */
    static final class MachineEnergy extends EnergyStorage {
        MachineEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0, 0); }
        void setEnergy(int e) { this.energy = Math.max(0, Math.min(e, getMaxEnergyStored())); }
    }
}
