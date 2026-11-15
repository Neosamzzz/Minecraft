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
        final IItemHandler handler; final int slot;
        public Source(IItemHandler handler, int slot) { this.handler = handler; this.slot = slot; }
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
                rollback(withdrawals,0,recovery); return "普通材料不足，或输入槽不接受该材料";
            }
            for (int i=0;i<inventory.length;i++) inventory[i]=working[i].copy();
            for (Map.Entry<Integer,ItemStack> entry:targets.entrySet()) container.getSlot(entry.getKey()).putStack(entry.getValue());
            return null;
        } catch (RuntimeException error) {
            for (int i=0;i<inventory.length;i++) inventory[i]=originalPlayer[i].copy();
            for (Map.Entry<Integer,ItemStack> entry:original.entrySet()) container.getSlot(entry.getKey()).putStack(entry.getValue());
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
                ItemStack peek=source.handler.extractItem(source.slot,remaining,true);
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
