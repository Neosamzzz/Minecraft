/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.noise.module;

import newearth.terrain.world.worldgen.util.PosUtil;

@Deprecated
public record Cache2d(Noise noise, ThreadLocal<Cached> cache) implements Noise {

	public Cache2d(Noise noise) {
		this(noise, ThreadLocal.withInitial(() -> {
			return new Cached(noise);
		}));
	}
	
	@Override
	public float compute(float x, float z, int seed) {
		return this.cache.get().compute(x, z, seed);
	}

	@Override
	public float minValue() {
		return this.noise.minValue();
	}

	@Override
	public float maxValue() {
		return this.noise.maxValue();
	}

	@Override
	public Noise mapAll(Visitor visitor) {
		return visitor.apply(new Cache2d(this.noise.mapAll(visitor)));
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Cache2d other && other.noise.equals(this.noise);
	}

	public static class Cached implements Noise {
		public Noise noise;
		public long lastPos = Long.MIN_VALUE;
		public float value;
		
		public Cached(Noise noise) {
			this.noise = noise;
		}
		
		@Override
		public float compute(float x, float z, int seed) {
			long newPos = PosUtil.packf(x, z);
			if(this.lastPos != newPos) {
				this.value = this.noise.compute(x, z, seed);
				this.lastPos = newPos;
			}
			return this.value;
		}

		@Override
		public float minValue() {
			return this.noise.minValue();
		}

		@Override
		public float maxValue() {
			return this.noise.maxValue();
		}

		@Override
		public Noise mapAll(Visitor visitor) {
			return visitor.apply(this);
		}

	}
}
