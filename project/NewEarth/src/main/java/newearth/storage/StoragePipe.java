package newearth.storage;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.logging.log4j.LogManager;

/** The pipe has no inventory. The drawer controller is the only automatic destination. */
public final class StoragePipe extends Block {
    public static final String ID = "storage_pipe";
    private static final EnumFacing[] SIDES = EnumFacing.values();
    private static final WeakHashMap<World, Pass> PASSES = new WeakHashMap<>();

    public StoragePipe() {
        super(Material.IRON);
        setHardness(2F);
        setCreativeTab(CreativeTabs.MISC);
    }

    @Override public boolean hasTileEntity(IBlockState state) { return true; }

    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new PipeTile(); }

    public static final class PipeTile extends TileEntity implements ITickable {
        @Override public void update() {
            if (world.isRemote || world.getTotalWorldTime() % 8 != 0) return;
            Pass pass = PASSES.computeIfAbsent(world, unused -> new Pass());
            if (pass.time != world.getTotalWorldTime()) {
                pass.time = world.getTotalWorldTime();
                pass.visited.clear();
                pass.moved.clear();
            }
            if (pass.visited.contains(pos)) return;
            try {
                Route route = scan(world, pos, true);
                pass.visited.addAll(route.pipes);
                if (route.controller == null || route.multiple || route.incomplete) return;
                IItemHandler target = route.endpoint.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, route.inputFace);
                if (target == null || target.getSlots() == 0) return;
                for (BlockPos neighbor : route.machines) {
                    TileEntity tile = world.getTileEntity(neighbor);
                    if (tile instanceof ISidedInventory && !pass.moved.contains(neighbor)
                            && TransferProfiles.outputs(tile) != null && singleDestination(world, neighbor, route)
                            && moveOne((ISidedInventory) tile, target)) pass.moved.add(neighbor);
                }
            } catch (RuntimeException error) {
                pass.visited.add(pos);
                if (!pass.loggedFailure) {
                    pass.loggedFailure = true;
                    LogManager.getLogger("NewEarth").warn("Storage pipe transfer stopped at {}", pos, error);
                }
            }
        }
    }

    private static final class Pass {
        long time = Long.MIN_VALUE;
        boolean loggedFailure;
        final Set<BlockPos> visited = new HashSet<>();
        final Set<BlockPos> moved = new HashSet<>();
    }

    static final class Route {
        final Set<BlockPos> pipes = new HashSet<>();
        final Set<BlockPos> machines = new LinkedHashSet<>();
        TileEntity controller;
        TileEntity endpoint;
        EnumFacing inputFace;
        boolean multiple;
        boolean incomplete;
    }

    static Route scan(World world, BlockPos origin, boolean fromPipe) {
        Route route = new Route();
        Queue<BlockPos> pending = new ArrayDeque<>();
        pending.add(origin);
        if (fromPipe) route.pipes.add(origin);
        while (!pending.isEmpty()) {
            BlockPos current = pending.remove();
            boolean pipe = fromPipe || !current.equals(origin);
            for (EnumFacing side : SIDES) {
                BlockPos next = current.offset(side);
                if (next.getY() < 0 || next.getY() >= world.getHeight()) continue;
                if (!world.isBlockLoaded(next)) {
                    route.incomplete |= pipe;
                    continue;
                }
                TileEntity tile = world.getTileEntity(next);
                if (tile instanceof PipeTile && route.pipes.add(next)) pending.add(next);
                else if (DrawerNetwork.isEndpoint(tile)) {
                    TileEntity controller = DrawerNetwork.resolveEndpoint(world, tile);
                    if (controller == null) route.incomplete |= pipe;
                    else if (route.controller == null) {
                        route.controller = controller;
                        route.endpoint = tile;
                        route.inputFace = side.getOpposite();
                    } else if (route.controller != controller) route.multiple = true;
                } else if (fromPipe && tile instanceof ISidedInventory) route.machines.add(next);
            }
        }
        return route;
    }

    private static boolean singleDestination(World world, BlockPos machine, Route current) {
        for (EnumFacing side : SIDES) {
            BlockPos next = machine.offset(side);
            if (next.getY() < 0 || next.getY() >= world.getHeight()) continue;
            if (!world.isBlockLoaded(next)) return false;
            TileEntity neighbor = world.getTileEntity(next);
            if ((neighbor instanceof PipeTile && !current.pipes.contains(next)) || DrawerNetwork.isEndpoint(neighbor)) {
                Route all = scan(world, machine, false);
                return !all.multiple && !all.incomplete && all.controller == current.controller;
            }
        }
        return true;
    }

    static boolean moveOne(ISidedInventory source, IItemHandler target) {
        int[] outputs = TransferProfiles.outputs(source);
        if (outputs == null) return false;
        for (int slot : outputs) {
            if (slot >= source.getSizeInventory()) continue;
            ItemStack before = source.getStackInSlot(slot).copy();
            if (before.isEmpty() || !exposesOutput(source, slot, before)) continue;
            ItemStack offered = before.copy();
            offered.setCount(1);
            if (!ItemHandlerHelper.insertItem(target, offered, true).isEmpty()) continue;
            ItemStack removed = source.decrStackSize(slot, 1);
            if (removed.isEmpty()) continue;
            if (ItemHandlerHelper.insertItem(target, removed, false).isEmpty()) {
                source.markDirty();
                return true;
            }
            source.setInventorySlotContents(slot, before);
            source.markDirty();
        }
        return false;
    }

    private static boolean exposesOutput(ISidedInventory source, int slot, ItemStack stack) {
        for (EnumFacing side : SIDES)
            for (int exposed : source.getSlotsForFace(side))
                if (exposed == slot && source.canExtractItem(slot, stack, side)) return true;
        return false;
    }
}
