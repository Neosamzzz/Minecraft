package newearth;

import net.minecraft.world.World;

public class ItemTier10Rocket extends ItemTier9Rocket {
    @Override protected EntityTier9Rocket createRocket(World world, double x, double y, double z) {
        return new EntityTier10Rocket(world, x, y, z);
    }
}
