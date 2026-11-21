package newearth.compat.ae2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

/** Resolves alternate ports on the same neighboring machine, using its existing sided handlers. */
public final class Ae2BusPorts {
    private Ae2BusPorts() { }

    private static EnumFacing[] faces(TileEntity target, EnumFacing preferred) {
        if (target.getClass().getName().startsWith("appeng.")) return new EnumFacing[]{preferred};
        EnumFacing[] ordered = EnumFacing.values();
        int index = preferred.ordinal();
        System.arraycopy(ordered, 0, ordered, 1, index);
        ordered[0] = preferred;
        return ordered;
    }

    public static Object itemAdaptor(TileEntity tile, EnumFacing preferred, boolean extracting) {
        if (tile == null) return null;
        EnumFacing[] sides = faces(tile, preferred);
        try {
            if (tile instanceof ISidedInventory) {
                SidedItems items = new SidedItems(tile, (ISidedInventory) tile, sides, extracting);
                if (items.hasPorts()) return ItemAdaptors.WRAPPER.newInstance(items);
            }
            for (EnumFacing face : sides) {
                Object adaptor = ItemAdaptors.NATIVE.invoke(null, tile, face);
                if (adaptor != null) return adaptor;
            }
            return null;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot resolve AE2 bus inventory", error);
        }
    }

    private static final class ItemAdaptors {
        static final Method NATIVE;
        static final Constructor<?> WRAPPER;
        static {
            try {
                NATIVE = Class.forName("appeng.util.InventoryAdaptor").getMethod("getAdaptor", TileEntity.class, EnumFacing.class);
                WRAPPER = Class.forName("appeng.util.inv.AdaptorItemHandler").getConstructor(IItemHandler.class);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unsupported AE2 item adaptor", error);
            }
        }
    }

    static final class SidedItems implements IItemHandler {
        private final ISidedInventory inventory;
        private final boolean extracting;
        private final EnumFacing[] sides;
        private final IItemHandler[] handlers;
        private final boolean anyPort;

        SidedItems(TileEntity tile, ISidedInventory inventory, EnumFacing[] sides, boolean extracting) {
            this.inventory = inventory;
            this.extracting = extracting;
            this.sides = sides;
            this.handlers = new IItemHandler[sides.length];
            boolean found = false;
            for (int i = 0; i < sides.length; i++) {
                IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, sides[i]);
                if (handler instanceof SidedInvWrapper) {
                    handlers[i] = handler;
                    found = true;
                }
            }
            this.anyPort = found;
        }

        boolean hasPorts() { return anyPort; }

        // Each physical slot appears once, including slots exposed on several faces.
        @Override public int getSlots() { return inventory.getSizeInventory(); }

        private int portSlot(int port, int slot) {
            if (handlers[port] == null) return -1;
            int[] exposed = inventory.getSlotsForFace(sides[port]);
            if (exposed == null) return -1;
            for (int i = 0; i < exposed.length; i++) if (exposed[i] == slot) return i;
            return -1;
        }

        @Override public ItemStack getStackInSlot(int slot) {
            for (int port = 0; port < sides.length; port++) {
                int index = portSlot(port, slot);
                if (index < 0) continue;
                ItemStack stack = handlers[port].getStackInSlot(index);
                if (!stack.isEmpty() && (!extracting || !handlers[port].extractItem(index, 1, true).isEmpty()))
                    return stack;
            }
            return ItemStack.EMPTY;
        }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            for (int port = 0; port < sides.length; port++) {
                int index = portSlot(port, slot);
                if (index < 0) continue;
                ItemStack remaining = handlers[port].insertItem(index, stack, simulate);
                if (remaining.getCount() < stack.getCount()) return remaining;
            }
            return stack;
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            for (int port = 0; port < sides.length; port++) {
                int index = portSlot(port, slot);
                if (index < 0) continue;
                ItemStack extracted = handlers[port].extractItem(index, amount, simulate);
                if (!extracted.isEmpty()) return extracted;
            }
            return ItemStack.EMPTY;
        }

        @Override public int getSlotLimit(int slot) {
            for (int port = 0; port < sides.length; port++) {
                int index = portSlot(port, slot);
                if (index >= 0) return handlers[port].getSlotLimit(index);
            }
            return 0;
        }
    }

    public static boolean hasFluidPort(TileEntity tile, Capability<?> capability, EnumFacing preferred) {
        for (EnumFacing face : faces(tile, preferred)) if (tile.hasCapability(capability, face)) return true;
        return false;
    }

    @Nullable
    public static IFluidHandler fluidPort(TileEntity tile, Capability<?> capability, EnumFacing preferred,
                                           @Nullable IFluidHandler filter) {
        List<IFluidHandler> ports = new ArrayList<>();
        for (EnumFacing face : faces(tile, preferred)) {
            if (!tile.hasCapability(capability, face)) continue;
            Object handler = tile.getCapability(capability, face);
            if (handler instanceof IFluidHandler && !ports.contains(handler)) ports.add((IFluidHandler) handler);
        }
        return ports.isEmpty() ? null : new FluidPorts(ports, filter);
    }

    static final class FluidPorts implements IFluidHandler {
        private final List<IFluidHandler> ports;
        private final List<FluidStack> filter = new ArrayList<>();
        private IFluidHandler sampledPort;
        private FluidStack sampledFluid;

        FluidPorts(List<IFluidHandler> ports, @Nullable IFluidHandler settings) {
            this.ports = ports;
            if (settings != null) for (IFluidTankProperties property : settings.getTankProperties()) {
                FluidStack fluid = property.getContents();
                if (fluid != null) filter.add(fluid.copy());
            }
        }

        @Override public IFluidTankProperties[] getTankProperties() {
            return ports.stream().flatMap(port -> Arrays.stream(port.getTankProperties())).toArray(IFluidTankProperties[]::new);
        }

        @Override public int fill(FluidStack resource, boolean execute) {
            for (IFluidHandler port : ports) {
                int amount = port.fill(resource, execute);
                if (amount > 0) return amount;
            }
            return 0;
        }

        @Nullable @Override public FluidStack drain(FluidStack resource, boolean execute) {
            if (resource == null || resource.amount <= 0) return null;
            if (sampledPort != null && sampledFluid.isFluidEqual(resource))
                return sampledPort.drain(resource, execute);
            for (IFluidHandler port : ports) {
                FluidStack fluid = port.drain(resource, execute);
                if (fluid != null && fluid.amount > 0) return fluid;
            }
            return null;
        }

        @Nullable @Override public FluidStack drain(int maxAmount, boolean execute) {
            sampledPort = null;
            sampledFluid = null;
            if (maxAmount <= 0) return null;
            for (IFluidHandler port : ports) {
                FluidStack sample = null;
                if (filter.isEmpty()) sample = port.drain(maxAmount, execute);
                else for (FluidStack configured : filter) {
                    FluidStack request = configured.copy();
                    request.amount = maxAmount;
                    sample = port.drain(request, execute);
                    if (sample != null && sample.amount > 0) break;
                }
                if (sample != null && sample.amount > 0) {
                    if (!execute) {
                        sampledPort = port;
                        sampledFluid = sample.copy();
                    }
                    return sample;
                }
            }
            return null;
        }
    }
}
