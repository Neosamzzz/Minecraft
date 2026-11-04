package newearth.dimension;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;

/** One persistent natural landing site per destination, shared by arriving players. */
public final class LandingSite extends WorldSavedData {
    private static final String KEY = "newearth_landing";
    private BlockPos position;

    public LandingSite(String name) { super(name); }

    public static BlockPos get(WorldServer world) {
        LandingSite saved = (LandingSite) world.getPerWorldStorage().getOrLoadData(LandingSite.class, KEY);
        if (saved == null) {
            saved = new LandingSite(KEY);
            world.getPerWorldStorage().setData(KEY, saved);
        }
        if (saved.position != null && safe(world, saved.position) && world.canSeeSky(saved.position)) return saved.position;
        BlockPos origin = saved.position == null ? world.getSpawnPoint() : saved.position;
        // Start locally; only expand if the original OTG spawn column is unsuitable.
        for (int radius = 0; radius <= 4; radius++) {
            for (int dz = -radius; dz <= radius; dz++) for (int dx = -radius; dx <= radius; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                int x = origin.getX() + dx * 16, z = origin.getZ() + dz * 16;
                if (!world.getWorldBorder().contains(new BlockPos(x, 64, z))) continue;
                BlockPos surface = surface(world, x, z);
                if (surface == null) continue;
                saved.position = surface;
                saved.markDirty();
                world.setSpawnPoint(surface);
                return surface;
            }
        }
        // A small emergency deck is preferable to spawning inside water or a cave.
        BlockPos top = world.getHeight(origin);
        int y = Math.min(world.getActualHeight() - 4, Math.max(world.getSeaLevel() + 3, top.getY() + 1));
        BlockPos landing = new BlockPos(origin.getX(), y, origin.getZ());
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            world.setBlockState(landing.add(dx, -1, dz), Blocks.STONE.getDefaultState(), 2);
            for (int dy = 0; dy <= 2; dy++) world.setBlockToAir(landing.add(dx, dy, dz));
        }
        saved.position = landing;
        saved.markDirty();
        world.setSpawnPoint(landing);
        return landing;
    }

    private static BlockPos surface(WorldServer world, int x, int z) {
        int top = Math.min(world.getActualHeight() - 3, world.getHeight(x, z));
        for (int y = top; y > world.getSeaLevel(); y--) {
            BlockPos ground = new BlockPos(x, y, z);
            IBlockState state = world.getBlockState(ground);
            if (state.getMaterial().isReplaceable() && !state.getMaterial().isLiquid()) continue;
            BlockPos feet = ground.up();
            return safe(world, feet) && world.canSeeSky(feet) ? feet : null;
        }
        return null;
    }

    private static boolean safe(WorldServer world, BlockPos feet) {
        return safeBlocks(world.getBlockState(feet.down()), world.getBlockState(feet), world.getBlockState(feet.up()));
    }

    public static boolean safeBlocks(IBlockState floor, IBlockState feet, IBlockState head) {
        Material material = floor.getMaterial();
        return floor.isFullCube() && floor.getBlock() != Blocks.MAGMA
                && (material == Material.GRASS || material == Material.GROUND || material == Material.ROCK
                || material == Material.SAND || material == Material.CLAY || material == Material.SNOW)
                && feet.getMaterial().isReplaceable() && !feet.getMaterial().isLiquid()
                && head.getMaterial().isReplaceable() && !head.getMaterial().isLiquid();
    }

    @Override public void readFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("Position")) position = BlockPos.fromLong(nbt.getLong("Position"));
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if (position != null) nbt.setLong("Position", position.toLong());
        return nbt;
    }
}
