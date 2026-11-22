package newearth.storage;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import net.minecraft.item.ItemStack;

/** Storage Drawers 5.5.2 automation reports and extracts the same usable stock. */
public final class DrawerAutomation {
    private static final String PACKAGE = "com.jaquadro.minecraft.storagedrawers.";
    private static final String FRACTIONAL = PACKAGE + "block.tile.tiledata.FractionalDrawerGroup$FractionalDrawer";
    private static final ClassValue<Access> ACCESS = new ClassValue<Access>() {
        @Override protected Access computeValue(Class<?> drawer) {
            return new Access(drawer);
        }
    };

    private DrawerAutomation() { }

    public static int available(Object drawer, int count) {
        if (count <= 1) return 0;
        if (count == Integer.MAX_VALUE) return count - 1;
        return available(drawer, count, ACCESS.get(drawer.getClass()));
    }

    private static int available(Object drawer, int count, Access access) {
        if (count <= 1) return 0;
        if (count == Integer.MAX_VALUE) return count - 1;
        if (access.storage == null) return count - 1;
        try {
            Object storage = access.storage.get(drawer);
            return fractionalAvailable(access.pool.getInt(storage), (int[]) access.rates.get(storage), access.slot.getInt(drawer));
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Cannot read compacting drawer stock", error);
        }
    }

    static int fractionalAvailable(int pool, int[] rates, int slot) {
        int unit = rates[slot];
        if (pool <= 0 || unit <= 0) return 0;
        int reserve = 0;
        for (int rate : rates)
            if (rate > reserve && rate <= pool) reserve = rate;
        return Math.max(0, pool - reserve) / unit;
    }

    public static boolean empty(Object drawer, boolean upstreamEmpty) {
        return upstreamEmpty || available(drawer) == 0;
    }

    public static ItemStack visibleStack(Object drawer, ItemStack publicStack) {
        if (publicStack.isEmpty()) return publicStack;
        int count = available(drawer);
        if (count == 0) return ItemStack.EMPTY;
        if (count >= publicStack.getCount()) return publicStack;
        ItemStack copy = publicStack.copy();
        copy.setCount(count);
        return copy;
    }

    // Storage Drawers returns the amount left over from the requested change.
    public static int extract(Object drawer, int delta) {
        if (delta == 0) return 0;
        int requested = -delta;
        Access access = ACCESS.get(drawer.getClass());
        int allowed = Math.min(requested, available(drawer, access.stored(drawer), access));
        if (allowed == 0) return requested;
        try {
            return requested - allowed + (int) access.adjust.invokeExact(drawer, -allowed);
        } catch (RuntimeException | Error error) {
            throw error;
        } catch (Throwable error) {
            throw new IllegalStateException("Cannot withdraw from Storage Drawers", error);
        }
    }

    private static int available(Object drawer) {
        Access access = ACCESS.get(drawer.getClass());
        return available(drawer, access.stored(drawer), access);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class Access {
        final MethodHandle count, adjust;
        final Field storage, pool, rates, slot;

        Access(Class<?> drawer) {
            try {
                Class<?> api = Class.forName(PACKAGE + "api.storage.IDrawer", false, drawer.getClassLoader());
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                count = lookup.findVirtual(api, "getStoredItemCount", MethodType.methodType(int.class))
                        .asType(MethodType.methodType(int.class, Object.class));
                adjust = lookup.findVirtual(api, "adjustStoredItemCount", MethodType.methodType(int.class, int.class))
                        .asType(MethodType.methodType(int.class, Object.class, int.class));
                if (drawer.getName().equals(FRACTIONAL)) {
                    storage = field(drawer, "storage");
                    slot = field(drawer, "slot");
                    pool = field(storage.getType(), "pooledCount");
                    rates = field(storage.getType(), "convRate");
                } else {
                    storage = pool = rates = slot = null;
                }
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unsupported Storage Drawers inventory", error);
            }
        }

        int stored(Object drawer) {
            try {
                return (int) count.invokeExact(drawer);
            } catch (RuntimeException | Error error) {
                throw error;
            } catch (Throwable error) {
                throw new IllegalStateException("Cannot read Storage Drawers stock", error);
            }
        }
    }
}
