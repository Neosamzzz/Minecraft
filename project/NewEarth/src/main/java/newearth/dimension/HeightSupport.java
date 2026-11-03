package newearth.dimension;

import java.lang.reflect.Method;
import net.minecraft.world.World;

/** Height is supplied by Depths Update; ordinary worlds do not enter this adapter. */
final class HeightSupport {
    static void require(World world) {
        try {
            Class<?> api=Class.forName("sayys.depthsupdate.api.DepthsUpdateAPI");
            Object info=api.getMethod("getHeightInfo",int.class).invoke(null,world.provider.getDimension());
            Method min=info.getClass().getMethod("minY"),max=info.getClass().getMethod("maxY");
            if(((Number)min.invoke(info)).intValue()!=TerrainSampler.MIN_Y
                    || ((Number)max.invoke(info)).intValue()!=TerrainSampler.MAX_Y)
                throw new IllegalStateException("NewEarth terrain requires Depths Update bounds 0..384 for this dimension");
        } catch(ReflectiveOperationException error) {
            throw new IllegalStateException("Install Depths Update and its NewEarth height configuration before creating the terrain prototype",error);
        }
    }
}
