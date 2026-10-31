package diagnostic;
import java.util.*;
import java.lang.reflect.*;
import sun.misc.Unsafe;
import net.minecraft.world.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.*;
import net.minecraft.entity.Entity;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import galaxyspace.systems.SolarSystem.planets.overworld.tile.TileEntityAdvLandingPad;
import galaxyspace.core.prefab.entities.EntityTier5Rocket;

public class BindingProbe {
    static Unsafe unsafe;
    static int checks;
    public static class ProbeWorld extends WorldServer {
        Map<BlockPos,TileEntity> tiles;
        Entity rocket;
        public ProbeWorld() { super(null,null,null,0,null); }
        @Override public net.minecraft.world.chunk.Chunk func_72964_e(int x,int z) { return new net.minecraft.world.chunk.Chunk(this,x,z); }
        @Override public java.util.List<AxisAlignedBB> func_184144_a(Entity entity, AxisAlignedBB box) { return java.util.Collections.emptyList(); }
        @Override public TileEntity func_175625_s(BlockPos pos) { return tiles.get(pos); }
        @Override public boolean func_175667_e(BlockPos pos) { return true; }
        @Override public boolean func_175668_a(BlockPos pos, boolean allowEmpty) { return true; }
        @Override public <T extends Entity> List<T> func_72872_a(Class<? extends T> type, AxisAlignedBB box) {
            return rocket == null ? Collections.emptyList() : Collections.singletonList(type.cast(rocket));
        }
    }
    static <T> T allocate(Class<T> type) throws Exception { return type.cast(unsafe.allocateInstance(type)); }
    static void check(boolean ok,String message) { checks++; if(!ok) throw new AssertionError(message); }
    static <T extends TileEntity> T put(ProbeWorld w,T tile,int x,int y,int z) {
        tile.func_145834_a(w); tile.func_174878_a(new BlockPos(x,y,z)); w.tiles.put(tile.func_174877_v(),tile); return tile;
    }
    public static void main(String[] args) throws Exception {
        Field uf=Unsafe.class.getDeclaredField("theUnsafe"); uf.setAccessible(true); unsafe=(Unsafe)uf.get(null);
        check(TileEntityLaunchController.class.getClassLoader()==net.minecraft.launchwrapper.Launch.classLoader,"GC must load through the transforming classloader");
        net.minecraft.init.Bootstrap.func_151354_b();
        micdoodle8.mods.galacticraft.core.util.ConfigManagerCore.rocketFuelFactor=1;
        System.out.println("BOOTSTRAP_OK");
        ProbeWorld w=allocate(ProbeWorld.class); w.tiles=new HashMap<>();
        Field provider=World.class.getDeclaredField("field_73011_w"); provider.setAccessible(true); provider.set(w,new WorldProviderSurface());
        for(int[] offset:new int[][]{{-3,0},{3,0},{0,-3},{0,3},{-3,-2},{3,2},{-2,-3},{2,3}}) {
            w.tiles.clear();
            TileEntityAdvLandingPad pad=put(w,allocate(TileEntityAdvLandingPad.class),0,64,0);
            Field init=TileEntityAdvLandingPad.class.getDeclaredField("initialised"); init.setAccessible(true); init.setBoolean(pad,true);
            TileEntityLaunchController controller=put(w,allocate(TileEntityLaunchController.class),offset[0],64,offset[1]);
            controller.controlEnabled=true; controller.launchDropdownSelection=EntityAutoRocket.EnumAutoLaunch.INSTANT.ordinal();
            EntityAutoRocket rocket=allocate(galaxyspace.core.prefab.entities.EntityTier4Rocket.class); rocket.field_70170_p=w; w.rocket=rocket;
            boolean can=controller.canAttachToLandingPad(w,pad.func_174877_v());
            System.out.println("ATTACH_CHECK "+Arrays.toString(offset)+" = "+can);
            check(can,"Controller rejects actual GS pad");
            pad.func_73660_a();
            check(controller.attachedDock==pad,"Controller attachedDock was not set by actual pad update");
            check(pad.getDockedEntity()==rocket,"Platform did not dock actual GS rocket");
            check(rocket.getLandingPad()==pad,"Rocket does not reference the GS platform");
            check(rocket.autoLaunchSetting==EntityAutoRocket.EnumAutoLaunch.INSTANT,"Rocket did not read controller's launch mode");
            check(rocket.autoLaunchCountdown==12,"Rocket countdown not initialized");
            Field active=EntityAutoRocket.class.getDeclaredField("activeLaunchController"); active.setAccessible(true);
            check(active.get(rocket)!=null,"Rocket activeLaunchController is null");
            controller.launchDropdownSelection=EntityAutoRocket.EnumAutoLaunch.TIME_30_SECONDS.ordinal(); rocket.autoLaunchCountdown=0;
            controller.updateRocketOnDockSettings();
            check(rocket.autoLaunchSetting==EntityAutoRocket.EnumAutoLaunch.TIME_30_SECONDS,"Controller changes did not reach rocket");
            check(rocket.autoLaunchCountdown==600,"Controller did not update countdown");
            BlockPos sentinel=new BlockPos(90,70,90);
            rocket.targetVec=sentinel;
            pad.func_73660_a();
            check(rocket.targetVec==sentinel,"Repeated pad tick cleared destination");
            Field tank=micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase.class.getDeclaredField("fuelTank"); tank.setAccessible(true); tank.set(rocket,new net.minecraftforge.fluids.FluidTank(2800));
            Field passengers=Entity.class.getDeclaredField("field_184244_h"); passengers.setAccessible(true); passengers.set(rocket,Collections.emptyList());
            for(Method m:controller.getClass().getDeclaredMethods()) {
                if(m.getName().contains("diagnoseBinding")) { m.setAccessible(true); m.invoke(controller,new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("probe",false)); }
            }

            rocket.setLaunchPhase(micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase.EnumLaunchPhase.LAUNCHED);
            rocket.targetVec=sentinel;
            pad.func_73660_a();
            check(rocket.launchPhase==micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase.EnumLaunchPhase.LAUNCHED.ordinal(),"Pad reset launched rocket");
            check(rocket.targetVec==sentinel,"Pad erased launched destination");
            check(pad.getDockedEntity()==null,"Pad retained flying rocket");
        }
        System.out.println("ACTUAL_JAR_BINDING_PASS checks="+checks);
    }
}
