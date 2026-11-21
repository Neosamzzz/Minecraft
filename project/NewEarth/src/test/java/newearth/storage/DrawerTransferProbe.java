package newearth.storage;

import java.util.*;
import java.util.zip.ZipFile;
import java.nio.file.*;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.*;
import newearth.DrawerJeiTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Allocation, rollback and installed JEI bytecode regressions; no game instance or world is opened. */
public final class DrawerTransferProbe {
    private static int checks;
    private static void check(boolean value,String label) { if(!value)throw new AssertionError(label);checks++; }
    private static ItemStack stone(int n) { return new ItemStack(Blocks.STONE,n); }
    private static ItemStack[] player() { ItemStack[] values=new ItemStack[36];Arrays.fill(values,ItemStack.EMPTY);return values; }
    private static final class Bench extends Container {
        final InventoryBasic input;
        Bench(int count) { input=new InventoryBasic("probe",false,count);for(int i=0;i<count;i++)addSlotToContainer(new Slot(input,i,0,0)); }
        public boolean canInteractWith(EntityPlayer player){return true;}
    }
    private static DrawerTransfer.Request request(int slot,ItemStack... values) { return new DrawerTransfer.Request(slot,Arrays.asList(values)); }
    private static String run(Bench bench,ItemStack[] player,List<DrawerTransfer.Request> requests,IItemHandler... handlers) {
        List<DrawerTransfer.Source> sources=new ArrayList<>();
        for(IItemHandler handler:handlers) {
            if(handler instanceof AliasDrawer)sources.addAll(DrawerTransfer.reservedSources(handler,0,new int[]{1,9}));
            else for(int i=0;i<handler.getSlots();i++)sources.add(new DrawerTransfer.Source(handler,i));
        }
        return DrawerTransfer.transfer(bench,requests,player,sources,stack->{throw new AssertionError("Unexpected recovery "+stack);});
    }
    public static void main(String[] args)throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard=new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment",true);Bootstrap.register();
        Bench bench=new Bench(2);ItemStack[] inventory=player();inventory[0]=stone(3);
        ItemStackHandler drawers=new ItemStackHandler(1);drawers.setStackInSlot(0,stone(10));
        check(run(bench,inventory,Arrays.asList(request(0,stone(4)),request(1,stone(2))),drawers)==null,"Backpack and drawers fill one batch");
        check(inventory[0].isEmpty()&&drawers.getStackInSlot(0).getCount()==7,"Backpack is consumed first and repeated ingredients are counted once");
        check(bench.input.getStackInSlot(0).getCount()==4&&bench.input.getStackInSlot(1).getCount()==2,"Recipe counts retained");
        check(run(bench,inventory,Arrays.asList(request(0,stone(4)),request(1,stone(2))),drawers)==null,"Repeated plus keeps existing complete inputs");
        check(drawers.getStackInSlot(0).getCount()==7,"No continuous restocking");

        bench=new Bench(2);inventory=player();inventory[0]=stone(1);drawers.setStackInSlot(0,stone(2));
        check(run(bench,inventory,Arrays.asList(request(0,stone(2)),request(1,stone(2))),drawers)!=null,"Insufficient material fails");
        check(inventory[0].getCount()==1&&drawers.getStackInSlot(0).getCount()==2,"Failure rolls back prior drawer withdrawals and leaves backpack unchanged");
        check(bench.input.isEmpty(),"Failed allocation never touches destination");

        bench=new Bench(1);inventory=player();for(int i=0;i<36;i++)inventory[i]=new ItemStack(Blocks.DIRT,64);
        bench.input.setInventorySlotContents(0,new ItemStack(Blocks.COBBLESTONE,12));
        check(run(bench,inventory,Arrays.asList(request(0,stone(1))),drawers)!=null,"Full backpack cannot discard old input");
        check(bench.input.getStackInSlot(0).getCount()==12&&drawers.getStackInSlot(0).getCount()==2,"No loss with full backpack");

        bench=new Bench(3);inventory=player();inventory[0]=stone(1);inventory[1]=new ItemStack(Blocks.DIRT);inventory[2]=new ItemStack(Blocks.COBBLESTONE);
        check(run(bench,inventory,Arrays.asList(request(0,stone(1),new ItemStack(Blocks.DIRT)),
                request(1,stone(1),new ItemStack(Blocks.COBBLESTONE)),request(2,stone(1),new ItemStack(Blocks.COBBLESTONE))))==null,
                "Overlapping alternatives backtrack instead of rejecting available materials");
        check(bench.input.getStackInSlot(0).getItem()==Item.getItemFromBlock(Blocks.DIRT),"Constrained matching chooses the viable combination");

        bench=new Bench(1);inventory=player();bench.input.setInventorySlotContents(0,new ItemStack(Blocks.DIRT,4));inventory[0]=stone(1);
        check(run(bench,inventory,Arrays.asList(request(0,stone(1))))==null,"Changing recipe returns old ordinary input to backpack");
        check(Arrays.stream(inventory).anyMatch(s->!s.isEmpty()&&s.getItem()==Item.getItemFromBlock(Blocks.DIRT)&&s.getCount()==4),"Old input retained");
        check(run(bench,inventory,Arrays.asList(request(0)))==null&&bench.input.isEmpty(),"Recipe holes clear only mapped ordinary cells");

