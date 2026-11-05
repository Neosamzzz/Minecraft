package newearth;

import net.minecraft.block.state.IBlockState;
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
    private final IEnergyStorage energyView = new IEnergyStorage() {
        @Override public int getEnergyStored() { return energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
        @Override public int receiveEnergy(int amount, boolean simulate) { return 0; }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public boolean canReceive() { return false; }
        @Override public boolean canExtract() { return false; }
    };
    private final NonNullList<ItemStack> legacyItems = NonNullList.withSize(27, ItemStack.EMPTY);
    private int processTicks;
    private Manufacturing.Batch activeBatch;
    private int status = STATUS_IDLE;

    static int forgeEnergyFromJoules(float joules) {
        return Math.max(0, Math.round(joules * micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler.TO_RF_RATIO));
    }

    public static boolean battery(ItemStack stack) {
        return !stack.isEmpty() && (stack.hasCapability(CapabilityEnergy.ENERGY, null)
                || stack.getItem() instanceof micdoodle8.mods.galacticraft.api.item.IItemElectric);
    }

    /** Drains an energy item in the battery slot into internal storage. */
    private void chargeFromBattery() {
        if (engine()) return;
        ItemStack battery = inventory.get(BATTERY_SLOT);
        if (battery.isEmpty()) return;
        if (battery.getItem() instanceof micdoodle8.mods.galacticraft.api.item.IItemElectric) {
            int room = Math.min(ENERGY_MAX_RECEIVE, energy.getMaxEnergyStored() - energy.getEnergyStored());
            if (room <= 0) return;
            micdoodle8.mods.galacticraft.api.item.IItemElectric electric =
                    (micdoodle8.mods.galacticraft.api.item.IItemElectric) battery.getItem();
            float limit = Math.min(electric.getMaxElectricityStored(battery),
                    room * micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler.RF_RATIO);
            int fe = Math.min(room, forgeEnergyFromJoules(electric.discharge(battery, limit, false)));
            if (fe <= 0) return;
            float taken = electric.discharge(battery, fe * micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler.RF_RATIO, true);
            int stored = Math.min(room, forgeEnergyFromJoules(taken));
            if (stored > 0 && energy.receiveEnergy(stored, false) > 0) markDirty();
            return;
        }
        if (!battery.hasCapability(CapabilityEnergy.ENERGY, null)) return;
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
        process(world.getBlockState(pos).getBlock().getRegistryName().getPath());
    }

    public boolean engine() {
        return world != null && "engine_station".equals(world.getBlockState(pos).getBlock().getRegistryName().getPath());
    }

    public int inputLimit() { return engine() ? 2 : INPUT_COUNT; }

    /** Same side as GS's component assembler: the player's left while facing the front. */
    static EnumFacing powerSide(EnumFacing front) { return front.rotateY(); }

    EnumFacing powerFace() {
        if (world == null) return null;
        IBlockState state = world.getBlockState(pos);
        if (!state.getPropertyKeys().contains(net.minecraft.block.BlockHorizontal.FACING)) return null;
        return powerSide(state.getValue(net.minecraft.block.BlockHorizontal.FACING));
    }

    void process(String id) {
        if (returnLegacyItem()) return;
        int limit = inputLimit();
        Manufacturing.Batch match = null;
        for (Manufacturing.Batch batch : Manufacturing.BATCHES) {
            if (!batch.machine.equals(id)) continue;
            int[] plan = batch.debit(this, limit);
            if (plan != null && outputFits(batch.output)) { match = batch; break; }
        }
        if (activeBatch != match) {
            if (activeBatch != null) processTicks = 0;
            activeBatch = match;
        }
        int oldStatus = status;
        boolean powered = engine() || energy.getEnergyStored() >= ENERGY_PER_TICK;
        if (match == null) {
            processTicks = 0;
            status = STATUS_IDLE;
        } else if (!powered) {
            status = STATUS_NO_POWER;
        } else {
            if (!engine()) energy.consume(ENERGY_PER_TICK);
            processTicks++;
            status = STATUS_WORKING;
            if (processTicks >= PROCESS_TIME) {
                // Re-validate inputs at completion time; abort quietly if they changed.
                int[] finalDebit = match.debit(this, limit);
                if (finalDebit != null && outputFits(match.output)) {
                    for (int slot = 0; slot < limit; slot++)
                        if (finalDebit[slot] > 0) decrStackSize(slot, finalDebit[slot]);
                    ItemStack existing = inventory.get(OUTPUT_SLOT);
                    if (existing.isEmpty()) inventory.set(OUTPUT_SLOT, match.output.copy());
                    else existing.grow(match.output.getCount());
                    if (world != null && match.output.getMetadata() == 7
                            && "galaxyspace:rocket_modules".equals(String.valueOf(match.output.getItem().getRegistryName()))) {
                        world.playEvent(2001, pos, net.minecraft.block.Block.getStateId(world.getBlockState(pos)));
                        world.playSound(null, pos, net.minecraft.init.SoundEvents.ENTITY_GENERIC_EXPLODE,
                                net.minecraft.util.SoundCategory.BLOCKS, .7F, 1.4F);
                        world.destroyBlock(pos, false);
                        return;
                    }
                }
                processTicks = 0;
            }
        }
        if (status == STATUS_WORKING || status != oldStatus) markDirty();
    }

    // ---- energy capability: this is what GC aluminium wire detects ----
    private boolean acceptsPower(EnumFacing side) {
        return !engine() && side == powerFace();
    }

    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap, EnumFacing side) {
        if (cap == CapabilityEnergy.ENERGY) return !engine() && (side == null || acceptsPower(side));
        return super.hasCapability(cap, side);
    }
    @Override @SuppressWarnings("unchecked")
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, EnumFacing side) {
        if (cap == CapabilityEnergy.ENERGY) {
            if (!engine() && side == null) return CapabilityEnergy.ENERGY.cast(energyView);
            if (acceptsPower(side)) return CapabilityEnergy.ENERGY.cast(energy);
            return null;
        }
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
        tag.setInteger("InventoryVersion", 1);
        NBTTagCompound legacy = new NBTTagCompound();
        ItemStackHelper.saveAllItems(legacy, legacyItems);
        tag.setTag("LegacyInventory", legacy);
        tag.setInteger("Energy", energy.getEnergyStored());
        tag.setInteger("ProcessTicks", processTicks);
        return tag;
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        inventory.clear();
        for (int i = 0; i < SIZE; i++) inventory.set(i, ItemStack.EMPTY);
        legacyItems.clear();
        if (!tag.hasKey("InventoryVersion") && !tag.hasKey("Energy")) {
            ItemStackHelper.loadAllItems(tag, legacyItems);
        } else {
            ItemStackHelper.loadAllItems(tag, inventory);
            ItemStackHelper.loadAllItems(tag.getCompoundTag("LegacyInventory"), legacyItems);
        }
        energy.setEnergy(tag.getInteger("Energy"));
        processTicks = tag.getInteger("ProcessTicks");
        activeBatch = null;
    }

    private boolean returnLegacyItem() {
        for (int i = 0; i < legacyItems.size(); i++) if (!legacyItems.get(i).isEmpty()) {
            if (inventory.get(OUTPUT_SLOT).isEmpty()) {
                inventory.set(OUTPUT_SLOT, legacyItems.get(i));
                legacyItems.set(i, ItemStack.EMPTY);
                markDirty();
            }
            status = STATUS_IDLE;
            processTicks = 0;
            return true;
        }
        return false;
    }

    void dropLegacyItems() {
        for (ItemStack stack : legacyItems) if (!stack.isEmpty())
            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        legacyItems.clear();
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
        if (i < inputLimit()) return true;
        return !engine() && i == BATTERY_SLOT && battery(s);
    }
    @Override public void clear() { inventory.clear(); markDirty(); }
    @Override public String getName() { return "tile.newearth." + (world == null ? "material_station" : world.getBlockState(pos).getBlock().getRegistryName().getPath()) + ".name"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public String getGuiID() { return NewEarth.MOD_ID + ":manufacturing"; }
    @Override public Container createContainer(InventoryPlayer playerInventory, EntityPlayer player) { return new ManufacturingContainer(playerInventory, this); }
    @Override public int[] getSlotsForFace(EnumFacing face) {
        if (face == EnumFacing.DOWN) return new int[] { OUTPUT_SLOT };
        int limit = inputLimit();
        int[] in = new int[limit];
        for (int i = 0; i < limit; i++) in[i] = i;
        return in;
    }
    @Override public boolean canInsertItem(int i, ItemStack s, EnumFacing face) { return i < inputLimit(); }
    @Override public boolean canExtractItem(int i, ItemStack s, EnumFacing face) { return i == OUTPUT_SLOT; }

    /** EnergyStorage with a setter so NBT and GUI sync can write back. */
    final class MachineEnergy extends EnergyStorage {
        MachineEnergy(int capacity, int maxReceive) { super(capacity, maxReceive, 0, 0); }
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (received > 0 && !simulate) markDirty();
            return received;
        }
        void consume(int amount) { this.energy -= amount; }
        void setEnergy(int e) { this.energy = Math.max(0, Math.min(e, getMaxEnergyStored())); }
    }
}
