package newearth.dimension;

/** Seeded, world-coordinate fields shared by terrain, biomes and decoration. */
public final class Landscape {
    public static final int WATER_LEVEL = 62;
    private final long seed;
    private final double directionX, directionZ;

    public Landscape(long seed) {
        this.seed = seed;
        double angle = unit(0, 0, 50) * Math.PI;
        directionX = Math.cos(angle);
        directionZ = Math.sin(angle);
    }

    public double height(int x, int z) {
        // Warped mountain belts keep a substantial range within exploring distance of the lowlands.
        double u = x * directionX + z * directionZ;
        double v = -x * directionZ + z * directionX;
        double pu = u + 350 * signed(u / 850.0, v / 850.0, 40);
        double pv = v + 350 * signed(u / 850.0, v / 850.0, 41);
        double distance = Math.abs(gradientNoise(pu / 1400.0 + 0.37, pv / 1400.0 + 0.61, 1));
        double massif = 1 - smooth(0.015, 0.32, distance);
        double detail = 1 - Math.abs(gradientNoise(pu / 360.0, pv / 360.0, 2));
        double crest = 0.66 + 0.34 * detail;
        double shoulder = noise(u / 190.0, v / 260.0, 4);
        double base = 75 + 10 * noise(x / 650.0, z / 650.0, 3)
                + 2 * signed(x / 180.0, z / 180.0, 5);
        double branches = 1 - Math.abs(gradientNoise(pu / 145.0, pv / 145.0, 52));
        double crags = 1 - Math.abs(gradientNoise(pu / 65.0, pv / 65.0, 53));
        double relief = 132 * crest + 22 * branches + 10 * crags * branches
                + 4 * branches * crags * signed(pu / 31.0, pv / 31.0, 54);
        double height = base + Math.pow(massif, 1.15) * relief
                + 18 * massif * (1 - massif) * shoulder;
        if (height > 235) height = 235 + (height - 235) * 0.3;
        // Water belongs to the lowlands; it must not cut every mountain down to sea level.
        double px = x + 220 * signed(x / 800.0, z / 800.0, 42);
        double pz = z + 220 * signed(x / 800.0, z / 800.0, 43);
        double river = Math.abs(noise(px / 950.0, pz / 950.0, 6) * 2 - 1);
        double lowland = 1 - smooth(0.015, 0.20, massif);
        double valley = (1 - smooth(0.012, 0.075, river)) * lowland;
        height = mix(height, 58 + 5 * smooth(0, 0.035, river), valley);
        double lake = (1 - smooth(0.13, 0.23, noise(px / 1100.0, pz / 1100.0, 7))) * lowland;
        height = mix(height, 55 + 4 * noise(x / 260.0, z / 260.0, 8), lake);
        return Math.max(5, Math.min(245, height));
    }

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

    private double noise(double x, double z, int salt) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        double fx = fade(x - ix), fz = fade(z - iz);
        return mix(mix(unit(ix, iz, salt), unit(ix + 1, iz, salt), fx),
                mix(unit(ix, iz + 1, salt), unit(ix + 1, iz + 1, salt), fx), fz);
    }
    private double gradientNoise(double x, double z, int salt) {
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
    private double signed(double x, double z, int salt) { return noise(x, z, salt) * 2 - 1; }
    private static double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static double smooth(double a, double b, double v) {
        double t = Math.max(0, Math.min(1, (v - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }
    private static double mix(double a, double b, double t) { return a + (b - a) * t; }
}
