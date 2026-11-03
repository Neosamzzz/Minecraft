package newearth.dimension;

import net.minecraft.world.World;
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
    @Override public BiomeProvider getBiomeProvider(World world) { return new LandscapeBiomes(world.getSeed()); }
    @Override public IChunkGenerator getChunkGenerator(World world, String options) {
        return new LandscapeGenerator(world);
    }
    @Override public int getMinimumSpawnHeight(World world) { return Landscape.WATER_LEVEL + 1; }
}
