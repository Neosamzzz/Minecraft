package galaxyspace.core.telemetry;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;
import micdoodle8.mods.galacticraft.core.client.screen.DrawGameScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

/** Reuses GC's texture and drawing code; each screen retains its own pixels. */
public final class SurfaceMapClient {
    private static WeakReference<ScreenSurfaceMap> lastMap = new WeakReference<>(null);
    private static DynamicTexture lastTexture;
    private static int lastRevision;
    private static WeakReference<World> cachedWorld = new WeakReference<>(null);
    private static final Map<BlockPos, ScreenSurfaceMap> cachedMaps = new LinkedHashMap<BlockPos, ScreenSurfaceMap>(32, 0.75F, true);

    private SurfaceMapClient() { }

    public static void prepare(DrawGameScreen screen) {
        if (screen.driver == null || screen.driver.getWorld() == null) return;
        World world = screen.driver.getWorld();
        if (cachedWorld.get() != world) {
            cachedMaps.clear();
            cachedWorld = new WeakReference<>(world);
        }
        BlockPos position = screen.driver.getPos().toImmutable();
        ScreenSurfaceMap map = cachedMaps.get(position);
        if (map == null) {
            map = new ScreenSurfaceMap();
            cachedMaps.put(position, map);
            if (cachedMaps.size() > 32) cachedMaps.remove(cachedMaps.keySet().iterator().next());
        }
        map.update(world, position, world.getTotalWorldTime());
        DynamicTexture texture = DrawGameScreen.reusableMap;
        if (texture == null || texture.getTextureData().length != ScreenSurfaceMap.SIZE * ScreenSurfaceMap.SIZE) {
            texture = new DynamicTexture(ScreenSurfaceMap.SIZE, ScreenSurfaceMap.SIZE);
            DrawGameScreen.reusableMap = texture;
        }
        if (lastMap.get() != map || lastTexture != texture || lastRevision != map.revision()) {
            System.arraycopy(map.pixels(), 0, texture.getTextureData(), 0, map.pixels().length);
            texture.updateDynamicTexture();
            lastMap = new WeakReference<>(map);
            lastTexture = texture;
            lastRevision = map.revision();
        }
        screen.localMap = map.pixels();
        screen.mapDone = true;
    }
}
