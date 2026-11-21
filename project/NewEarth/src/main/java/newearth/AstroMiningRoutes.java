package newearth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.core.GCBlocks;
import micdoodle8.mods.galacticraft.planets.asteroids.blocks.AsteroidBlocks;
import micdoodle8.mods.galacticraft.planets.asteroids.dimension.AsteroidSaveData;
import micdoodle8.mods.galacticraft.planets.asteroids.dimension.WorldProviderAsteroids;
import micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityAstroMiner;
import micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityMinerBase;
import micdoodle8.mods.galacticraft.planets.asteroids.world.gen.SpecialAsteroidBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

/** Stores the matrix cursor on the dock and leaves GC in charge of active waypoints. */
final class AstroMiningRoutes {
    private static final String PLAN = "NewEarthMiningPlan";
    private static final int MATRIX_RADIUS = 500;
    private static final int FORWARD_STEP = EntityAstroMiner.MINE_LENGTH + 6;
    private static final int SIDE_STEP = 38;
    private static final int FLOOR = 10;
    private static final int LAYER_STEP = 24;
    private static final List<BlockVec3> MATRIX = matrixOffsets();
    private static final int[][] SHAFTS = {{0, 0}, {4, 0}, {2, 3}, {-2, 3}, {-4, 0}, {-2, -3}, {2, -3}};

    private AstroMiningRoutes() { }

    static void start(TileEntityMinerBase dock, List<BlockVec3> queue, EnumFacing facing) {
        queue.clear();
        NBTTagCompound plan = new NBTTagCompound();
        if (dock.getWorld().provider instanceof WorldProviderAsteroids) {
            selectAsteroids(dock, queue, facing);
        } else {
            BlockVec3 centre = new BlockVec3(dock).modifyPositionFromSide(facing, 32 + dock.getWorld().rand.nextInt(16));
            int y = dock.getPos().getY();
            int firstY = Math.max(5, Math.min(y * 2 - 90, y - 22)) + 5 + dock.getWorld().rand.nextInt(4);
            centre.y = Math.max(FLOOR, Math.min(firstY, dock.getWorld().getActualHeight() - 6));
            plan.setTag("Origin", centre.writeToNBT(new NBTTagCompound()));
            plan.setInteger("Height", centre.y);
        }
        dock.getTileData().setTag(PLAN, plan);
        dock.markDirty();
    }

    static void refill(TileEntityMinerBase dock, List<BlockVec3> queue, EnumFacing facing) {
        NBTTagCompound data = dock.getTileData();
        if (!data.hasKey(PLAN, Constants.NBT.TAG_COMPOUND)) {
            // An existing ground route may finish its saved queue before adopting the matrix.
            if (!queue.isEmpty() && !(dock.getWorld().provider instanceof WorldProviderAsteroids)) return;
            start(dock, queue, facing);
        }
        if (!queue.isEmpty()) return;
        NBTTagCompound plan = data.getCompoundTag(PLAN);
        if (!plan.hasKey("Origin", Constants.NBT.TAG_COMPOUND)) return;
        BlockVec3 origin = BlockVec3.readFromNBT(plan.getCompoundTag("Origin"));
        EnumFacing side = facing.rotateY();
        int row = plan.getInteger("Next");
        int y = plan.getInteger("Height");
        if (y < FLOOR) return;
        while (queue.isEmpty() && y >= FLOOR) {
            BlockVec3 offset = MATRIX.get(row++);
            BlockVec3 centre = origin.clone().modifyPositionFromSide(facing, offset.z)
                    .modifyPositionFromSide(side, offset.x);
            centre.y = y;
            addGroundRow(dock.getWorld(), queue, centre, dock.getPos(), facing, side);
            if (row == MATRIX.size()) {
                row = 0;
                y = y == FLOOR ? FLOOR - 1 : Math.max(FLOOR, y - LAYER_STEP);
            }
        }
        plan.setInteger("Next", row);
        plan.setInteger("Height", y);
        dock.markDirty();
    }

