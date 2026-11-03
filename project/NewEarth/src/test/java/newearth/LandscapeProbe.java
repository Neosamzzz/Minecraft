package newearth;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import javax.imageio.ImageIO;
import net.minecraft.block.Block;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import newearth.dimension.Landscape;
import newearth.dimension.LandscapeBiomes;
import newearth.dimension.LandscapeGenerator;
import newearth.dimension.LandscapeWorldType;

/** Runs the production terrain builder without a client, server, or world save. */
public final class LandscapeProbe {
    private static int checks;
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard = new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Bootstrap.register();
        WorldType original = WorldType.DEFAULT;
        LandscapeWorldType.register(null);
        WorldType preview = WorldType.byName("newearth_preview");
        check(preview != null && preview != original, "Explicit preview world type exists");
        LandscapeWorldType.register(null);
        check(WorldType.byName("newearth_preview") == preview && WorldType.DEFAULT == original,
                "Registration is idempotent and leaves default world alone");
        check(LandscapeProbe.class.getResource("/META-INF/LICENSE-ReTerraForged.txt") != null, "Adapted algorithm ships its MIT notice");
        check(new Landscape(0).height(0,0) != new Landscape(42).height(0,0), "Origin is not pinned to a seed-independent noise-lattice summit");
        verifyPeacefulWorld(preview);
        for (long seed : new long[] {42, -20261103L}) distribution(seed);
        Landscape field = new Landscape(42);
        LandscapeBiomes provider = new LandscapeBiomes(42);
        for (int[] at : new int[][] {{0,0}, {-1,-1}}) {
            int x = at[0], z = at[1];
            ChunkPrimer first = LandscapeGenerator.terrain(field, x, z);
            LandscapeGenerator.terrain(field, x + 1, z);
            LandscapeGenerator.terrain(field, x, z - 1);
            ChunkPrimer second = LandscapeGenerator.terrain(new Landscape(42), x, z);
            for (int bx = 0; bx < 16; bx++) for (int bz = 0; bz < 16; bz++) {
                check(first.getBlockState(bx, 0, bz).getBlock() == Blocks.BEDROCK, "Solid bedrock floor");
                int wx = x * 16 + bx, wz = z * 16 + bz;
                int top = (int) Math.floor(field.height(wx, wz));
                check(first.getBlockState(bx, top, bz).isFullBlock(), "No air gaps at sampled surface");
                if (top < Landscape.WATER_LEVEL)
                    check(first.getBlockState(bx, Landscape.WATER_LEVEL, bz).getBlock() == Blocks.WATER,
                            "Basin filled at consistent water level");
                for (int y = 0; y < 256; y++)
                    if (first.getBlockState(bx,y,bz) != second.getBlockState(bx,y,bz))
                        throw new AssertionError("Chunk depends on generation order at " + wx + "," + y + "," + wz);
            }
            check(true, "Full block states repeat after neighbour generation");
            Biome[] bulk = provider.getBiomes(null, x * 16, z * 16, 16, 16);
            for (int i = 0; i < 256; i++) check(bulk[i] == provider.getBiome(
                    new BlockPos(x * 16 + i % 16, 0, z * 16 + i / 16)), "Point and bulk biomes agree");
        }
        Biome[] coarse = provider.getBiomesForGeneration(null, -13, -19, 7, 5);
        for (int i = 0; i < coarse.length; i++) check(coarse[i] == provider.getBiome(
                new BlockPos((-13 + i % 7) * 4, 0, (-19 + i / 7) * 4)), "Quarter-scale biome coordinates");
        check(provider.findBiomePosition(0, 0, 256, Arrays.asList(Biomes.PLAINS, Biomes.FOREST, Biomes.OCEAN,
                Biomes.RIVER, Biomes.EXTREME_HILLS, Biomes.TAIGA, Biomes.JUNGLE, Biomes.SAVANNA, Biomes.ICE_MOUNTAINS), new java.util.Random(1)) != null, "Spawn biome search works");
        check(!provider.areBiomesViable(0,0,64,java.util.Collections.<Biome>emptyList()), "Viability uses actual distribution");
        verifyCrossingCanopy(field);
        // Warm up separately; report chunk construction only, not lighting, disk I/O or DH work.
        for (int i = 0; i < 4; i++) LandscapeGenerator.terrain(field, i, 0);
        long start = System.nanoTime();
        for (int i = 0; i < 32; i++) LandscapeGenerator.terrain(field, i % 8, i / 8);
        System.out.printf(java.util.Locale.ROOT, "Terrain construction: %.3f ms/chunk (32 adjacent chunks, one thread)%n",
                (System.nanoTime() - start) / 32000000.0);
        map(field);
        System.out.println("NewEarth landscape: " + checks + " checks passed (isolated; no game, save or shaders)");
    }
    // Forge injects this override at launch; the isolated runner has no event transformer.
    private static final class JoinEvent extends net.minecraftforge.event.entity.EntityJoinWorldEvent {
        JoinEvent(net.minecraft.entity.Entity entity, net.minecraft.world.World world) { super(entity, world); }
        @Override public boolean isCancelable() { return true; }
    }
    private static void verifyPeacefulWorld(WorldType preview) throws Exception {
        check(LandscapeWorldType.peacefulLandscape(preview, 0), "Preview overworld is peaceful");
        check(!LandscapeWorldType.peacefulLandscape(WorldType.DEFAULT, 0), "Ordinary overworld unchanged");
        check(!LandscapeWorldType.peacefulLandscape(preview, -1), "Nether unchanged even in a preview save");
        check(!LandscapeWorldType.peacefulLandscape(preview, 1), "End unchanged even in a preview save");
        java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer)
                unsafe.allocateInstance(net.minecraft.world.WorldServer.class);
        net.minecraft.world.storage.WorldInfo info = new net.minecraft.world.storage.WorldInfo(new net.minecraft.nbt.NBTTagCompound());
        java.lang.reflect.Field infoField = net.minecraft.world.World.class.getDeclaredField("worldInfo");
        infoField.setAccessible(true); infoField.set(world, info);
        java.lang.reflect.Field providerField = net.minecraft.world.World.class.getDeclaredField("provider");
        providerField.setAccessible(true); providerField.set(world, new net.minecraft.world.WorldProviderSurface());
        world.provider.setDimension(0);
        info.setTerrainType(preview);
        net.minecraft.entity.Entity zombie = (net.minecraft.entity.Entity) unsafe.allocateInstance(net.minecraft.entity.monster.EntityZombie.class);
        net.minecraft.entity.Entity cow = (net.minecraft.entity.Entity) unsafe.allocateInstance(net.minecraft.entity.passive.EntityCow.class);
        net.minecraftforge.event.entity.EntityJoinWorldEvent hostile = new JoinEvent(zombie,world);
        LandscapeWorldType.joinWorld(hostile);
        check(hostile.isCanceled(), "Hostile joining preview is rejected, including loaded entities and eggs");
        net.minecraftforge.event.entity.EntityJoinWorldEvent animal = new JoinEvent(cow,world);
        LandscapeWorldType.joinWorld(animal);
        check(!animal.isCanceled(), "Passive animals remain allowed");
        net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn natural =
                new net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn((net.minecraft.entity.EntityLiving)zombie,world,0,80,0);
        LandscapeWorldType.checkSpawn(natural);
        check(natural.getResult() == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY, "Natural hostile spawning denied");
        net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn passive =
                new net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn((net.minecraft.entity.EntityLiving)cow,world,0,80,0);
        LandscapeWorldType.checkSpawn(passive);
        check(passive.getResult() == net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT, "Passive spawn decision unchanged");
        info.setTerrainType(WorldType.DEFAULT);
        net.minecraftforge.event.entity.EntityJoinWorldEvent ordinary = new JoinEvent(zombie,world);
        LandscapeWorldType.joinWorld(ordinary);
        check(!ordinary.isCanceled(), "Ordinary world retains hostile mobs");
        info.setTerrainType(preview); world.provider.setDimension(-1);
        net.minecraftforge.event.entity.EntityJoinWorldEvent nether = new JoinEvent(zombie,world);
        LandscapeWorldType.joinWorld(nether);
        check(!nether.isCanceled(), "Other dimensions retain hostile mobs");
    }
    private static void distribution(long seed) throws Exception {
        Landscape field = new Landscape(seed);
        Landscape other = new Landscape(seed ^ 99);
        boolean differs = false;
        double maxStep = 0;
        for (int[] region : new int[][] {{0,0}, {-128,-128}, {256,640}}) {
            for (int dz = 0; dz < 128; dz += 4) for (int dx = 0; dx < 128; dx += 4) {
                int x = region[0] + dx, z = region[1] + dz;
                double h = field.height(x,z);
                check(Double.isFinite(h) && h >= 5 && h < 252, "Mapped terrain fits the 1.12 block range");

                maxStep = Math.max(maxStep, Math.abs(h-field.height(x+1,z)));
                differs |= h != other.height(x,z);
            }
        }
        check(differs, "World seed changes landscape");
        int[] spawn = field.spawnPoint();
        check(field.height(spawn[0],spawn[2]) > Landscape.WATER_LEVEL + 1,
                "Spawn search selects dry land on the nearest continent");
        System.out.printf("Seed %d spawn: /tp %d %d %d; sampled maximum step %.2f%n",
                seed, spawn[0], spawn[1], spawn[2], maxStep);
        if (seed == 42) {
            double peak = field.height(2048,3968);
            check(peak > 200 && peak < 252, "Original RTF peak remains above Y200 after height adaptation");
            System.out.printf("Mountain: /tp 2048 %d 3968%n", (int)peak+3);
            profile(field,2048,3968,2048,3488);
        }
    }
    private static void verifyCrossingCanopy(Landscape field) {
        for (int cz = 64; cz < 128; cz++) for (int cx = 0; cx < 64; cx++) {
            int tx = cx * 8 + 1 + (int)(field.unit(cx,cz,30)*6);
            int tz = cz * 8 + 1 + (int)(field.unit(cx,cz,31)*6);
            if (Math.floorMod(tx,16) != 1 || !field.tree(tx,tz)) continue;
            int ground = (int)Math.floor(field.height(tx,tz));
            int trunk = 5 + (int)(field.unit(tx,tz,32)*3);
            ChunkPrimer left = LandscapeGenerator.terrain(field, Math.floorDiv(tx,16)-1, Math.floorDiv(tz,16));
            ChunkPrimer right = LandscapeGenerator.terrain(field, Math.floorDiv(tx,16), Math.floorDiv(tz,16));
            Block a = left.getBlockState(15, ground+trunk-1, Math.floorMod(tz,16)).getBlock();
            Block b = right.getBlockState(0, ground+trunk-1, Math.floorMod(tz,16)).getBlock();
            check(a == Blocks.LEAVES && b == Blocks.LEAVES, "Tree canopy spans independently generated chunks");
            return;
        }
        throw new AssertionError("No boundary tree tested");
    }
    private static void profile(Landscape field, int peakX, int peakZ, int viewX, int viewZ) throws Exception {
        BufferedImage image = new BufferedImage(1200,340,BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.setColor(new java.awt.Color(180,210,230)); g.fillRect(0,0,1200,340);
        double distance = Math.hypot(peakX-viewX, peakZ-viewZ);
        double ux = (peakX-viewX)/distance, uz = (peakZ-viewZ)/distance;
        double scale = 0.8;
        for (int i = 0; i < 1200; i++) {
            double along = i/scale - 250;
            int x = viewX + (int)(ux*along), z = viewZ+(int)(uz*along);
            double h = field.height(x,z);
            int top = 300-(int)(h*scale);
            g.setColor(h > 205 ? java.awt.Color.WHITE : h > 178 ? java.awt.Color.GRAY : new java.awt.Color(112,155,73));
            g.drawLine(i,top,i,300);
            if (h < Landscape.WATER_LEVEL) {
                g.setColor(new java.awt.Color(55,118,164));
                g.drawLine(i,300-(int)(Landscape.WATER_LEVEL*scale),i,top);
            }
        }
        g.setColor(java.awt.Color.BLACK);
        g.drawString("Seed 42 | terrain section through meadow viewpoint and mountain | equal horizontal / vertical scale",15,20);
        g.drawString("Y=200",15,300-(int)(200*scale));
        g.drawLine(75,300-(int)(200*scale),1180,300-(int)(200*scale));
        g.dispose(); ImageIO.write(image,"png",new File("mountain-profile.png"));
    }
    private static void map(Landscape field) throws Exception {
        BufferedImage image = new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < 256; z++) for (int x = 0; x < 256; x++) {
            int wx = x + 1920, wz = z + 3840;
            double h = field.height(wx,wz), wood = field.woodland(wx,wz);
            int r,g,b;
            if (h < Landscape.WATER_LEVEL) { r=55; g=118; b=164; }
            else if (h > 178) { r=155; g=153; b=140; }
            else { r=(int)(133-wood*80); g=(int)(166-wood*60); b=(int)(88-wood*28); }
            double shade = Math.max(0.55,Math.min(1.2,1+(field.height(wx-3,wz-3)-h)*0.045));
            image.setRGB(x,z,((int)(r*shade)<<16)|((int)(g*shade)<<8)|(int)(b*shade));
        }
        ImageIO.write(image,"png",new File("landscape.png"));
    }
}
