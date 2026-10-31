import galaxyspace.core.util.LaunchControllerCompatibility;
import galaxyspace.systems.SolarSystem.planets.overworld.tile.TileEntityAdvLandingPad;
import micdoodle8.mods.galacticraft.api.prefab.entity.EntityAutoRocket;
import micdoodle8.mods.galacticraft.api.tile.IFuelDock;
import micdoodle8.mods.galacticraft.api.entity.IDockable;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import micdoodle8.mods.galacticraft.planets.mars.tile.TileEntityLaunchController;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

public class CompatibilityTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static <T extends TileEntity> T place(WorldServer world, T tile, int x, int z) {
        tile.world = world; tile.pos = new BlockPos(x, 64, z);
        world.tiles.put(tile.pos, tile); world.loadedTileEntityList.add(tile);
        return tile;
    }
    public static void main(String[] args) {
        WorldServer origin = new WorldServer(), destination = new WorldServer();
        destination.provider.dimension = 42;
        GCCoreUtil.worlds = new WorldServer[]{origin, destination};
        EntityAutoRocket rocket = new EntityAutoRocket(); rocket.world = origin;
        TileEntityLaunchController controller = place(destination, new TileEntityLaunchController(), 0, 0);
        controller.frequency = 123;
        TileEntityAdvLandingPad pad = place(destination, new TileEntityAdvLandingPad(), 3, 0);
        pad.connected.add(controller);
        check(LaunchControllerCompatibility.canAttachToPad(destination, pad.pos), "GS pad must accept controller attachment");
        check(!LaunchControllerCompatibility.canAttachToPad(destination, controller.pos), "ordinary tiles must not become pads");
        check(!LaunchControllerCompatibility.canAttachToPad(destination, new BlockPos(9,64,9)), "empty positions must not attach");

        pad.rocket = rocket;
        check(LaunchControllerCompatibility.updateRocketOnDockSettings(pad), "GS settings update must be handled");
        check(rocket.updates == 1 && rocket.updatedPad == pad, "settings must reach the docked rocket through the pad");
        check(!LaunchControllerCompatibility.updateRocketOnDockSettings(null), "null dock must preserve GC behavior");
        IFuelDock other = new IFuelDock() { public IDockable getDockedEntity() { return rocket; } };
        check(!LaunchControllerCompatibility.updateRocketOnDockSettings(other), "non-GS docks must preserve GC behavior");
        pad.rocket = null;
        check(LaunchControllerCompatibility.updateRocketOnDockSettings(pad), "empty GS pad must be safe");

        BlockPos sentinel = new BlockPos(99,99,99);
        rocket.targetVec = sentinel; rocket.targetDimension = -99;
        check(LaunchControllerCompatibility.setTarget(rocket, false, 123), "dry run must find GS destination");
        check(rocket.targetVec == sentinel && rocket.targetDimension == -99, "dry run must not mutate destination");
        check(LaunchControllerCompatibility.setTarget(rocket, true, 123), "actual lookup must find destination");
        check(rocket.targetVec.equals(pad.pos) && rocket.targetDimension == 42, "lookup must select pad center and dimension");
        check(!LaunchControllerCompatibility.setTarget(rocket, false, 456), "unmatched frequency must fail");
        pad.connected.clear();
        check(!LaunchControllerCompatibility.setTarget(rocket, true, 123), "nearby but unconnected pad must be rejected");
        check(rocket.targetVec == null, "failed destination lookup must clear stale target");
        pad.connected.add(controller);
        destination.unloaded.add(pad.pos);
        check(!LaunchControllerCompatibility.setTarget(rocket, false, 123), "lookup must not load chunks to find pads");
        destination.unloaded.clear();
        destination.tiles.remove(pad.pos);
        for (int[] offset : new int[][]{{3,0},{-3,0},{0,3},{0,-3},{3,2},{-2,3}}) {
            place(destination, pad, offset[0], offset[1]);
            check(LaunchControllerCompatibility.setTarget(rocket, true, 123), "all GS platform sides must be reachable");
            check(rocket.targetVec.equals(pad.pos), "target must be the actual center");
            destination.tiles.remove(pad.pos);
        }
        place(destination, pad, 4, 0);
        check(!LaunchControllerCompatibility.setTarget(rocket, false, 123), "unrelated distant pad must not be selected");
        System.out.println("Compatibility behavior: " + checks + " checks PASS");
    }
}
