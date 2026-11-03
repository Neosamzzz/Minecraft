package newearth.dimension;

import net.minecraft.world.World;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.IMob;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** An opt-in overworld test entry; no celestial body or dimension is registered. */
@Mod.EventBusSubscriber(modid = "newearth")
public final class LandscapeWorldType extends WorldType {
    private LandscapeWorldType() { super("newearth_preview"); }
    @SubscribeEvent public static void register(RegistryEvent.Register<Biome> event) {
        if (WorldType.byName("newearth_preview") == null) new LandscapeWorldType();
    }
    public static boolean peacefulLandscape(WorldType type, int dimension) {
        return type instanceof LandscapeWorldType && dimension == 0;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        World world = event.getWorld();
        if (peacefulLandscape(world.getWorldType(), world.provider.getDimension())
                && (event.getEntityLiving() instanceof IMob
                || event.getEntityLiving().isCreatureType(EnumCreatureType.MONSTER, false)))
            event.setResult(Event.Result.DENY);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST) public static void joinWorld(EntityJoinWorldEvent event) {
        World world = event.getWorld();
        if (!world.isRemote && peacefulLandscape(world.getWorldType(), world.provider.getDimension())
                && (event.getEntity() instanceof IMob
                || event.getEntity().isCreatureType(EnumCreatureType.MONSTER, false)))
            event.setCanceled(true);
    }
    @SubscribeEvent public static void createSpawn(net.minecraftforge.event.world.WorldEvent.CreateSpawnPosition event) {
        World world = event.getWorld();
        if (!peacefulLandscape(world.getWorldType(), world.provider.getDimension())) return;
        int[] at = new Landscape(world.getSeed()).spawnPoint();
        world.setSpawnPoint(new net.minecraft.util.math.BlockPos(at[0], at[1], at[2]));
        event.setCanceled(true);
    }
    @SubscribeEvent public static void unload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        World world = event.getWorld();
        if (!world.isRemote && peacefulLandscape(world.getWorldType(), world.provider.getDimension()))
            ReterraTerrain.release(world.getSeed());
    }
    @Override public BiomeProvider getBiomeProvider(World world) {
        HeightSupport.require(world);
        return new LandscapeBiomes(world.getSeed());
    }
    @Override public IChunkGenerator getChunkGenerator(World world, String options) {
        HeightSupport.require(world);
        return new LandscapeGenerator(world);
    }
    @Override public int getMinimumSpawnHeight(World world) { return Landscape.WATER_LEVEL + 1; }
}
