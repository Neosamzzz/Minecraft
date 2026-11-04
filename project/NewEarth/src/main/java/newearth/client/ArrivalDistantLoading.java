package newearth.client;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;

/** Temporary DH API overrides; leaves the user's disk configuration untouched. */
final class ArrivalDistantLoading {
    private static final Map<Object, Object> PREVIOUS = new LinkedHashMap<>();
    private static boolean attempted;
    private static Class<?> valueType;

    static void start() {
        if (attempted) return;
        try {
            Object config = Class.forName("com.seibel.distanthorizons.api.DhApi$Delayed").getField("configs").get(null);
            if (config == null) return; // DH has not finished initializing yet.
            attempted = true;
            valueType = Class.forName("com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue");
            Class<?> root = Class.forName("com.seibel.distanthorizons.api.interfaces.config.IDhApiConfig");
            Object generation = root.getMethod("worldGenerator").invoke(config);
            Class<?> generator = Class.forName("com.seibel.distanthorizons.api.interfaces.config.both.IDhApiWorldGenerationConfig");
            override(generator.getMethod("enableDistantWorldGeneration").invoke(generation), true);
            Object threads = root.getMethod("multiThreading").invoke(config);
            Class<?> threading = Class.forName("com.seibel.distanthorizons.api.interfaces.config.client.IDhApiMultiThreadingConfig");
            Object count = threading.getMethod("threadCount").invoke(threads);
            int current = ((Number) valueType.getMethod("getValue").invoke(count)).intValue();
            int desired = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
            override(count, Math.max(current, desired));
            Object ratio = threading.getMethod("threadRuntimeRatio").invoke(threads);
            double previous = ((Number) valueType.getMethod("getValue").invoke(ratio)).doubleValue();
            override(ratio, Math.max(previous, 0.8D));
        } catch (ClassNotFoundException ignored) { attempted = true; }
        catch (Exception error) {
            LogManager.getLogger("NewEarth").warn("DH arrival boost unavailable; normal loading continues", error);
            stop(); attempted = true;
        }
    }

    private static void override(Object value, Object desired) throws Exception {
        Object previous = valueType.getMethod("getApiValue").invoke(value);
        PREVIOUS.put(value, previous);
        valueType.getMethod("setValue", Object.class).invoke(value, desired);
    }

    static void stop() {
        for (Map.Entry<Object, Object> entry : PREVIOUS.entrySet()) try {
            if (entry.getValue() == null) valueType.getMethod("clearValue").invoke(entry.getKey());
            else valueType.getMethod("setValue", Object.class).invoke(entry.getKey(), entry.getValue());
        } catch (Exception error) { LogManager.getLogger("NewEarth").warn("Could not restore DH API setting", error); }
        PREVIOUS.clear(); attempted = false;
    }
}
