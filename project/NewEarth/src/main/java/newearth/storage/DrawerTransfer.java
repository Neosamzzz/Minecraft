package newearth.storage;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

/** Server-thread transaction. Drawer withdrawals are reversible; target/player slots change only after allocation. */
public final class DrawerTransfer {
    public static final class Request {
        public final int slot;
        public final List<ItemStack> alternatives;
        public Request(int slot, List<ItemStack> alternatives) { this.slot = slot; this.alternatives = alternatives; }
    }
    public static final class Source {
        final IItemHandler handler; final int slot, reserve, unit;
        public Source(IItemHandler handler, int slot) { this(handler,slot,1,0); }
        private Source(IItemHandler handler,int slot,int reserve,int unit) { this.handler=handler;this.slot=slot;this.reserve=reserve;this.unit=unit; }
        private ItemStack peek(int amount) {
            int stored=handler.extractItem(slot,Integer.MAX_VALUE,true).getCount();
            int available=Math.max(0,stored-reserve);
            return available==0?ItemStack.EMPTY:handler.extractItem(slot,Math.min(amount,available),true);
        }
    }
    /** Rates use Storage Drawers' items-per-most-compressed-item convention; ordinary cells use 1. */
    static List<Source> reservedSources(IItemHandler handler,int first,int[] rates) {
        int smallestRate=Integer.MAX_VALUE,maxRate=0;boolean fractional=false;
        for(int rate:rates){maxRate=Math.max(maxRate,rate);fractional|=rate>1;}
        for(int i=0;i<rates.length;i++)if(rates[i]>0&&!handler.extractItem(first+i,1,true).isEmpty())
            smallestRate=Math.min(smallestRate,rates[i]);
        List<Source> sources=new ArrayList<>();
        for(int i=0;i<rates.length;i++) {
            // Preserve one of the largest currently stocked compacting forms across all aliases.
            int reserve=smallestRate==Integer.MAX_VALUE?1:(int)Math.max(1,((long)rates[i]+smallestRate-1)/smallestRate);
            sources.add(new Source(handler,first+i,reserve,fractional&&rates[i]>0?maxRate/rates[i]:0));
        }
        return sources;
    }
    private static final class Withdrawal {
        final Source source; final ItemStack stack;
        Withdrawal(Source source, ItemStack stack) { this.source = source; this.stack = stack; }
    }
    private static boolean exact(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && ItemStack.areItemsEqual(a,b) && ItemStack.areItemStackTagsEqual(a,b);
    }
    static boolean matches(ItemStack wanted, ItemStack stack) {
        return !stack.isEmpty() && OreDictionary.itemMatches(wanted, stack, false)
                && (!wanted.hasTagCompound() || ItemStack.areItemStackTagsEqual(wanted, stack));
    }
    private static ItemStack[] copy(ItemStack[] values) {
        ItemStack[] result = new ItemStack[values.length];
        for (int i=0;i<values.length;i++) result[i]=values[i].copy(); return result;
    }
    private static boolean put(ItemStack[] inventory, ItemStack value) {
        ItemStack stack = value.copy();
        for (int phase=0;phase<2;phase++) for (int i=0;i<inventory.length && !stack.isEmpty();i++) {
            ItemStack old=inventory[i];
            if (phase==0 ? !exact(old,stack) : !old.isEmpty()) continue;
            int amount=Math.min(stack.getCount(),Math.min(64,stack.getMaxStackSize())-(old.isEmpty()?0:old.getCount()));
            if (amount<=0) continue;
            if (old.isEmpty()) { inventory[i]=stack.copy(); inventory[i].setCount(amount); }
            else old.grow(amount);
            stack.shrink(amount);
        }
        return stack.isEmpty();
    }
    public static String transfer(Container container, List<Request> requests, ItemStack[] inventory,
            List<Source> sources, Consumer<ItemStack> recovery) {
        return transfer(container,requests,inventory,sources,recovery,true);
    }
    /** All allocation and rollback run on snapshots; previews never extract from real drawers or write slots. */
    public static String preview(Container container,List<Request> requests,ItemStack[] inventory,List<Source> sources) {
        Map<IItemHandler,SnapshotHandler> snapshots=new IdentityHashMap<>();List<Source> copied=new ArrayList<>();
        for(Source source:sources) {
            SnapshotHandler handler=snapshots.get(source.handler);
            if(handler==null){handler=new SnapshotHandler(source.handler,sources);snapshots.put(source.handler,handler);}
            copied.add(new Source(handler,source.slot,source.reserve,source.unit));
        }
        return transfer(container,requests,inventory,copied,stack->{throw new IllegalStateException("Snapshot rollback failed");},false);
    }
    private static final class SnapshotHandler implements IItemHandler {
        final ItemStack[] prototypes;final long[] counts;final int[] units;long shared;boolean fractional;
        SnapshotHandler(IItemHandler original,List<Source> sources) {
            prototypes=new ItemStack[original.getSlots()];Arrays.fill(prototypes,ItemStack.EMPTY);
            counts=new long[prototypes.length];units=new int[prototypes.length];
            for(Source source:sources)if(source.handler==original) {
                ItemStack stack=original.extractItem(source.slot,Integer.MAX_VALUE,true).copy();
                prototypes[source.slot]=stack;counts[source.slot]=stack.getCount();units[source.slot]=source.unit;
                if(source.unit>0){fractional=true;shared=Math.max(shared,counts[source.slot]*source.unit);}
            }
        }
        public int getSlots(){return prototypes.length;}
        public int getSlotLimit(int slot){return Integer.MAX_VALUE;}
        public ItemStack getStackInSlot(int slot) {
            long count=fractional&&units[slot]>0?shared/units[slot]:counts[slot];
            if(count<=0)return ItemStack.EMPTY;
            ItemStack stack=prototypes[slot].copy();stack.setCount((int)Math.min(Integer.MAX_VALUE,count));return stack;
        }
        public ItemStack extractItem(int slot,int amount,boolean simulate) {
            ItemStack stack=getStackInSlot(slot);if(stack.isEmpty()||amount<=0)return ItemStack.EMPTY;
            stack.setCount(Math.min(stack.getCount(),amount));
            if(!simulate){if(fractional&&units[slot]>0)shared-=(long)stack.getCount()*units[slot];else counts[slot]-=stack.getCount();}
            return stack;
        }
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
            if(!exact(prototypes[slot],stack))return stack;
            if(!simulate){if(fractional&&units[slot]>0)shared+=(long)stack.getCount()*units[slot];else counts[slot]+=stack.getCount();}
            return ItemStack.EMPTY;
        }
    }
    private static String transfer(Container container,List<Request> requests,ItemStack[] inventory,
            List<Source> sources,Consumer<ItemStack> recovery,boolean apply) {
        ItemStack[] originalPlayer=copy(inventory), working=copy(inventory);
        Map<Integer,ItemStack> original=new LinkedHashMap<>(), targets=new LinkedHashMap<>();
        List<Withdrawal> withdrawals=new ArrayList<>();
        try {
            for (Request request:requests) {
                if (request.slot<0 || request.slot>=container.inventorySlots.size() || original.containsKey(request.slot))
                    return "无效的输入槽位";
                Slot slot=container.getSlot(request.slot); ItemStack old=slot.getStack();
                if (TransferProfiles.special(old)) return "特殊输入请手动放置";
                for (ItemStack wanted:request.alternatives)
                    if (wanted.isEmpty() || wanted.getCount()<1 || wanted.getCount()>64 || TransferProfiles.special(wanted))
                        return "特殊输入请手动放置";
                original.put(request.slot,old.copy());
                boolean keep=false;
                for (ItemStack wanted:request.alternatives) if (matches(wanted,old)) { keep=true;break; }
                if (!old.isEmpty() && !keep && !put(working,old)) return "背包没有空间收回原料，请先清理合成格";
                targets.put(request.slot,keep?old.copy():ItemStack.EMPTY);
            }
            // Constrained slots go first; DFS handles overlapping alternatives (e.g. any planks plus oak only).
            List<Request> order=new ArrayList<>(requests);
            order.sort(Comparator.comparingInt(r -> r.alternatives.size()));
            int[] budget={512};
            if (!allocate(container,order,0,working,targets,sources,withdrawals,recovery,budget)) {
                rollback(withdrawals,0,recovery); return "普通材料不足（抽屉保留最后一个），或输入槽不接受该材料";
            }
            if(apply) {
                for (int i=0;i<inventory.length;i++) inventory[i]=working[i].copy();
                for (Map.Entry<Integer,ItemStack> entry:targets.entrySet()) container.getSlot(entry.getKey()).putStack(entry.getValue());
            }
            return null;
        } catch (RuntimeException error) {
            for (int i=0;i<inventory.length;i++) inventory[i]=originalPlayer[i].copy();
            if(apply)for (Map.Entry<Integer,ItemStack> entry:original.entrySet()) container.getSlot(entry.getKey()).putStack(entry.getValue());
            rollback(withdrawals,0,recovery);
            throw error;
        }
    }
    private static boolean allocate(Container container,List<Request> order,int index,ItemStack[] inventory,
            Map<Integer,ItemStack> targets,List<Source> sources,List<Withdrawal> withdrawals,Consumer<ItemStack> recovery,int[] budget) {
        if (index==order.size()) return true;
        if (--budget[0]<0) return false;
        Request request=order.get(index); Slot slot=container.getSlot(request.slot);
        ItemStack old=targets.get(request.slot);
        if (request.alternatives.isEmpty()) return allocate(container,order,index+1,inventory,targets,sources,withdrawals,recovery,budget);
        List<ItemStack> candidates=new ArrayList<>();
        if (!old.isEmpty()) candidates.add(old.copy());
        for (ItemStack stack:inventory) if (!stack.isEmpty()) candidates.add(stack.copy());
        for (Source source:sources) {
            ItemStack stack=source.handler.getStackInSlot(source.slot);
            if (!stack.isEmpty()) candidates.add(stack.copy());
        }
        List<ItemStack> tried=new ArrayList<>();
        for (ItemStack candidate:candidates) {
            if (TransferProfiles.special(candidate) || !slot.isItemValid(candidate)) continue;
            boolean duplicate=false; for (ItemStack previous:tried) duplicate |= exact(previous,candidate);
            if (duplicate) continue; tried.add(candidate.copy());
            if (!old.isEmpty() && !exact(old,candidate)) continue;
            int required=-1;
            for (ItemStack wanted:request.alternatives) if (matches(wanted,candidate)) {
                required=wanted.getCount();break;
            }
            if (required<0 || required>Math.min(slot.getItemStackLimit(candidate),candidate.getMaxStackSize())) continue;
            int remaining=Math.max(0,required-(old.isEmpty()?0:old.getCount()));
            ItemStack[] saved=copy(inventory); int mark=withdrawals.size();
            for (ItemStack stack:inventory) if (exact(stack,candidate) && remaining>0) {
                int count=Math.min(remaining,stack.getCount()); stack.shrink(count); remaining-=count;
            }
            for (Source source:sources) if (remaining>0) {
                ItemStack peek=source.peek(remaining);
                if (!exact(peek,candidate)) continue;
                ItemStack taken=source.handler.extractItem(source.slot,Math.min(remaining,peek.getCount()),false);
                if (taken.isEmpty()) continue;
                withdrawals.add(new Withdrawal(source,taken.copy()));
                if (!exact(taken,candidate) || taken.getCount()>remaining) { remaining=-1;break; }
                remaining-=taken.getCount();
            }
            if (remaining==0) {
                ItemStack value=candidate.copy(); value.setCount(Math.max(required,old.isEmpty()?0:old.getCount()));
                targets.put(request.slot,value);
                if (allocate(container,order,index+1,inventory,targets,sources,withdrawals,recovery,budget)) return true;
            }
            rollback(withdrawals,mark,recovery);
            System.arraycopy(saved,0,inventory,0,inventory.length); targets.put(request.slot,old);
        }
        return false;
    }
    private static void rollback(List<Withdrawal> withdrawals,int mark,Consumer<ItemStack> recovery) {
        boolean failed=false;
        for (int i=withdrawals.size()-1;i>=mark;i--) {
            Withdrawal withdrawal=withdrawals.remove(i);
            ItemStack rest=withdrawal.source.handler.insertItem(withdrawal.source.slot,withdrawal.stack.copy(),false);
            if (!rest.isEmpty()) { recovery.accept(rest); failed=true; }
        }
        if (failed) throw new IllegalStateException("抽屉拒绝返还材料，剩余材料已退还玩家");
    }
}
