package newearth.worldgen;

/**
 * Island layout for the main world: one large isolated island around the origin, sparse small islets
 * further out, and ocean everywhere else. Pure math so the shape can be tuned without launching the game.
 */
public final class IslandField {
    public enum Surface { DEEP_OCEAN, OCEAN, BEACH, LAND }

    public static final int PLAINS = 0, FOREST = 1, TAIGA = 2, DESERT = 3, HILLS = 4;

    private final Perlin2D shape;
    private final Perlin2D coast;
    private final Perlin2D temperature;
    private final Perlin2D humidity;

    /** 1 / islet feature size in blocks. */
    public double isletScale = 1.0 / 160.0;
    /** Raw islet noise value where an islet starts; higher means fewer islets. */
    public double isletLevel = 0.52;
    /** Shared field value where ocean becomes land. */
    public double landLevel = 0.60;
    /** Width of the beach ring, in field units. */
    public double beachWidth = 0.03;
    /** Below this value the sea is deep ocean. */
    public double deepLevel = 0.36;
    /** Radius of the guaranteed main island around the origin, in blocks. */
    public double mainIslandRadius = 140.0;
    /** How far the main island field rises above the land level at its centre. */
    public double mainIslandRise = 0.12;
    /** No islets closer than this to the origin, so the main island stays isolated. */
    public double isletFadeStart = 420.0;
    /** Islets reach full density from this distance on. */
    public double isletFadeEnd = 900.0;

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
        double distance = Math.sqrt(x * x + z * z);
        double fade = clamp((distance - isletFadeStart) / Math.max(1.0, isletFadeEnd - isletFadeStart), 0.0, 1.0);
        double islets = landLevel + (isletNoise(x, z) - isletLevel) * fade - (1.0 - fade) * 0.5;
        double main = Double.NEGATIVE_INFINITY;
        if (distance < mainIslandRadius) {
            double t = distance / mainIslandRadius;
            main = landLevel + mainIslandRise * (1.0 - t * t)
                    + 0.04 * coast.noise(x * 0.02, z * 0.02) * (1.0 - t);
        }
        return Math.max(islets, main);
    }

    public Surface surfaceAt(int x, int z) {
        return surface(value(x, z));
    }

    public Surface surface(double v) {
        if (v >= landLevel + beachWidth) return Surface.LAND;
        if (v >= landLevel) return Surface.BEACH;
        if (v >= deepLevel) return Surface.OCEAN;
        return Surface.DEEP_OCEAN;
    }

    public int landBiomeAt(int x, int z) {
        return landBiomeAt(value(x, z), x, z);
    }

    public int landBiomeAt(double v, int x, int z) {
        if (v >= landLevel + beachWidth + 0.09) return HILLS;
        double t = temperature.noise(x / 1450.0, z / 1450.0);
        if (t < -0.32) return TAIGA;
        if (t > 0.36) return DESERT;
        return humidity.noise(x / 900.0, z / 900.0) > 0.05 ? FOREST : PLAINS;
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : value > max ? max : value;
    }
}
