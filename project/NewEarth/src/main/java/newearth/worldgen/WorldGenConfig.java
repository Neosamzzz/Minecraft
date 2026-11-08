package newearth.worldgen;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/** Player-facing switches for the main-world island layout. */
public final class WorldGenConfig {
    public static boolean enabled = true;
    public static double mainIslandRadius = 140.0;
    public static double isletScaleBlocks = 160.0;
    public static double isletLevel = 0.52;
    public static double landLevel = 0.60;
    public static double beachWidth = 0.03;
    public static double deepLevel = 0.36;
    public static double isletFadeStart = 420.0;
    public static double isletFadeEnd = 900.0;

    private WorldGenConfig() { }

    public static void load(File configDirectory) {
        Configuration config = new Configuration(new File(configDirectory, "newearth-worldgen.cfg"));
        config.load();
        enabled = config.getBoolean("enabled", "worldgen", true,
                "Replace the main world layout with one spawn island and sparse small islands.");
        mainIslandRadius = config.getFloat("mainIslandRadius", "worldgen", 140.0F, 40.0F, 600.0F,
                "Radius of the spawn island in blocks.");
        isletScaleBlocks = config.getFloat("isletScaleBlocks", "worldgen", 160.0F, 40.0F, 800.0F,
                "Feature size of the small islands in blocks.");
        isletLevel = config.getFloat("isletLevel", "worldgen", 0.52F, 0.30F, 0.80F,
                "How rarely small islands appear; higher means fewer.");
        landLevel = config.getFloat("landLevel", "worldgen", 0.60F, 0.35F, 0.80F,
                "Field value where ocean becomes land.");
        beachWidth = config.getFloat("beachWidth", "worldgen", 0.03F, 0.005F, 0.12F,
                "Width of the beach ring.");
        deepLevel = config.getFloat("deepLevel", "worldgen", 0.36F, 0.10F, 0.70F,
                "Field value below which the sea is deep ocean.");
        isletFadeStart = config.getFloat("isletFadeStart", "worldgen", 420.0F, 100.0F, 2000.0F,
                "No small islands closer than this to the spawn island.");
        isletFadeEnd = config.getFloat("isletFadeEnd", "worldgen", 900.0F, 200.0F, 4000.0F,
                "Small islands reach full density from this distance on.");
        if (config.hasChanged()) config.save();
    }
}