        bench=new Bench(1);inventory=player();ItemStack tagged=stone(2);tagged.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        tagged.getTagCompound().setString("marker","kept");inventory[0]=tagged;
        check(run(bench,inventory,Arrays.asList(request(0,stone(1))))==null,"Untagged recipe accepts concrete tagged stock");
        check("kept".equals(bench.input.getStackInSlot(0).getTagCompound().getString("marker")),"Source NBT survives transfer");
        ItemStack wrong=stone(1);wrong.setTagCompound(new net.minecraft.nbt.NBTTagCompound());wrong.getTagCompound().setString("marker","other");
        check(run(new Bench(1),inventory,Arrays.asList(request(0,wrong)))!=null,"Required NBT is enforced");

        bench=new Bench(1);inventory=player();ItemStack engine=new ItemStack(new Item().setRegistryName("newearth:probe_engine"));inventory[0]=engine;
        check(run(bench,inventory,Arrays.asList(request(0,engine)))!=null&&inventory[0]==engine,"Engine requests do not move special items");
        check(run(new Bench(1),player(),Arrays.asList(request(0,stone(1)),request(0,stone(1))))!=null,"Duplicate destination rejected");
        check(run(new Bench(1),player(),Arrays.asList(request(9,stone(1))))!=null,"Out-of-bounds destination rejected");

        AliasDrawer compact=new AliasDrawer(19);bench=new Bench(2);inventory=player();
        check(run(bench,inventory,Arrays.asList(request(0,new ItemStack(Blocks.IRON_BLOCK,1)),request(1,new ItemStack(Items.IRON_INGOT,2))),compact)!=null,
                "Compacting views cannot spend the same underlying stock twice");
        check(compact.units==19&&bench.input.isEmpty(),"Compacting rollback restores the original units and reserve");
        check(run(bench,inventory,Arrays.asList(request(0,new ItemStack(Blocks.IRON_BLOCK,1)),request(1,new ItemStack(Items.IRON_INGOT,1))),compact)==null
                &&compact.units==9,"Compacting mixed denominations preserve one of the largest stocked form");

        bench=new Bench(1);inventory=player();drawers.setStackInSlot(0,stone(64));
        check(run(bench,inventory,Arrays.asList(request(0,stone(63))),drawers)==null&&drawers.getStackInSlot(0).getCount()==1,
                "A drawer with 64 can supply 63 and retains its final item");
        check(run(new Bench(1),player(),Arrays.asList(request(0,stone(1))),drawers)!=null&&drawers.getStackInSlot(0).getCount()==1,
                "The final drawer item is unavailable to a later transfer");
        inventory=player();inventory[0]=stone(1);bench=new Bench(1);
        check(run(bench,inventory,Arrays.asList(request(0,stone(1))),drawers)==null&&inventory[0].isEmpty()
                &&drawers.getStackInSlot(0).getCount()==1,"Backpack can empty while the drawer reserve remains");
        ItemStackHandler separate=new ItemStackHandler(2);separate.setStackInSlot(0,stone(2));separate.setStackInSlot(1,stone(2));
        check(run(new Bench(1),player(),Arrays.asList(request(0,stone(3))),separate)!=null
                &&separate.getStackInSlot(0).getCount()==2&&separate.getStackInSlot(1).getCount()==2,
                "Each independent drawer cell keeps its own final item and failed attempts roll back");
        check(run(new Bench(1),player(),Arrays.asList(request(0,stone(2))),separate)==null
                &&separate.getStackInSlot(0).getCount()==1&&separate.getStackInSlot(1).getCount()==1,
                "Surplus from multiple cells is combined without consuming either reserve");
        compact=new AliasDrawer(8);
        check(run(new Bench(1),player(),Arrays.asList(request(0,new ItemStack(Items.IRON_INGOT,7))),compact)==null&&compact.units==1,
                "A compacting drawer below one block keeps one ingot instead of reserving a nonexistent block");
        previews();
        maximumTransfers();

