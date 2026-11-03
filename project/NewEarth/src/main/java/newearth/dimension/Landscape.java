package newearth.dimension;

/** Seeded, world-coordinate fields shared by terrain, biomes and decoration. */
public final class Landscape {
    public static final int WATER_LEVEL = 62;
    private final long seed;

    public Landscape(long seed) { this.seed = seed; }

    public double height(int x, int z) {
        double px = x + 650 * (noise(x / 1300.0, z / 1300.0, 40) * 2 - 1);
        double pz = z + 650 * (noise(x / 1300.0, z / 1300.0, 41) * 2 - 1);
        double land = noise(px / 2400.0, pz / 2400.0, 1);
        double mountains = smooth(0.50, 0.79, land);
        double ridge = 1.0 - Math.abs(noise((px + pz * 0.35) / 620.0, (pz - px * 0.35) / 620.0, 2) * 2 - 1);
        double base = 76 + 15 * noise(x / 900.0, z / 900.0, 3);
        double height = base + mountains * (48 + 104 * ridge)
                + mountains * 9 * noise(x / 145.0, z / 145.0, 4)
                + 2 * noise(x / 100.0, z / 100.0, 5);
        double river = Math.abs(noise(px / 1100.0, pz / 1100.0, 6) * 2 - 1);
        double valley = 1 - smooth(0.018, 0.12, river);
        // Cut into the landscape before surfacing, leaving broad banks and valley floors.
        height = mix(height, 57 + mountains * 65 + 5 * smooth(0.0, 0.055, river), valley);
        double lake = 1 - smooth(0.22, 0.34, noise(px / 780.0, pz / 780.0, 7));
        height = mix(height, 54 + 7 * noise(x / 260.0, z / 260.0, 8), lake * (1 - mountains));
        return Math.max(5, Math.min(245, height));
    }

    public double slope(int x, int z) {
        return Math.max(Math.abs(height(x + 4, z) - height(x - 4, z)),
                Math.abs(height(x, z + 4) - height(x, z - 4))) / 8;
    }

    public double woodland(int x, int z) {
        double px = x + 180 * (noise(x / 480.0, z / 480.0, 42) * 2 - 1);
        double pz = z + 180 * (noise(x / 480.0, z / 480.0, 43) * 2 - 1);
        return smooth(0.49, 0.66, noise(px / 420.0, pz / 420.0, 9));
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

    private double noise(double x, double z, int salt) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = fade(x - ix), fz = fade(z - iz);
        return mix(mix(unit(ix, iz, salt), unit(ix + 1, iz, salt), fx),
                mix(unit(ix, iz + 1, salt), unit(ix + 1, iz + 1, salt), fx), fz);
    }
    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double smooth(double a, double b, double v) {
        double t = Math.max(0, Math.min(1, (v - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }
    private static double mix(double a, double b, double t) { return a + (b - a) * t; }
}
