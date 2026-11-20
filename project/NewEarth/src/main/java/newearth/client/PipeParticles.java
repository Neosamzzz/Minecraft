package newearth.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PipeParticles {
    // AE2 rv6's Fluix core and outer covered cable textures are byte-identical.
    private static final String CORE = "newearth:blocks/transfer_pipe/covered_fluix";
    private static final String BUS = "newearth:blocks/transfer_pipe/monitor_back";
    private static final int GRID = 4;

    private PipeParticles() { }

    public static boolean hit(World world, RayTraceResult target, IBlockState state, int ports, ParticleManager effects) {
        TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite core = atlas.getAtlasSprite(CORE);
        if (core == atlas.getMissingSprite()) return false;
        if (world.rand.nextBoolean()) {
            Fragment fragment = new Fragment(world, target.hitVec, Vec3d.ZERO, state);
            fragment.setBlockPos(target.getBlockPos()).multipleParticleScaleBy(0.8F);
            fragment.setParticleTexture(sprite(world, atlas, core, ports));
            effects.addEffect(fragment);
        }
        return true;
    }

    public static boolean destroy(World world, BlockPos pos, IBlockState state, int ports, ParticleManager effects) {
        TextureMap atlas = Minecraft.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite core = atlas.getAtlasSprite(CORE);
        if (core == atlas.getMissingSprite()) return false;
        for (int i = 0; i < GRID * GRID * GRID; i++) {
            double x = (i % GRID + 0.5) / GRID;
            double y = (i / GRID % GRID + 0.5) / GRID;
            double z = (i / (GRID * GRID) + 0.5) / GRID;
            Vec3d at = new Vec3d(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
            Fragment fragment = new Fragment(world, at, new Vec3d(x - 0.5, y - 0.5, z - 0.5), state);
            fragment.setBlockPos(pos);
            fragment.setParticleTexture(sprite(world, atlas, core, ports));
            effects.addEffect(fragment);
        }
        return true;
    }

    private static TextureAtlasSprite sprite(World world, TextureMap atlas, TextureAtlasSprite core, int ports) {
        if (ports == 0 || world.rand.nextInt(ports + 1) == 0) return core;
        TextureAtlasSprite bus = atlas.getAtlasSprite(BUS);
        return bus == atlas.getMissingSprite() ? core : bus;
    }

    private static final class Fragment extends ParticleDigging {
        Fragment(World world, Vec3d at, Vec3d velocity, IBlockState state) {
            super(world, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z, state);
        }
    }
}