    static List<BlockVec3> matrixOffsets() {
        List<BlockVec3> result = new ArrayList<>();
        for (int side = -MATRIX_RADIUS / SIDE_STEP * SIDE_STEP; side <= MATRIX_RADIUS; side += SIDE_STEP) {
            for (int forward = -MATRIX_RADIUS / FORWARD_STEP * FORWARD_STEP; forward <= MATRIX_RADIUS; forward += FORWARD_STEP) {
                if (side * side + forward * forward <= MATRIX_RADIUS * MATRIX_RADIUS)
                    result.add(new BlockVec3(side, 0, forward));
            }
        }
        result.sort(Comparator.comparingInt((BlockVec3 point) -> point.x * point.x + point.z * point.z)
                .thenComparingInt(point -> point.z).thenComparingInt(point -> point.x));
        return Collections.unmodifiableList(result);
    }

    private static void addGroundRow(World world, List<BlockVec3> queue, BlockVec3 centre, BlockPos dock,
            EnumFacing facing, EnumFacing side) {
        addGroundPoint(world, queue, centre.clone(), dock, facing);
        for (int direction : new int[] {1, -1}) {
            addGroundPoint(world, queue, centre.clone().modifyPositionFromSide(side, 13 * direction), dock, facing);
            BlockVec3 shifted = centre.clone().modifyPositionFromSide(side, 7 * direction);
            addGroundPoint(world, queue, shifted.clone().modifyPositionFromSide(EnumFacing.UP, 11), dock, facing);
            addGroundPoint(world, queue, shifted.modifyPositionFromSide(EnumFacing.DOWN, 11), dock, facing);
        }
    }

    private static void addGroundPoint(World world, List<BlockVec3> queue, BlockVec3 start, BlockPos dock, EnumFacing facing) {
        if (start.y < FLOOR || start.y + 5 >= world.getActualHeight()) return;
        BlockVec3 end = start.clone().modifyPositionFromSide(facing, EntityAstroMiner.MINE_LENGTH);
        int margin = 8;
        if (Math.abs(start.y - dock.getY()) <= 8
                && dock.getX() >= Math.min(start.x, end.x) - margin && dock.getX() <= Math.max(start.x, end.x) + margin
                && dock.getZ() >= Math.min(start.z, end.z) - margin && dock.getZ() <= Math.max(start.z, end.z) + margin) return;
        if (!world.getWorldBorder().contains(new BlockPos(start.x, start.y, start.z))
                || !world.getWorldBorder().contains(new BlockPos(end.x, end.y, end.z))) return;
        queue.add(start);
    }

    private static NBTTagList asteroidRecords(World world) {
        AsteroidSaveData data = (AsteroidSaveData) world.loadData(AsteroidSaveData.class, AsteroidSaveData.saveDataID);
        return data == null ? new NBTTagList() : data.datacompound.getTagList("coords", Constants.NBT.TAG_COMPOUND);
    }

    static boolean mineralCore(int type) {
        if (type < 0 || type >= SpecialAsteroidBlock.register.size()) return false;
        SpecialAsteroidBlock core = SpecialAsteroidBlock.register.get(type);
        return core.block == Blocks.DIAMOND_ORE || (core.block == GCBlocks.basicBlock && (core.meta == 8 || core.meta == 12))
                || (core.block == AsteroidBlocks.blockBasic && core.meta >= 3 && core.meta <= 5);
    }

