package local.telemetrytest;
import java.util.UUID;
import galaxyspace.core.telemetry.TelemetryNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraft.world.WorldServer;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import micdoodle8.mods.galacticraft.core.tile.TileEntityTelemetry;
@Mod(modid="gslctelemetrytest",name="Temporary telemetry integration probe",version="1",dependencies="required-after:gslaunchtelemetry",acceptableRemoteVersions="*")
public class ForgeIntegrationProbe {
 @Mod.EventHandler public void started(FMLServerStartedEvent event) {
  WorldServer home=DimensionManager.getWorld(0);
  home.func_72964_e(0,0);
  EntityCow cow=new EntityCow(home);cow.func_70107_b(8,70,8);
  if(!home.func_72838_d(cow))throw new AssertionError("Cow not loaded");
  UUID uuid=cow.func_110124_au();
  TileEntityTelemetry tile=new TileEntityTelemetry();tile.func_145834_a(home);tile.func_174878_a(new BlockPos(8,70,9));tile.addTrackedEntity(cow);
  tile.func_73660_a();tile.func_73660_a();
  home.func_72973_f(cow);cow.func_70106_y();
  for(int i=0;i<21;i++)tile.func_73660_a();
  if(tile.linkedEntity!=null)throw new AssertionError("Lost target remained live");
  NBTTagCompound nbt=new NBTTagCompound();tile.func_189515_b(nbt);
  if(!uuid.toString().equals(nbt.func_74779_i("gslcTrackedUUID")))throw new AssertionError("Lost target binding not saved");
  DimensionManager.initDimension(-1);WorldServer other=DimensionManager.getWorld(-1);other.func_72964_e(0,0);
  EntityCow moved=new EntityCow(other);moved.func_184221_a(uuid);moved.func_70107_b(8,70,8);
  if(!other.func_72838_d(moved))throw new AssertionError("Moved target not loaded");
  for(int i=0;i<21;i++)tile.func_73660_a();
  if(tile.linkedEntity!=moved)throw new AssertionError("Cross-world target not recovered");
  TelemetryNetwork.Update packet=new TelemetryNetwork.Update(0,tile.func_174877_v(),true,tile.linkedEntity);
  if(!packet.known||packet.targetDimension!=-1)throw new AssertionError("Wrong cross-world packet");
  TileEntityTelemetry restored=new TileEntityTelemetry();restored.func_145839_a(nbt);restored.func_145834_a(home);restored.func_73660_a();
  if(restored.linkedEntity!=moved)throw new AssertionError("Saved cross-world binding not recovered");
  int mars=micdoodle8.mods.galacticraft.planets.mars.ConfigManagerMars.dimensionIDMars;
  DimensionManager.initDimension(mars);WorldServer marsWorld=DimensionManager.getWorld(mars);
  EntityCow marsCow=new EntityCow(marsWorld);TelemetryNetwork.Update marsPacket=new TelemetryNetwork.Update(0,tile.func_174877_v(),true,marsCow);
  if(!"planet.mars".equals(marsPacket.key))throw new AssertionError("Localized celestial key unavailable: "+marsPacket.key);
  System.out.println("FORGE_TELEMETRY_INTEGRATION_PASS live packet send, loss, cross-world replacement, save/reload");
 }
}
