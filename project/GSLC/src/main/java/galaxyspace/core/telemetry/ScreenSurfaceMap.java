package galaxyspace.core.telemetry;

import java.util.Arrays;
import java.lang.ref.WeakReference;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

/** One screen's real surface pixels. Never requests, loads or generates chunks. */
public final class ScreenSurfaceMap {
    public static final int SIZE = 352; // GC's existing satellite-view texture size.
    public static final int UNKNOWN = 0xff101010;
    private final int[] pixels = new int[SIZE * SIZE];
    private final int[] pending = new int[SIZE * SIZE];
    private WeakReference<World> world = new WeakReference<>(null);
    private BlockPos centre;
    private int dimension, nextRow, revision;
    private long lastTick = Long.MIN_VALUE;
    private long nextScanTick = Long.MIN_VALUE;
    private boolean scanning;

    public ScreenSurfaceMap() { Arrays.fill(pixels, UNKNOWN); }
    public int[] pixels() { return pixels; }
    public int revision() { return revision; }

    public void update(World current, BlockPos position, long tick) {
        if (world.get() != current || dimension != current.provider.getDimension() || !position.equals(centre)) {
            world = new WeakReference<>(current);
            dimension = current.provider.getDimension();
            centre = position.toImmutable();
            nextRow = 0;
            lastTick = Long.MIN_VALUE;
            nextScanTick = Long.MIN_VALUE;
            scanning = false;
            Arrays.fill(pixels, UNKNOWN);
            revision++;
        }
        if (lastTick != Long.MIN_VALUE && tick < lastTick) {
            scanning = false;
            nextScanTick = Long.MIN_VALUE;
        }
        if (!scanning) {
            if (nextScanTick != Long.MIN_VALUE && tick < nextScanTick) return;
            System.arraycopy(pixels, 0, pending, 0, pixels.length);
            nextRow = 0;
            scanning = true;
            nextScanTick = tick + 600L; // Start a new scan no more than every 30 seconds.
            lastTick = Long.MIN_VALUE;
        }
        // Spread each scan over ticks, then publish a complete frame in one step.
        if (lastTick != Long.MIN_VALUE && tick >= lastTick && tick - lastTick < 2) return;
        lastTick = tick;
        int rows = Math.min(8, SIZE - nextRow);
        sample(current, centre, SIZE, nextRow, rows, pending);
        nextRow += rows;
        if (nextRow == SIZE) {
            scanning = false;
            if (!Arrays.equals(pixels, pending)) {
                System.arraycopy(pending, 0, pixels, 0, pixels.length);
                revision++;
            }
        }
    }

    public static void sample(World world, BlockPos centre, int size, int firstRow, int rows, int[] pixels) {
        if (size <= 0 || pixels.length != size * size || firstRow < 0 || rows < 0 || firstRow + rows > size) {
            throw new IllegalArgumentException("Invalid surface-map region");
        }
        IChunkProvider provider = world.getChunkProvider();
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        for (int row = firstRow; row < firstRow + rows; row++) {
            int z = centre.getZ() - size / 2 + row;
            for (int column = 0; column < size; column++) {
                int x = centre.getX() - size / 2 + column;
                int index = row * size + column;
                Chunk chunk = provider.getLoadedChunk(x >> 4, z >> 4);
                if (chunk == null || chunk.isEmpty()) continue;
                // Unavailable chunks retain their last known pixels; loaded void may clear them.
                pixels[index] = UNKNOWN;
                int top = Math.min(world.getHeight() - 1, chunk.getTopFilledSegment() + 15);
                for (int y = top; y >= 0; y--) {
                    IBlockState state = chunk.getBlockState(x & 15, y, z & 15);
                    position.setPos(x, y, z);
                    MapColor colour = state.getMapColor(world, position);
                    if (colour == MapColor.AIR) continue;
                    Chunk north = (z & 15) == 0 ? provider.getLoadedChunk(x >> 4, (z - 1) >> 4) : chunk;
                    int northHeight = north == null ? y : north.getHeightValue(x & 15, (z - 1) & 15) - 1;
                    int shade = y > northHeight ? 2 : y < northHeight ? 0 : 1;
                    pixels[index] = colour.getMapColor(shade);
                    break;
                }
            }
        }
    }
}
