package newearth.dimension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/** Keeps modern classes out of the legacy Forge remapper and mod class scanner. */
final class TerrainLoader extends ClassLoader {
    private final Map<String, byte[]> entries = new HashMap<>();
    private static TerrainLoader instance;

    private TerrainLoader() throws IOException {
        super(TerrainSampler.class.getClassLoader());
        InputStream source = TerrainLoader.class.getResourceAsStream("/META-INF/newearth-terrain.jar");
        if (source == null) throw new IOException("Missing embedded NewEarth terrain engine");
        try (JarInputStream jar = new JarInputStream(source)) {
            JarEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = jar.getNextJarEntry()) != null) {
                if (entry.isDirectory()) continue;
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                int count;
                while ((count = jar.read(buffer)) != -1) data.write(buffer, 0, count);
                entries.put(entry.getName(), data.toByteArray());
            }
        }
    }

    static synchronized TerrainSampler create(long seed) {
        String version = System.getProperty("java.specification.version", "8");
        if (version.startsWith("1.") || Integer.parseInt(version) < 25)
            throw new IllegalStateException("NewEarth terrain requires Cleanroom and Java 25");
        try {
            if (instance == null) instance = new TerrainLoader();
            return (TerrainSampler) instance.loadClass("newearth.terrain.TerrainEngine")
                    .getConstructor(long.class).newInstance(seed);
        } catch (ReflectiveOperationException | IOException error) {
            throw new IllegalStateException("Cannot initialize NewEarth terrain", error);
        }
    }

    @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] data = entries.get(name.replace('.', '/') + ".class");
        if (data == null) throw new ClassNotFoundException(name);
        return defineClass(name, data, 0, data.length);
    }

    @Override public InputStream getResourceAsStream(String name) {
        byte[] data = entries.get(name);
        return data == null ? super.getResourceAsStream(name) : new ByteArrayInputStream(data);
    }
}
