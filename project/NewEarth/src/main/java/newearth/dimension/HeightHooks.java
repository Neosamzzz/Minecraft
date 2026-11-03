package newearth.dimension;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime DH bounds, scoped by Depths Update's per-dimension height information. */
public final class HeightHooks {
    private static final ConcurrentHashMap<Integer,int[]> BOUNDS=new ConcurrentHashMap<>();
    private static int[] bounds(int dimension) {
        try {
            Class<?> api=Class.forName("sayys.depthsupdate.api.DepthsUpdateAPI");
            Object info=api.getMethod("getHeightInfo",int.class).invoke(null,dimension);
            return new int[]{((Number)info.getClass().getMethod("minY").invoke(info)).intValue(),
                    ((Number)info.getClass().getMethod("maxY").invoke(info)).intValue()};
        }catch(ReflectiveOperationException error){return new int[]{0,256};}
    }
    private static int[] bounds(Chunk chunk){return BOUNDS.computeIfAbsent(chunk.getWorld().provider.getDimension(),HeightHooks::bounds);}
    public static int max(Chunk chunk){return bounds(chunk)[1];}
    public static int min(Chunk chunk){return bounds(chunk)[0];}
    public static int top(Chunk chunk){return max(chunk)-1;}
}
