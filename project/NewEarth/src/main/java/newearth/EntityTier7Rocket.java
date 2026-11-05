package newearth;

import asmodeuscore.core.astronomy.SpaceData.Engine_Type;
import galaxyspace.core.prefab.entities.EntityTier6Rocket;
import micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType;
import net.minecraft.world.World;

/** GS tier-6 rocket behavior with the tier-7 hull. */
public class EntityTier7Rocket extends EntityTier6Rocket {
    public EntityTier7Rocket(World world) {
        super(world);
    }

    public EntityTier7Rocket(World world, double x, double y, double z, EnumRocketType type) {
        super(world, x, y, z, type);
    }

    public EntityTier7Rocket(World world, double x, double y, double z, EnumRocketType type, Engine_Type engine) {
        super(world, x, y, z, type, engine);
    }
}
