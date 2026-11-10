package newearth;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.nbt.NBTTagCompound;
import newearth.worldgen.IslandField;
import newearth.worldgen.IslandHooks;
import newearth.worldgen.IslandTransformer;
import newearth.worldgen.WorldGenConfig;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Exercises island isolation and coordinate/cache regressions without generating chunks or saves. */
public final class WorldgenProbe {
    private static int checks;
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard = new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Bootstrap.register();
        World main = world(123, 0, false);
        IslandHooks.registerWorld(main);
        check(IslandHooks.field(main) != null, "Vanilla overworld is registered before generation");
        BiomeProvider provider = main.getBiomeProvider();
        // These positions collide under the former qx * 31 + qz cache key.
        BlockPos a = new BlockPos(2, 64, 126), b = new BlockPos(6, 64, 2);
        Object first = IslandHooks.biomeAt(provider, a);
        Object second = IslandHooks.biomeAt(provider, b);
        check(first != second, "Colliding coordinates return their own ocean/land biomes");
        check(first == expected(provider, a) && second == expected(provider, b), "Lookup agrees with uncached grid");
        World other = world(456, 0, false);
        IslandHooks.registerWorld(other);
        check(IslandHooks.field(main) != IslandHooks.field(other), "Worlds keep separate generation state");
        check(IslandHooks.biomeAt(provider, a) == first, "Registering another world preserves the first cache");
        WorldGenConfig.mainIslandRadius = 600;
        IslandHooks.registerWorld(main);
        check(IslandHooks.biomeAt(provider, a) != first, "Re-registering resets cached state");
        WorldGenConfig.mainIslandRadius = 100;
        java.util.List<Biome> allowed = java.util.Arrays.asList(Biomes.PLAINS, Biomes.FOREST, Biomes.TAIGA, Biomes.DESERT);
        Object viability = IslandHooks.areBiomesViable(provider, 0, 0, 8, allowed);
        check(viability != null, "Island viability uses island data");
        boolean viable = true;
        for (int x = -8; x <= 8; x += 2) for (int z = -8; z <= 8; z += 2) {
            if (!allowed.contains(expected(provider, new BlockPos(x, 64, z)))) viable = false;
        }
        check(Boolean.valueOf(viable).equals(viability), "Viability matches island grid");
        Object position = IslandHooks.findBiomePosition(provider, 0, 0, 64, allowed);
        check(position instanceof BlockPos, "Spawn search finds island land");
        check(allowed.contains(expected(provider, (BlockPos) position)), "Spawn search returns allowed island biome");
        for (World excluded : new World[] {world(123, 7, false), world(123, 0, true)}) {
            IslandHooks.registerWorld(excluded);
            check(IslandHooks.field(excluded) == null, "Planet/custom providers are excluded");
            check(IslandHooks.biomeAt(excluded.getBiomeProvider(), b) == null, "Excluded lookup falls through");
            Biome[] cells = {Biomes.DESERT};
            IslandHooks.apply(excluded.getBiomeProvider(), cells, 0, 0, 1, 1);
            check(cells[0] == Biomes.DESERT, "Excluded grid remains unchanged");
            check(IslandHooks.villageAllowed(excluded, 100, 100), "Excluded villages keep their own rules");
            check(IslandHooks.areBiomesViable(excluded.getBiomeProvider(), 0, 0, 8, allowed) == null,
                    "Excluded viability falls through");
            check(IslandHooks.findBiomePosition(excluded.getBiomeProvider(), 0, 0, 64, allowed) == null,
                    "Excluded position search falls through");
        }
        WorldGenConfig.enabled = false;
        IslandHooks.registerWorld(main);
        check(IslandHooks.field(main) == null, "Disabled generation removes registration");
        WorldGenConfig.enabled = true;
        IslandField field = new IslandField(123);
        int anchors = 0;
        for (int x = -150; x <= 150; x++) for (int z = -150; z <= 150; z++) {
            long key = field.largeIslandAnchorAtChunk(x, z);
            if (key == 0) continue;
            int[] center = field.largeIslandCenter(key);
            check(center != null && (center[0] >> 4) == x && (center[1] >> 4) == z,
                    "Island key round-trips in every quadrant");
            anchors++;
        }
        check(anchors > 20, "Scanned enough grid islands");
        for (String name : new String[] {"net.minecraft.world.WorldProvider", "net.minecraft.world.biome.BiomeProvider",
                "net.minecraft.world.gen.structure.MapGenVillage"}) verifyTransformer(name);
        System.out.println("Worldgen checks passed: " + checks);
    }

    private static Object expected(BiomeProvider provider, BlockPos pos) {
        Biome[] result = {null};
        IslandHooks.apply(provider, result, ((pos.getX() >> 2) << 2) + 2, ((pos.getZ() >> 2) << 2) + 2, 1, 1);
        return result[0];
    }

    private static World world(long seed, int dimension, boolean customBiomeProvider) throws Exception {
        // Only provider and world metadata are needed; never create a server, chunks or save storage.
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        World world = (World) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(WorldServer.class);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setLong("RandomSeed", seed);
        nbt.setString("generatorName", "default");
        WorldInfo info = new WorldInfo(nbt);
        set(World.class, world, "worldInfo", info);
        WorldProviderSurface provider = new WorldProviderSurface();
        set(World.class, world, "provider", provider);
        provider.setDimension(dimension);
        set(WorldProvider.class, provider, "world", world);
        BiomeProvider biomes = customBiomeProvider ? new BiomeProvider(info) { } : new BiomeProvider(info);
        set(WorldProvider.class, provider, "biomeProvider", biomes);
        return world;
    }

    private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void verifyTransformer(String name) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = WorldgenProbe.class.getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) bytes.write(buffer, 0, count);
        }
        for (boolean renamed : new boolean[] {false, true}) {
            byte[] original = bytes.toByteArray();
            if (renamed) {
                Map<String, String> names = new HashMap<>();
                names.put("net/minecraft/world/World", "obf/W");
                names.put("net/minecraft/world/storage/WorldInfo", "obf/I");
                names.put("net/minecraft/world/biome/Biome", "obf/B");
                names.put("net/minecraft/util/math/BlockPos", "obf/P");
                ClassWriter writer = new ClassWriter(0);
                new ClassReader(original).accept(new ClassRemapper(writer, new SimpleRemapper(names)), 0);
                original = writer.toByteArray();
            }
            byte[] patched = new IslandTransformer().transform(name, name, original);
            ClassNode node = new ClassNode();
            new ClassReader(patched).accept(node, ClassReader.EXPAND_FRAMES);
            int hooks = 0;
            for (MethodNode method : node.methods) {
                if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0)
                    new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
                for (AbstractInsnNode insn : method.instructions.toArray())
                    if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).owner.equals("newearth/worldgen/IslandHooks")) hooks++;
            }
            check(hooks == (name.endsWith("BiomeProvider") ? 8 : 1), "Expected hook count: " + name + " renamed=" + renamed);
        }
    }
}
