package newearth;

import asmodeuscore.core.astronomy.SpaceData.Engine_Type;
import galaxyspace.core.prefab.entities.EntityTier6Rocket;
import micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType;
import net.minecraft.world.World;

/** GS tier-6 rocket behavior with the tier-7 hull. */
public class EntityTier7Rocket extends EntityTier6Rocket {
    public static final float HULL_SCALE_XZ = 0.97F;
    public static final float HULL_SCALE_Y = 1.36F;

    @Override public float getRenderOffsetY() { return -0.021F; }

    public EntityTier7Rocket(World world) {
        super(world);
        setSize(2.2F, 14.1F);
    }

    public EntityTier7Rocket(World world, double x, double y, double z, EnumRocketType type) {
        super(world, x, y, z, type);
        setSize(2.2F, 14.1F);
    }

    public EntityTier7Rocket(World world, double x, double y, double z, EnumRocketType type, Engine_Type engine) {
        super(world, x, y, z, type, engine);
        setSize(2.2F, 14.1F);
    }
}
