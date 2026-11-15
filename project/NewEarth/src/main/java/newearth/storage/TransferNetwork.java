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
    public static void init() {
        CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("newearth_drawers");
        CHANNEL.registerMessage(Handler.class,Fill.class,0,Side.SERVER);
    }
    public static void fill(int window,String category,List<DrawerTransfer.Request> requests) {
        CHANNEL.sendToServer(new Fill(window,category,requests));
    }
    public static final class Fill implements IMessage {
        public int window;
        public String category;
        public List<DrawerTransfer.Request> requests=new ArrayList<>();
        public boolean valid;
        public Fill() { }
        Fill(int window,String category,List<DrawerTransfer.Request> requests) {
            this.window=window;this.category=category;this.requests=requests;
        }
        public void toBytes(ByteBuf buf) {
            buf.writeInt(window);ByteBufUtils.writeUTF8String(buf,category);buf.writeByte(requests.size());
            for (DrawerTransfer.Request request:requests) {
                buf.writeByte(request.slot);buf.writeByte(request.alternatives.size());
                for (ItemStack stack:request.alternatives) ByteBufUtils.writeItemStack(buf,stack);
            }
        }
        public void fromBytes(ByteBuf buf) {
            if (buf.readableBytes()>65536) return;
            try {
                window=buf.readInt();category=ByteBufUtils.readUTF8String(buf);
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
            String error=DrawerTransfer.transfer(container,message.requests,inventory,sources,stack -> {
                if (!player.inventory.addItemStackToInventory(stack) && !stack.isEmpty()) player.dropItem(stack,false);
            });
            if (error!=null) player.sendStatusMessage(new TextComponentString(error),true);
            else for (int i=0;i<36;i++) player.inventory.setInventorySlotContents(i,inventory[i]);
            player.inventory.markDirty();container.detectAndSendChanges();player.updateHeldItem();
        } catch (IllegalArgumentException error) {
            player.sendStatusMessage(new TextComponentString(error.getMessage()),true);
        } catch (RuntimeException error) {
            player.sendStatusMessage(new TextComponentString("取料未完成，已保留或退还材料"),true);
            org.apache.logging.log4j.LogManager.getLogger("NewEarth").warn("Drawer recipe transfer failed",error);
        }
    }
}
