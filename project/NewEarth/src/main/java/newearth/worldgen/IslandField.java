package newearth.worldgen;

/**
 * Island layout for the main world: one large isolated island around the origin, sparse small islets
 * further out, and ocean everywhere else. Pure math so the shape can be tuned without launching the game.
 */
public final class IslandField {
    public enum Surface { DEEP_OCEAN, OCEAN, SHELF, BEACH, LAND }

    public static final int PLAINS = 0, FOREST = 1, TAIGA = 2, DESERT = 3, HILLS = 4;

    private final Perlin2D shape;
    private final Perlin2D coast;
    private final Perlin2D temperature;
    private final Perlin2D humidity;

    /** 1 / islet feature size in blocks. */
    public double isletScale = 1.0 / 60.0;
    /** Raw islet noise value where an islet starts; higher means fewer islets. */
    public double isletLevel = 0.38;
    /** Shared field value where ocean becomes land. */
    public double landLevel = 0.60;
    /** Width of the beach ring, in field units. */
    public double beachWidth = 0.03;
    /** Width of the shallow shelf between beach and ocean, in field units. */
    public double shelfWidth = 0.06;
    /** Below this value the sea is deep ocean. */
    public double deepLevel = 0.50;
    /** Radius of the guaranteed main island around the origin, in blocks. */
    public double mainIslandRadius = 100.0;
    /** How far the main island field rises above the land level at its centre. */
    public double mainIslandRise = 0.08;


    public IslandField(long seed) {
        shape = new Perlin2D(seed * 341873128712L + 132897987541L);
        coast = new Perlin2D(seed * 981625601L + 271828182L);
        temperature = new Perlin2D(seed * 1414213562L + 173205080L);
        humidity = new Perlin2D(seed * 2236067977L + 316227766L);
    }

    /** Raw islet noise before the land-level mapping; exposed for tuning. */
    public double isletNoise(double x, double z) {
        return shape.fractal(x * isletScale, z * isletScale, 3);
    }

    /** Island field; land is at or above {@link #landLevel}. */
    public double value(double x, double z) {
        double islets = landLevel + (isletNoise(x, z) - isletLevel);

        double warpScale = Math.max(18.0, mainIslandRadius * 0.45);
        double warpAmount = mainIslandRadius * 0.45;
        double warpedX = x + warpAmount * coast.noise(x / warpScale, z / warpScale);
        double warpedZ = z + warpAmount * coast.noise(x / warpScale + 37.0, z / warpScale - 21.0);
        double warpedDistance = Math.sqrt(warpedX * warpedX + warpedZ * warpedZ);

        double main = Double.NEGATIVE_INFINITY;
        if (warpedDistance < mainIslandRadius) {
            double t = warpedDistance / mainIslandRadius;
            double taper = 1.0 - t * t;
            main = landLevel + mainIslandRise * taper
                    + 0.18 * shape.noise(x / 22.0, z / 22.0) * t * t
                    + 0.07 * shape.noise(x / 10.0, z / 10.0) * t * t * t;
        }
        return Math.max(islets, main);
    }
    public Surface surface(double v) {
        if (v >= landLevel + beachWidth) return Surface.LAND;
        if (v >= landLevel) return Surface.BEACH;
        if (v >= landLevel - shelfWidth) return Surface.SHELF;
        if (v >= deepLevel) return Surface.OCEAN;
        return Surface.DEEP_OCEAN;
    }

    public int landBiomeAt(int x, int z) {
        return landBiomeAt(value(x, z), x, z);
    }

    public int landBiomeAt(double v, int x, int z) {
        if (v >= landLevel + beachWidth + 0.22) return HILLS;
        double t = temperature.noise(x / 1450.0, z / 1450.0);
        if (t < -0.32) return TAIGA;
        if (t > 0.36) return DESERT;
        return humidity.noise(x / 900.0, z / 900.0) > 0.05 ? FOREST : PLAINS;
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : value > max ? max : value;
    }
}
