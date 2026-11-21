package newearth.storage;

import java.util.*;
import java.lang.reflect.Method;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/** Optional Storage Drawers bridge. Traversal follows its six-neighbour network and controller bounds. */
public final class DrawerNetwork {
    private static final String PREFIX = "com.jaquadro.minecraft.storagedrawers.";
    /** Client preview cannot rely on the controller's server-only network cache or remote chunks. */
    public static boolean hasEndpoint(World world,BlockPos anchor) {
        if(anchor==null)return false;
        for(EnumFacing side:EnumFacing.values()) {
            BlockPos pos=anchor.offset(side);if(!world.isBlockLoaded(pos))continue;
            TileEntity tile=world.getTileEntity(pos);
            if(tile!=null&&(is(tile,"block.tile.TileEntityController")||is(tile,"block.tile.TileEntitySlave")))return true;
        }
        return false;
    }
    public static TileEntity controller(World world, BlockPos anchor) {
        if (anchor == null) return null;
        TileEntity found = null;
        for (EnumFacing side : EnumFacing.values()) {
            BlockPos pos = anchor.offset(side);
            if (!world.isBlockLoaded(pos)) continue;
            TileEntity tile = world.getTileEntity(pos);
            if (tile == null) continue;
            try {
                if (is(tile, "block.tile.TileEntitySlave")) {
                    Object linkedPos=tile.getClass().getMethod("getControllerPos").invoke(tile);
                    if (!(linkedPos instanceof BlockPos) || !world.isBlockLoaded((BlockPos)linkedPos)) continue;
                    Object linked = tile.getClass().getMethod("getController").invoke(tile);
                    if (!(linked instanceof TileEntity) || !(Boolean) linked.getClass()
                            .getMethod("isValidSlave", BlockPos.class).invoke(linked, pos)) continue;
                    tile = (TileEntity) linked;
                }
                if (!is(tile, "block.tile.TileEntityController")) continue;
                if (found != null && found != tile) throw new IllegalArgumentException("工作台旁连接了多个抽屉网络，请只保留一个入口");
                found = tile;
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("无法读取抽屉控制器", error);
            }
        }
        return found;
    }
    public static List<DrawerTransfer.Source> sources(EntityPlayer player, TileEntity controller) {
        List<DrawerTransfer.Source> result = new ArrayList<>();
        if (controller == null) return result;
        try {
            Class<?> storage = Class.forName(PREFIX + "StorageDrawers");
            Object config = storage.getField("config").get(null);
            if (!(Boolean) config.getClass().getMethod("allowControllerIO").invoke(config))
                throw new IllegalArgumentException("抽屉控制器的物品输入输出已关闭");
            int range = ((Number) config.getClass().getMethod("getControllerRange").invoke(config)).intValue();
            Class<?> networked = Class.forName(PREFIX + "api.storage.INetworked");
            Class<?> protectable = Class.forName(PREFIX + "api.storage.attribute.IProtectable");
            Method access = Class.forName(PREFIX + "security.SecurityManager").getMethod("hasAccess",
                    com.mojang.authlib.GameProfile.class, protectable);
            World world = controller.getWorld(); BlockPos root = controller.getPos();
            Set<BlockPos> seen = new HashSet<>(); Queue<BlockPos> queue = new ArrayDeque<>();
            queue.add(root); seen.add(root);
            while (!queue.isEmpty()) {
                if (seen.size() > 65536) throw new IllegalArgumentException("抽屉网络过大");
                BlockPos pos = queue.remove();
                if (Math.abs(pos.getX()-root.getX()) > range || Math.abs(pos.getY()-root.getY()) > range
                        || Math.abs(pos.getZ()-root.getZ()) > range || !world.isBlockLoaded(pos)) continue;
                if (!networked.isInstance(world.getBlockState(pos).getBlock())) continue;
                TileEntity tile = world.getTileEntity(pos);
                if (tile != null && is(tile, "block.tile.TileEntityDrawers")
                        && (Boolean) access.invoke(null, player.getGameProfile(), tile)
                        && !(Boolean) tile.getClass().getMethod("isSealed").invoke(tile)) {
                    IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
                    if (handler != null) {
                        // Slot zero is virtual insertion; fractional cells share one underlying stock.
                        Class<?> groupType=Class.forName(PREFIX+"api.storage.IDrawerGroup");
                        Class<?> fractional=Class.forName(PREFIX+"api.storage.IFractionalDrawer");
                        Object group=tile.getClass().getMethod("getGroup").invoke(tile);
                        int[] order=(int[])groupType.getMethod("getAccessibleDrawerSlots").invoke(group);
                        int[] rates=new int[handler.getSlots()-1];Arrays.fill(rates,1);
                        for(int i=0;i<rates.length&&i<order.length;i++) {
                            Object drawer=groupType.getMethod("getDrawer",int.class).invoke(group,order[i]);
                            if(fractional.isInstance(drawer))rates[i]=((Number)fractional.getMethod("getConversionRate").invoke(drawer)).intValue();
                        }
                        List<DrawerTransfer.Source> cells=DrawerTransfer.automationSources(handler,1,rates);
                        for(int i=0;i<cells.size()&&i<order.length;i++){cells.get(i).origin=tile;cells.get(i).drawerIndex=order[i];}
                        result.addAll(cells);
                    }
                }
                for (EnumFacing side : EnumFacing.values()) {
                    BlockPos next = pos.offset(side); if (seen.add(next)) queue.add(next);
                }
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("抽屉接口不兼容", error);
        }
        return result;
    }
    /** Re-resolve the original physical drawer; a stale capability must never write into a removed tile. */
    static ItemStack returnItems(DrawerTransfer.Source source,net.minecraft.entity.player.EntityPlayer player,ItemStack stack) {
        if(source.origin==null)return source.handler.insertItem(source.slot,stack,false);
        TileEntity tile=source.origin;World world=tile.getWorld();
        if(world==null || world!=player.world || tile.isInvalid() || !world.isBlockLoaded(tile.getPos())
                || world.getTileEntity(tile.getPos())!=tile)return stack;
        try {
            Class<?> protectable=Class.forName(PREFIX+"api.storage.attribute.IProtectable");
            if(!(Boolean)Class.forName(PREFIX+"security.SecurityManager").getMethod("hasAccess",com.mojang.authlib.GameProfile.class,protectable)
                    .invoke(null,player.getGameProfile(),tile) || (Boolean)tile.getClass().getMethod("isSealed").invoke(tile))return stack;
            Class<?> groupType=Class.forName(PREFIX+"api.storage.IDrawerGroup"),drawerType=Class.forName(PREFIX+"api.storage.IDrawer");
            Object group=tile.getClass().getMethod("getGroup").invoke(tile);
            Object drawer=groupType.getMethod("getDrawer",int.class).invoke(group,source.drawerIndex);
            // getRemainingCapacity excludes void-upgrade overflow; refunds must not be discarded by a void drawer.
            int capacity=((Number)drawerType.getMethod("getRemainingCapacity").invoke(drawer)).intValue();
            int[] order=(int[])groupType.getMethod("getAccessibleDrawerSlots").invoke(group);
            IItemHandler handler=tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,null);
            if(handler==null || capacity<=0)return stack;
            for(int i=0;i<order.length;i++)if(order[i]==source.drawerIndex) {
                ItemStack offered=stack.copy();offered.setCount(Math.min(stack.getCount(),capacity));
                int offeredCount=offered.getCount();
                ItemStack rejected=handler.insertItem(i+1,offered,false);
                ItemStack remaining=stack.copy();remaining.shrink(offeredCount-rejected.getCount());return remaining;
            }
        } catch(ReflectiveOperationException error) {
            org.apache.logging.log4j.LogManager.getLogger("NewEarth").warn("Cannot resolve original drawer for leftover return",error);
        }
        return stack;
    }
    private static boolean is(Object object, String suffix) {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass())
            if (type.getName().equals(PREFIX + suffix)) return true;
        return false;
    }
}
