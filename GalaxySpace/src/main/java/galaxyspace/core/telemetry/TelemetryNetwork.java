package galaxyspace.core.telemetry;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;

public final class TelemetryNetwork {
    private static SimpleNetworkWrapper channel;
    private TelemetryNetwork() { }
    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel("gslc_dimension");
        channel.registerMessage(Handler.class, Update.class, 0, Side.CLIENT);
    }
    public static void send(TileEntity tile, Entity entity, boolean tracked) {
        if (channel == null) return;
        Update packet = new Update(tile.getWorld().provider.getDimension(), tile.getPos(), tracked, entity);
        channel.sendToAllAround(packet, new NetworkRegistry.TargetPoint(packet.sourceDimension,
            tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ(), 320));
    }
    public static class Update implements IMessage {
        public int sourceDimension, targetDimension;
        public BlockPos pos;
        public boolean tracked, known;
        public String key = "", fallback = "";
        public Update() { }
        public Update(int source, BlockPos pos, boolean tracked, Entity entity) {
            this.sourceDimension = source; this.pos = pos; this.tracked = tracked;
            this.known = entity != null && !entity.isDead && entity.world != null;
            if (known) {
                targetDimension = entity.world.provider.getDimension();
                key = TelemetryTracking.dimensionKey(entity.world);
                fallback = entity.world.provider.getDimensionType().getName();
            }
        }
        @Override public void toBytes(ByteBuf out) {
            out.writeInt(sourceDimension); out.writeLong(pos.toLong());
            out.writeBoolean(tracked); out.writeBoolean(known); out.writeInt(targetDimension);
            ByteBufUtils.writeUTF8String(out, bounded(key)); ByteBufUtils.writeUTF8String(out, bounded(fallback));
        }
        @Override public void fromBytes(ByteBuf in) {
            sourceDimension = in.readInt(); pos = BlockPos.fromLong(in.readLong());
            tracked = in.readBoolean(); known = in.readBoolean(); targetDimension = in.readInt();
            key = bounded(ByteBufUtils.readUTF8String(in)); fallback = bounded(ByteBufUtils.readUTF8String(in));
        }
        private static String bounded(String value) { return value == null ? "" : value.substring(0, Math.min(256, value.length())); }
    }
    public static class Handler implements IMessageHandler<Update, IMessage> {
        @Override public IMessage onMessage(Update update, MessageContext context) {
            TelemetryAddon.proxy.receive(update);
            return null;
        }
    }
}
