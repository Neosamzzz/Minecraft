package newearth.storage;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** Per-open-workbench provenance. No item tags or persistent world data are added. */
public final class DrawerReturns {
    private static final Map<Container,Map<Integer,Cell>> LOANS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Container> UPDATING=Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<Container,Boolean>()));
    private static final class Loan {
        final DrawerTransfer.Source source;final ItemStack item;int count;
        Loan(DrawerTransfer.Withdrawal withdrawal){source=withdrawal.source;item=withdrawal.stack.copy();count=item.getCount();}
    }
    private static final class Cell {
        ItemStack seen;final List<Loan> loans=new ArrayList<>();
        Cell(ItemStack seen){this.seen=seen.copy();}
    }
    private static boolean same(ItemStack a,ItemStack b) {
        return !a.isEmpty()&&!b.isEmpty()&&ItemStack.areItemsEqual(a,b)&&ItemStack.areItemStackTagsEqual(a,b);
    }
    private static boolean supported(Container container) {
        // Other devices can keep processing or share a persistent grid between players after one GUI closes.
        return container.getClass().getName().equals("net.minecraft.inventory.ContainerWorkbench");
    }
    static void begin(Container container){observe(container);UPDATING.add(container);}
    static void end(Container container){UPDATING.remove(container);}
    static void committed(Container container,List<DrawerTransfer.Withdrawal> withdrawals) {
        if(!supported(container))return;
        Map<Integer,Cell> cells=LOANS.computeIfAbsent(container,c->new LinkedHashMap<>());
        for(Iterator<Map.Entry<Integer,Cell>> it=cells.entrySet().iterator();it.hasNext();) {
            Map.Entry<Integer,Cell> entry=it.next();ItemStack current=container.getSlot(entry.getKey()).getStack();
            if(!same(current,entry.getValue().seen))it.remove();else entry.getValue().seen=current.copy();
        }
        for(DrawerTransfer.Withdrawal withdrawal:withdrawals) {
            Cell cell=cells.computeIfAbsent(withdrawal.target,i->new Cell(container.getSlot(i).getStack()));
            cell.loans.add(new Loan(withdrawal));cell.seen=container.getSlot(withdrawal.target).getStack().copy();
        }
        if(cells.isEmpty())LOANS.remove(container);
    }
    public static void observe(Object raw) {
        if(!(raw instanceof Container))return;Container container=(Container)raw;
        if(UPDATING.contains(container))return;
        Map<Integer,Cell> cells=LOANS.get(container);if(cells==null)return;
        for(Iterator<Map.Entry<Integer,Cell>> it=cells.entrySet().iterator();it.hasNext();) {
            Map.Entry<Integer,Cell> entry=it.next();Cell cell=entry.getValue();ItemStack current=container.getSlot(entry.getKey()).getStack();
            if(!same(cell.seen,current)){it.remove();continue;}
            int removed=Math.max(0,cell.seen.getCount()-current.getCount());
            // Consume borrowed portions first, so later manually added stock cannot be mistaken for a refund.
            for(Loan loan:cell.loans){int count=Math.min(loan.count,removed);loan.count-=count;removed-=count;}
            cell.loans.removeIf(loan->loan.count==0);cell.seen=current.copy();if(cell.loans.isEmpty())it.remove();
        }
        if(cells.isEmpty())LOANS.remove(container);
    }
    public static void close(Object raw,Object rawPlayer) {
        if(!(raw instanceof Container)||!(rawPlayer instanceof EntityPlayer))return;
        EntityPlayer player=(EntityPlayer)rawPlayer;if(player.world.isRemote)return;
        returnRemaining((Container)raw,player,stack->{if(!player.inventory.addItemStackToInventory(stack)&&!stack.isEmpty())player.dropItem(stack,false);});
    }
    static void returnRemaining(Container container,EntityPlayer player,Consumer<ItemStack> fallback) {
        observe(container);Map<Integer,Cell> cells=LOANS.remove(container);if(cells==null)return;
        for(Map.Entry<Integer,Cell> entry:cells.entrySet()) {
            Slot slot=container.getSlot(entry.getKey());
            for(Loan loan:entry.getValue().loans) {
                ItemStack current=slot.getStack();if(!same(current,loan.item))break;
                ItemStack taken=slot.decrStackSize(Math.min(loan.count,current.getCount()));if(taken.isEmpty())continue;
                ItemStack rest;
                try {rest=DrawerNetwork.returnItems(loan.source,player,taken.copy());}
                catch(RuntimeException error){rest=taken;org.apache.logging.log4j.LogManager.getLogger("NewEarth").warn("Drawer leftover return rejected",error);}
                if(!rest.isEmpty())fallback.accept(rest);
            }
        }
    }
}
