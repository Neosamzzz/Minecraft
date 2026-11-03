package newearth.dimension;

/*
 * ReTerraForged 1.20.1 adaptation: PerlinRidge, WorleyEdge, Blender and Populators.
 * Copyright (c) 2023 ReTerraForged. SPDX-License-Identifier: MIT
 * https://github.com/racoonman2/ReTerraForged/tree/1.20.1
 * Full notice ships in META-INF/LICENSE-ReTerraForged.txt.
 */
final class ReterraTerrain {
    private final Landscape noise;
    private final double offsetX, offsetZ;
    ReterraTerrain(Landscape noise) {
        this.noise = noise;
        offsetX = noise.unit(0, 0, 98) * 10000;
        offsetZ = noise.unit(0, 0, 99) * 10000;
    }

    double height(int worldX, int worldZ) {
        double x = worldX + offsetX, z = worldZ + offsetZ;
        double px = x + fractal(x / 420.0, z / 420.0, 60, 2) * 240;
        double pz = z + fractal(x / 420.0, z / 420.0, 62, 2) * 240;
        double chain = cellular(px / 1100.0, pz / 1100.0);
        // Independent plains and mountain surfaces blend through a broad foothill region.
        double mountain = smooth(0.48, 0.70, chain);
        double plain = 78 + 5 * fractal(x / 700.0, z / 700.0, 70, 2)
                + 1.5 * fractal(x / 150.0, z / 150.0, 72, 2);
        double ridges = ridge(px / 470.0, pz / 470.0, 80);
        double broad = 1 - Math.abs(noise.gradientNoise(px / 950.0, pz / 950.0, 86));
        double highland = 124 + 75 * ridges + 37 * broad;
        double height = mix(plain, highland, mountain);
        // Three nested widths describe dry valley, river bank and submerged bed.
        double lowland = 1 - smooth(0.03, 0.30, mountain);
        double river = Math.abs(fractal(px / 1300.0, pz / 1300.0, 90, 2));
        double valley = (1 - smooth(0.035, 0.20, river)) * lowland;
        height = mix(height, Math.min(height, 68 + 8 * river / 0.20), valley);
        double bank = (1 - smooth(0.018, 0.055, river)) * lowland;
        height = mix(height, Math.min(height, 63 + 4 * river / 0.055), bank);
        double bed = (1 - smooth(0.006, 0.026, river)) * lowland;
        height = mix(height, 57 + 4 * river / 0.026, bed);
        double lake = (1 - smooth(0.12, 0.22, noise.noise(px / 1100, pz / 1100, 94))) * lowland;
        height = mix(height, 55 + 4 * noise.noise(x / 260.0, z / 260.0, 95), lake);
        if (height > 240) height = 240 + (height - 240) * 0.25;
        return Math.max(5, Math.min(245, height));
    }

    private double ridge(double x, double z, int salt) {
        double value = 0, weight = 1, amplitude = 2, spectral = 1, max = 0;
        for (int octave = 0; octave < 4; octave++) {
            double signal = 1 - Math.abs(noise.gradientNoise(x, z, salt + octave));
            signal *= signal * weight;
            weight = clamp(signal * amplitude);
            value += signal * spectral;
            max += spectral;
            spectral /= 2.35;
            amplitude *= 1.15;
            x *= 2.35; z *= 2.35;
        }
        return value / max;
    }

    private double cellular(double x, double z) {
        int ix = (int)Math.floor(x), iz = (int)Math.floor(z);
        double first = Double.MAX_VALUE, second = Double.MAX_VALUE;
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            int cx = ix + dx, cz = iz + dz;
            double vx = cx + 0.15 + noise.unit(cx, cz, 64) * 0.7 - x;
            double vz = cz + 0.15 + noise.unit(cx, cz, 65) * 0.7 - z;
            double distance = Math.sqrt(vx * vx + vz * vz);
            if (distance < first) { second = first; first = distance; }
            else if (distance < second) second = distance;
        }
        return clamp((first + second) / 2.0);
    }
    private double fractal(double x, double z, int salt, int octaves) {
        double value = 0, amplitude = 1, max = 0;
        for (int i = 0; i < octaves; i++) {
            value += noise.gradientNoise(x, z, salt + i) * amplitude;
            max += amplitude;
            x *= 2; z *= 2; amplitude *= 0.5;
        }
        return value / max;
    }
    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    private static double smooth(double a, double b, double value) {
        double t = clamp((value - a) / (b - a));
        return t * t * (3 - 2 * t);
    }
    private static double mix(double a, double b, double t) { return a + (b - a) * t; }
}
