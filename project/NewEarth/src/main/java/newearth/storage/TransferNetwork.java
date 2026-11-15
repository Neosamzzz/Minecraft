package newearth.storage;

import io.netty.buffer.ByteBuf;
import java.util.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

public final class TransferNetwork {
    private static SimpleNetworkWrapper CHANNEL;
    @net.minecraftforge.fml.common.SidedProxy(modId="newearth",clientSide="newearth.client.DrawerTransferJei$Bridge",
            serverSide="newearth.storage.TransferNetwork$ClientBridge")
    public static ClientBridge client;
    public static class ClientBridge {
        public void receive(Endpoint message) { }
        public void receive(PreviewResult message) { }
    }
    public static void init() {
        CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("newearth_drawers");
        CHANNEL.registerMessage(Handler.class,Fill.class,0,Side.SERVER);
        CHANNEL.registerMessage(QueryHandler.class,Query.class,1,Side.SERVER);
        CHANNEL.registerMessage(EndpointHandler.class,Endpoint.class,2,Side.CLIENT);
        CHANNEL.registerMessage(PreviewHandler.class,PreviewResult.class,3,Side.CLIENT);
    }
    public static void query(int window,int token) { CHANNEL.sendToServer(new Query(window,token)); }
    public static class Query implements IMessage {
        public int window,token;
        public Query() { }
        public Query(int window,int token) { this.window=window;this.token=token; }
        public void toBytes(ByteBuf buf) { buf.writeInt(window);buf.writeInt(token); }
        public void fromBytes(ByteBuf buf) { window=buf.readInt();token=buf.readInt(); }
    }
    public static final class Endpoint extends Query {
        public boolean connected;
        public Endpoint() { }
        public Endpoint(int window,int token,boolean connected) { super(window,token);this.connected=connected; }
        public void toBytes(ByteBuf buf) { super.toBytes(buf);buf.writeBoolean(connected); }
        public void fromBytes(ByteBuf buf) { super.fromBytes(buf);connected=buf.readBoolean(); }
    }
    /** A reply belongs to one opening, even when Minecraft reuses a window ID. */
    public static final class EndpointState {
        public final int window,token;
        public Boolean connected;
        public long lastQuery;
        public EndpointState(int window,int token) { this.window=window;this.token=token; }
        public boolean accept(Endpoint message) {
            if(message.window!=window || message.token!=token)return false;
            connected=message.connected;return true;
        }
    }
    public static final class QueryHandler implements IMessageHandler<Query,IMessage> {
        public IMessage onMessage(Query message,MessageContext ctx) {
            EntityPlayerMP player=ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Container container=player.openContainer;
                if(container==null || container.windowId!=message.window || player.isDead || player.isSpectator()
                        || !container.canInteractWith(player))return;
                boolean supported=false;
                for(TransferProfiles.Profile profile:TransferProfiles.all())
                    if(TransferProfiles.get(container,profile.category)!=null){supported=true;break;}
                if(!supported)return;
                net.minecraft.util.math.BlockPos anchor=TransferProfiles.anchor(container);
                if(anchor==null || player.getDistanceSq(anchor)>64 || !player.world.isBlockLoaded(anchor))return;
                CHANNEL.sendTo(new Endpoint(message.window,message.token,DrawerNetwork.hasEndpoint(player.world,anchor)),player);
            });
            return null;
        }
    }
    public static final class EndpointHandler implements IMessageHandler<Endpoint,IMessage> {
        public IMessage onMessage(Endpoint message,MessageContext ctx) { client.receive(message);return null; }
    }
    public static final class PreviewResult extends Query {
        public String error;
        public PreviewResult() { }
        public PreviewResult(int window,int token,String error){super(window,token);this.error=error;}
        public void toBytes(ByteBuf buf){super.toBytes(buf);buf.writeBoolean(error!=null);if(error!=null)ByteBufUtils.writeUTF8String(buf,error);}
        public void fromBytes(ByteBuf buf){super.fromBytes(buf);error=buf.readBoolean()?ByteBufUtils.readUTF8String(buf):null;}
    }
    public static final class PreviewHandler implements IMessageHandler<PreviewResult,IMessage> {
        public IMessage onMessage(PreviewResult message,MessageContext ctx){client.receive(message);return null;}
    }
    public static final class PreviewState {
        public final int window,token;
        public final long requestedAt;
        public long receivedAt;
        public boolean ready;
        public String error;
        public PreviewState(int window,int token,long requestedAt){this.window=window;this.token=token;this.requestedAt=requestedAt;}
        public boolean accept(PreviewResult result) {
            if(result.window!=window||result.token!=token)return false;
            ready=true;error=result.error;receivedAt=System.nanoTime();return true;
        }
    }
    public static void preview(int window,int token,String category,List<DrawerTransfer.Request> requests) {
        Fill message=new Fill(window,category,requests);message.preview=true;message.token=token;CHANNEL.sendToServer(message);
    }
    public static void fill(int window,String category,List<DrawerTransfer.Request> requests) {
        CHANNEL.sendToServer(new Fill(window,category,requests));
    }
    public static final class Fill implements IMessage {
        public int window,token;
        public String category;
        public List<DrawerTransfer.Request> requests=new ArrayList<>();
        public boolean valid,preview;
        public Fill() { }
        Fill(int window,String category,List<DrawerTransfer.Request> requests) {
            this.window=window;this.category=category;this.requests=requests;
        }
        public void toBytes(ByteBuf buf) {
            buf.writeInt(window);buf.writeBoolean(preview);buf.writeInt(token);ByteBufUtils.writeUTF8String(buf,category);buf.writeByte(requests.size());
            for (DrawerTransfer.Request request:requests) {
                buf.writeByte(request.slot);buf.writeByte(request.alternatives.size());
                for (ItemStack stack:request.alternatives) ByteBufUtils.writeItemStack(buf,stack);
            }
        }
        public void fromBytes(ByteBuf buf) {
            if (buf.readableBytes()>65536) return;
            try {
                window=buf.readInt();preview=buf.readBoolean();token=buf.readInt();category=ByteBufUtils.readUTF8String(buf);
                if (category.length()>128) return;
                int count=buf.readUnsignedByte();if (count<1 || count>32) return;
                for (int i=0;i<count;i++) {
                    int slot=buf.readUnsignedByte(),choices=buf.readUnsignedByte();
                    if (choices>128) return;
                    List<ItemStack> alternatives=new ArrayList<>();
                    for (int j=0;j<choices;j++) {
                        ItemStack stack=ByteBufUtils.readItemStack(buf);
                        if (stack.isEmpty() || stack.getCount()<1 || stack.getCount()>64) return;
                        alternatives.add(stack);
                    }
                    requests.add(new DrawerTransfer.Request(slot,alternatives));
                }
                valid=!buf.isReadable();
            } catch (RuntimeException ignored) { valid=false; }
        }
    }
    public static final class Handler implements IMessageHandler<Fill,IMessage> {
        public IMessage onMessage(Fill message,MessageContext ctx) {
            if (!message.valid) return null;
            EntityPlayerMP player=ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> apply(player,message));return null;
        }
    }
    static boolean allowed(Container container,Fill message) {
        TransferProfiles.Profile profile=TransferProfiles.get(container,message.category);
        if (profile==null) return false;
        Set<Integer> allowed=new HashSet<>(profile.slots(container).values()),seen=new HashSet<>();
        for (DrawerTransfer.Request request:message.requests)
            if (!allowed.contains(request.slot) || !seen.add(request.slot)) return false;
        return !seen.isEmpty();
    }
    private static void apply(EntityPlayerMP player,Fill message) {
        Container container=player.openContainer;
        if (player.isSpectator() || player.isDead || container==null || container.windowId!=message.window
                || !container.canInteractWith(player) || !player.inventory.getItemStack().isEmpty() || !allowed(container,message)) return;
        try {
            net.minecraft.util.math.BlockPos anchor=TransferProfiles.anchor(container);
            if (anchor==null || player.getDistanceSq(anchor)>64 || !player.world.isBlockLoaded(anchor)) return;
            net.minecraft.tileentity.TileEntity controller=DrawerNetwork.controller(player.world,anchor);
            if(controller==null&&DrawerNetwork.hasEndpoint(player.world,anchor))
                throw new IllegalArgumentException("传动方块尚未连接有效的抽屉控制器");
            List<DrawerTransfer.Source> sources=DrawerNetwork.sources(player,controller);
            ItemStack[] inventory=new ItemStack[36];
            for (int i=0;i<36;i++) inventory[i]=player.inventory.getStackInSlot(i).copy();
            if(message.preview) {
                CHANNEL.sendTo(new PreviewResult(message.window,message.token,
                        DrawerTransfer.preview(container,message.requests,inventory,sources)),player);return;
            }
            String error=DrawerTransfer.transfer(container,message.requests,inventory,sources,stack -> {
                if (!player.inventory.addItemStackToInventory(stack) && !stack.isEmpty()) player.dropItem(stack,false);
            });
            if (error!=null) player.sendStatusMessage(new TextComponentString(error),true);
            else for (int i=0;i<36;i++) player.inventory.setInventorySlotContents(i,inventory[i]);
            player.inventory.markDirty();container.detectAndSendChanges();player.updateHeldItem();
        } catch (IllegalArgumentException error) {
            failure(player,message,error.getMessage());
        } catch (RuntimeException error) {
            failure(player,message,message.preview?"取料检查未完成，请稍后重试":"取料未完成，已保留或退还材料");
            org.apache.logging.log4j.LogManager.getLogger("NewEarth").warn("Drawer recipe transfer failed",error);
        }
    }
    private static void failure(EntityPlayerMP player,Fill message,String error) {
        if(message.preview)CHANNEL.sendTo(new PreviewResult(message.window,message.token,error),player);
        else player.sendStatusMessage(new TextComponentString(error),true);
    }
}
