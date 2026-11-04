package newearth.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import newearth.NewEarth;
import newearth.dimension.EndDimension;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Client-local shader override for the destination; never changes the user's saved preferences. */
@Mod.EventBusSubscriber(modid = NewEarth.MOD_ID, value = Side.CLIENT)
public final class DimensionShaders {
    private static final Logger LOG = LogManager.getLogger("NewEarth");
    private static final String PACK = "ComplementaryReimagined_r5.9.3.zip";
    private static boolean inside;
    private static Object config;
    private static Class<?> iris;
    private static String previousPack;
    private static boolean previousEnabled;

    private DimensionShaders() { }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        // A transient null world during transfer must not cause two extra shader compilations.
        if (mc.world == null && mc.getConnection() != null) return;
        boolean destination = mc.world != null && EndDimension.isDestination(mc.world);
        if (destination == inside) return;
        inside = destination;
        if (destination) enter();
        else restore();
    }

    private static void enter() {
        try {
            iris = Class.forName("net.coderbot.iris.Iris");
            Path packs = (Path) iris.getMethod("getShaderpacksDirectory").invoke(null);
            if (!Files.isRegularFile(packs.resolve(PACK))) {
                LOG.warn("NewEarth shader pack missing: {}", PACK);
                return;
            }
            config = iris.getMethod("getIrisConfig").invoke(null);
            previousPack = (String) ((Optional<?>) config.getClass().getMethod("getShaderPackName")
                    .invoke(config)).orElse(null);
            previousEnabled = (Boolean) config.getClass().getMethod("areShadersEnabled").invoke(config);
            apply(PACK, true);
        } catch (ClassNotFoundException ignored) {
            // Actinium is optional, including on dedicated servers.
        } catch (Exception e) {
            LOG.warn("Could not enable NewEarth shaders; restoring previous settings", e);
            restore();
        }
    }

    private static void restore() {
        if (config == null) return;
        try { apply(previousPack, previousEnabled); }
        catch (Exception e) { LOG.warn("Could not reload previous shader settings", e); }
        finally { config = null; previousPack = null; }
    }

    private static void apply(String pack, boolean enabled) throws Exception {
        Class<?> type = config.getClass();
        String current = (String) ((Optional<?>) type.getMethod("getShaderPackName").invoke(config)).orElse(null);
        boolean active = (Boolean) type.getMethod("areShadersEnabled").invoke(config);
        if (java.util.Objects.equals(current, pack) && active == enabled) return;
        // Iris.reload re-reads shaders.properties. Temporarily write the override, then put
        // the exact original bytes back even if compilation fails; the loaded state stays local.
        Path file = Minecraft.getMinecraft().gameDir.toPath().resolve("config/shaders.properties");
        byte[] saved = Files.exists(file) ? Files.readAllBytes(file) : null;
        try {
            type.getMethod("setShaderPackName", String.class).invoke(config, pack);
            type.getMethod("setShadersEnabled", boolean.class).invoke(config, enabled);
            type.getMethod("save").invoke(config);
            iris.getMethod("reload").invoke(null);
        } finally {
            if (saved != null) Files.write(file, saved);
            else Files.deleteIfExists(file);
        }
    }
}