    private static void selectAsteroids(TileEntityMinerBase dock, List<BlockVec3> queue, EnumFacing facing) {
        NBTTagList records = asteroidRecords(dock.getWorld());
        List<BlockVec3> candidates = new ArrayList<>();
        BlockPos base = dock.getPos();
        for (int i = 0; i < records.tagCount(); i++) {
            NBTTagCompound record = records.getCompoundTagAt(i);
            if (!record.hasKey("coreAndFlag", Constants.NBT.TAG_INT) || !mineralCore(record.getInteger("coreAndFlag"))) continue;
            BlockVec3 centre = BlockVec3.readFromNBT(record);
            int dx = centre.x - base.getX();
            int dz = centre.z - base.getZ();
            if (dx * (double) dx + dz * (double) dz >= 512 * 512
                    || dx * facing.getXOffset() + dz * facing.getZOffset() < 16) continue;
            candidates.add(centre);
        }
        candidates.sort(Comparator.comparingLong((BlockVec3 point) -> {
            long dx = point.x - base.getX();
            long dz = point.z - base.getZ();
            return dx * dx + dz * dz;
        }).thenComparingInt(point -> point.x).thenComparingInt(point -> point.y).thenComparingInt(point -> point.z));
        for (int i = 0; i < Math.min(candidates.size(), 100); i++) {
            queue.add(candidates.get(i).modifyPositionFromSide(facing.getOpposite(), EntityAstroMiner.MINE_LENGTH_AST / 2));
        }
    }

    static void addAsteroidFaces(EntityAstroMiner miner, List<BlockVec3> points, EnumFacing facing, BlockVec3 target) {
        if (!(miner.world.provider instanceof WorldProviderAsteroids) || target == null) return;
        int length = EntityAstroMiner.MINE_LENGTH_AST;
        BlockVec3 centre = target.clone().modifyPositionFromSide(facing, length / 2);
        NBTTagList records = asteroidRecords(miner.world);
        int size = 0;
        for (int i = 0; i < records.tagCount(); i++) {
            NBTTagCompound record = records.getCompoundTagAt(i);
            if (centre.equals(BlockVec3.readFromNBT(record))) {
                size = record.getInteger("sizeAndFlag") & 127;
                break;
            }
        }
        if (size <= 0) return;
        EnumFacing side = facing.rotateY();
        for (EnumFacing face : EnumFacing.VALUES) {
            int spacing = face.getAxis() == facing.getAxis() ? length : 8;
            BlockVec3 start = target.clone().modifyPositionFromSide(face, spacing);
            if (start.y < 5 || start.y + 5 >= miner.world.getActualHeight()) continue;
            if (hasRock(miner.world, centre, size, start, facing, side, length)) appendShafts(points, start, facing, side, length);
        }
    }

    private static boolean hasRock(World world, BlockVec3 centre, int radius, BlockVec3 start,
            EnumFacing facing, EnumFacing side, int length) {
        boolean unobserved = false;
        for (int distance = 0; distance <= length; distance += 3) {
            for (int[] shaft : SHAFTS) {
                BlockVec3 point = start.clone().modifyPositionFromSide(facing, distance)
                        .modifyPositionFromSide(side, shaft[0]).modifyPositionFromSide(EnumFacing.UP, shaft[1]);
                int dx = centre.x - point.x, dy = centre.y - point.y, dz = centre.z - point.z;
                if (dx * dx + dy * dy + dz * dz > radius * radius) continue;
                BlockPos pos = new BlockPos(point.x, point.y, point.z);
                if (!world.isBlockLoaded(pos)) {
                    unobserved = true;
                    continue;
                }
                IBlockState state = world.getBlockState(pos);
                if (state.getBlock() == AsteroidBlocks.blockBasic || state.getBlock() == AsteroidBlocks.blockDenseIce
                        || state.getBlock() == Blocks.DIAMOND_ORE || state.getBlock() == GCBlocks.basicBlock) return true;
            }
        }
        // The asteroid's recorded radius supplies bounds for as-yet unloaded faces.
        return unobserved;
    }

    private static void appendShafts(List<BlockVec3> points, BlockVec3 start, EnumFacing facing, EnumFacing side, int length) {
        boolean forward = true;
        for (int[] shaft : SHAFTS) {
            BlockVec3 near = start.clone().modifyPositionFromSide(side, shaft[0]).modifyPositionFromSide(EnumFacing.UP, shaft[1]);
            BlockVec3 far = near.clone().modifyPositionFromSide(facing, length);
            points.add(forward ? near : far);
            points.add(forward ? far : near);
            forward = !forward;
        }
    }
}
