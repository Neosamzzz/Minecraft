package newearth.storage;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

/** An explicit allow-list: output, fuel, battery and special-purpose slots never become transfer targets. */
public final class TransferProfiles {
    private static final String GC = "micdoodle8.mods.galacticraft.";
    private static final String GS = "galaxyspace.systems.SolarSystem.planets.overworld.inventory.";
    private static final Map<String, Profile> PROFILES = new LinkedHashMap<>();
    static {
        add("net.minecraft.inventory.ContainerWorkbench", "minecraft.crafting", range(1, 9), range(1, 9));
        add(GC + "core.inventory.ContainerCrafting", "minecraft.crafting", range(1, 9), range(1, 9));
        add(GC + "core.inventory.ContainerIngotCompressor", "galacticraft.ingotcompressor", range(0, 9), range(0, 9));
        add(GC + "core.inventory.ContainerElectricIngotCompressor", "galacticraft.ingotcompressor", range(0, 9), range(0, 9));
        add(GC + "core.inventory.ContainerCircuitFabricator", "galacticraft.circuits", range(0, 5), range(1, 5));
        add(GS + "ContainerAdvCircuitFabricator", "galacticraft.circuits", range(0, 5), range(1, 5));
        add(GS + "ContainerAssembler", "galaxyspace.assembler", range(0, 9), range(0, 9));
        add(GS + "ContainerUniversalRecycler", "galaxyspace.universal_recycler", new int[] {0}, new int[] {1});
        add(GS + "ContainerRocketAssembler", "galaxyspace.rocket_assembler",
                new int[] {0,1,2,4,5,6,7,10,11,12}, new int[] {2,3,4,6,7,8,9,11,12,13});
        add("newearth.ManufacturingContainer", "newearth.material_station", range(0, 9), range(0, 9));
        add("newearth.AssemblyStationContainer", "newearth.assembly_station", range(0, 7), range(0, 7));
        nasa(GC + "core.inventory.ContainerSchematicTier1Rocket", "galacticraft.rocketT1", 17, 12);
        nasa(GC + "core.inventory.ContainerBuggyBench", "galacticraft.buggy", 19);
        nasa(GC + "planets.mars.inventory.ContainerSchematicTier2Rocket", "galacticraft.rocketT2", 21, 15);
        nasa(GC + "planets.asteroids.inventory.ContainerSchematicTier3Rocket", "galacticraft.rocketT3", 21, 15);
        nasa(GC + "planets.mars.inventory.ContainerSchematicCargoRocket", "galacticraft.cargoRocket", 16, 11);
        nasa(GC + "planets.asteroids.inventory.ContainerSchematicAstroMiner", "galacticraft.astroMiner", 14);
        nasa(GS + "schematics.ContainerSchematicCone", "galaxyspace.nose_cone", 24);
        nasa(GS + "schematics.ContainerSchematicBody", "galaxyspace.body", 24);
        nasa(GS + "schematics.ContainerSchematicEngine", "galaxyspace.engine", 18);
        nasa(GS + "schematics.ContainerSchematicBooster", "galaxyspace.booster", 10);
        nasa(GS + "schematics.ContainerSchematicFins", "galaxyspace.stabiliser", 12);
        nasa(GS + "schematics.ContainerSchematicOxTank", "galaxyspace.ox_tank", 12);
        nasa(GS + "schematics.ContainerSchematicTier2Rocket", "galaxyspace.rocket_tier_2", 21, 15);
    }
    public static final class Profile {
        public final String container, category;
        public final int[] ingredients, targets;
        public final boolean nasa;
        private Profile(String container, String category, int[] ingredients, int[] targets, boolean nasa) {
            this.container = container; this.category = category; this.ingredients = ingredients;
            this.targets = targets; this.nasa = nasa;
        }
        public Map<Integer, Integer> slots(Container container) {
            Map<Integer, Integer> result = new LinkedHashMap<>();
            for (int i = 0; i < targets.length; i++) {
                int target = targets[i];
                if (nasa) {
                    target = -1;
                    for (Slot slot : container.inventorySlots)
                        if (!(slot.inventory instanceof net.minecraft.entity.player.InventoryPlayer)
                                && !(slot.inventory instanceof net.minecraft.inventory.InventoryCraftResult)
                                && slot.getSlotIndex() == targets[i]) { target = slot.slotNumber; break; }
                }
                if (target < 0 || target >= container.inventorySlots.size()) return Collections.emptyMap();
                result.put(ingredients[i], target);
            }
            // Material and engine stations share a container class, but the latter has no nine-slot matrix.
            if (this.container.equals("newearth.ManufacturingContainer") && container.inventorySlots.size() != 47)
                return Collections.emptyMap();
            return result;
        }
    }
    private static int[] range(int first, int count) {
        int[] result = new int[count]; for (int i = 0; i < count; i++) result[i] = first + i; return result;
    }
    private static void add(String container, String category, int[] ingredients, int[] slots) {
        PROFILES.put(container, new Profile(container, category, ingredients, slots, false));
    }
    private static void nasa(String container, String category, int count, int... excluded) {
        List<Integer> indices = new ArrayList<>(), slots = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            boolean skip = false; for (int value : excluded) skip |= i == value;
            if (!skip) { indices.add(i - 1); slots.add(i); }
        }
        PROFILES.put(container, new Profile(container, category, indices.stream().mapToInt(i -> i).toArray(),
                slots.stream().mapToInt(i -> i).toArray(), true));
    }
    public static Profile get(Container container, String category) {
        Profile profile = PROFILES.get(container.getClass().getName());
        return profile != null && profile.category.equals(category) ? profile : null;
    }
    public static Collection<Profile> all() { return Collections.unmodifiableCollection(PROFILES.values()); }

    public static boolean special(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = (stack.getItem().getRegistryName() + " " + stack.getTranslationKey()).toLowerCase(Locale.ROOT);
        return name.contains("schematic") || name.contains("blueprint") || name.contains("license")
                || name.contains("engine") || name.contains("control_panel") || name.contains("information_panel")
                || name.contains("controlpanel") || name.contains("informationpanel");
    }

    /** Resolve the actual open workstation; clients never supply a world position. */
    public static BlockPos anchor(Container container) {
        Object value = find(container, TileEntity.class);
        if (value instanceof TileEntity) return ((TileEntity) value).getPos();
        for (Slot slot : container.inventorySlots)
            if (slot.inventory instanceof TileEntity) return ((TileEntity) slot.inventory).getPos();
        value = find(container, BlockPos.class);
        if (value instanceof BlockPos) return (BlockPos) value;
        for (Slot slot : container.inventorySlots) {
            value = find(slot, BlockPos.class);
            if (value instanceof BlockPos) return (BlockPos) value;
        }
        return null;
    }
    private static Object find(Object owner, Class<?> wanted) {
        for (Class<?> type = owner.getClass(); type != null && type != Object.class; type = type.getSuperclass())
            for (Field field : type.getDeclaredFields())
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && wanted.isAssignableFrom(field.getType()))
                    try { field.setAccessible(true); Object value = field.get(owner); if (value != null) return value; }
                    catch (ReflectiveOperationException | RuntimeException ignored) { }
        return null;
    }
}
