package galaxyspace.core.telemetry;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;

public class TelemetryClient extends TelemetryProxy {
    @Override
    public void receive(final TelemetryNetwork.Update update) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().world == null
                    || Minecraft.getMinecraft().world.provider.getDimension() != update.sourceDimension
                    || !Minecraft.getMinecraft().world.isBlockLoaded(update.pos)) return;
            TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(update.pos);
            if (tile instanceof TelemetryDimensionState) {
                ((TelemetryDimensionState) tile).galaxyspace$receiveDimension(update.tracked, update.known,
                    update.targetDimension, update.key, update.fallback);
            }
        });
    }
}
