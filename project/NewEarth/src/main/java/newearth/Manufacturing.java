package newearth;

import java.util.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.*;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistryModifiable;

/** Pack manufacturing data and the two reusable processing stations. */
@Mod.EventBusSubscriber(modid = NewEarth.MOD_ID)
public final class Manufacturing {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    static final List<Batch> BATCHES = new ArrayList<>();
    static final String[] PARTS = {"composite_plate", "thermal_liner", "avionics", "connector",
            "nose_cone", "rocket_body", "rocket_engine", "booster", "fin", "blueprint_t4", "blueprint_t5", "blueprint_t6",
            "blueprint_engine_station", "blueprint_material_station", "blueprint_t7", "blueprint_sublight",
            "amethyst_fragment", "amethyst_ingot", "amethyst_plate", "amethyst_component",
            "tier9_nose_cone", "tier9_body", "tier9_engine", "tier9_booster", "tier9_fin", "control_panel", "blueprint_plasma"};
    private Manufacturing() { }

    @SubscribeEvent public static void blocks(RegistryEvent.Register<Block> event) {
        for (String id : new String[] {"material_station", "engine_station"}) {
            Block block = new Station(id).setRegistryName(NewEarth.MOD_ID, id)
                    .setTranslationKey("newearth." + id).setCreativeTab(CreativeTabs.MISC).setHardness(3F);
            BLOCKS.put(id, block); event.getRegistry().register(block);
        }
        GameRegistry.registerTileEntity(ManufacturingTile.class, new ResourceLocation(NewEarth.MOD_ID, "manufacturing"));
    }

    @SubscribeEvent public static void items(RegistryEvent.Register<Item> event) {
        for (String id : PARTS) register(event, id, new Item().setCreativeTab(CreativeTabs.MISC));
        for (Map.Entry<String, Block> block : BLOCKS.entrySet()) register(event, block.getKey(), new ItemBlock(block.getValue()));
        try {
            // Reuse GS's placement and modification interfaces; the transformer
            // preserves the new item identity on the existing rocket entity.
            Item rocket = (Item) Class.forName("galaxyspace.core.prefab.items.rockets.ItemTier6Rocket")
                    .getConstructor(String.class).newInstance("newearth.tier7_rocket");
            register(event, "tier7_rocket", rocket);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("GS T6 item API changed", e); }
    }

    private static void register(RegistryEvent.Register<Item> event, String id, Item item) {
        item.setRegistryName(NewEarth.MOD_ID, id).setTranslationKey("newearth." + id);
        ITEMS.put(id, item); event.getRegistry().register(item);
    }
    public static ItemStack own(String id) { return new ItemStack(ITEMS.get(id)); }
    public static List<Batch> batches() { return Collections.unmodifiableList(BATCHES); }

    public static final class Batch {
        public final String machine;
        public final ItemStack output;
        public final List<ItemStack> inputs;
        Batch(String machine, ItemStack output, List<ItemStack> inputs) {
            this.machine = machine; this.output = output; this.inputs = inputs;
        }
        /** Computes a complete debit plan before consuming any inventory. */
        int[] debit(net.minecraft.inventory.IInventory inventory, int limit) {
            int[] used = new int[limit];
            for (ItemStack need : inputs) {
                int left = need.getCount();
                for (int i = 0; i < limit && left > 0; i++) {
                    ItemStack have = inventory.getStackInSlot(i);
                    if (ItemStack.areItemsEqual(have, need) && ItemStack.areItemStackTagsEqual(have, need)) {
                        int take = Math.min(left, have.getCount() - used[i]);
                        used[i] += take; left -= take;
                    }
                }
                if (left > 0) return null;
            }
            // Reject unexpected ingredients rather than silently consuming a subset.
            for (int i = 0; i < limit; i++) if (!inventory.getStackInSlot(i).isEmpty() && used[i] == 0) return null;
            return used;
        }
    }

