package newearth.worldgen;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

/** Player-facing switches for the main-world island layout. */
public final class WorldGenConfig {
    public static boolean enabled = true;
    public static boolean mainVillage = true;
    public static double mainIslandRadius = 100.0;
    public static double isletScaleBlocks = 60.0;
    public static double isletLevel = 0.38;
    public static double landLevel = 0.60;
    public static double beachWidth = 0.03;
    public static double shelfWidth = 0.06;
    public static double deepLevel = 0.50;

    private WorldGenConfig() { }

    public static void load(File configDirectory) {
        Configuration config = new Configuration(new File(configDirectory, "newearth-worldgen.cfg"));
        config.load();
        enabled = config.getBoolean("enabled", "worldgen", true,
                "Replace the main world layout with one spawn island and sparse small islands.");
        mainVillage = config.getBoolean("mainVillage", "worldgen", true,
                "Place one village on the main island.");
        mainIslandRadius = config.getFloat("mainIslandRadius", "worldgen", 100.0F, 20.0F, 600.0F,
                "Radius of the spawn island in blocks.");
        isletScaleBlocks = config.getFloat("isletScaleBlocks", "worldgen", 60.0F, 40.0F, 800.0F,
                "Feature size of the small islands in blocks.");
        isletLevel = config.getFloat("isletLevel", "worldgen", 0.38F, 0.10F, 0.80F,
                "How rarely small islands appear; higher means fewer.");
        landLevel = config.getFloat("landLevel", "worldgen", 0.60F, 0.35F, 0.80F,
                "Field value where ocean becomes land.");
        beachWidth = config.getFloat("beachWidth", "worldgen", 0.03F, 0.005F, 0.12F,
                "Width of the beach ring.");
        shelfWidth = config.getFloat("shelfWidth", "worldgen", 0.06F, 0.0F, 0.25F,
                "Width of the shallow shelf between beach and ocean.");
        deepLevel = config.getFloat("deepLevel", "worldgen", 0.50F, 0.10F, 0.70F,
                "Field value below which the sea is deep ocean.");
        if (config.hasChanged()) config.save();
    }
}
