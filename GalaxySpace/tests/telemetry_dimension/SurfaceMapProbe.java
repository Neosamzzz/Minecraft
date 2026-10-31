package diagnostic;

import java.lang.reflect.*;
import java.util.*;
import sun.misc.Unsafe;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.*;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.tileentity.TileEntity;
import micdoodle8.mods.galacticraft.core.client.screen.DrawGameScreen;

/** Real Minecraft chunk/block APIs; rejects any access which could load a chunk. */
public class SurfaceMapProbe {
    static Unsafe unsafe;
    static int checks;
    static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    static <T> T alloc(Class<T> type) throws Exception { return type.cast(unsafe.allocateInstance(type)); }
    public static class Provider extends ChunkProviderClient {
        Map<Long,Chunk> chunks;
        public Provider() { super(null); }
        @Override public Chunk func_186026_b(int x,int z) { return chunks.get(((long)x << 32) ^ (z & 0xffffffffL)); }
        @Override public Chunk func_186025_d(int x,int z) { throw new AssertionError("Tried to load/generate a chunk"); }
    }
    public static class ClientWorld extends WorldClient {
        Provider chunks;
        long tick;
        public ClientWorld() { super(null,null,0,null,null); }
        @Override public ChunkProviderClient func_72863_F() { return chunks; }
        @Override public int func_72800_K() { return 256; }
        @Override public long func_82737_E() { return tick; }
    }
    public static class Texture extends DynamicTexture {
        int[] data;int uploads;
        public Texture() { super(352,352); }
        @Override public int[] func_110565_c() { return data; }
        @Override public void func_110564_a() { uploads++; }
    }
    static DrawGameScreen screen(ClientWorld world) throws Exception {
        DrawGameScreen screen=alloc(DrawGameScreen.class);
        TileEntity driver=new TileEntity() {};driver.func_145834_a(world);driver.func_174878_a(new BlockPos(176,64,176));
        screen.driver=driver;return screen;
    }
    public static class SurfaceChunk extends Chunk {
        static int reads;
        IBlockState surface;
        boolean transparentRoof;
        public SurfaceChunk() { super(null,0,0); }
        @Override public int func_76625_h() { return 64; }
        @Override public int func_76611_b(int x,int z) { return 71; }
        @Override public IBlockState func_186032_a(int x,int y,int z) {
            reads++;
            if (y == 70) return surface;
            if (y == 74 && transparentRoof) return Blocks.field_150359_w.func_176223_P();
            return Blocks.field_150350_a.func_176223_P();
        }
    }
    static ClientWorld world(int dimension,IBlockState block) throws Exception {
        ClientWorld w=alloc(ClientWorld.class);w.chunks=alloc(Provider.class);w.chunks.chunks=new HashMap<>();
        WorldProvider p=new WorldProviderSurface();p.setDimension(dimension);
        Field f=World.class.getDeclaredField("field_73011_w");f.setAccessible(true);f.set(w,p);
        for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++) {
            SurfaceChunk c=alloc(SurfaceChunk.class);c.surface=block;
            w.chunks.chunks.put(((long)x<<32)^(z & 0xffffffffL),c);
        }
        return w;
    }
    public static void main(String[] args) throws Exception {
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);unsafe=(Unsafe)f.get(null);
        net.minecraft.init.Bootstrap.func_151354_b();
        Class<?> map=Class.forName("galaxyspace.core.telemetry.ScreenSurfaceMap");
        Method sample=map.getMethod("sample",World.class,BlockPos.class,int.class,int.class,int.class,int[].class);
        Method update=map.getMethod("update",World.class,BlockPos.class,long.class);
        Method pixels=map.getMethod("pixels");
        int blank=map.getField("UNKNOWN").getInt(null),size=map.getField("SIZE").getInt(null);
        ClientWorld earth=world(0,Blocks.field_150349_c.func_176223_P());
        ClientWorld moon=world(-28,Blocks.field_150348_b.func_176223_P());
        ClientWorld mars=world(-29,Blocks.field_150354_m.func_176223_P());
        int[] colours=new int[3];int i=0;
        for(ClientWorld w:Arrays.asList(earth,moon,mars)) {
            int[] image=new int[16];sample.invoke(null,w,new BlockPos(0,64,0),4,0,4,image);
            colours[i++]=image[0];check(image[0]!=blank,"Loaded planet mapped as unknown");
            check(Arrays.stream(image).distinct().count()==1,"Negative coordinates/flat terrain differ");
        }
        check(colours[0]!=colours[1]&&colours[1]!=colours[2],"Same-coordinate planets share pixels");
        SurfaceChunk changed=(SurfaceChunk)earth.chunks.func_186026_b(-1,-1);
        changed.surface=Blocks.field_150348_b.func_176223_P();changed.transparentRoof=true;
        int[] image=new int[16];sample.invoke(null,earth,new BlockPos(0,64,0),4,0,4,image);
        check(image[0]==colours[1],"Building change or transparent block skip not reflected");
        earth.chunks.chunks.remove(((long)-1<<32)^(-1 & 0xffffffffL));
        sample.invoke(null,earth,new BlockPos(0,64,0),4,0,4,image);
        check(image[0]==colours[1],"Unloaded chunk erased previously mapped terrain");
        Object cache=map.newInstance();BlockPos centre=new BlockPos(0,64,0);
        update.invoke(cache,moon,centre,0L);int[] first=(int[])pixels.invoke(cache);
        first[first.length-1]=0xff123456;
        update.invoke(cache,mars,centre,0L);check(((int[])pixels.invoke(cache))[first.length-1]==blank,"World switch reused stale pixels");
        int[] second=(int[])pixels.invoke(cache);second[second.length-1]=0xff123456;
        update.invoke(cache,mars,new BlockPos(1000,64,0),0L);check(((int[])pixels.invoke(cache))[second.length-1]==blank,"Screen move reused stale pixels");
        check(size==352,"Changed original map dimensions");
        Class<?> screen=Class.forName("micdoodle8.mods.galacticraft.core.client.screen.DrawGameScreen");
        boolean ready=false,blocked=false;
        for(Method m:screen.getDeclaredMethods()) {
            if(m.getName().contains("prepareSurfaceMap"))ready=true;
            if(m.getName().contains("skipPredictedMap"))blocked=true;
        }
        check(ready&&blocked,"Actual GC map pipeline not intercepted");
        Texture texture=alloc(Texture.class);texture.data=new int[size*size];DrawGameScreen.reusableMap=texture;
        // Two simultaneously visible screens share a world but have different map centres.
        earth.chunks.chunks.put(1L<<32,moon.chunks.func_186026_b(0,0));
        DrawGameScreen a=screen(earth),b=screen(earth);
        b.driver.func_174878_a(new BlockPos(192,64,176));
        Method prepare=null;for(Method m:screen.getDeclaredMethods())if(m.getName().contains("prepareSurfaceMap")){prepare=m;m.setAccessible(true);}
        org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("doDraw",false);
        prepare.invoke(a,2,0F,ci);check(texture.uploads==0,"Other page touched satellite texture");
        prepare.invoke(a,1,0F,ci);check(a.mapDone,"Map page was not made ready");
        int firstUploads=texture.uploads;
        prepare.invoke(a,1,0F,ci);check(texture.uploads==firstUploads,"Same-frame render reuploaded unchanged data");
        for(long tick=2;tick<86;tick+=2){earth.tick=tick;prepare.invoke(a,1,0F,ci);}
        check(texture.uploads==firstUploads,"Partial scan uploaded intermediate frames");
        earth.tick=86;prepare.invoke(a,1,0F,ci);int earthColour=texture.data[0];
        check(earthColour!=blank,"First completed scan was not displayed");
        check(texture.uploads==firstUploads+1,"Completed scan did not upload exactly once");
        for(long tick=0;tick<=86;tick+=2){earth.tick=tick;prepare.invoke(b,1,0F,ci);}
        int moonColour=texture.data[0];check(earthColour!=moonColour,"Two screens shared cached terrain");
        prepare.invoke(a,1,0F,ci);check(texture.data[0]==earthColour,"Second screen overwrote first screen texture");
        firstUploads=texture.uploads;
        int reads=SurfaceChunk.reads;
        ((SurfaceChunk)earth.chunks.func_186026_b(0,0)).surface=Blocks.field_150348_b.func_176223_P();
        for(long tick=88;tick<600;tick+=2){earth.tick=tick;prepare.invoke(a,1,0F,ci);}
        check(texture.uploads==firstUploads,"Map refreshed before 30-second interval");
        check(SurfaceChunk.reads==reads,"Terrain was sampled between refresh cycles");
        for(long tick=600;tick<686;tick+=2){earth.tick=tick;prepare.invoke(a,1,0F,ci);}
        check(texture.data[0]==earthColour,"Partial scan replaced complete map");
        earth.tick=686;prepare.invoke(a,1,0F,ci);
        check(texture.data[0]==moonColour,"Complete refresh did not update edited terrain");
        check(texture.uploads==firstUploads+1,"Refresh uploaded more than one frame");
        firstUploads=texture.uploads;
        earth.chunks.chunks.remove(0L);
        for(long tick=688;tick<=1286;tick+=2){earth.tick=tick;prepare.invoke(a,1,0F,ci);}
        check(texture.data[0]==moonColour,"Refresh erased unloaded terrain");
        check(texture.uploads==firstUploads,"Unchanged map was uploaded again");
        DrawGameScreen reloaded=screen(earth);
        prepare.invoke(reloaded,1,0F,ci);
        check(texture.data[0]==moonColour,"Screen unload/recreation lost previously mapped terrain");
        // A genuinely loaded column cleared to void must not keep obsolete terrain forever.
        SurfaceChunk voidChunk=alloc(SurfaceChunk.class);voidChunk.surface=Blocks.field_150350_a.func_176223_P();
        earth.chunks.chunks.put(0L,voidChunk);
        for(long tick=1800;tick<=1886;tick+=2){earth.tick=tick;prepare.invoke(reloaded,1,0F,ci);}
        check(texture.data[0]==blank,"Loaded void did not clear removed terrain");
        Method cancel=null;for(Method m:screen.getDeclaredMethods())if(m.getName().contains("skipPredictedMap")){cancel=m;m.setAccessible(true);}
        org.spongepowered.asm.mixin.injection.callback.CallbackInfo cancelInfo=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("makeMap",true);
        cancel.invoke(a,cancelInfo);check(cancelInfo.isCancelled(),"Old Overworld prediction/request pipeline still runs");
        DrawGameScreen otherSession=screen(world(0,Blocks.field_150348_b.func_176223_P()));
        prepare.invoke(otherSession,1,0F,ci);
        check(texture.data[0]==blank,"New world with same dimension reused old session terrain");
        System.out.println("SURFACE_MAP_PASS checks="+checks+" simulated earth/moon/mars; real chunk/block APIs and injected GC entry point; negative coordinates, terrain edit, transparent block, unload, world/move reset, two-screen texture isolation, 30-second cycles, atomic frame updates, screen reload retention; no chunk generation");
    }
}
