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
    public static boolean validRocketSlot(int index, ItemStack stack) {
        if(stack.isEmpty() || !Manufacturing.ITEMS.containsValue(stack.getItem())) return false;
        String id=stack.getItem().getRegistryName().getPath();
        switch(index) {
            case 2:return id.equals("nose_cone");
            case 3:case 4:return id.equals("rocket_body");
            case 5:return id.equals("rocket_engine");
            case 6:case 7:return id.equals("booster");
            case 8:case 9:return id.equals("fin");
            case 10:return id.equals("blueprint_t7");
            default:return false;
        }
    }
    public static boolean tier7(Item item) { return "newearth:tier7_rocket".equals(String.valueOf(item.getRegistryName())); }
    public static void powered(ItemStack stack) {
        try {
            Class<?> type=Class.forName("asmodeuscore.core.astronomy.SpaceData$Engine_Type");
            Object engine=type.getField("SUBLIGHT_ENGINE").get(null);
            if(!stack.hasTagCompound())stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            stack.getTagCompound().setBoolean((String)type.getMethod("getName").invoke(engine),true);
            String count=(String)Class.forName("galaxyspace.systems.SolarSystem.planets.overworld.items.armor.ItemSpaceSuit").getField("mod_count").get(null);
            stack.getTagCompound().setInteger(count,0);
        } catch(ReflectiveOperationException e) {throw new IllegalStateException(e);}
    }
    public static boolean assemblyEngine(ItemStack stack) {
        return !stack.isEmpty() && "galaxyspace:rocket_modules".equals(String.valueOf(stack.getItem().getRegistryName()))
                && (stack.getMetadata()==6 || stack.getMetadata()==5 || stack.getMetadata()==7);
    }
    public static void assemblyContainer(net.minecraft.inventory.Container container,Object tile) {
        try {
            net.minecraft.inventory.IInventory matrix=(net.minecraft.inventory.IInventory)tile.getClass().getField("rocketCraftMatrix").get(tile);
            for(net.minecraft.inventory.Slot slot:container.inventorySlots) if(slot.inventory==matrix) {
                if(slot.getSlotIndex()==2)slot.yPos=25;
                if(slot.getSlotIndex()==3)slot.yPos=50;
                if(slot.getSlotIndex()==4)slot.yPos=75;
                if(slot.getSlotIndex()==5)slot.yPos=130;
                if(slot.getSlotIndex()==6 || slot.getSlotIndex()==7)slot.yPos=100;
                if(slot.getSlotIndex()==8 || slot.getSlotIndex()==9)slot.yPos=130;
            }
            net.minecraft.inventory.Slot engine=new net.minecraft.inventory.Slot(matrix,14,40,100) {
                @Override public boolean isItemValid(ItemStack stack){return assemblyEngine(stack);}
                @Override public int getSlotStackLimit(){return 1;}
            };
            engine.slotNumber=container.inventorySlots.size();
            container.inventorySlots.add(engine);container.inventoryItemStacks.add(ItemStack.EMPTY);
        } catch(ReflectiveOperationException e) {throw new IllegalStateException(e);}
    }
    public static ItemStack transferAssemblyEngine(net.minecraft.inventory.Container container,net.minecraft.entity.player.EntityPlayer player,int index) {
        net.minecraft.inventory.Slot source=container.inventorySlots.get(index);
        net.minecraft.inventory.Slot engine=container.inventorySlots.get(container.inventorySlots.size()-1);
        if(!source.getHasStack())return null;
        ItemStack stack=source.getStack(),copy=stack.copy();
        if(source==engine) {
            if(!player.inventory.addItemStackToInventory(stack))return ItemStack.EMPTY;
            if(stack.isEmpty())source.putStack(ItemStack.EMPTY);else source.onSlotChanged();
            return copy;
        }
        if(source.inventory==player.inventory && assemblyEngine(stack)) {
            if(engine.getHasStack())return ItemStack.EMPTY;
            ItemStack one=stack.copy();one.setCount(1);engine.putStack(one);stack.shrink(1);
            if(stack.isEmpty())source.putStack(ItemStack.EMPTY);else source.onSlotChanged();
            return copy;
        }
        return null;
    }
    public static boolean readyStack(ItemStack stack) {
        if(stack.isEmpty() || !tier7(stack.getItem()) || !stack.hasTagCompound()) return false;
        try {
            Class<?> type=Class.forName("asmodeuscore.core.astronomy.SpaceData$Engine_Type");
            Object engine=type.getField("SUBLIGHT_ENGINE").get(null);
            return stack.getTagCompound().getBoolean((String)type.getMethod("getName").invoke(engine));
        } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    public static Entity swapIfTier7(Entity created, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !tier7(stack.getItem())) return created;
        if (!(created instanceof galaxyspace.core.prefab.entities.EntityTier6Rocket)) return created;
        galaxyspace.core.prefab.entities.EntityTier6Rocket old = (galaxyspace.core.prefab.entities.EntityTier6Rocket) created;
        return new EntityTier7Rocket(created.world, old.posX, old.posY, old.posZ, old.getType());
    }

    public static void placed(Entity entity, ItemStack stack) { entity.getEntityData().setBoolean(T7,tier7(stack.getItem())); }
    public static int rocketTier(Entity entity) {
        if(!entity.getEntityData().getBoolean(T7)) return 6;
        return "SUBLIGHT_ENGINE".equals(engine(entity))?7:0;
    }
    public static boolean canIgnite(Entity entity) {
        return !entity.getEntityData().getBoolean(T7) || "SUBLIGHT_ENGINE".equals(engine(entity));
    }
    public static Item rocketItem(Entity entity) {
        return SolarSystemBridge.requireItem(entity.getEntityData().getBoolean(T7)?"newearth:tier7_rocket":"galaxyspace:rocket_tier_6");
    }
    private static String engine(Entity entity) {
        try { return entity.getClass().getMethod("getEngine").invoke(entity).toString(); }
        catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    public static Object modulesFor(Item item) {
        try {
            Class<?> base=Class.forName("galaxyspace.core.prefab.items.modules.ItemModule");
            Object result=java.lang.reflect.Array.newInstance(base,1);
            String name=tier7(item)
                    ? "galaxyspace.systems.SolarSystem.planets.overworld.items.modules.SublightEngine"
                    : "newearth.Tier6IonModule";
            java.lang.reflect.Array.set(result,0,Class.forName(name).newInstance());
            return result;
        } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    static String moduleType(Item item) { return tier7(item)?"SublightEngine":"IonEngine"; }
    public static void recover(Entity entity, Object player) {
        if(!entity.getEntityData().getBoolean(T7) || !(player instanceof EntityPlayerMP)) return;
        GCPlayerStats stats=GCPlayerStats.get((EntityPlayerMP)player);
        stats.setRocketItem(rocketItem(entity));
        List<ItemStack> stacks=stats.getRocketStacks();
        if(stacks==null || stacks.isEmpty()) throw new IllegalStateException("Missing T7 recovery inventory");
        ItemStack stack=new ItemStack(rocketItem(entity),1,stats.getRocketType());
        stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        try {
            Object value=entity.getClass().getMethod("getEngine").invoke(entity);
            String key=(String)value.getClass().getMethod("getName").invoke(value);
            if(!"FUEL_ENGINE".equals(value.toString())) stack.getTagCompound().setBoolean(key,true);
            String count=(String)Class.forName("galaxyspace.systems.SolarSystem.planets.overworld.items.armor.ItemSpaceSuit").getField("mod_count").get(null);
            stack.getTagCompound().setInteger(count,"FUEL_ENGINE".equals(value.toString())?1:0);
        } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
        stacks.set(stacks.size()-1,stack);
    }
    public static List<ItemStack> dropped(List<ItemStack> stacks) {
        for(ItemStack stack:stacks) if(!stack.isEmpty() && tier7(stack.getItem()) && stack.hasTagCompound()) {
            try {
                String count=(String)Class.forName("galaxyspace.systems.SolarSystem.planets.overworld.items.armor.ItemSpaceSuit").getField("mod_count").get(null);
                stack.getTagCompound().setInteger(count,readyStack(stack)?0:1);
            } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
        return stacks;
    }
}
