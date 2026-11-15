package newearth.storage;

import java.util.*;
import java.util.zip.ZipFile;
import java.nio.file.*;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
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
        for(IItemHandler handler:handlers)for(int i=0;i<handler.getSlots();i++)sources.add(new DrawerTransfer.Source(handler,i));
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

        AliasDrawer compact=new AliasDrawer(10);bench=new Bench(2);inventory=player();
        check(run(bench,inventory,Arrays.asList(request(0,new ItemStack(Blocks.IRON_BLOCK,1)),request(1,new ItemStack(Items.IRON_INGOT,2))),compact)!=null,
                "Compacting views cannot spend the same underlying stock twice");
        check(compact.units==10&&bench.input.isEmpty(),"Compacting rollback restores the original units");
        check(run(bench,inventory,Arrays.asList(request(0,new ItemStack(Blocks.IRON_BLOCK,1)),request(1,new ItemStack(Items.IRON_INGOT,1))),compact)==null
                &&compact.units==0,"Compacting mixed denominations work when enough stock exists");

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
        System.out.println("Drawer recipe transfer: "+checks+" checks passed (isolated; no game or save modified)");
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
    private static final class AliasDrawer implements IItemHandler {
        int units;AliasDrawer(int units){this.units=units;}
        public int getSlots(){return 2;}
        public ItemStack getStackInSlot(int slot){int count=units/(slot==0?9:1);return count==0?ItemStack.EMPTY:slot==0?new ItemStack(Blocks.IRON_BLOCK,count):new ItemStack(Items.IRON_INGOT,count);}
        public int getSlotLimit(int slot){return 64;}
        public ItemStack extractItem(int slot,int count,boolean simulate){int n=Math.min(count,units/(slot==0?9:1));if(n==0)return ItemStack.EMPTY;if(!simulate)units-=n*(slot==0?9:1);return slot==0?new ItemStack(Blocks.IRON_BLOCK,n):new ItemStack(Items.IRON_INGOT,n);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){if(!simulate)units+=stack.getCount()*(slot==0?9:1);return ItemStack.EMPTY;}
    }
    private static void installedJei()throws Exception {
        String directory=System.getenv("NEWEARTH_RUNTIME_MODS");if(directory==null){System.out.println("Set NEWEARTH_RUNTIME_MODS for installed JEI checks");return;}
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
            }
        }
        check(found,"Installed JEI binary was checked");
    }
}
