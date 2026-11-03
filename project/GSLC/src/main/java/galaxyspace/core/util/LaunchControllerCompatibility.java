package galaxyspace.core.util;

import java.util.ArrayList;
import galaxyspace.systems.SolarSystem.planets.overworld.tile.TileEntityAdvLandingPad;
import micdoodle8.mods.galacticraft.api.entity.IDockable;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldServer;

/** GS additions to GC launch control; original GC behavior remains in GC. */
public final class LaunchControllerCompatibility {
    private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger("GSLaunchControl");
    private static final java.util.Map<Object, String> STATES = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<Object, String>());
    static { LOG.info("[GSLC] compatibility helper loaded"); }
    private LaunchControllerCompatibility() { }

    public static void logState(Object key, String state) {
        String previous = STATES.put(key, state);
        if (!state.equals(previous)) LOG.info("[GSLC] {}", state);
    }

    public static void logController(TileEntityLaunchController controller) {
        Object dock = controller.attachedDock;
        String rocketState = "none";
        if (dock instanceof TileEntityAdvLandingPad) {
            IDockable entity = ((TileEntityAdvLandingPad) dock).getDockedEntity();
            if (entity instanceof EntityAutoRocket) {
                EntityAutoRocket rocket = (EntityAutoRocket) entity;
                rocketState = entity.getClass().getSimpleName() + ",mode=" + rocket.autoLaunchSetting
                    + ",phase=" + rocket.launchPhase + ",target=" + rocket.targetVec + ",targetDim=" + rocket.targetDimension
                    + ",passengers=" + rocket.getPassengers().size() + ",fuel=" + rocket.getScaledFuelLevel(100);
            }
        }
        logState(controller, "controller=" + controller.getPos() + ",dim=" + controller.getWorld().provider.getDimension()
            + ",dock=" + (dock instanceof net.minecraft.tileentity.TileEntity ? ((net.minecraft.tileentity.TileEntity)dock).getPos() : dock)
            + ",enabled=" + controller.controlEnabled + ",scheduled=" + controller.launchSchedulingEnabled
            + ",energy=" + controller.hasEnoughEnergyToRun + ",frequency=" + controller.frequency
            + ",destination=" + controller.destFrequency + ",rocket=" + rocketState);
    }

    public static boolean canAttachToPad(IBlockAccess world, BlockPos pos) {
        boolean accepted = world.getTileEntity(pos) instanceof TileEntityAdvLandingPad;
        return accepted;
    }

    public static boolean updateRocketOnDockSettings(Object dock) {
        if (!(dock instanceof TileEntityAdvLandingPad)) {
            return false;
        }
        TileEntityAdvLandingPad pad = (TileEntityAdvLandingPad) dock;
        IDockable rocket = pad.getDockedEntity();
        if (rocket instanceof EntityAutoRocket) {
            ((EntityAutoRocket) rocket).updateControllerSettings(pad);
        }
        return true;
    }

    public static boolean setTarget(EntityAutoRocket rocket, boolean doSet, int destFrequency) {
        for (WorldServer world : GCCoreUtil.getWorldServerList(rocket.world)) {
            for (TileEntity tile : new ArrayList<>(world.loadedTileEntityList)) {
                if (!(tile instanceof TileEntityLaunchController)) {
                    continue;
                }
                TileEntity current = world.getTileEntity(tile.getPos());
                if (!(current instanceof TileEntityLaunchController)) {
                    continue;
                }
                TileEntityLaunchController controller = (TileEntityLaunchController) current;
                if (controller.frequency != destFrequency) {
                    continue;
                }
                // The center of a 5x5 pad is three blocks from an adjacent controller.
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        BlockPos pos = controller.getPos().add(x, 0, z);
                        if (!world.isBlockLoaded(pos)) {
                            continue;
                        }
                        TileEntity pad = world.getTileEntity(pos);
                        if (pad instanceof TileEntityAdvLandingPad
                                && ((TileEntityAdvLandingPad) pad).getConnectedTiles().contains(controller)) {
                            if (doSet) {
                                rocket.targetVec = pos;
                                rocket.targetDimension = world.provider.getDimension();
                            }
                            return true;
                        }
                    }
                }
                if (doSet) {
                    rocket.targetVec = null;
                }
                return false;
            }
        }
        return false;
    }
}
