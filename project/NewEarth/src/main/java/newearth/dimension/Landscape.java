package newearth.dimension;

/** Seeded, world-coordinate fields shared by terrain, biomes and decoration. */
public final class Landscape {
    public static final int WATER_LEVEL = 62;
    private final long seed;
    private final ReterraTerrain terrain;

    public Landscape(long seed) {
        this.seed = seed;
        terrain = new ReterraTerrain(this);
    }

    public double height(int x, int z) { return terrain.height(x, z); }

    public double slope(int x, int z) {
        return Math.max(Math.abs(height(x + 4, z) - height(x - 4, z)),
                Math.abs(height(x, z + 4) - height(x, z - 4))) / 8;
    }

    public double woodland(int x, int z) {
        double px = x + 180 * (noise(x / 480.0, z / 480.0, 42) * 2 - 1);
        double pz = z + 180 * (noise(x / 480.0, z / 480.0, 43) * 2 - 1);
        return smooth(0.55, 0.72, noise(px / 600.0, pz / 600.0, 9));
    }

    public boolean tree(int x, int z) {
        double h = height(x, z);
        return h > WATER_LEVEL + 3 && h < 150 && slope(x, z) < 0.55
                && unit(x, z, 10) < woodland(x, z) * 0.85;
    }

    public double unit(int x, int z, int salt) {
        long n = seed ^ ((long) x * 0x632BE59BD9B4E019L)
                ^ ((long) z * 0x9E3779B97F4A7C15L) ^ ((long) salt * 0xD1B54A32D192ED03L);
        n = (n ^ (n >>> 30)) * 0xBF58476D1CE4E5B9L;
        n = (n ^ (n >>> 27)) * 0x94D049BB133111EBL;
        return ((n ^ (n >>> 31)) >>> 11) * 0x1.0p-53;
    }

    double noise(double x, double z, int salt) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = fade(x - ix), fz = fade(z - iz);
        return mix(mix(unit(ix, iz, salt), unit(ix + 1, iz, salt), fx),
                mix(unit(ix, iz + 1, salt), unit(ix + 1, iz + 1, salt), fx), fz);
    }
    double gradientNoise(double x, double z, int salt) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = x - ix, fz = z - iz;
        return mix(mix(gradient(ix, iz, salt, fx, fz), gradient(ix + 1, iz, salt, fx - 1, fz), fade(fx)),
                mix(gradient(ix, iz + 1, salt, fx, fz - 1),
                        gradient(ix + 1, iz + 1, salt, fx - 1, fz - 1), fade(fx)), fade(fz)) * 1.5;
    }
    private double gradient(int x, int z, int salt, double dx, double dz) {
        switch ((int) (unit(x, z, salt) * 8)) {
            case 0: return dx;
            case 1: return -dx;
            case 2: return dz;
            case 3: return -dz;
            case 4: return (dx + dz) * 0.7071067811865476;
            case 5: return (dx - dz) * 0.7071067811865476;
            case 6: return (-dx + dz) * 0.7071067811865476;
            default: return (-dx - dz) * 0.7071067811865476;
        }
    }
    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double smooth(double a, double b, double v) {
        double t = Math.max(0, Math.min(1, (v - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }
    private static double mix(double a, double b, double t) { return a + (b - a) * t; }
}
