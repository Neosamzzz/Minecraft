package newearth.worldgen;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraftforge.fml.common.IWorldGenerator;

/**
 * Places one village on the main island and one on every grid island. The vanilla village generator is
 * reused: the layout lives in its structure map (saved with the world) and every chunk writes only the
 * pieces that intersect it.
 */
public final class IslandVillage implements IWorldGenerator {
    private static final Map<World, VillageGenerator> GENERATORS = new WeakHashMap<World, VillageGenerator>();
    private static final ThreadLocal<ChunkPrimer> PRIMER = ThreadLocal.withInitial(ChunkPrimer::new);

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (world.provider.getDimension() != 0 || !WorldGenConfig.enabled || !WorldGenConfig.mainVillage) return;
        if (IslandHooks.field(world) == null) return;
        VillageGenerator villages;
        synchronized (GENERATORS) {
            villages = GENERATORS.get(world);
            if (villages == null) {
                villages = new VillageGenerator();
                GENERATORS.put(world, villages);
            }
        }
        villages.generate(world, chunkX, chunkZ, PRIMER.get());
        villages.generateStructure(world, random, new ChunkPos(chunkX, chunkZ));
    }

    /** Vanilla village generator restricted to the island anchor chunks. */
    private static final class VillageGenerator extends MapGenVillage {
        @Override
        public String getStructureName() {
            return "NewEarthVillages";
        }

        @Override
        protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
            return IslandHooks.isVillageAnchor(world, chunkX, chunkZ);
        }
    }
}
