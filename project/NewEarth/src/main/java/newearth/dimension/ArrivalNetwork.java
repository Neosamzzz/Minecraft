package newearth.dimension;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.SidedProxy;

public final class ArrivalNetwork {
    public static final int POEM = 0, WAIT = 1, RELEASE = 2;
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("newearth_arrival");
    @SidedProxy(modId = "newearth", clientSide = "newearth.client.ArrivalClient", serverSide = "newearth.dimension.ArrivalNetwork$ClientBridge")
    public static ClientBridge client;

    public static class ClientBridge {
        public void receive(State state) { }
    }

    public static void init() {
        CHANNEL.registerMessage(StateHandler.class, State.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(ReadyHandler.class, Ready.class, 1, Side.SERVER);
    }

    public static void send(EntityPlayerMP player, String session, int stage, BlockPos landing) {
        CHANNEL.sendTo(new State(session, player.dimension, stage, landing), player);
    }

    public static void ready(String session, boolean region) { CHANNEL.sendToServer(new Ready(session, region)); }

    public static class State implements IMessage {
        public String session;
        public int dimension, stage;
        public BlockPos landing;
        public State() { }
        State(String session, int dimension, int stage, BlockPos landing) {
            this.session = session; this.dimension = dimension; this.stage = stage; this.landing = landing;
        }
        public void fromBytes(ByteBuf buf) {
            session = ByteBufUtils.readUTF8String(buf); dimension = buf.readInt(); stage = buf.readUnsignedByte();
            landing = buf.readBoolean() ? BlockPos.fromLong(buf.readLong()) : null;
        }
        public void toBytes(ByteBuf buf) {
            ByteBufUtils.writeUTF8String(buf, session); buf.writeInt(dimension); buf.writeByte(stage);
            buf.writeBoolean(landing != null); if (landing != null) buf.writeLong(landing.toLong());
        }
    }

    public static class Ready implements IMessage {
        public String session;
        public boolean region;
        public Ready() { }
        Ready(String session, boolean region) { this.session = session; this.region = region; }
        public void fromBytes(ByteBuf buf) { session = ByteBufUtils.readUTF8String(buf); region = buf.readBoolean(); }
        public void toBytes(ByteBuf buf) { ByteBufUtils.writeUTF8String(buf, session); buf.writeBoolean(region); }
    }

    public static class StateHandler implements IMessageHandler<State, IMessage> {
        public IMessage onMessage(State message, MessageContext ctx) { client.receive(message); return null; }
    }

    public static class ReadyHandler implements IMessageHandler<Ready, IMessage> {
        public IMessage onMessage(Ready message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> ArrivalSession.ready(player, message.session, message.region));
            return null;
        }
    }
}
