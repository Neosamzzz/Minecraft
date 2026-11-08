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
 * Places one village on the main island. Vanilla's village grid almost never lands on a small island,
 * so the structure is generated once from the island centre instead.
 */
public final class IslandVillage implements IWorldGenerator {
    private static final int ANCHOR_CHUNK_X = 0;
    private static final int ANCHOR_CHUNK_Z = 0;
    private static final int ATTEMPTS = 3;

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (world.provider.getDimension() != 0 || !WorldGenConfig.enabled || !WorldGenConfig.mainVillage) return;
        if (chunkX != ANCHOR_CHUNK_X || chunkZ != ANCHOR_CHUNK_Z) return;
        IslandVillageData data = IslandVillageData.get(world);
        if (data.isPlaced()) return;
        if (place(world, random)) {
            data.setPlaced();
            System.out.println("[NewEarth] island village placed near " + new BlockPos(ANCHOR_CHUNK_X * 16, 64, ANCHOR_CHUNK_Z * 16));
        }
    }

    private static boolean place(World world, Random random) {
        int[][] offsets = {{0, 0}, {1, 0}, {0, 1}};
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int chunkX = ANCHOR_CHUNK_X + offsets[attempt][0];
            int chunkZ = ANCHOR_CHUNK_Z + offsets[attempt][1];
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
