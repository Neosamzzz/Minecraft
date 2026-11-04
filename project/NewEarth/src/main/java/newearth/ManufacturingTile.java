package newearth;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.*;

/** Two input rows and one output row, using the standard chest container. */
public final class ManufacturingTile extends TileEntityLockable implements ITickable, ISidedInventory {
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(27, ItemStack.EMPTY);
    private int ticks;
    @Override public void update() {
        if (world == null || world.isRemote || ++ticks % 20 != 0) return;
        String id = world.getBlockState(pos).getBlock().getRegistryName().getPath();
        process(id);
    }
    boolean process(String id) {
        for (Manufacturing.Batch batch : Manufacturing.BATCHES) if (batch.machine.equals(id)) {
            int[] debit = batch.debit(this, 18);
            if (debit == null) continue;
            for (int i = 18; i < 27; i++) {
                ItemStack existing = inventory.get(i), result = batch.output;
                if (!existing.isEmpty() && (!ItemStack.areItemsEqual(existing, result)
                        || !ItemStack.areItemStackTagsEqual(existing, result)
                        || existing.getCount() + result.getCount() > existing.getMaxStackSize())) continue;
                for (int slot = 0; slot < 18; slot++) if (debit[slot] > 0) decrStackSize(slot, debit[slot]);
                if (existing.isEmpty()) inventory.set(i, result.copy()); else existing.grow(result.getCount());
                markDirty(); return true;
            }
        }
        return false;
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) { super.writeToNBT(tag); ItemStackHelper.saveAllItems(tag, inventory); return tag; }
    @Override public void readFromNBT(NBTTagCompound tag) { super.readFromNBT(tag); inventory.clear(); for(int i=0;i<27;i++) inventory.set(i, ItemStack.EMPTY); ItemStackHelper.loadAllItems(tag, inventory); }
    @Override public int getSizeInventory() { return 27; }
    @Override public boolean isEmpty() { for(ItemStack s:inventory) if(!s.isEmpty()) return false; return true; }
    @Override public ItemStack getStackInSlot(int i) { return inventory.get(i); }
    @Override public ItemStack decrStackSize(int i,int n) { ItemStack s=ItemStackHelper.getAndSplit(inventory,i,n); if(!s.isEmpty()) markDirty(); return s; }
    @Override public ItemStack removeStackFromSlot(int i) { ItemStack s=ItemStackHelper.getAndRemove(inventory,i); markDirty(); return s; }
    @Override public void setInventorySlotContents(int i,ItemStack s) { inventory.set(i,s); markDirty(); }
    @Override public int getInventoryStackLimit() { return 64; }
    @Override public boolean isUsableByPlayer(EntityPlayer player) { return world.getTileEntity(pos)==this && player.getDistanceSq(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64; }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    @Override public boolean isItemValidForSlot(int i,ItemStack s) { return i<18; }
    @Override public int getField(int i) { return 0; }
    @Override public void setField(int i,int n) { }
    @Override public int getFieldCount() { return 0; }
    @Override public void clear() { inventory.clear(); markDirty(); }
    @Override public String getName() { return "tile.newearth."+(world==null?"material_station":world.getBlockState(pos).getBlock().getRegistryName().getPath())+".name"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public String getGuiID() { return "minecraft:chest"; }
    @Override public Container createContainer(InventoryPlayer playerInventory,EntityPlayer player) { return new ContainerChest(playerInventory,this,player); }
    @Override public int[] getSlotsForFace(EnumFacing face) { return face==EnumFacing.DOWN?new int[]{18,19,20,21,22,23,24,25,26}:new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17}; }
    @Override public boolean canInsertItem(int i,ItemStack s,EnumFacing face) { return i<18; }
    @Override public boolean canExtractItem(int i,ItemStack s,EnumFacing face) { return i>=18; }
}
