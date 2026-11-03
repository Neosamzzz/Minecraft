package newearth.dimension;

import java.util.List;
import java.util.Collections;
import javax.annotation.Nullable;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

public final class LandscapeGenerator implements IChunkGenerator {
    private final World world;
    private final Landscape landscape;
    public LandscapeGenerator(World world) { this.world = world; landscape = new Landscape(world.getSeed()); }

    /** Pure chunk construction also used by the regression probe. Decoration never loads neighbours. */
    public static ChunkPrimer terrain(Landscape field, int chunkX, int chunkZ) {
        return terrain(field,chunkX,chunkZ,new ChunkPrimer());
    }

    public static ChunkPrimer terrain(Landscape field,int chunkX,int chunkZ,ChunkPrimer primer) {
        byte[] volume=field.terrain(chunkX,chunkZ);
        int height=TerrainSampler.MAX_Y-TerrainSampler.MIN_Y;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = chunkX * 16 + x, wz = chunkZ * 16 + z;
            int top = (int) Math.floor(field.height(wx, wz));
            double slope = field.slope(wx, wz);
            int biome = field.biome(wx, wz);
            boolean sandy = biome == TerrainSampler.DESERT || top <= Landscape.WATER_LEVEL + 1;
            boolean rock = slope > 1.10 || (top > 190 && slope > 0.45);
            for(int y=TerrainSampler.MIN_Y;y<TerrainSampler.MAX_Y;y++) {
                int voxel=volume[(x*16+z)*height+y-TerrainSampler.MIN_Y];
                IBlockState block;
                if(y==TerrainSampler.MIN_Y)block=Blocks.BEDROCK.getDefaultState();
                else if(voxel==0)continue;
                else if(voxel==2)block=Blocks.WATER.getDefaultState();
                else if(voxel==3)block=Blocks.LAVA.getDefaultState();
                else {
                    block=Blocks.STONE.getDefaultState();
                    if(!rock && y>=top-3 && y<=top)block=(sandy?Blocks.SAND:y==top?Blocks.GRASS:Blocks.DIRT).getDefaultState();
                }
                primer.setBlockState(x,y,z,block);
            }
            if (biome == TerrainSampler.SNOW && top > Landscape.WATER_LEVEL && slope < 0.7 && primer.getBlockState(x,top,z).isFullBlock()) primer.setBlockState(x, top + 1, z, Blocks.SNOW_LAYER.getDefaultState());
            if (!rock && !sandy && biome != TerrainSampler.SNOW && top > Landscape.WATER_LEVEL + 1 && top < TerrainSampler.MAX_Y-1 && slope < 0.6 && primer.getBlockState(x,top,z).getBlock()==Blocks.GRASS) {
                double plant = field.unit(wx, wz, 20);
                if (plant < 0.018) primer.setBlockState(x, top + 1, z,
                        Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(),
                                BlockFlower.EnumFlowerType.POPPY));
                else if (plant < 0.22) primer.setBlockState(x, top + 1, z,
                        Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS));
            }
        }
        // Each global cell owns a tree. Render intersecting canopies in both chunks, independent of load order.
        int startX = chunkX * 16, startZ = chunkZ * 16;
        for (int cx = Math.floorDiv(startX - 3, Landscape.TREE_SPACING); cx <= Math.floorDiv(startX + 18, Landscape.TREE_SPACING); cx++)
            for (int cz = Math.floorDiv(startZ - 3, Landscape.TREE_SPACING); cz <= Math.floorDiv(startZ + 18, Landscape.TREE_SPACING); cz++) {
                int tx = cx * Landscape.TREE_SPACING + 1 + (int) (field.unit(cx, cz, 30) * (Landscape.TREE_SPACING-2));
                int tz = cz * Landscape.TREE_SPACING + 1 + (int) (field.unit(cx, cz, 31) * (Landscape.TREE_SPACING-2));
                if (!field.tree(tx, tz)) continue;
                int ground = (int) Math.floor(field.height(tx, tz));
                int trunk = 5 + (int) (field.unit(tx, tz, 32) * 3);
                for (int dy = trunk - 3; dy <= trunk + 1; dy++) {
                    int radius = dy >= trunk ? 1 : 2;
                    for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) == radius && Math.abs(dz) == radius) continue;
                        put(primer, tx + dx - startX, ground + dy, tz + dz - startZ,
                                Blocks.LEAVES.getDefaultState().withProperty(BlockLeaves.CHECK_DECAY, false));
                    }
                }
                for (int dy = 1; dy <= trunk; dy++) put(primer, tx - startX, ground + dy, tz - startZ,
                        Blocks.LOG.getDefaultState());
            }
        return primer;
    }
    private static void put(ChunkPrimer primer, int x, int y, int z, IBlockState state) {
        if (x < 0 || x > 15 || z < 0 || z > 15 || y <= TerrainSampler.MIN_Y || y >= TerrainSampler.MAX_Y) return;
        IBlockState old = primer.getBlockState(x, y, z);
        if (old.getBlock() == Blocks.AIR || old.getBlock() == Blocks.TALLGRASS
                || old.getBlock() == Blocks.RED_FLOWER || old.getBlock() == Blocks.LEAVES)
            primer.setBlockState(x, y, z, state);
    }
    @Override public Chunk generateChunk(int x, int z) {
        Chunk chunk = new Chunk(world, terrain(landscape, x, z), x, z);
        Biome[] biomes = world.getBiomeProvider().getBiomes(null, x * 16, z * 16, 16, 16);
        for (int i = 0; i < biomes.length; i++) chunk.getBiomeArray()[i] = (byte) Biome.getIdForBiome(biomes[i]);
        chunk.generateSkylightMap();
        return chunk;
    }
    @Override public void populate(int x, int z) { }
    @Override public boolean generateStructures(Chunk chunk, int x, int z) { return false; }
    @Override public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        return type == EnumCreatureType.MONSTER ? Collections.emptyList() : world.getBiome(pos).getSpawnableList(type);
    }
    @Nullable @Override public BlockPos getNearestStructurePos(World w, String name, BlockPos pos, boolean unexplored) { return null; }
    @Override public void recreateStructures(Chunk chunk, int x, int z) { }
    @Override public boolean isInsideStructure(World w, String name, BlockPos pos) { return false; }
}