        TransferNetwork.Fill message=new TransferNetwork.Fill(7,"minecraft.crafting",Arrays.asList(request(1,stone(3))));
        io.netty.buffer.ByteBuf buffer=Unpooled.buffer();message.toBytes(buffer);TransferNetwork.Fill decoded=new TransferNetwork.Fill();decoded.fromBytes(buffer);
        check(decoded.valid&&decoded.window==7&&decoded.requests.get(0).alternatives.get(0).getCount()==3,"Transfer packet roundtrip");buffer.release();
        buffer=Unpooled.buffer();message.toBytes(buffer);buffer.writeByte(1);decoded=new TransferNetwork.Fill();decoded.fromBytes(buffer);
        check(!decoded.valid,"Trailing packet payload rejected");buffer.release();
        buffer=Unpooled.buffer(65537);buffer.writeZero(65537);decoded=new TransferNetwork.Fill();decoded.fromBytes(buffer);
        check(!decoded.valid,"Oversized packet rejected");buffer.release();
        check(!TransferNetwork.allowed(new Bench(1),message),"Unsupported container cannot use network requests");
        for(TransferProfiles.Profile profile:TransferProfiles.all()) {
            check(profile.ingredients.length==profile.targets.length,"Complete slot map: "+profile.container);
            Set<Integer> unique=new HashSet<>();for(int slot:profile.targets)check(unique.add(slot),"Unique allowed slot "+profile.container+":"+slot);
            if(profile.container.endsWith("ContainerRocketAssembler"))check(!unique.contains(0)&&!unique.contains(1)&&!unique.contains(5)&&!unique.contains(10),"GS power, output, engine and blueprint excluded");
            if(profile.container.equals("newearth.AssemblyStationContainer"))check(!unique.contains(7)&&!unique.contains(8)&&!unique.contains(9)&&!unique.contains(10),"NewEarth engines, panel and output excluded");
        }
        nasaContainers();
        check(Class.forName("newearth.client.DrawerTransferJei$Bridge").newInstance() instanceof TransferNetwork.ClientBridge,
                "Client network proxy loads without optional JEI classes on the test runtime");
        installedJei();
        manualStackTake();
        System.out.println("Drawer recipe transfer: "+checks+" checks passed (isolated; no game or save modified)");
    }
    private static void maximumTransfers() {
        Bench bench=new Bench(5);ItemStack[] inventory=player();
        inventory[0]=new ItemStack(Items.IRON_INGOT,20);inventory[1]=new ItemStack(Items.STICK,8);
        List<DrawerTransfer.Request> pickaxe=Arrays.asList(request(0,new ItemStack(Items.IRON_INGOT)),
                request(1,new ItemStack(Items.IRON_INGOT)),request(2,new ItemStack(Items.IRON_INGOT)),
                request(3,new ItemStack(Items.STICK)),request(4,new ItemStack(Items.STICK)));
        check(DrawerTransfer.maximumBatches(bench,pickaxe,inventory,Collections.emptyList())==4,"Shift is limited by the scarcest complete recipe ingredient");
        check(inventory[0].getCount()==20&&inventory[1].getCount()==8&&bench.input.isEmpty(),"Searching the maximum never changes real inventories");
        check(DrawerTransfer.transferMaximum(bench,pickaxe,inventory,Collections.emptyList(),s->{throw new AssertionError();})==null,
                "Shift commits the selected batch");
        for(int i=0;i<5;i++)check(bench.input.getStackInSlot(i).getCount()==4,"Every mapped input receives four complete sets");
        check(inventory[0].getCount()==8&&inventory[1].isEmpty(),"Shift consumes 12 iron and 8 sticks, keeping surplus iron");

        ItemStackHandler drawers=new ItemStackHandler(2);drawers.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,20));
        drawers.setStackInSlot(1,new ItemStack(Items.STICK,9));
        List<DrawerTransfer.Source> sources=Arrays.asList(new DrawerTransfer.Source(drawers,0),new DrawerTransfer.Source(drawers,1));
        bench=new Bench(5);inventory=player();
        check(DrawerTransfer.transferMaximum(bench,pickaxe,inventory,sources,s->{throw new AssertionError();})==null
                &&drawers.getStackInSlot(0).getCount()==8&&drawers.getStackInSlot(1).getCount()==1,
                "Shift combines repeated drawer ingredients while keeping each cell's final item");

        bench=new Bench(1);inventory=player();inventory[0]=stone(64);inventory[1]=stone(64);
        check(DrawerTransfer.transferMaximum(bench,Arrays.asList(request(0,stone(1))),inventory,Collections.emptyList(),s->{throw new AssertionError();})==null
                &&bench.input.getStackInSlot(0).getCount()==64&&inventory[1].getCount()==64,"Shift respects the normal stack limit");
        bench=new Bench(1);inventory=player();inventory[0]=stone(64);
        check(DrawerTransfer.transferMaximum(bench,Arrays.asList(request(0,stone(1),new ItemStack(Blocks.DIRT,2))),inventory,Collections.emptyList(),s->{throw new AssertionError();})==null
                &&bench.input.getStackInSlot(0).getCount()==64,"An oversized unused alternative does not block a valid maximum batch");
        bench=new Bench(1);bench.input.setInventorySlotContents(0,new ItemStack(Blocks.DIRT));inventory=player();inventory[0]=stone(64);
        check(DrawerTransfer.transferMaximum(bench,Arrays.asList(request(0,new ItemStack(Blocks.DIRT,2),stone(1))),inventory,Collections.emptyList(),s->{throw new AssertionError();})==null
                &&bench.input.getStackInSlot(0).getCount()==64,"A feasible large batch is found even when smaller batches keep an insufficient old alternative");

        bench=new Bench(1);inventory=player();inventory[0]=new ItemStack(Items.WATER_BUCKET);
        check(DrawerTransfer.maximumBatches(bench,Arrays.asList(request(0,new ItemStack(Items.WATER_BUCKET))),inventory,Collections.emptyList())==1,
                "Nonstackable ingredients limit Shift to one batch");
        bench=new Bench(1);bench.input.setInventorySlotContents(0,stone(2));inventory=player();inventory[0]=stone(3);
        check(DrawerTransfer.transferMaximum(bench,Arrays.asList(request(0,stone(1))),inventory,Collections.emptyList(),s->{throw new AssertionError();})==null
                &&bench.input.getStackInSlot(0).getCount()==5&&inventory[0].isEmpty(),"Shift includes material already in the crafting input");

        bench=new Bench(1);bench.inventorySlots.set(0,new Slot(bench.input,0,0,0){@Override public int getSlotStackLimit(){return 16;}});
        inventory=player();inventory[0]=stone(40);
        check(DrawerTransfer.transferMaximum(bench,Arrays.asList(request(0,stone(2))),inventory,Collections.emptyList(),s->{throw new AssertionError();})==null
                &&bench.input.getStackInSlot(0).getCount()==16&&inventory[0].getCount()==24,"Machine slot limits and per-batch counts constrain Shift");
        AliasDrawer compact=new AliasDrawer(19);bench=new Bench(2);
        sources=DrawerTransfer.reservedSources(compact,0,new int[]{1,9});
        check(DrawerTransfer.transferMaximum(bench,Arrays.asList(request(0,new ItemStack(Blocks.IRON_BLOCK)),request(1,new ItemStack(Items.IRON_INGOT))),
                player(),sources,s->{throw new AssertionError();})==null&&compact.units==9,"Shift respects the shared compacting reserve");
        drawers=new ItemStackHandler(1);drawers.setStackInSlot(0,stone(1));bench=new Bench(1);sources=Arrays.asList(new DrawerTransfer.Source(drawers,0));
        check(DrawerTransfer.transferMaximum(bench,Arrays.asList(request(0,stone(1))),player(),sources,s->{throw new AssertionError();})!=null
                &&drawers.getStackInSlot(0).getCount()==1&&bench.input.isEmpty(),"Shift cannot consume the final reserved item when no batch is possible");
        TransferNetwork.Fill packet=new TransferNetwork.Fill(4,"minecraft.crafting",Arrays.asList(request(1,stone(1))));packet.maximum=true;
        io.netty.buffer.ByteBuf buffer=Unpooled.buffer();packet.toBytes(buffer);TransferNetwork.Fill decoded=new TransferNetwork.Fill();decoded.fromBytes(buffer);buffer.release();
        check(decoded.valid&&decoded.maximum&&!decoded.preview,"The Shift flag survives the client/server transfer packet");
    }
    private static void previews() {
        IItemHandler readOnly=new ItemStackHandler(1) {
            {setStackInSlot(0,stone(64));}
            @Override public ItemStack extractItem(int slot,int amount,boolean simulate) {
                if(!simulate)throw new AssertionError("Preview extracted from real storage");
                return super.extractItem(slot,amount,true);
            }
            @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) {
                throw new AssertionError("Preview inserted into real storage");
            }
        };
        Bench bench=new Bench(1);ItemStack[] inventory=player();
        List<DrawerTransfer.Source> sources=Arrays.asList(new DrawerTransfer.Source(readOnly,0));
        check(DrawerTransfer.preview(bench,Arrays.asList(request(0,stone(63))),inventory,sources)==null,"Preview enables plus for available surplus");
        check(DrawerTransfer.preview(bench,Arrays.asList(request(0,stone(64))),inventory,sources)!=null,"Preview disables plus when the final item is needed");
        check(bench.input.isEmpty()&&readOnly.getStackInSlot(0).getCount()==64,"Preview leaves real drawer and crafting inputs untouched");
        inventory[0]=stone(1);
        check(DrawerTransfer.preview(bench,Arrays.asList(request(0,stone(64))),inventory,sources)==null&&inventory[0].getCount()==1,
                "Preview combines backpack and surplus without consuming the backpack");
        AliasDrawer compact=new AliasDrawer(19);bench=new Bench(2);
        sources=DrawerTransfer.reservedSources(compact,0,new int[]{1,9});
        check(DrawerTransfer.preview(bench,Arrays.asList(request(0,new ItemStack(Blocks.IRON_BLOCK)),request(1,new ItemStack(Items.IRON_INGOT,2))),player(),sources)!=null,
                "Preview accounts for shared compacting stock and its reserved block");
        check(DrawerTransfer.preview(bench,Arrays.asList(request(0,new ItemStack(Blocks.IRON_BLOCK)),request(1,new ItemStack(Items.IRON_INGOT))),player(),sources)==null,
                "Preview accepts viable mixed denominations");
        check(compact.units==19&&bench.input.isEmpty(),"Compacting preview never modifies the real shared pool");
        TransferNetwork.PreviewState state=new TransferNetwork.PreviewState(2,30,0);
        check(!state.ready,"Plus stays disabled before the initial server preview");
        check(!state.accept(new TransferNetwork.PreviewResult(2,29,null))&&!state.ready,"Stale recipe reply cannot enable plus");
        check(!state.accept(new TransferNetwork.PreviewResult(3,30,null))&&!state.ready,"Other window reply cannot enable plus");
        List<Integer> missing=new ArrayList<>();
        List<DrawerTransfer.Request> recipe=Arrays.asList(request(0,stone(63)),request(1,stone(1)),request(2,new ItemStack(Items.STICK)));
        check(DrawerTransfer.preview(new Bench(3),recipe,player(),Arrays.asList(new DrawerTransfer.Source(readOnly,0)),missing)!=null
                &&missing.equals(Arrays.asList(1,2)),"Missing slots share available stock, exclude the reserved item, and include independent shortages");
        check(readOnly.getStackInSlot(0).getCount()==64,"Highlight diagnostics do not change the drawer's inventory");
        TransferNetwork.PreviewResult result=new TransferNetwork.PreviewResult(2,30,"missing",missing);
        io.netty.buffer.ByteBuf buffer=Unpooled.buffer();
        result.toBytes(buffer);
        TransferNetwork.PreviewResult decoded=new TransferNetwork.PreviewResult();
        decoded.fromBytes(buffer);
        buffer.release();
        check(!state.accept(new TransferNetwork.PreviewResult(2,29,"missing",missing))&&state.missingSlots.isEmpty(),
                "An outdated result cannot highlight another preview");
        check(state.accept(decoded)&&state.ready&&state.error!=null&&state.missingSlots.equals(missing),
                "Reported slots survive the packet and keep plus disabled");
        check(state.accept(new TransferNetwork.PreviewResult(2,30,null))&&state.ready&&state.error==null&&state.missingSlots.isEmpty(),
                "A successful preview enables plus and clears all highlights");
    }
    private static void nasaContainers()throws Exception {
        java.lang.reflect.Field unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe=(sun.misc.Unsafe)unsafeField.get(null);
        net.minecraft.world.World world=(net.minecraft.world.World)unsafe.allocateInstance(net.minecraft.world.WorldServer.class);
        java.lang.reflect.Field remote=net.minecraft.world.World.class.getDeclaredField("isRemote");remote.setAccessible(true);remote.setBoolean(world,true);
        EntityPlayer player=(EntityPlayer)unsafe.allocateInstance(net.minecraft.entity.player.EntityPlayerMP.class);
        player.world=world;player.inventory=new net.minecraft.entity.player.InventoryPlayer(player);
        net.minecraft.util.math.BlockPos origin=new net.minecraft.util.math.BlockPos(2,64,3);
        Container workbench=new ContainerWorkbench(player.inventory,world,origin);
        returns(player,world,origin);
        check(origin.equals(TransferProfiles.anchor(workbench)),"Vanilla anchor resolves without a client-provided position");
        net.minecraft.client.gui.inventory.GuiCrafting gui=new net.minecraft.client.gui.inventory.GuiCrafting(player.inventory,world);
        check(net.minecraft.util.math.BlockPos.ORIGIN.equals(TransferProfiles.anchor(gui.inventorySlots)),
                "Actual vanilla client GUI loses the real workbench position");
        check(!origin.equals(TransferProfiles.anchor(gui.inventorySlots)),"Client anchor must not decide drawer connectivity");
        TransferNetwork.EndpointState state=new TransferNetwork.EndpointState(1,20);
        check(state.connected==null,"Connection is unknown until the authoritative reply arrives");
        TransferNetwork.Endpoint response=new TransferNetwork.Endpoint(1,20,true);
        io.netty.buffer.ByteBuf endpointBuffer=Unpooled.buffer();response.toBytes(endpointBuffer);
        TransferNetwork.Endpoint decoded=new TransferNetwork.Endpoint();decoded.fromBytes(endpointBuffer);endpointBuffer.release();
        check(state.accept(decoded)&&state.connected,"Server endpoint reply enables drawer transfer despite client origin");
        check(!state.accept(new TransferNetwork.Endpoint(2,20,false))&&state.connected,"Other window reply cannot change connection");
        check(!state.accept(new TransferNetwork.Endpoint(1,19,false))&&state.connected,"Reused window ignores a prior opening reply");
        check(state.accept(new TransferNetwork.Endpoint(1,20,false))&&!state.connected,"No server endpoint restores the original handler");
        TransferNetwork.Fill valid=new TransferNetwork.Fill(1,"minecraft.crafting",Arrays.asList(request(1,stone(1))));
        check(TransferNetwork.allowed(workbench,valid),"Actual vanilla input is allowed");
        check(!TransferNetwork.allowed(workbench,new TransferNetwork.Fill(1,"minecraft.crafting",Arrays.asList(request(0,stone(1))))),"Forged output target rejected");
        check(!TransferNetwork.allowed(workbench,new TransferNetwork.Fill(1,"minecraft.crafting",Arrays.asList(request(10,stone(1))))),"Forged player inventory target rejected");
        check(!TransferNetwork.allowed(workbench,new TransferNetwork.Fill(1,"minecraft.crafting",Arrays.asList(request(1,stone(1)),request(1,stone(1))))),"Duplicate network targets rejected");
        check(!TransferNetwork.allowed(workbench,new TransferNetwork.Fill(1,"wrong.category",Arrays.asList(request(1,stone(1))))),"Forged recipe category rejected");
        for(TransferProfiles.Profile profile:TransferProfiles.all())if(profile.nasa) {
            Class<?> type=Class.forName(profile.container);Container container;
            try { container=(Container)type.getConstructor(net.minecraft.entity.player.InventoryPlayer.class,
                    net.minecraft.util.math.BlockPos.class).newInstance(player.inventory,origin); }
            catch(NoSuchMethodException missing) { container=(Container)type.getConstructor(net.minecraft.entity.player.InventoryPlayer.class,
                    net.minecraft.util.math.BlockPos.class,EntityPlayer.class).newInstance(player.inventory,origin,player); }
            Map<Integer,Integer> slots=profile.slots(container);
            check(slots.size()==profile.targets.length,"Actual NASA container slot count "+profile.container);
            int index=0;
            for(int target:slots.values())check(container.getSlot(target).getSlotIndex()==profile.targets[index++],"Actual NASA inventory index "+profile.container);
            check(origin.equals(TransferProfiles.anchor(container)),"NASA anchor comes from the actual workbench slot");
        }
    }
    private static void returns(EntityPlayer player,net.minecraft.world.World world,net.minecraft.util.math.BlockPos origin)throws Exception {
        ContainerWorkbench bench=new ContainerWorkbench(player.inventory,world,origin);
        ItemStackHandler drawer=new ItemStackHandler(1);drawer.setStackInSlot(0,stone(11));
        List<DrawerTransfer.Source> sources=Arrays.asList(new DrawerTransfer.Source(drawer,0));
        List<DrawerTransfer.Request> recipe=Arrays.asList(request(1,stone(5)));
        check(DrawerTransfer.transfer(bench,recipe,player(),sources,s->{throw new AssertionError();})==null,"Return test fills a real vanilla workbench");
        DrawerReturns.returnRemaining(bench,null,s->{throw new AssertionError("Unexpected fallback");});
        check(drawer.getStackInSlot(0).getCount()==11&&bench.craftMatrix.getStackInSlot(0).isEmpty(),"Closing without taking output returns all borrowed input");
        DrawerReturns.returnRemaining(bench,null,s->{throw new AssertionError();});
        check(drawer.getStackInSlot(0).getCount()==11,"A repeated close cannot duplicate a refund");

        bench=new ContainerWorkbench(player.inventory,world,origin);
        check(DrawerTransfer.transfer(bench,recipe,player(),sources,s->{throw new AssertionError();})==null,"Refill for consumed remainder test");
        bench.craftMatrix.decrStackSize(0,2);DrawerReturns.observe(bench);
        DrawerReturns.returnRemaining(bench,null,s->{throw new AssertionError();});
        check(drawer.getStackInSlot(0).getCount()==9&&bench.craftMatrix.isEmpty(),"Closing after crafting returns only the three unconsumed items");

        drawer.setStackInSlot(0,stone(11));bench=new ContainerWorkbench(player.inventory,world,origin);ItemStack[] backpack=player();backpack[0]=stone(2);
        check(DrawerTransfer.transfer(bench,recipe,backpack,sources,s->{throw new AssertionError();})==null,"Mixed backpack and drawer fill");
        DrawerReturns.returnRemaining(bench,null,s->{throw new AssertionError();});
        check(drawer.getStackInSlot(0).getCount()==11&&bench.craftMatrix.getStackInSlot(0).getCount()==2,"Closing leaves the backpack-owned part in the grid for vanilla handling");

        bench=new ContainerWorkbench(player.inventory,world,origin);backpack=player();
        check(DrawerTransfer.transfer(bench,recipe,backpack,sources,s->{throw new AssertionError();})==null,"Refill before manual movement");
        bench.craftMatrix.decrStackSize(0,5);DrawerReturns.observe(bench);
        bench.craftMatrix.setInventorySlotContents(0,stone(5));DrawerReturns.observe(bench);
        DrawerReturns.returnRemaining(bench,null,s->{throw new AssertionError();});
        check(drawer.getStackInSlot(0).getCount()==6&&bench.craftMatrix.getStackInSlot(0).getCount()==5,"Taken materials are no longer borrowed and manual replacement is not refunded");

        drawer.setStackInSlot(0,stone(11));bench=new ContainerWorkbench(player.inventory,world,origin);
        check(DrawerTransfer.transfer(bench,Arrays.asList(request(1,stone(3))),player(),sources,s->{throw new AssertionError();})==null,"First fill for repeated transfer");
        check(DrawerTransfer.transfer(bench,recipe,player(),sources,s->{throw new AssertionError();})==null,"Second fill retains earlier provenance");
        DrawerReturns.returnRemaining(bench,null,s->{throw new AssertionError();});
        check(drawer.getStackInSlot(0).getCount()==11&&bench.craftMatrix.isEmpty(),"Repeated plus returns both loans once");

        bench=new ContainerWorkbench(player.inventory,world,origin);drawer.setStackInSlot(0,stone(63));
        check(DrawerTransfer.transfer(bench,recipe,player(),sources,s->{throw new AssertionError();})==null,"Fill before source capacity changes");
        drawer.setStackInSlot(0,stone(62));List<ItemStack> fallback=new ArrayList<>();
        DrawerReturns.returnRemaining(bench,null,s->fallback.add(s.copy()));
        check(drawer.getStackInSlot(0).getCount()==64&&fallback.size()==1&&fallback.get(0).getCount()==3&&bench.craftMatrix.isEmpty(),
                "Partial refunds return rejected surplus to backpack without losing or duplicating it");
        drawer.setStackInSlot(0,stone(11));bench=new ContainerWorkbench(player.inventory,world,origin);
        check(DrawerTransfer.transfer(bench,recipe,player(),sources,s->{throw new AssertionError();})==null,"Fill before original drawer removal");
        sources.get(0).origin=new net.minecraft.tileentity.TileEntity(){};sources.get(0).origin.setWorld(world);sources.get(0).origin.invalidate();
        fallback.clear();DrawerReturns.returnRemaining(bench,player,s->fallback.add(s.copy()));
        check(drawer.getStackInSlot(0).getCount()==6&&fallback.size()==1&&fallback.get(0).getCount()==5,
                "A removed original tile never receives items through its stale handler; all leftovers fall back");
        sources.get(0).origin=null;
        Bench machine=new Bench(1);drawer.setStackInSlot(0,stone(11));
        check(DrawerTransfer.transfer(machine,Arrays.asList(request(0,stone(5))),player(),sources,s->{throw new AssertionError();})==null,"Fill an ordinary persistent device");
        DrawerReturns.returnRemaining(machine,null,s->{throw new AssertionError();});
        check(machine.input.getStackInSlot(0).getCount()==5&&drawer.getStackInSlot(0).getCount()==6,"Closing other devices does not remove their processing inputs");

        java.io.InputStream input=Container.class.getResourceAsStream("/net/minecraft/inventory/Container.class");
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
        while((count=input.read(buffer))!=-1)out.write(buffer,0,count);input.close();byte[] bytes=out.toByteArray();
        newearth.DrawerReturnTransformer transformer=new newearth.DrawerReturnTransformer();
        byte[] transformed=transformer.transform("net.minecraft.inventory.Container","net.minecraft.inventory.Container",bytes);
        check(transformed!=bytes,"Actual Forge Container lifecycle receives the return hooks");
        check(transformer.transform("net.minecraft.inventory.Container","net.minecraft.inventory.Container",transformed)==transformed,"Container hooks are idempotent");
        ClassNode node=new ClassNode();new ClassReader(transformed).accept(node,0);int closeHooks=0,observeHooks=0;
        for(MethodNode method:node.methods) {
            if((method.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);
            for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode) {
                MethodInsnNode call=(MethodInsnNode)instruction;
                if(call.owner.equals("newearth/storage/DrawerReturns")){if(call.name.equals("close"))closeHooks++;else observeHooks++;}
            }
        }
        check(closeHooks==1&&observeHooks>=3,"Close hook runs once and interactions observe before and after changes");
    }
    private static final class AliasDrawer implements IItemHandler {
        int units;AliasDrawer(int units){this.units=units;}
        public int getSlots(){return 2;}
        public ItemStack getStackInSlot(int slot){int count=units/(slot==0?9:1);return count==0?ItemStack.EMPTY:slot==0?new ItemStack(Blocks.IRON_BLOCK,count):new ItemStack(Items.IRON_INGOT,count);}
        public int getSlotLimit(int slot){return 64;}
        public ItemStack extractItem(int slot,int count,boolean simulate){int n=Math.min(count,units/(slot==0?9:1));if(n==0)return ItemStack.EMPTY;if(!simulate)units-=n*(slot==0?9:1);return slot==0?new ItemStack(Blocks.IRON_BLOCK,n):new ItemStack(Items.IRON_INGOT,n);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(!simulate)units+=stack.getCount()*(slot==0?9:1);return ItemStack.EMPTY;}
    }
    private static void manualStackTake() throws Exception {
        check(DrawerTransfer.manualStackAmount(64, 80) == 64, "Bulk withdrawal leaves stock above one stack alone");
        check(DrawerTransfer.manualStackAmount(64, 64) == 63, "A full stack leaves one item");
        check(DrawerTransfer.manualStackAmount(64, 2) == 1, "A partial stack leaves one item");
        check(DrawerTransfer.manualStackAmount(64, 1) == 64, "Native extraction can take the final item");
        check(DrawerTransfer.manualStackAmount(16, 16) == 15, "The selected item's stack limit is honored");
        check(DrawerTransfer.manualStackAmount(1, 2) == 1, "Nonstackable items still take one at a time");

        String directory = System.getenv("NEWEARTH_RUNTIME_MODS");
        if (directory == null) return;
        String target = "com.jaquadro.minecraft.storagedrawers.block.BlockDrawers";
        String entryName = target.replace('.', '/') + ".class";
        byte[] original = null;
        try (java.util.stream.Stream<Path> files = Files.list(Paths.get(directory))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".jar"))::iterator) {
                try (ZipFile jar = new ZipFile(file.toFile())) {
                    java.util.zip.ZipEntry entry = jar.getEntry(entryName);
                    if (entry == null) continue;
                    check(original == null, "Only one Storage Drawers JAR is installed");
                    try (java.io.InputStream input = jar.getInputStream(entry)) {
                        original = com.google.common.io.ByteStreams.toByteArray(input);
                    }
                }
            }
        }
        check(original != null, "Found the installed Storage Drawers manual extraction class");
        newearth.DrawerManualTakeTransformer transformer = new newearth.DrawerManualTakeTransformer();
        byte[] patched = transformer.transform(target, target, original);
        check(patched != original, "Patched the installed manual extraction class");
        check(transformer.transform(target, target, patched) == patched, "Repeated transformation does not add another adjustment");
        ClassNode node = new ClassNode();
        new ClassReader(patched).accept(node, 0);
        int bulk = 0, single = 0;
        for (MethodNode method : node.methods) {
            if (!"extractItem".equals(method.name)) continue;
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name, method);
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode) || !"takeItemsFromSlot".equals(((MethodInsnNode) instruction).name)) continue;
                AbstractInsnNode amount = instruction.getPrevious();
                while (amount != null && amount.getOpcode() < 0) amount = amount.getPrevious();
                if (amount instanceof MethodInsnNode && "manualStackAmount".equals(((MethodInsnNode) amount).name)) bulk++;
                if (amount != null && amount.getOpcode() == Opcodes.ICONST_1) single++;
            }
        }
        check(bulk == 1 && single == 1, "Only bulk extraction is adjusted, and single-item extraction remains native");
    }

    private static void installedJei()throws Exception {
        String directory=System.getenv("NEWEARTH_RUNTIME_MODS");if(directory==null){System.out.println("Set NEWEARTH_RUNTIME_MODS for installed JEI checks");return;}
        Path instance=Paths.get(directory).getParent();Path vanilla=instance.resolve(instance.getFileName()+".jar");
        try(ZipFile jar=new ZipFile(vanilla.toFile());java.io.InputStream input=jar.getInputStream(jar.getEntry("afr.class"))) {
            byte[] raw=com.google.common.io.ByteStreams.toByteArray(input);
            newearth.DrawerReturnTransformer transformer=new newearth.DrawerReturnTransformer();
            byte[] patched=transformer.transform("afr","net.minecraft.inventory.Container",raw);
            check(patched!=raw,"Actual installed obfuscated Container receives lifecycle hooks before remapping");
            check(transformer.transform("afr","net.minecraft.inventory.Container",patched)==patched,"Obfuscated lifecycle hooks are idempotent");
            ClassNode node=new ClassNode();new ClassReader(patched).accept(node,0);int closes=0,observers=0;
            for(MethodNode method:node.methods) {
                if((method.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);
                for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode) {
                    MethodInsnNode call=(MethodInsnNode)instruction;
                    if(call.owner.equals("newearth/storage/DrawerReturns")){if(call.name.equals("close"))closes++;else observers++;}
                }
            }
            check(closes==1&&observers>=3,"Production obfuscated hooks target exactly the player close and interaction methods");
        }
        boolean found=false;
        try(java.util.stream.Stream<Path> files=Files.list(Paths.get(directory))) {
            for(Path file:(Iterable<Path>)files.filter(p->p.toString().endsWith(".jar"))::iterator)try(ZipFile jar=new ZipFile(file.toFile())){
                java.util.zip.ZipEntry entry=jar.getEntry("mezz/jei/recipes/RecipeRegistry.class");if(entry==null)continue;
                check(!found,"Only one JEI installed");found=true;
                byte[] bytes;try(java.io.InputStream input=jar.getInputStream(entry)){
                    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
                    while((n=input.read(buf))!=-1)out.write(buf,0,n);bytes=out.toByteArray();
                }
                DrawerJeiTransformer transformer=new DrawerJeiTransformer();
                byte[] result=transformer.transform("mezz.jei.recipes.RecipeRegistry","mezz.jei.recipes.RecipeRegistry",bytes);
                check(result!=bytes,"Installed JEI transfer selection wrapped");
                check(transformer.transform("mezz.jei.recipes.RecipeRegistry","mezz.jei.recipes.RecipeRegistry",result)==result,"JEI wrapper is idempotent");
                ClassNode node=new ClassNode();new ClassReader(result).accept(node,0);int hooks=0;
                for(MethodNode method:node.methods){
                    if((method.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);
                    for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode&&((MethodInsnNode)instruction).owner.equals("newearth/client/DrawerTransferJei"))hooks++;
                }
                check(hooks>0,"Every actual JEI handler return can reach the integration");
                java.util.zip.ZipEntry factory = jar.getEntry("mezz/jei/plugins/vanilla/ingredients/item/ItemStackListFactory.class");
                if (factory == null) factory = jar.getEntry("mezz/jei/plugins/vanilla/ingredients/ItemStackListFactory.class");
                check(factory != null, "Installed JEI has a supported item list factory");
                String target = factory.getName().replace('/', '.').replace(".class", "");
                try (java.io.InputStream input = jar.getInputStream(factory)) { bytes = com.google.common.io.ByteStreams.toByteArray(input); }
                result = transformer.transform(target, target, bytes);
                check(bytes != result && transformer.transform(target, target, result) == result, "Installed JEI catalogue hook is applied once");
                node = new ClassNode();
                new ClassReader(result).accept(node, 0);
                hooks = 0;
                for (MethodNode method : node.methods) if (method.name.equals("create")) {
                    new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name, method);
                    for (AbstractInsnNode instruction : method.instructions.toArray())
                        if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("newearth/ItemCatalogue")) hooks++;
                }
                check(hooks == 1, "The NewEarth ordering is applied to the actual JEI creation return");
            }
        }
        check(found,"Installed JEI binary was checked");
    }
}
