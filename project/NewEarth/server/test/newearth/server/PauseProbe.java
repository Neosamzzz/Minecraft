package newearth.server;

import java.io.InputStream;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class PauseProbe {
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    private static final class Service implements PauseHooks.Service {
        int players, saves, connections, transitions;
        Runnable login;
        public int players() { return players; }
        public void save() { saves++; }
        public void maintenance() { connections++; if (login != null) { login.run(); login = null; } }
        public void state(boolean sleeping) { transitions++; }
    }
    public static void main(String[] args) throws Exception {
        Service server = new Service();
        check(PauseHooks.step(server), "first empty tick must skip simulation");
        check(server.saves == 1 && server.connections == 1, "startup pause saves and services connections");
        check(PauseHooks.step(server) && server.saves == 1, "remaining empty must not repeatedly save");
        server.login = () -> server.players = 1;
        check(PauseHooks.step(server) && server.players == 1, "login is serviced while simulation is paused");
        check(!PauseHooks.step(server), "first connected player resumes normal simulation");
        check(server.transitions == 2, "startup pause and first resume are distinct transitions");
        server.players = 2;
        check(!PauseHooks.step(server), "multiple players keep simulation running");
        server.players = 1;
        check(!PauseHooks.step(server), "one remaining player must not pause the server");
        server.players = 0;
        check(PauseHooks.step(server) && server.saves == 2, "last logout saves and pauses immediately");
        server.players = 1;
        check(!PauseHooks.step(server) && server.transitions == 4, "subsequent login resumes again");
        InputStream in = PauseProbe.class.getResourceAsStream("/net/minecraft/server/MinecraftServer.class");
        check(in != null, "actual Minecraft server class is available");
        byte[] original = new ClassReader(in).b;
        byte[] patched = new PauseTransformer().transform("net.minecraft.server.MinecraftServer", "net.minecraft.server.MinecraftServer", original);
        ClassNode node = new ClassNode();
        new ClassReader(patched).accept(node, 0);
        int hooks = 0;
        for (MethodNode method : node.methods) for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("newearth/server/PauseHooks")) {
                hooks++;
                AbstractInsnNode previous = instruction.getPrevious();
                check(previous.getOpcode() == Opcodes.ALOAD && previous.getPrevious() == null,
                        "pause hook precedes all original simulation and Forge tick work");
                check(instruction.getNext().getOpcode() == Opcodes.IFEQ && instruction.getNext().getNext().getOpcode() == Opcodes.RETURN,
                        "paused path returns while occupied path retains original tick body");
            }
        }
        check(hooks == 1, "exactly one real server tick method is patched");
        ClassNode plugin = new ClassNode();
        new ClassReader(PauseProbe.class.getResourceAsStream("/newearth/server/PauseLoadingPlugin.class")).accept(plugin, 0);
        boolean afterRemapping = false;
        for (AnnotationNode annotation : plugin.visibleAnnotations) {
            if (annotation.desc.endsWith("IFMLLoadingPlugin$SortingIndex;") && annotation.values.contains(1001)) afterRemapping = true;
        }
        check(afterRemapping, "coremod must run after runtime name remapping on Cleanroom");
        ClassNode adapter = new ClassNode();
        new ClassReader(PauseProbe.class.getResourceAsStream("/newearth/server/PauseHooks$1.class")).accept(adapter, 0);
        boolean console = false, network = false;
        for (MethodNode method : adapter.methods) if (method.name.equals("maintenance")) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction instanceof MethodInsnNode) {
                String name = ((MethodInsnNode) instruction).name;
                console |= name.equals("executePendingCommands");
                network |= name.equals("networkTick");
            }
        }
        check(console, "paused adapter explicitly services DedicatedServer's separate console command queue");
        check(network, "paused adapter keeps the actual network login tick alive");
        net.minecraft.network.ServerStatusResponse status = new net.minecraft.network.ServerStatusResponse();
        PauseHooks.emptyStatus(status, 20);
        check(status.getPlayers() != null && status.getPlayers().getOnlinePlayerCount() == 0,
                "startup pause publishes a player count before any normal server tick");
        status.setPlayers(new net.minecraft.network.ServerStatusResponse.Players(20, 2));
        PauseHooks.emptyStatus(status, 20);
        check(status.getPlayers().getOnlinePlayerCount() == 0 && status.getPlayers().getMaxPlayers() == 20,
                "last logout clears stale online count and retains server capacity");
        System.out.println("PauseProbe: " + checks + " checks passed");
    }
}
