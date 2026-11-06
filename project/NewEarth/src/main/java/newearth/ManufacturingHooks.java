package newearth;

import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.*;
import net.minecraft.init.Items;
import micdoodle8.mods.galacticraft.api.GalacticraftRegistry;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;

/** Hooks are dimension-specific so EP/GS shared chest tiers stay independent. */
public final class ManufacturingHooks {
    private static final String T7 = "newearthTier7";
    private ManufacturingHooks() { }
    public static ItemStack reward(Object boss, Random random) {
        Entity entity = (Entity) boss;
        String provider = entity.world.provider.getClass().getName();
        int license = licenseMeta(provider);
        if (license >= 0) return modules(license);
        String blueprint = blueprint(provider);
        if (blueprint != null) return Manufacturing.own(blueprint);
        // GS Io formerly selected ion/sublight from a two-entry pool (50% each).
        if (provider.contains(".io.")) return random.nextInt(100) < 5
                ? modules(4) : new ItemStack(Items.IRON_INGOT, 4);
        return null;
    }
    /** GS rocket-module metas already accepted by the tier 4/5/6 rocket recipes. */
    static int licenseMeta(String provider) {
        if (!provider.startsWith("com.mjr.extraplanets.planets.")) return -1;
        if (provider.contains(".Mercury.")) return 8;
        if (provider.contains(".Jupiter.")) return 9;
        if (provider.contains(".Saturn.")) return 10;
        return -1;
    }
    static String blueprint(String provider) {
        if (provider.equals("galaxyspace.systems.BarnardsSystem.moons.barnarda_c1.dimension.WorldProviderBarnarda_C1_WE")) return "blueprint_plasma";
        if (!provider.startsWith("com.mjr.extraplanets.planets.")) return null;
        String[] names = {"Uranus", "Neptune", "Pluto", "Eris"};
        String[] ids = {"blueprint_engine_station",
                "blueprint_material_station", "blueprint_t7", "blueprint_sublight"};
        for (int i=0;i<names.length;i++) if(provider.contains("."+names[i]+".")) return ids[i];
        return null;
    }
    static ItemStack modules(int meta) { return new ItemStack(SolarSystemBridge.requireItem("galaxyspace:rocket_modules"),1,meta); }
    static void cleanLoot() {
        for (int tier=1;tier<=10;tier++) {
            List<ItemStack> pool=GalacticraftRegistry.getDungeonLoot(tier);
            if(pool!=null) pool.removeIf(s -> s.isEmpty() || ContentPruning.retiredOutput(s) || engineLoot(s));
        }
    }
    private static boolean engineLoot(ItemStack s) {
        return !s.isEmpty() && "galaxyspace:rocket_modules".equals(String.valueOf(s.getItem().getRegistryName()))
                && s.getMetadata()>=4 && s.getMetadata()<=7;
    }
    public static boolean tier7(Item item) { return "newearth:tier7_rocket".equals(String.valueOf(item.getRegistryName())); }
    public static Entity swapIfTier7(Entity created, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !tier7(stack.getItem())) return created;
        if (!(created instanceof galaxyspace.core.prefab.entities.EntityTier6Rocket)) return created;
        galaxyspace.core.prefab.entities.EntityTier6Rocket old = (galaxyspace.core.prefab.entities.EntityTier6Rocket) created;
        return new EntityTier7Rocket(created.world, old.posX, old.posY, old.posZ, old.getType());
    }

    public static void placed(Entity entity, ItemStack stack) { entity.getEntityData().setBoolean(T7,tier7(stack.getItem())); }
    public static Item rocketItem(Entity entity) {
        return SolarSystemBridge.requireItem(entity.getEntityData().getBoolean(T7)?"newearth:tier7_rocket":"galaxyspace:rocket_tier_6");
    }
    public static void recover(Entity entity, Object player) {
        if(!entity.getEntityData().getBoolean(T7) || !(player instanceof EntityPlayerMP)) return;
        GCPlayerStats stats=GCPlayerStats.get((EntityPlayerMP)player);
        stats.setRocketItem(rocketItem(entity));
        List<ItemStack> stacks=stats.getRocketStacks();
        if(stacks==null || stacks.isEmpty()) throw new IllegalStateException("Missing T7 recovery inventory");
        ItemStack stack=new ItemStack(rocketItem(entity),1,stats.getRocketType());
        stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        stacks.set(stacks.size()-1,stack);
    }
    public static galaxyspace.core.prefab.items.modules.ItemModule[] modulesFor(Item item) {
        if (tier7(item)) return new galaxyspace.core.prefab.items.modules.ItemModule[0];
        return new galaxyspace.core.prefab.items.modules.ItemModule[] {new Tier6IonModule()};
    }

    public static void filterRocketVariants(Item item, List<ItemStack> stacks) {
        String key = asmodeuscore.core.astronomy.SpaceData.Engine_Type.SUBLIGHT_ENGINE.getName();
        stacks.removeIf(stack -> stack.getItem() == item && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean(key));
    }

    public static List<ItemStack> dropped(List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || !tier7(stack.getItem()) || !stack.hasTagCompound()) continue;
            for (asmodeuscore.core.astronomy.SpaceData.Engine_Type engine : asmodeuscore.core.astronomy.SpaceData.Engine_Type.values()) {
                stack.getTagCompound().removeTag(engine.getName());
            }
            stack.getTagCompound().removeTag(galaxyspace.systems.SolarSystem.planets.overworld.items.armor.ItemSpaceSuit.mod_count);
        }
        return stacks;
    }
}
