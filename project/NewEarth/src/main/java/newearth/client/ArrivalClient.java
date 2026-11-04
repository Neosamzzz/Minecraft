package newearth.client;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiWinGame;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import newearth.dimension.ArrivalNetwork;
import newearth.dimension.EndDimension;

@Mod.EventBusSubscriber(modid = "newearth", value = Side.CLIENT)
public final class ArrivalClient extends ArrivalNetwork.ClientBridge {
    private static ArrivalNetwork.State pending;
    private static String session;
    private static int stage = -1;
    private static GuiScreen screen;
    private static BlockPos landing;
    private static boolean regionSent;
    private static int serverLevel;

    @SubscribeEvent public static void travelSound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        // OTG emits effect 1032 after transfer, which vanilla maps to block.portal.travel.
        if (mc.world != null && EndDimension.isDestination(mc.world)
                && "minecraft:block.portal.travel".equals(event.getSound().getSoundLocation().toString()))
            event.setResultSound(null);
    }

    @Override public void receive(ArrivalNetwork.State state) {
        Minecraft.getMinecraft().addScheduledTask(() -> pending = state);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getConnection() == null || (mc.world != null && !EndDimension.isDestination(mc.world))) {
            pending = null; session = null; stage = -1;
            landing = null; regionSent = false; serverLevel = 0;
            if (screen != null && mc.currentScreen == screen) mc.displayGuiScreen(null);
            screen = null;
            ArrivalDistantLoading.stop();
            return;
        }
        if (pending != null && mc.world != null && mc.player != null && mc.player.dimension == pending.dimension) {
            ArrivalNetwork.State state = pending;
            pending = null;
            serverLevel = state.loadLevel;
            if (!state.session.equals(session)) { session = state.session; stage = -1; regionSent = false; landing = null; }
            if (state.landing != null && !state.landing.equals(landing)) { landing = state.landing; regionSent = false; }
            if (state.stage > stage) {
                stage = state.stage;
                if (stage == ArrivalNetwork.POEM) {
                    final String id = session;
                    screen = new GuiWinGame(true, () -> {
                        // Vanilla invokes this callback and closes the GUI; no vanilla respawn packet is sent.
                        if (id.equals(session)) {
                            stage = ArrivalNetwork.WAIT;
                            ArrivalNetwork.ready(id, false);
                            screen = new Waiting();
                        }
                    }) {
                        @Override public boolean doesGuiPauseGame() { return false; }
                    };
                } else if (stage == ArrivalNetwork.WAIT) screen = new Waiting();
                else {
                    if (mc.currentScreen == screen) mc.displayGuiScreen(null);
                    screen = null;
                    if (stage == ArrivalNetwork.LANDED) ArrivalDistantLoading.stop();
                }
            }
        }
        if (stage == ArrivalNetwork.WAIT && !regionSent && landing != null && mc.world != null
                && mc.world.isBlockLoaded(landing) && !mc.world.isAirBlock(landing.down())) {
            ArrivalNetwork.ready(session, true);
            regionSent = true;
        }
        if (stage >= ArrivalNetwork.POEM && stage < ArrivalNetwork.LANDED && mc.world != null) ArrivalDistantLoading.tick(serverLevel);
        if (screen != null && mc.world != null) {
            if (mc.currentScreen != screen) mc.displayGuiScreen(screen);
        }
    }

    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END && stage >= ArrivalNetwork.POEM && stage < ArrivalNetwork.LANDED)
            ArrivalDistantLoading.frame();
    }

    private static final class Waiting extends GuiScreen {
        @Override public boolean doesGuiPauseGame() { return false; }
        @Override protected void keyTyped(char c, int key) throws IOException { }
        @Override public void drawScreen(int x, int y, float partial) {
            drawDefaultBackground();
            drawCenteredString(fontRenderer, I18n.format("newearth.arrival.wait"), width / 2, height / 2 - 5, 0xFFFFFF);
            super.drawScreen(x, y, partial);
        }
    }
}
