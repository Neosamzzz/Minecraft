package newearth.worldgen;

import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.fml.common.IWorldGenerator;

/**
 * Places one village on the main island and one on every grid island. Vanilla's village grid almost
 * never lands on a small island, so structures are generated directly from island centres instead.
 */
public final class IslandVillage implements IWorldGenerator {
    private static final int ATTEMPTS = 3;
    private static final int[][] OFFSETS = {{0, 0}, {1, 0}, {0, 1}};

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (world.provider.getDimension() != 0 || !WorldGenConfig.enabled || !WorldGenConfig.mainVillage) return;
        IslandField field = IslandHooks.field();
        if (field == null) return;

        long key;
        int[] center;
        if (chunkX == 0 && chunkZ == 0) {
            key = 0L;
            center = new int[]{0, 0};
        } else {
            key = field.largeIslandAnchorAtChunk(chunkX, chunkZ);
            if (key == 0L) return;
            center = field.largeIslandCenter(key);
            if (center == null) return;
        }

        IslandVillageData data = IslandVillageData.get(world);
        if (data.isPlaced(key)) return;
        if (place(world, random, center[0] >> 4, center[1] >> 4)) {
            data.markPlaced(key);
            System.out.println("[NewEarth] island village placed near " + new BlockPos(center[0], 64, center[1]));
        }
    }

    private static boolean place(World world, Random random, int centerChunkX, int centerChunkZ) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int chunkX = centerChunkX + OFFSETS[attempt][0];
            int chunkZ = centerChunkZ + OFFSETS[attempt][1];
            StructureStart start = new VillageGenerator().start(world, random, chunkX, chunkZ);
            if (start != null && start.isSizeableStructure()) {
                start.generateStructure(world, random, start.getBoundingBox());
                return true;
            }
        }
        return false;
    }

    /** Exposes the protected structure start of the vanilla village generator. */
    private static final class VillageGenerator extends MapGenVillage {
        StructureStart start(World world, Random random, int chunkX, int chunkZ) {
            this.world = world;
            this.rand = random;
            return getStructureStart(chunkX, chunkZ);
        }
    }
}
