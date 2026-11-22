package newearth.compat.ae2;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final EnumFacing[][] ALL_PORTS = new EnumFacing[EnumFacing.VALUES.length][];
    private static final EnumFacing[][] NATIVE_PORT = new EnumFacing[EnumFacing.VALUES.length][];
    private static final int[] NO_SLOTS = new int[0];

    static {
        for (EnumFacing preferred : EnumFacing.VALUES) {
            int index = preferred.ordinal();
            EnumFacing[] ordered = EnumFacing.values();
            System.arraycopy(ordered, 0, ordered, 1, index);
            ordered[0] = preferred;
            ALL_PORTS[index] = ordered;
            NATIVE_PORT[index] = new EnumFacing[]{preferred};
        }
    }

    private Ae2BusPorts() { }

    private static EnumFacing[] faces(TileEntity target, EnumFacing preferred) {
        return (target.getClass().getName().startsWith("appeng.") ? NATIVE_PORT : ALL_PORTS)[preferred.ordinal()];
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
        // This adaptor is scoped to one bus operation. Clear the snapshot after a committed transfer.
        private final int[][] exposedSlots;
        private final boolean anyPort;

        SidedItems(TileEntity tile, ISidedInventory inventory, EnumFacing[] sides, boolean extracting) {
            this.inventory = inventory;
            this.extracting = extracting;
            this.sides = sides;
            this.handlers = new IItemHandler[sides.length];
            this.exposedSlots = new int[sides.length][];
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
            int[] exposed = exposedSlots[port];
            if (exposed == null) {
                exposed = inventory.getSlotsForFace(sides[port]);
                exposedSlots[port] = exposed = exposed == null ? NO_SLOTS : exposed;
            }
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
                if (remaining.getCount() < stack.getCount()) {
                    if (!simulate) Arrays.fill(exposedSlots, null);
                    return remaining;
                }
            }
            return stack;
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            for (int port = 0; port < sides.length; port++) {
                int index = portSlot(port, slot);
                if (index < 0) continue;
                ItemStack extracted = handlers[port].extractItem(index, amount, simulate);
                if (!extracted.isEmpty()) {
                    if (!simulate) Arrays.fill(exposedSlots, null);
                    return extracted;
                }
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

    @Nullable
    public static IFluidHandler fluidPort(TileEntity tile, Capability<?> capability, EnumFacing preferred,
                                           @Nullable IFluidHandler filter) {
        EnumFacing[] sides = faces(tile, preferred);
        for (int i = 0; i < sides.length; i++) {
            IFluidHandler handler = fluidHandler(tile, capability, sides[i]);
            if (handler == null) continue;
            if (i + 1 == sides.length && filter == null) return handler;
            return new FluidPorts(tile, capability, sides, i + 1, handler, filter);
        }
        return null;
    }

    @Nullable
    private static IFluidHandler fluidHandler(TileEntity tile, Capability<?> capability, EnumFacing side) {
        if (!tile.hasCapability(capability, side)) return null;
        Object handler = tile.getCapability(capability, side);
        return handler instanceof IFluidHandler ? (IFluidHandler) handler : null;
    }

    /** Port discovery is scoped to a single bus call and stops when a transfer succeeds. */
    private static final class FluidPorts implements IFluidHandler {
        private final TileEntity tile;
        private final Capability<?> capability;
        private final EnumFacing[] sides;
        private final IFluidHandler first;
        private int nextSide;
        private List<IFluidHandler> alternatives;
        private final List<FluidStack> filter;
        private IFluidHandler sampledPort;
        private FluidStack sampledFluid;

        FluidPorts(TileEntity tile, Capability<?> capability, EnumFacing[] sides, int nextSide,
                   IFluidHandler first, @Nullable IFluidHandler settings) {
            this.tile = tile;
            this.capability = capability;
            this.sides = sides;
            this.nextSide = nextSide;
            this.first = first;
            List<FluidStack> configured = null;
            if (settings != null) for (IFluidTankProperties property : settings.getTankProperties()) {
                FluidStack fluid = property.getContents();
                if (fluid != null) {
                    if (configured == null) configured = new ArrayList<>();
                    configured.add(fluid.copy());
                }
            }
            this.filter = configured == null ? Collections.emptyList() : configured;
        }

        @Nullable
        private IFluidHandler port(int index) {
            if (index == 0) return first;
            int alternative = index - 1;
            if (alternatives != null && alternative < alternatives.size()) return alternatives.get(alternative);
            while (nextSide < sides.length) {
                IFluidHandler handler = fluidHandler(tile, capability, sides[nextSide++]);
                if (handler == null || handler.equals(first)) continue;
                if (alternatives == null) alternatives = new ArrayList<>();
                if (!alternatives.contains(handler)) {
                    alternatives.add(handler);
                    return handler;
                }
            }
            return null;
        }

        @Override public IFluidTankProperties[] getTankProperties() {
            List<IFluidTankProperties> properties = new ArrayList<>();
            IFluidHandler port;
            for (int index = 0; (port = port(index)) != null; index++)
                Collections.addAll(properties, port.getTankProperties());
            return properties.toArray(new IFluidTankProperties[0]);
        }

        @Override public int fill(FluidStack resource, boolean execute) {
            IFluidHandler port;
            for (int index = 0; (port = port(index)) != null; index++) {
                int amount = port.fill(resource, execute);
                if (amount > 0) return amount;
            }
            return 0;
        }

        @Nullable @Override public FluidStack drain(FluidStack resource, boolean execute) {
            if (resource == null || resource.amount <= 0) return null;
            if (sampledPort != null && sampledFluid.isFluidEqual(resource))
                return sampledPort.drain(resource, execute);
            IFluidHandler port;
            for (int index = 0; (port = port(index)) != null; index++) {
                FluidStack fluid = port.drain(resource, execute);
                if (fluid != null && fluid.amount > 0) return fluid;
            }
            return null;
        }

        @Nullable @Override public FluidStack drain(int maxAmount, boolean execute) {
            sampledPort = null;
            sampledFluid = null;
            if (maxAmount <= 0) return null;
            IFluidHandler port;
            for (int index = 0; (port = port(index)) != null; index++) {
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
