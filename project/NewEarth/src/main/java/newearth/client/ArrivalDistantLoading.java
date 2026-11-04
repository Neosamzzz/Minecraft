package newearth.client;

import java.util.LinkedHashMap;
import java.util.Map;
import newearth.dimension.ArrivalLoadBudget;
import org.apache.logging.log4j.LogManager;

/** Temporary DH API overrides; leaves the user's disk configuration untouched. */
final class ArrivalDistantLoading {
    private static final Map<Object, Object> PREVIOUS = new LinkedHashMap<>();
    private static boolean attempted;
    private static Class<?> valueType;
    private static Object threadCount, runtimeRatio;
    private static int maximumThreads, appliedLevel = Integer.MIN_VALUE;
    private static long lastFrame;
    private static ArrivalLoadBudget clientLoad = new ArrivalLoadBudget(24, 45);

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
            // The boolean API converts true to SURFACE_THEN_CHUNKS, unsupported in 1.12.2.
            // Its listener then recursively changes the underlying value while the override wins.
            Class<?> planType = Class.forName("com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiGeneratorPlan");
            Object plan = planType.getField("CHUNKS_ONLY").get(null);
            override(generator.getMethod("GeneratorPlan").invoke(generation), plan);
            Object threads = root.getMethod("multiThreading").invoke(config);
            Class<?> threading = Class.forName("com.seibel.distanthorizons.api.interfaces.config.client.IDhApiMultiThreadingConfig");
            threadCount = threading.getMethod("threadCount").invoke(threads);
            runtimeRatio = threading.getMethod("threadRuntimeRatio").invoke(threads);
            maximumThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
            Object maximum = valueType.getMethod("getMaxValue").invoke(threadCount);
            if (maximum instanceof Number) maximumThreads = Math.max(1, Math.min(maximumThreads, ((Number) maximum).intValue()));
            override(runtimeRatio, .15D);
            override(threadCount, 1);
            LogManager.getLogger("NewEarth").info("Arrival DH ramp active: CHUNKS_ONLY, starting at 1 thread / 0.15, maximum {} threads", maximumThreads);
        } catch (ClassNotFoundException ignored) { attempted = true; }
        catch (Exception error) {
            LogManager.getLogger("NewEarth").warn("DH arrival boost unavailable; normal loading continues", error);
            stop(); attempted = true;
        }
    }

    static void frame() {
        long now = System.nanoTime();
        if (lastFrame != 0) clientLoad.sample(now, (now - lastFrame) / 1_000_000.0);
        lastFrame = now;
    }

    static void tick(int serverLevel) {
        start();
        if (threadCount == null || runtimeRatio == null) return;
        long now = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        long headroom = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
        if (headroom < runtime.maxMemory() / 10) clientLoad.sample(now, 150);
        int level = Math.min(serverLevel, clientLoad.level(now));
        if (level == appliedLevel) return;
        int threads = 1 + (maximumThreads - 1) * Math.max(0, level) / ArrivalLoadBudget.MAX_LEVEL;
        double ratio = level < 0 ? .05 : .15 + .85 * level / ArrivalLoadBudget.MAX_LEVEL;
        try {
            // Lower duty cycle first when backing off, before resizing the worker pool.
            if (level < appliedLevel) override(runtimeRatio, ratio);
            override(threadCount, threads);
            override(runtimeRatio, ratio);
            appliedLevel = level;
        } catch (Exception error) {
            LogManager.getLogger("NewEarth").warn("Could not adjust DH arrival load; restoring normal settings", error);
            stop(); attempted = true;
        }
    }

    private static void override(Object value, Object desired) throws Exception {
        Object previous = valueType.getMethod("getApiValue").invoke(value);
        if (java.util.Objects.equals(valueType.getMethod("getValue").invoke(value), desired)) return;
        // Check the API result: a rejected override must not be reported as successful.
        if (!PREVIOUS.containsKey(value)) PREVIOUS.put(value, previous);
        if (!(Boolean) valueType.getMethod("setValue", Object.class, String.class).invoke(value, desired, "NewEarth arrival"))
            throw new IllegalStateException("DH rejected arrival override");
    }

    static void stop() {
        for (Map.Entry<Object, Object> entry : PREVIOUS.entrySet()) try {
            if (entry.getValue() == null) valueType.getMethod("clearValue").invoke(entry.getKey());
            else valueType.getMethod("setValue", Object.class, String.class).invoke(entry.getKey(), entry.getValue(), "NewEarth arrival restore");
        } catch (Exception error) { LogManager.getLogger("NewEarth").warn("Could not restore DH API setting", error); }
        PREVIOUS.clear(); attempted = false;
        threadCount = runtimeRatio = null;
        appliedLevel = Integer.MIN_VALUE; lastFrame = 0;
        clientLoad = new ArrivalLoadBudget(24, 45);
    }
}
