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
        for (long seed : new long[] {0, 42, -20261103L, Long.MAX_VALUE}) distribution(seed);
        Landscape field = new Landscape(42);
        LandscapeBiomes provider = new LandscapeBiomes(42);
        for (int[] at : new int[][] {{0,0}, {-1,-1}, {81,-37}, {-130,210}}) {
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
        check(provider.findBiomePosition(0, 0, 256, Arrays.asList(Biomes.PLAINS, Biomes.FOREST,
                Biomes.RIVER, Biomes.EXTREME_HILLS), new java.util.Random(1)) != null, "Spawn biome search works");
        check(!provider.areBiomesViable(0,0,64,Arrays.asList(Biomes.DESERT)), "Viability uses actual distribution");
        verifyCrossingCanopy(field);
        // Warm up separately; report chunk construction only, not lighting, disk I/O or DH work.
        for (int i = 0; i < 16; i++) LandscapeGenerator.terrain(field, i, -i);
        long start = System.nanoTime();
        for (int i = 0; i < 256; i++) LandscapeGenerator.terrain(field, i - 128, i % 19 - 9);
        System.out.printf(java.util.Locale.ROOT, "Terrain construction: %.3f ms/chunk (256 chunks, one thread)%n",
                (System.nanoTime() - start) / 256000000.0);
        map(field);
        System.out.println("NewEarth landscape: " + checks + " checks passed (isolated; no game, save or shaders)");
    }
    private static void distribution(long seed) throws Exception {
        Landscape field = new Landscape(seed);
        int water = 0, meadow = 0, forest = 0, mountain = 0, high = 0;
        int nearbyHigh = 0, peakX = 0, peakZ = 0;
        double peak = 0;
        double maxStep = 0;
        boolean differs = false;
        Landscape other = new Landscape(seed ^ 99);
        for (int z = -4096; z < 4096; z += 32) for (int x = -4096; x < 4096; x += 32) {
            double h = field.height(x,z);
            if (h > 200) {
                high++;
                if (Math.abs(x) <= 1024 && Math.abs(z) <= 1024) nearbyHigh++;
            }
            if (Math.abs(x) <= 1024 && Math.abs(z) <= 1024 && h > peak) {
                peak = h; peakX = x; peakZ = z;
            }
            if (h < Landscape.WATER_LEVEL) water++;
            else if (h > 150) mountain++;
            else if (field.woodland(x,z) < 0.02) meadow++;
            else if (field.woodland(x,z) > 0.9) forest++;
            maxStep = Math.max(maxStep, Math.abs(h - field.height(x + 1,z)));
            check(h >= 5 && h <= 245, "Terrain stays within build height");
            differs |= h != other.height(x,z);
        }
        check(water > 100 && meadow > 1000 && forest > 100 && mountain > 100,
                "Water, open meadow, forest interiors and mountains all occupy substantial areas");
        check(high > 1000 && nearbyHigh > 50,
                "Substantial terrain above Y200, including a range within 1024 blocks of origin");
        int wide = 0;
        for (int dz = -64; dz <= 64; dz += 16) for (int dx = -64; dx <= 64; dx += 16)
            if (field.height(peakX + dx, peakZ + dz) > 180) wide++;
        check(wide > 50, "High peak has a substantial mountain body, not an isolated spike");
        int viewX = peakX, viewZ = peakZ;
        double viewHeight = peak;
        outer: for (int radius = 128; radius <= 1408; radius += 32) {
            for (int direction = 0; direction < 32; direction++) {
                double angle = direction * Math.PI / 16;
                int vx = peakX + (int) (Math.cos(angle) * radius);
                int vz = peakZ + (int) (Math.sin(angle) * radius);
                double h = field.height(vx,vz);
                if (h > 70 && h < 100 && field.slope(vx,vz) < 0.25 && field.woodland(vx,vz) < 0.05) {
                    viewX = vx; viewZ = vz; viewHeight = h;
                    break outer;
                }
            }
        }
        check(viewHeight < 100, "Open lowland viewpoint exists near the mountain");
        System.out.printf(java.util.Locale.ROOT, "Mountain viewpoint: /tp %d %d %d %.1f 0%n",
                viewX, (int)Math.ceil(viewHeight)+2, viewZ,
                Math.toDegrees(Math.atan2(-(peakX-viewX), peakZ-viewZ)));
        if (seed == 42) profile(field, peakX, peakZ, viewX, viewZ);
        check(maxStep < 12, "No one-block discontinuities across sampled chunk and negative-coordinate boundaries");
        check(differs, "World seed changes landscape");
        System.out.printf(java.util.Locale.ROOT,
                "High terrain: %.1f%%; nearby peak (%d, %.1f, %d), %d nearby samples above Y200%n",
                high/655.36, peakX, peak, peakZ, nearbyHigh);
        System.out.printf(java.util.Locale.ROOT,
                "Seed %d: water %.1f%%, open meadow %.1f%%, forest interior %.1f%%, mountain %.1f%%; max step %.2f%n",
                seed, water/655.36, meadow/655.36, forest/655.36, mountain/655.36, maxStep);
    }
    private static void verifyCrossingCanopy(Landscape field) {
        for (int cx = -128; cx < 128; cx++) for (int cz = -128; cz < 128; cz++) {
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
        BufferedImage image = new BufferedImage(768,768,BufferedImage.TYPE_INT_RGB);
        for (int z = 0; z < 768; z++) for (int x = 0; x < 768; x++) {
            int wx = (x-384)*12, wz = (z-384)*12;
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