    static ItemStack resolve(JsonObject spec) {
        int count = spec.has("count") ? spec.get("count").getAsInt() : 1;
        int meta = spec.has("meta") ? spec.get("meta").getAsInt() : 0;
        if (spec.has("field")) {
            String field = spec.get("field").getAsString(); int dot = field.lastIndexOf('.');
            try {
                Object value = Class.forName(field.substring(0, dot)).getField(field.substring(dot + 1)).get(null);
                ItemStack result = value instanceof Block ? new ItemStack((Block)value, count, meta) : new ItemStack((Item)value, count, meta);
                if (result.isEmpty()) throw new IllegalStateException("Unregistered manufacturing material: " + field);
                return result;
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("Missing manufacturing material " + field, e); }
        }
        return new ItemStack(SolarSystemBridge.requireItem(spec.get("item").getAsString()), count, meta);
    }

    static void install() {
        removeOldLicences();
        BATCHES.clear();
        try (Reader reader = new InputStreamReader(Manufacturing.class.getResourceAsStream("/assets/newearth/manufacturing.json"), StandardCharsets.UTF_8)) {
            for (JsonElement element : new JsonParser().parse(reader).getAsJsonArray()) {
                JsonObject recipe = element.getAsJsonObject(); List<ItemStack> inputs = new ArrayList<>();
                for (JsonElement input : recipe.getAsJsonArray("inputs")) inputs.add(resolve(input.getAsJsonObject()));
                ItemStack output = resolve(recipe.getAsJsonObject("output"));
                String machine = recipe.get("machine").getAsString();
                if (machine.equals("material_station") || machine.equals("engine_station")) BATCHES.add(new Batch(machine, output, inputs));
                else if (processingRecipe(machine,output,inputs)) { }
                else {
                    if (recipe.has("layout")) addShapedGsRecipe(machine,output,recipe.getAsJsonArray("layout"));
                    else addGsRecipe(machine, output, inputs);
                    if (machine.equals("RocketAssemblyRecipes")) for (int cargo=1;cargo<=3;cargo++) {
                        List<ItemStack> cargoInputs=new ArrayList<>(inputs);
                        cargoInputs.add(new ItemStack(net.minecraft.init.Blocks.CHEST,cargo));
                        ItemStack variant=output.copy();variant.setItemDamage(cargo);
                        addGsRecipe(machine,variant,cargoInputs);
                    }
                }
            }
        } catch (IOException e) { throw new IllegalStateException(e); }
        ManufacturingHooks.cleanLoot();
    }

    static boolean processingRecipe(String machine, ItemStack output, List<ItemStack> inputs) {
        if(machine.equals("smelting")) {
            ItemStack input=inputs.get(0);
            net.minecraft.item.crafting.FurnaceRecipes.instance().getSmeltingList().keySet().removeIf(k -> k.getItem()==input.getItem());
            GameRegistry.addSmelting(input,output,0.2F);
        } else if (machine.equals("compressor")) {
            List<Object> expanded=new ArrayList<>();
            for(ItemStack input:inputs) for(int i=0;i<input.getCount();i++){ItemStack one=input.copy();one.setCount(1);expanded.add(one);}
            micdoodle8.mods.galacticraft.api.recipe.CompressorRecipes.addShapelessRecipe(output,expanded.toArray());
        } else if (machine.equals("circuit")) {
            micdoodle8.mods.galacticraft.api.recipe.CircuitFabricatorRecipes.removeRecipe(output);
            List<Object> slots=new ArrayList<>();
            for(ItemStack input:inputs) for(int i=0;i<input.getCount();i++){ItemStack one=input.copy();one.setCount(1);slots.add(one);}
            if(slots.size()!=5)throw new IllegalStateException("Circuit recipe requires five slots");
            micdoodle8.mods.galacticraft.api.recipe.CircuitFabricatorRecipes.addRecipe(output,slots);
        } else return false;
        return true;
    }

