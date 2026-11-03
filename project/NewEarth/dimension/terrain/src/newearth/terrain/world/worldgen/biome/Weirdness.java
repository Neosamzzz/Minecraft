/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.biome;


public enum Weirdness implements BiomeParameter
{
    MID_SLICE_NORMAL_ASCENDING(-1.0f, -0.93333334f),
    HIGH_SLICE_NORMAL_ASCENDING(-0.93333334f, -0.7666667f),
    PEAK_NORMAL(-0.7666667f, -0.56666666f),
    HIGH_SLICE_NORMAL_DESCENDING(-0.56666666f, -0.4f),
    MID_SLICE_NORMAL_DESCENDING(-0.4f, -0.26666668f),
    LOW_SLICE_NORMAL_DESCENDING(-0.26666668f, -0.05f),
    VALLEY(-0.05f, 0.05f),
    LOW_SLICE_VARIANT_ASCENDING(0.05f, 0.26666668f),
    MID_SLICE_VARIANT_ASCENDING(0.26666668f, 0.4f),
    HIGH_SLICE_VARIANT_ASCENDING(0.4f, 0.56666666f),
    PEAK_VARIANT(0.56666666f, 0.7666667f),
    HIGH_SLICE_VARIANT_DESCENDING(0.7666667f, 0.93333334f),
    MID_SLICE_VARIANT_DESCENDING(0.93333334f, 1.0f);

    private float min;
    private float max;

    private Weirdness(float min, float max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public float min() {
        return this.min;
    }

    @Override
    public float max() {
        return this.max;
    }
}

