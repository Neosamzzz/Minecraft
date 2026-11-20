package newearth;

import galaxyspace.core.util.GSCreativeTabs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ItemCatalogue {
    private static final List<String> ORDER = Arrays.asList(
            "material_station", "engine_station", "assembly_station", "disassembly_station",
            "storage_pipe", "sealed_storage_pipe", "blueprint_material_station", "blueprint_engine_station",
            "composite_plate", "thermal_liner", "avionics", "connector",
            "blueprint_t7", "blueprint_sublight", "nose_cone", "rocket_body", "rocket_engine", "booster", "fin", "tier7_rocket",
            "amethyst_fragment", "amethyst_ingot", "amethyst_plate", "amethyst_component", "control_panel", "blueprint_plasma",
            "tier9_nose_cone", "tier9_body", "tier9_engine", "tier9_booster", "tier9_fin", "tier9_rocket",
            "final_energy_core", "tier10_rocket");
    private static final Comparator<ItemStack> DISPLAY = Comparator.comparingInt(ItemCatalogue::rank)
            .thenComparing(ItemStack::getMetadata);

    public static final CreativeTabs TAB = new CreativeTabs(NewEarth.MOD_ID) {
        @Override @SideOnly(Side.CLIENT)
        public ItemStack createIcon() { return Manufacturing.own("final_energy_core"); }

        @Override public void displayAllRelevantItems(NonNullList<ItemStack> list) {
            NonNullList<ItemStack> entries = NonNullList.create();
            for (Item item : Manufacturing.ITEMS.values()) item.getSubItems(this, entries);
            entries.sort(DISPLAY);
            list.addAll(entries);
        }
    };

    private ItemCatalogue() { }

    public static int rank(ItemStack stack) {
        ResourceLocation id = stack.getItem().getRegistryName();
        int position = id == null || !NewEarth.MOD_ID.equals(id.getNamespace()) ? -1 : ORDER.indexOf(id.getPath());
        return position < 0 ? Integer.MAX_VALUE : position;
    }

    private static boolean owns(ItemStack stack) {
        ResourceLocation id = stack.getItem().getRegistryName();
        return id != null && NewEarth.MOD_ID.equals(id.getNamespace());
    }

    // JEI sees the vanilla Search tab before custom tabs; sort only our entries after it deduplicates them.
    public static List<ItemStack> orderJei(List<ItemStack> entries) {
        List<ItemStack> ordered = new ArrayList<>();
        for (ItemStack stack : entries) if (owns(stack)) ordered.add(stack);
        ordered.sort(DISPLAY);
        int next = 0;
        for (ListIterator<ItemStack> iterator = entries.listIterator(); iterator.hasNext();) {
            if (owns(iterator.next())) iterator.set(ordered.get(next++));
        }
        return entries;
    }

    @SideOnly(Side.CLIENT)
    public static void repairLegacyIcons() {
        Item magnesium = Item.REGISTRY.getObject(new ResourceLocation("galaxyspace", "ingots"));
        if (magnesium != null && magnesium != Items.AIR && GSCreativeTabs.GSItemsTab != null) {
            ItemStack icon = new ItemStack(magnesium, 1, 1);
            GSCreativeTabs.GSItemsTab.setItemForTab(icon);
            setCachedIcon(GSCreativeTabs.GSItemsTab, icon);
        }
        Item mercury = Item.REGISTRY.getObject(new ResourceLocation("extraplanets", "ingot_mercury"));
        if (mercury == null || mercury == Items.AIR) return;
        for (CreativeTabs tab : CreativeTabs.CREATIVE_TAB_ARRAY) {
            if (tab == null || !"SpaceItemsTab".equals(tab.getTabLabel())) continue;
            ItemStack icon = tab.getIcon();
            if (icon.isEmpty() || ContentPruning.retiredOutput(icon)) setCachedIcon(tab, new ItemStack(mercury));
        }
    }

    @SideOnly(Side.CLIENT)
    static void setCachedIcon(CreativeTabs tab, ItemStack icon) {
        ReflectionHelper.setPrivateValue(CreativeTabs.class, tab, icon, new String[] {"icon", "field_151245_t"});
    }
}
