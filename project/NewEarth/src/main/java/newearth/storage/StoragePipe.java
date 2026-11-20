package newearth.storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import micdoodle8.mods.galacticraft.api.block.IPartialSealableBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.logging.log4j.LogManager;

/** The pipe has no inventory. The drawer controller is the only automatic destination. */
public class StoragePipe extends Block implements IPartialSealableBlock {
    public static final String ID = "storage_pipe";
    public static final String SEALED_ID = "sealed_storage_pipe";
    private static final EnumFacing[] SIDES = EnumFacing.values();
    private static final PropertyEnum<Connection> DOWN = PropertyEnum.create("down", Connection.class);
    private static final PropertyEnum<Connection> UP = PropertyEnum.create("up", Connection.class);
    private static final PropertyEnum<Connection> NORTH = PropertyEnum.create("north", Connection.class);
    private static final PropertyEnum<Connection> SOUTH = PropertyEnum.create("south", Connection.class);
    private static final PropertyEnum<Connection> WEST = PropertyEnum.create("west", Connection.class);
    private static final PropertyEnum<Connection> EAST = PropertyEnum.create("east", Connection.class);
    private static final PropertyBool STRAIGHT = PropertyBool.create("straight");
    private static final AxisAlignedBB CENTER = box(5, 5, 5, 11, 11, 11);
    private static final AxisAlignedBB ARM = box(6, 6, 0, 10, 10, 5);
    private static final AxisAlignedBB SPAN = box(5, 5, 0, 11, 11, 16);
    private static final AxisAlignedBB[] DEVICE = {
            box(4, 4, 0, 12, 12, 2), box(5, 5, 2, 11, 11, 3), box(6, 6, 3, 10, 10, 5)
    };
    private static final WeakHashMap<World, Pass> PASSES = new WeakHashMap<>();
    private final boolean sealed;

    public StoragePipe() { this(false); }

    private StoragePipe(boolean sealed) {
        super(Material.IRON);
        this.sealed = sealed;
        setHardness(2F);
        setLightOpacity(sealed ? 255 : 0);
        setCreativeTab(CreativeTabs.MISC);
        if (!sealed) {
            IBlockState idle = blockState.getBaseState().withProperty(STRAIGHT, false);
            for (EnumFacing side : SIDES) idle = idle.withProperty(property(side), Connection.NONE);
            setDefaultState(idle);
        }
    }

    public static final class Sealed extends StoragePipe {
        public Sealed() { super(true); }
        @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this); }
    }

    enum Connection implements IStringSerializable {
        NONE("none"), PIPE("pipe"), DEVICE("device");
        private final String name;
        Connection(String name) { this.name = name; }
        @Override public String getName() { return name; }
    }

    private static PropertyEnum<Connection> property(EnumFacing side) {
        switch (side) {
            case DOWN: return DOWN;
            case UP: return UP;
            case NORTH: return NORTH;
            case SOUTH: return SOUTH;
            case WEST: return WEST;
            case EAST: return EAST;
            default: throw new AssertionError(side);
        }
    }

    @Override protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DOWN, UP, NORTH, SOUTH, WEST, EAST, STRAIGHT);
    }

    // Connections are derived from neighbours, so the original block keeps metadata 0 in old saves.
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState(); }
    @Override public int getMetaFromState(IBlockState state) { return 0; }
    @Override public boolean isOpaqueCube(IBlockState state) { return sealed; }
    @Override public boolean isFullCube(IBlockState state) { return sealed; }
    @Override public boolean isSealed(World world, BlockPos pos, EnumFacing side) { return sealed; }
    @Override public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side) {
        return sealed ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (sealed) return getDefaultState();
        for (EnumFacing side : SIDES) {
            BlockPos neighbor = pos.offset(side);
            Block block = world.getBlockState(neighbor).getBlock();
            Connection connection = block instanceof StoragePipe ? Connection.PIPE
                    : isDevice(block, world.getTileEntity(neighbor)) ? Connection.DEVICE : Connection.NONE;
            state = state.withProperty(property(side), connection);
        }
        return state.withProperty(STRAIGHT, straightSide(state) != null);
    }

    static boolean isDevice(Block block, @Nullable TileEntity tile) {
        if (DrawerNetwork.isEndpoint(tile) || tile != null && TransferProfiles.outputs(tile) != null) return true;
        ResourceLocation name = block.getRegistryName();
        return block == Blocks.CRAFTING_TABLE || name != null && "galacticraftcore".equals(name.getNamespace())
                && ("magnetic_table".equals(name.getPath()) || "rocket_workbench".equals(name.getPath()));
    }

    @Nullable
    private static EnumFacing straightSide(IBlockState state) {
        EnumFacing first = null;
        int count = 0;
        for (EnumFacing side : SIDES) {
            Connection connection = state.getValue(property(side));
            if (connection == Connection.DEVICE) return null;
            if (connection == Connection.PIPE) {
                if (first == null) first = side;
                else if (first.getOpposite() != side) return null;
                count++;
            }
        }
        return count == 2 ? first : null;
    }

    private List<AxisAlignedBB> shape(IBlockState state) {
        List<AxisAlignedBB> boxes = new ArrayList<>();
        if (sealed) {
            boxes.add(FULL_BLOCK_AABB);
            return boxes;
        }
        EnumFacing straight = straightSide(state);
        if (straight != null) {
            boxes.add(rotate(SPAN, straight));
            return boxes;
        }
        boxes.add(CENTER);
        for (EnumFacing side : SIDES) {
            Connection connection = state.getValue(property(side));
            if (connection == Connection.PIPE) boxes.add(rotate(ARM, side));
            if (connection == Connection.DEVICE)
                for (AxisAlignedBB box : DEVICE) boxes.add(rotate(box, side));
        }
        return boxes;
    }

    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        List<AxisAlignedBB> boxes = shape(getActualState(state, world, pos));
        AxisAlignedBB bounds = boxes.get(0);
        for (int i = 1; i < boxes.size(); i++) bounds = bounds.union(boxes.get(i));
        return bounds;
    }

    @Override public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox,
            List<AxisAlignedBB> collisions, @Nullable Entity entity, boolean actual) {
        for (AxisAlignedBB box : shape(actual ? state : getActualState(state, world, pos)))
            addCollisionBoxToList(pos, entityBox, collisions, box);
    }

    @Override public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult nearest = null;
        for (AxisAlignedBB box : shape(getActualState(state, world, pos))) {
            RayTraceResult hit = rayTrace(pos, start, end, box);
            if (hit != null && (nearest == null || hit.hitVec.squareDistanceTo(start) < nearest.hitVec.squareDistanceTo(start)))
                nearest = hit;
        }
        return nearest;
    }

    private static AxisAlignedBB box(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new AxisAlignedBB(x1 / 16D, y1 / 16D, z1 / 16D, x2 / 16D, y2 / 16D, z2 / 16D);
    }

    // Each piece is symmetric across the two axes perpendicular to its connection.
    private static AxisAlignedBB rotate(AxisAlignedBB box, EnumFacing side) {
        double min = side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 1 - box.maxZ : box.minZ;
        double max = side.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? 1 - box.minZ : box.maxZ;
        switch (side.getAxis()) {
            case X: return new AxisAlignedBB(min, box.minY, box.minX, max, box.maxY, box.maxX);
            case Y: return new AxisAlignedBB(box.minX, min, box.minY, box.maxX, max, box.maxY);
            default: return new AxisAlignedBB(box.minX, box.minY, min, box.maxX, box.maxY, max);
        }
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
