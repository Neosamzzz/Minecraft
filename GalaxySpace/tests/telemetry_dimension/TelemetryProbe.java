package diagnostic;
import java.util.*;
import java.lang.reflect.*;
import sun.misc.Unsafe;
import galaxyspace.core.telemetry.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.math.BlockPos;
import micdoodle8.mods.galacticraft.core.tile.TileEntityTelemetry;
import io.netty.buffer.*;
public class TelemetryProbe {
 static Unsafe u;static int checks;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static <T>T alloc(Class<T> c)throws Exception{return c.cast(u.allocateInstance(c));}
 public static class ProbeWorld extends WorldServer {
  Map<UUID,Entity> entities;MinecraftServer server;
  public ProbeWorld(){super(null,null,null,0,null);}
  @Override public Entity func_175733_a(UUID uuid){return entities.get(uuid);}
  @Override public MinecraftServer func_73046_m(){return server;}
 }
 static ProbeWorld world(int dim,MinecraftServer server)throws Exception{
  ProbeWorld w=alloc(ProbeWorld.class);w.entities=new HashMap<>();w.server=server;
  Field pf=World.class.getDeclaredField("field_73011_w");pf.setAccessible(true);WorldProvider p=new WorldProviderSurface();p.setDimension(dim);pf.set(w,p);return w;
 }
 static Method injected(Object o,String suffix)throws Exception{
  for(Method m:o.getClass().getDeclaredMethods())if(m.getName().endsWith(suffix)){m.setAccessible(true);return m;}
  throw new AssertionError("Missing injection "+suffix);
 }
 public static void main(String[]args)throws Exception{
  Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);u=(Unsafe)f.get(null);
  net.minecraft.init.Bootstrap.func_151354_b();
  Method register=TileEntity.class.getDeclaredMethod("func_190560_a",String.class,Class.class);register.setAccessible(true);register.invoke(null,"gslc_test:telemetry",TileEntityTelemetry.class);
  MinecraftServer server=alloc(DedicatedServer.class);ProbeWorld home=world(0,server),mars=world(-29,server);
  Field ws=MinecraftServer.class.getDeclaredField("field_71305_c");ws.setAccessible(true);ws.set(server,new WorldServer[]{home,mars});
  UUID uuid=UUID.randomUUID();EntityCow cow=alloc(EntityCow.class);cow.field_70170_p=mars;
  Field id=Entity.class.getDeclaredField("field_96093_i");id.setAccessible(true);id.set(cow,uuid);mars.entities.put(uuid,cow);
  check(TelemetryTracking.find(uuid,new WorldServer[]{home,mars})==cow,"Cross-dimension lookup failed");
  TileEntityTelemetry tile=alloc(TileEntityTelemetry.class);tile.func_145834_a(home);tile.func_174878_a(new BlockPos(1,64,2));
  check(tile instanceof TelemetryDimensionState,"Actual telemetry class was not mixed in");
  NBTTagCompound source=new NBTTagCompound();source.func_74778_a("gslcTrackedUUID",uuid.toString());tile.func_145839_a(source);
  injected(tile,"recoverLink").invoke(tile,new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("update",false));
  check(tile.linkedEntity==cow,"Saved target in another dimension not recovered");
  TelemetryNetwork.Update packet=new TelemetryNetwork.Update(0,new BlockPos(1,64,2),true,cow);
  check(packet.targetDimension==-29,"Packet used receiver rather than entity dimension");
  for(Class<? extends Entity> type:new Class[]{EntityCow.class,net.minecraft.entity.player.EntityPlayerMP.class,galaxyspace.core.prefab.entities.EntityTier4Rocket.class}) {
   Entity target=alloc(type);target.field_70170_p=mars;id.set(target,UUID.randomUUID());
   mars.entities.put(target.func_110124_au(),target);
   check(TelemetryTracking.find(target.func_110124_au(),new WorldServer[]{home,mars})==target,"Lookup failed for "+type);
   check(new TelemetryNetwork.Update(0,new BlockPos(1,64,2),true,target).targetDimension==-29,"Dimension failed for "+type);
  }
  packet.key="planet.mars";packet.fallback="火星";
  ByteBuf buffer=Unpooled.buffer();packet.toBytes(buffer);TelemetryNetwork.Update decoded=new TelemetryNetwork.Update();decoded.fromBytes(buffer);buffer.release();
  check(decoded.targetDimension==-29&&decoded.sourceDimension==0&&decoded.known&&decoded.tracked,"Packet scalar round trip failed");
  check(decoded.pos.equals(new BlockPos(1,64,2))&&decoded.fallback.equals("火星")&&decoded.key.equals("planet.mars"),"Packet text/position round trip failed");
  TelemetryDimensionState state=(TelemetryDimensionState)tile;state.galaxyspace$receiveDimension(true,true,-29,"planet.mars","火星");
  check(state.galaxyspace$dimensionKnown()&&state.galaxyspace$targetDimension()==-29,"Client state failed");
  Field received=null;for(Field fld:tile.getClass().getDeclaredFields())if(fld.getName().endsWith("lastReceived"))received=fld;
  check(received!=null,"Stale-state guard missing");received.setAccessible(true);received.setLong(tile,System.nanoTime()-6_000_000_000L);
  check(!state.galaxyspace$dimensionKnown(),"Stale dimension stayed live");
  mars.entities.clear();tile.linkedEntity=null;
  NBTTagCompound saved=new NBTTagCompound();tile.func_189515_b(saved);
  check(saved.func_74779_i("gslcTrackedUUID").equals(uuid.toString()),"Unloaded target UUID lost on save");
  tile.removeTrackedEntity();NBTTagCompound unlinked=new NBTTagCompound();tile.func_189515_b(unlinked);
  check(!unlinked.func_74764_b("gslcTrackedUUID"),"Explicit unlink retained UUID");
  NBTTagCompound malformed=new NBTTagCompound();malformed.func_74778_a("gslcTrackedUUID","bad uuid");malformed.func_74772_a("entityUUIDMost",uuid.getMostSignificantBits());malformed.func_74772_a("entityUUIDLeast",uuid.getLeastSignificantBits());
  tile.func_145839_a(malformed);
  Field pending=TileEntityTelemetry.class.getDeclaredField("toUpdate");pending.setAccessible(true);
  check(pending.get(tile)==null,"GC would reacquire an invalid saved UUID");
  NBTTagCompound legacy=new NBTTagCompound();legacy.func_74772_a("entityUUIDMost",uuid.getMostSignificantBits());legacy.func_74772_a("entityUUIDLeast",uuid.getLeastSignificantBits());tile.func_145839_a(legacy);
  NBTTagCompound migrated=new NBTTagCompound();tile.func_189515_b(migrated);check(migrated.func_74779_i("gslcTrackedUUID").equals(uuid.toString()),"Existing vanilla binding was not migrated");
  tile.func_145839_a(new NBTTagCompound());check(pending.get(tile)==null,"Empty NBT created a zero-UUID target");
  NBTTagCompound emptySaved=new NBTTagCompound();tile.func_189515_b(emptySaved);check(!emptySaved.func_74764_b("gslcTrackedUUID"),"Unbound tile persisted a fake target");
  NBTTagCompound zero=new NBTTagCompound();zero.func_74778_a("gslcTrackedUUID",new UUID(0,0).toString());tile.func_145839_a(zero);
  NBTTagCompound zeroSaved=new NBTTagCompound();tile.func_189515_b(zeroSaved);check(!zeroSaved.func_74764_b("gslcTrackedUUID"),"Persisted zero UUID was not cleaned");
  check(TelemetryTracking.find(uuid,new WorldServer[]{home,mars})==null,"Missing entity lookup invented a target");
  if(Boolean.getBoolean("probe.client")) {
   Class<?> screen=Class.forName("micdoodle8.mods.galacticraft.core.client.screen.GameScreenText");
   boolean applied=false;for(Method m:screen.getDeclaredMethods())if(m.getName().contains("boundedTelemetryText"))applied=true;
   check(applied,"Text mixin not applied");
   boolean centred=false;for(Method m:screen.getDeclaredMethods()) {
    if(m.getName().contains("removeRocketLift"))centred=true;
    check(!m.getName().contains("centreRocket"),"Old renderer redirect remains");
   }
   check(centred,"GC rocket lift cancellation not applied");
   boolean clock=false,inline=false;for(Method m:screen.getDeclaredMethods()) {
    if(m.getName().contains("drawCornerClock"))clock=true;
    if(m.getName().contains("removeInlineClock")) {
     inline=true;m.setAccessible(true);
     check("".equals(m.invoke(alloc(screen),null,123)),"Old inline clock still drawn");
    }
   }
   check(clock&&inline,"Corner clock injection missing");
   for(Method m:screen.getDeclaredMethods())check(!m.getName().contains("reserveTextColumn"),"Global screen scale was still modified");
  }
  System.out.println("TELEMETRY_PASS checks="+checks);
 }
}
