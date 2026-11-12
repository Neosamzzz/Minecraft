package newearth.server;

import java.util.Queue;
import java.util.concurrent.FutureTask;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.network.ServerStatusResponse;
import net.minecraftforge.common.chunkio.ChunkIOExecutor;
import org.apache.logging.log4j.LogManager;

/** Keep login and command work alive without ticking worlds or Forge tick handlers. */
public final class PauseHooks {
    private static boolean paused;

    interface Service {
        int players();
        void save();
        void maintenance();
        void state(boolean sleeping);
    }

    static void emptyStatus(ServerStatusResponse status, int maximum) {
        status.setPlayers(new ServerStatusResponse.Players(maximum, 0));
        status.getPlayers().setPlayers(new com.mojang.authlib.GameProfile[0]);
        status.invalidateJson();
    }

    static boolean step(Service service) {
        if (service.players() > 0) {
            if (paused) { paused = false; service.state(false); }
            return false;
        }
        if (!paused) {
            service.save();
            paused = true;
            service.state(true);
        }
        service.maintenance();
        return true;
    }

    public static boolean pause(final MinecraftServer server) {
        if (!server.isDedicatedServer()) return false;
        return step(new Service() {
            public int players() { return server.getCurrentPlayerCount(); }
            public void save() {
                server.getPlayerList().saveAllPlayerData();
                server.saveAllWorlds(false);
                emptyStatus(server.getServerStatusResponse(), server.getMaxPlayers());
            }
            public void maintenance() {
                Queue<FutureTask<?>> tasks = server.futureTaskQueue;
                synchronized (tasks) {
                    while (!tasks.isEmpty()) tasks.poll().run();
                }
                ChunkIOExecutor.tick();
                server.getNetworkSystem().networkTick();
                // DedicatedServer normally drains this queue from updateTimeLightAndEntities,
                // which is intentionally skipped by our paused tick path.
                ((DedicatedServer) server).executePendingCommands();
            }
            public void state(boolean sleeping) {
                LogManager.getLogger("NewEarth Server Pause").info(sleeping
                        ? "Paused: no players online; world and mod ticks suspended. Login and console remain active."
                        : "Resumed: a player is online; normal server ticks restored.");
            }
        });
    }
}
