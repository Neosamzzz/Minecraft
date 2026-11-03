/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.data.worldgen.preset.settings;

import newearth.terrain.world.worldgen.GeneratorContext;
import newearth.terrain.world.worldgen.cell.continent.Continent;
import newearth.terrain.world.worldgen.cell.continent.advanced.AdvancedContinentGenerator;
import newearth.terrain.world.worldgen.cell.continent.fancy.FancyContinentGenerator;
import newearth.terrain.world.worldgen.cell.continent.infinite.InfiniteContinentGenerator;
import newearth.terrain.world.worldgen.cell.continent.simple.MultiContinentGenerator;
import newearth.terrain.world.worldgen.cell.continent.simple.SingleContinentGenerator;
import newearth.terrain.world.worldgen.util.Seed;

public enum ContinentType {
    MULTI {
        
    	@Override
        public MultiContinentGenerator create(Seed seed, GeneratorContext context) {
            return new MultiContinentGenerator(seed, context);
        }
    }, 
    SINGLE {
        
    	@Override
        public SingleContinentGenerator create(Seed seed, GeneratorContext context) {
            return new SingleContinentGenerator(seed, context);
        }
    }, 
    MULTI_IMPROVED {
        
    	@Override
        public AdvancedContinentGenerator create(Seed seed, GeneratorContext context) {
            return new AdvancedContinentGenerator(seed, context);
        }
    }, 
    EXPERIMENTAL {
        
    	@Override
        public FancyContinentGenerator create(Seed seed, GeneratorContext context) {
            return new FancyContinentGenerator(seed, context);
        }
    },
    INFINITE {
        
    	@Override
        public InfiniteContinentGenerator create(Seed seed, GeneratorContext context) {
            return new InfiniteContinentGenerator(context);
        }
    };

    public abstract Continent create(Seed seed, GeneratorContext context);

    public String getSerializedName() {
		return this.name();
	}
}