    private static void addShapedGsRecipe(String machine,ItemStack output,JsonArray layout) {
        StringBuilder pattern=new StringBuilder();List<Object> keys=new ArrayList<>();
        for(int i=0;i<9;i++) {
            JsonElement cell=layout.get(i);
            if(cell.isJsonNull())pattern.append(' ');
            else {char key=(char)('A'+i);pattern.append(key);keys.add(key);keys.add(resolve(cell.getAsJsonObject()));}
        }
        List<Object> args=new ArrayList<>();for(int row=0;row<3;row++)args.add(pattern.substring(row*3,row*3+3));args.addAll(keys);
        try {
            Class.forName("galaxyspace.systems.SolarSystem.planets.overworld.recipes."+machine)
                    .getMethod("addRecipe",ItemStack.class,Object[].class).invoke(null,output,args.toArray());
        } catch(ReflectiveOperationException e) {throw new IllegalStateException("GS shaped recipe API changed",e);}
    }

    private static void addGsRecipe(String machine, ItemStack output, List<ItemStack> inputs) {
        List<ItemStack> expanded = new ArrayList<>();
        for (ItemStack input : inputs) for (int i = 0; i < input.getCount(); i++) { ItemStack one = input.copy(); one.setCount(1); expanded.add(one); }
        try {
            Class<?> type = Class.forName("galaxyspace.systems.SolarSystem.planets.overworld.recipes." + machine);
            type.getMethod("addShapelessRecipe", ItemStack.class, Object[].class).invoke(null, output, expanded.toArray());
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("GS recipe API changed", e); }
    }

    private static boolean oldLicence(ItemStack output) {
        return !output.isEmpty() && "galaxyspace:rocket_modules".equals(String.valueOf(output.getItem().getRegistryName()))
                && output.getMetadata() >= 8 && output.getMetadata() <= 10;
    }
    private static void removeOldLicences() {
        // Forge freezes its main registry after registration. Removal is performed
        // by the recipe event below, while GS's private list remains mutable.
        for (String type : new String[] {"AssemblyRecipes"}) try {
            @SuppressWarnings("unchecked") List<IRecipe> recipes = (List<IRecipe>) Class.forName("galaxyspace.systems.SolarSystem.planets.overworld.recipes." + type).getMethod("getRecipeList").invoke(null);
            recipes.removeIf(r -> oldLicence(r.getRecipeOutput()));
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    @SubscribeEvent(priority=net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
    public static void recipes(RegistryEvent.Register<IRecipe> event) {
        IForgeRegistryModifiable<IRecipe> registry = (IForgeRegistryModifiable<IRecipe>) event.getRegistry();
        List<ResourceLocation> remove = new ArrayList<>();
        for (IRecipe r : registry.getValuesCollection()) if (oldLicence(r.getRecipeOutput())) remove.add(r.getRegistryName());
        for (ResourceLocation id : remove) registry.remove(id);
    }

    private static final class Station extends BlockContainer {
        private static final net.minecraft.block.properties.PropertyDirection FACING = net.minecraft.block.BlockHorizontal.FACING;
        private final String id;
        Station(String id) { super(Material.IRON); this.id = id; this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)); }
        @Override protected net.minecraft.block.state.BlockStateContainer createBlockState() {
            return new net.minecraft.block.state.BlockStateContainer(this, FACING);
        }
        @Override public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
        }
        @Override public int getMetaFromState(IBlockState state) {
            return state.getValue(FACING).getHorizontalIndex();
        }
        @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, net.minecraft.entity.EntityLivingBase placer, ItemStack stack) {
            world.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
        }
        @Override public net.minecraft.tileentity.TileEntity createNewTileEntity(World world, int meta) { return new ManufacturingTile(); }
        @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }
        @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                EnumHand hand, EnumFacing facing, float x, float y, float z) {
            if (!world.isRemote) player.openGui(NewEarth.instance, ManufacturingGuiHandler.GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
            if (world.getTileEntity(pos) instanceof ManufacturingTile) InventoryHelper.dropInventoryItems(world, pos, (ManufacturingTile)world.getTileEntity(pos));
            super.breakBlock(world, pos, state);
        }
    }
}
