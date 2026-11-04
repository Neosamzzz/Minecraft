package newearth;

import java.util.*;
import java.io.*;
import java.util.jar.*;
import com.google.gson.*;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Transactional material debits and installed GS/EP bytecode compatibility. */
public final class ManufacturingProbe {
    public static final class AssemblyFixture {public final InventoryBasic rocketCraftMatrix=new InventoryBasic("assembly",false,16);}
    private static int checks;
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);checks++;}
    static void run() throws Exception {
        net.minecraft.item.Item six=new net.minecraft.item.Item().setRegistryName("galaxyspace:rocket_tier_6");
        net.minecraft.item.Item seven=new net.minecraft.item.Item().setRegistryName("newearth:tier7_rocket");
        check(ManufacturingHooks.moduleType(six).equals("IonEngine"),"T6 modification table permits ion only");
        check(ManufacturingHooks.moduleType(seven).equals("SublightEngine"),"T7 modification table permits sublight only");
        AssemblyFixture fixture=new AssemblyFixture();
        net.minecraft.inventory.Container container=new net.minecraft.inventory.Container(){@Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player){return true;}};
        net.minecraft.inventory.Slot prior=new net.minecraft.inventory.Slot(fixture.rocketCraftMatrix,4,40,85);
        container.inventorySlots.add(prior);container.inventoryItemStacks.add(ItemStack.EMPTY);
        ManufacturingHooks.assemblyContainer(container,fixture);
        net.minecraft.inventory.Slot central=container.inventorySlots.get(1);
        check(central.getSlotIndex()==14 && central.inventory==fixture.rocketCraftMatrix,"Central engine uses the existing spare matrix index");
        check(prior.yPos==75 && central.yPos==100,"Body and central engine slots do not overlap");
        net.minecraft.item.Item modules=new net.minecraft.item.Item().setRegistryName("galaxyspace:rocket_modules").setHasSubtypes(true);
        check(central.isItemValid(new ItemStack(modules,1,6)),"Central engine accepts sublight module");
        check(central.isItemValid(new ItemStack(modules,1,5)),"Central engine accepts plasma module");
        check(central.isItemValid(new ItemStack(modules,1,7)),"Central engine accepts black hole module");
        check(central.getSlotStackLimit()==1,"Central engine takes a single module");
        check(!central.isItemValid(new ItemStack(modules,1,4)),"Central engine rejects ion module");
        InventoryBasic inv=new InventoryBasic("test",false,27);
        inv.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,2));
        inv.setInventorySlotContents(1,new ItemStack(Items.IRON_INGOT,3));
        Manufacturing.Batch batch=new Manufacturing.Batch("material_station",new ItemStack(Items.GOLD_INGOT,2),Arrays.asList(new ItemStack(Items.IRON_INGOT,4)));
        int[] debit=batch.debit(inv,18);
        check(debit!=null&&debit[0]==2&&debit[1]==2,"Split stacks fund a whole batch");
        check(inv.getStackInSlot(0).getCount()==2,"Matching never consumes inventory");
        inv.setInventorySlotContents(1,new ItemStack(Items.IRON_INGOT,1));
        check(batch.debit(inv,18)==null,"Insufficient inputs fail atomically");
        inv.setInventorySlotContents(18,new ItemStack(Items.IRON_INGOT,64));
        check(batch.debit(inv,18)==null,"Output slots cannot fund inputs");
        inv.setInventorySlotContents(1,new ItemStack(Items.IRON_INGOT,3));
        inv.setInventorySlotContents(2,new ItemStack(Items.DIAMOND));
        check(batch.debit(inv,18)==null,"Unexpected ingredient rejects the batch");
        Manufacturing.Batch repeated=new Manufacturing.Batch("test",new ItemStack(Items.PAPER),Arrays.asList(new ItemStack(Items.IRON_INGOT,3),new ItemStack(Items.IRON_INGOT,3)));
        inv.setInventorySlotContents(2,ItemStack.EMPTY);
        check(repeated.debit(inv,18)==null,"Repeated ingredient cannot double spend a stack");
        net.minecraft.tileentity.TileEntity.register("newearth:manufacturing", ManufacturingTile.class);
        ManufacturingTile tile=new ManufacturingTile();
        ManufacturingContainer powerGui=new ManufacturingContainer(new net.minecraft.entity.player.InventoryPlayer(null),tile);
        for(int charge:new int[]{32767,32768,50000}) {
            powerGui.updateProgressBar(0,(short)charge);
            check(tile.getField(0)==charge,"GUI energy sync preserves unsigned 16-bit charge "+charge);
        }
        Manufacturing.BATCHES.add(batch);
        tile.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,8));
        tile.setInventorySlotContents(ManufacturingTile.OUTPUT_SLOT,new ItemStack(Items.GOLD_INGOT,64));
        tile.setField(0,8000);
        for(int tick=0;tick<200;tick++)tile.process("material_station");
        check(tile.getStackInSlot(0).getCount()==8 && tile.getField(0)==8000,"Full output consumes neither inputs nor power");
        tile.setInventorySlotContents(ManufacturingTile.OUTPUT_SLOT,ItemStack.EMPTY);
        for(int tick=0;tick<200;tick++)tile.process("material_station");
        check(tile.getStackInSlot(0).getCount()==4 && tile.getStackInSlot(ManufacturingTile.OUTPUT_SLOT).getCount()==2,"Complete batch consumes once and produces exact quantity");
        check(tile.getField(0)==0,"200 processing ticks consume exactly 8000 FE");
        tile.process("material_station");
        check(tile.getField(1)==0 && tile.getField(2)==ManufacturingTile.STATUS_NO_POWER,"Machine stops without power");
        check(!tile.canInsertItem(ManufacturingTile.OUTPUT_SLOT,new ItemStack(Items.IRON_INGOT),net.minecraft.util.EnumFacing.UP),"Automation cannot insert into output");
        net.minecraft.util.NonNullList<ItemStack> old=net.minecraft.util.NonNullList.withSize(27,ItemStack.EMPTY);
        for(int i=0;i<27;i++)old.set(i,new ItemStack(Items.IRON_INGOT,i+1));
        net.minecraft.nbt.NBTTagCompound oldTag=new net.minecraft.nbt.NBTTagCompound();
        net.minecraft.inventory.ItemStackHelper.saveAllItems(oldTag,old);
        ManufacturingTile migrated=new ManufacturingTile();migrated.readFromNBT(oldTag);
        ManufacturingTile reloaded=new ManufacturingTile();reloaded.readFromNBT(migrated.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        int recovered=0;
        for(int i=0;i<27;i++) {
            reloaded.process("material_station");
            ItemStack returned=reloaded.removeStackFromSlot(ManufacturingTile.OUTPUT_SLOT);
            check(returned.getCount()==i+1,"Migration preserves old slot "+i);
            recovered+=returned.getCount();
        }
        check(recovered==378,"All 27 legacy stacks survive save/reload and recovery");
        Manufacturing.BATCHES.clear();
        for (String id : Manufacturing.PARTS) registerPart(id);
        registerPart("tier7_rocket");registerPart("tier9_rocket");registerPart("tier10_rocket");
        if (!net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.containsKey(modules.getRegistryName()))
            net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(modules);
        net.minecraft.tileentity.TileEntity.register("newearth:assembly_station",AssemblyStationTile.class);
        for (int tier : new int[] {7,9,10}) {
            AssemblyStationTile station=new AssemblyStationTile();
            List<ItemStack> inputs=AssemblyStationTile.inputs(tier);
            net.minecraft.entity.player.InventoryPlayer playerInventory=new net.minecraft.entity.player.InventoryPlayer(null);
            AssemblyStationContainer assemblyGui=new AssemblyStationContainer(playerInventory,station);
            for(ItemStack input:inputs) {
                playerInventory.setInventorySlotContents(9,input.copy());
                check(!assemblyGui.transferStackInSlot(null,AssemblyStationTile.TOTAL_SLOTS).isEmpty(),"T"+tier+" shift-click routes "+input.getItem().getRegistryName());
                check(playerInventory.getStackInSlot(9).isEmpty(),"Shift-click consumes exactly the routed input");
            }
            for(int slot=0;slot<inputs.size();slot++) {
                check(ItemStack.areItemsEqual(inputs.get(slot),station.getStackInSlot(slot)),"T"+tier+" correct assembly slot "+slot);
                check(assemblyGui.getSlot(slot).getSlotStackLimit()==1,"Structure slots accept one part each");
            }
            check(!assemblyGui.getSlot(8).isItemValid(inputs.get(7)),"Structural propulsion cannot enter the independent engine slot");
            check(!assemblyGui.getSlot(7).isItemValid(inputs.get(8)),"Independent engine cannot enter the structural propulsion slot");
            check(!station.canInsertItem(8,inputs.get(7),net.minecraft.util.EnumFacing.UP),"Automation respects propulsion slot roles");
            station.setInventorySlotContents(9,ItemStack.EMPTY);
            check(!station.process(),"T"+tier+" cannot bypass blueprint/control panel");
            station.setInventorySlotContents(9,inputs.get(9).copy());
            station.setInventorySlotContents(8,ItemStack.EMPTY);
            check(!station.process(),"T"+tier+" cannot bypass installed propulsion module");
            station.setInventorySlotContents(8,inputs.get(8).copy());
            check(!station.process() && station.getField(0)==1,"T"+tier+" starts processing without immediate output");
            check(station.getStackInSlot(AssemblyStationTile.OUTPUT_SLOT).isEmpty(),"Output stays empty during processing");
            check(!station.hasCapability(net.minecraftforge.energy.CapabilityEnergy.ENERGY,net.minecraft.util.EnumFacing.NORTH),"Assembly has no power requirement");
            for(int slot=0;slot<inputs.size();slot++)check(station.getStackInSlot(slot).getCount()==1,"Inputs remain until assembly completes");
            station.setInventorySlotContents(9,ItemStack.EMPTY);
            check(!station.process() && station.getField(0)==0,"Missing part resets progress without creating a rocket");
            station.setInventorySlotContents(9,inputs.get(9).copy());
            int duration=AssemblyStationTile.processingTime();
            boolean early=false;
            for(int tick=0;tick<duration/2;tick++)early|=station.process();
            check(!early,"No early assembly result");
            AssemblyStationTile savedStation=new AssemblyStationTile();
            savedStation.readFromNBT(station.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
            check(savedStation.getField(0)==station.getField(0),"Assembly progress survives save/reload");
            station=savedStation;
            for(int tick=station.getField(0);tick<duration-1;tick++)early|=station.process();
            check(!early,"Reloaded assembly waits until its final tick");
            check(station.process(),"T"+tier+" completes after the full GS processing duration");
            check(!station.process(),"Completed assembly cannot duplicate its output");
            ItemStack rocket=station.getStackInSlot(AssemblyStationTile.OUTPUT_SLOT);
            check(rocket.getItem()==Manufacturing.ITEMS.get("tier"+tier+"_rocket"),"Assembly preserves exact tier item");
            if(tier==7)check(ManufacturingHooks.readyStack(rocket),"T7 assembled with sublight is ready for flight");
            for(int slot=0;slot<inputs.size();slot++)station.setInventorySlotContents(slot,inputs.get(slot).copy());
            check(!station.process() && station.getField(0)==0,"Occupied output prevents a second processing cycle");
            for(int slot=0;slot<inputs.size();slot++)check(station.getStackInSlot(slot).getCount()==1,"Blocked output preserves every input");
            DisassemblyStationTile recycling=new DisassemblyStationTile();
            recycling.setInventorySlotContents(0,rocket.copy());recycling.process();
            for(int slot=0;slot<inputs.size();slot++)check(ItemStack.areItemsEqual(inputs.get(slot),recycling.getStackInSlot(slot+1)),"T"+tier+" recycling returns exact part "+slot);
        }
        AssemblyStationTile syncTile=new AssemblyStationTile();
        AssemblyStationContainer syncContainer=new AssemblyStationContainer(new net.minecraft.entity.player.InventoryPlayer(null),syncTile);
        for(int value:new int[]{32768,65536,200000}) {
            syncContainer.updateProgressBar(0,(short)value);
            syncContainer.updateProgressBar(1,(short)(value>>>16));
            syncContainer.updateProgressBar(2,(short)value);
            syncContainer.updateProgressBar(3,(short)(value>>>16));
            check(syncTile.getField(0)==value && syncTile.getField(1)==value,"Progress synchronization preserves full 32-bit values "+value);
        }
        for(int i=0;i<AssemblyStationContainer.INPUT_POS.length;i++) {
            int[] a=AssemblyStationContainer.INPUT_POS[i];
            for(int j=i+1;j<AssemblyStationContainer.INPUT_POS.length;j++) {
                int[] b=AssemblyStationContainer.INPUT_POS[j];
                check(Math.abs(a[0]-b[0])>=20 || Math.abs(a[1]-b[1])>=21,"GS slot frames do not overlap: "+i+"/"+j);
            }
        }
        ItemStack unpoweredSix=new ItemStack(six);
        check(!PlanetHooks.readyErisStack(6,six,unpoweredSix),"Unmodified T6 cannot reach Eris");
        net.minecraft.nbt.NBTTagCompound ion=new net.minecraft.nbt.NBTTagCompound();
        ion.setBoolean(asmodeuscore.core.astronomy.SpaceData.Engine_Type.ION_ENGINE.getName(),true);
        unpoweredSix.setTagCompound(ion);
        check(PlanetHooks.readyErisStack(6,six,unpoweredSix),"Ion-equipped T6 reaches Eris after NBT recovery");
        check(!PlanetHooks.readyErisStack(6,Items.PAPER,unpoweredSix),"Inventory ion tag cannot authorize a different rocket item");
        ItemStack sevenReady=AssemblyStationTile.output(7);
        check(PlanetHooks.readyErisStack(7,sevenReady.getItem(),sevenReady),"T7 sublight remains eligible for Eris");
        for(int tier:new int[]{9,10}) {
            ItemStack rocket=AssemblyStationTile.output(tier);
            check(PlanetHooks.readyErisStack(tier,rocket.getItem(),rocket),"T"+tier+" recovered ship remains eligible for Eris");
        }
        check(ItemTier9Rocket.completePad("galacticraftcore:landing_pad_full",0),"Complete GC launch pad accepted");
        check(ItemTier9Rocket.completePad("galaxyspace:advanced_landing_pad_full",0),"Complete GS launch pad accepted");
        check(!ItemTier9Rocket.completePad("galacticraftcore:landing_pad",0),"Individual pad pieces rejected");
        check(!ItemTier9Rocket.completePad("galacticraftcore:landing_pad_full",1),"Buggy pad rejected");
        micdoodle8.mods.galacticraft.core.util.ConfigManagerCore.rocketFuelFactor=1;
        EntityTier9Rocket ship=new EntityTier9Rocket(null);
        check(ship.getMaxSeats()==6 && ship.getRocketTier()==9 && ship.fuelTank.getCapacity()==1500,"T9 concrete entity has six seats and a usable fuel tank");
        check(ship.isDockValid(new micdoodle8.mods.galacticraft.core.tile.TileEntityLandingPad()),"T9 docks on a real GC launch pad");
        check(ship.isDockValid(new galaxyspace.systems.SolarSystem.planets.overworld.tile.TileEntityAdvLandingPad()),"T9 docks on a real GS advanced pad");
        EntityTier10Rocket finalShip=new EntityTier10Rocket(null);
        check(finalShip.getRocketTier()==10 && finalShip.recoveredItem().getItem()==Manufacturing.ITEMS.get("tier10_rocket"),"T10 recovers its own item");
        for(int meta=0;meta<4;meta++) {
            ship.setRocketType(meta);
            check(ship.recoveredItem().getMetadata()==meta,"Recovery preserves cargo variant "+meta);
        }
        InventoryBasic matrix=new InventoryBasic("rocket",false,14);
        net.minecraft.item.Item[] parts={Items.DIAMOND,Items.IRON_INGOT,Items.IRON_INGOT,Items.REDSTONE,Items.FLINT,Items.FLINT,Items.GOLD_INGOT,Items.GOLD_INGOT,Items.PAPER};
        Object[] required=new Object[parts.length];
        for(int i=0;i<parts.length;i++){required[i]=new ItemStack(parts[i]);matrix.setInventorySlotContents(i+2,new ItemStack(parts[i]));}
        galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.addShapelessRecipe(new ItemStack(Items.BOAT),required);
        check(!galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.findMatchingRecipe(matrix,null).isEmpty(),"Real GS matcher accepts complete nine-part rocket assembly");
        matrix.setInventorySlotContents(5,ItemStack.EMPTY);
        check(galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.findMatchingRecipe(matrix,null).isEmpty(),"Real GS matcher rejects rocket assembly without the engine");
        galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.getRecipeList().clear();
        InventoryBasic nineMatrix=new InventoryBasic("extendedRocket",false,15);
        Object[] nineRequired=Arrays.copyOf(required,10);
        nineRequired[9]=new ItemStack(Items.EMERALD);
        for(int i=0;i<parts.length;i++)nineMatrix.setInventorySlotContents(i+2,new ItemStack(parts[i]));
        nineMatrix.setInventorySlotContents(14,new ItemStack(Items.EMERALD));
        galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.addShapelessRecipe(new ItemStack(Items.MINECART),nineRequired);
        check(!galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.findMatchingRecipe(nineMatrix,null).isEmpty(),"Real GS matcher supports ten required inputs in an extended assembly matrix");
        nineMatrix.setInventorySlotContents(14,ItemStack.EMPTY);
        check(galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.findMatchingRecipe(nineMatrix,null).isEmpty(),"Extended assembly rejects missing central propulsion module");
        nineMatrix.setInventorySlotContents(14,new ItemStack(Items.EMERALD));nineMatrix.setInventorySlotContents(10,ItemStack.EMPTY);
        check(galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.findMatchingRecipe(nineMatrix,null).isEmpty(),"Extended assembly rejects missing control panel");
        galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.getRecipeList().clear();
        check(ManufacturingHooks.blueprint("com.mjr.extraplanets.planets.Uranus.WorldProviderUranus").equals("blueprint_engine_station"),"Uranus unlocks the engine station");
        check("blueprint_plasma".equals(ManufacturingHooks.blueprint("galaxyspace.systems.BarnardsSystem.moons.barnarda_c1.dimension.WorldProviderBarnarda_C1_WE")),"C1 actual provider grants plasma blueprint");
        check(ManufacturingHooks.blueprint("galaxyspace.systems.SolarSystem.moons.io.dimension.WorldProviderIo")==null,"Shared chest tier never grants EP blueprint to GS Io");
        JsonArray recipes=new JsonParser().parse(new InputStreamReader(ManufacturingProbe.class.getResourceAsStream("/assets/newearth/manufacturing.json"),"UTF-8")).getAsJsonArray();
        check(recipes.size()>=34,"Complete manufacturing recipe data");
        Set<String> machines=new HashSet<>();
        for(JsonElement e:recipes) {
            JsonObject r=e.getAsJsonObject();String machine=r.get("machine").getAsString();machines.add(machine);
            int slots=0;for(JsonElement i:r.getAsJsonArray("inputs"))slots+=i.getAsJsonObject().has("count")?i.getAsJsonObject().get("count").getAsInt():1;
            if(machine.equals("AssemblyRecipes"))check(slots<=9,"GS component recipe fits nine input slots");
            if(r.has("layout"))check(summary(r.getAsJsonArray("layout")).equals(summary(r.getAsJsonArray("inputs"))),"Shaped layout preserves ingredient quantities");
            if(machine.equals("RocketAssemblyRecipes"))check(slots==10,"T7 assembly requires structural propulsion, sublight module and licence");
        }
        for(JsonElement element:recipes) {
            JsonObject recipe=element.getAsJsonObject();
            if(!recipe.has("layout"))continue;
            JsonArray layout=recipe.getAsJsonArray("layout");
            net.minecraft.inventory.Container eventHandler=new net.minecraft.inventory.Container(){@Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player){return true;}};
            net.minecraft.inventory.InventoryCrafting grid=new net.minecraft.inventory.InventoryCrafting(eventHandler,3,3);
            Map<String,net.minecraft.item.Item> symbols=new HashMap<>();
            StringBuilder pattern=new StringBuilder();List<Object> args=new ArrayList<>();int first=-1;
            for(int i=0;i<9;i++) {
                JsonElement cell=layout.get(i);
                if(cell.isJsonNull()){pattern.append(' ');continue;}
                if(first<0)first=i;
                net.minecraft.item.Item item=symbols.get(cell.toString());
                if(item==null){item=new net.minecraft.item.Item();symbols.put(cell.toString(),item);}
                ItemStack stack=new ItemStack(item);grid.setInventorySlotContents(i,stack);
                char symbol=(char)('A'+i);pattern.append(symbol);args.add(symbol);args.add(stack.copy());
            }
            List<Object> shaped=new ArrayList<>();for(int row=0;row<3;row++)shaped.add(pattern.substring(row*3,row*3+3));shaped.addAll(args);
            galaxyspace.systems.SolarSystem.planets.overworld.recipes.AssemblyRecipes.getRecipeList().clear();
            galaxyspace.systems.SolarSystem.planets.overworld.recipes.AssemblyRecipes.addRecipe(new ItemStack(Items.BOAT),shaped.toArray());
            check(!galaxyspace.systems.SolarSystem.planets.overworld.recipes.AssemblyRecipes.findMatchingRecipe(grid,null).isEmpty(),"Real GS assembler accepts the shaped layout");
            grid.setInventorySlotContents(first,ItemStack.EMPTY);
            check(galaxyspace.systems.SolarSystem.planets.overworld.recipes.AssemblyRecipes.findMatchingRecipe(grid,null).isEmpty(),"Real GS assembler rejects missing shaped ingredient");
        }
        galaxyspace.systems.SolarSystem.planets.overworld.recipes.AssemblyRecipes.getRecipeList().clear();
        check(machines.size()==7,"Both stations and both GS assemblers are connected");
        JsonObject wafer=null,plasma=null;
        int smelting=0;
        Set<String> ownOutputs=new HashSet<>();
        for(JsonElement e:recipes) {
            JsonObject recipe=e.getAsJsonObject(),output=recipe.getAsJsonObject("output");
            if(output.has("item"))ownOutputs.add(output.get("item").getAsString());
            if(recipe.get("machine").getAsString().equals("circuit"))wafer=recipe;
            if(recipe.get("machine").getAsString().equals("smelting"))smelting++;
            if(output.has("field") && output.get("field").getAsString().endsWith("ROCKET_MODULES") && output.get("meta").getAsInt()==5)plasma=recipe;
        }
        JsonObject core=null,blackHole=null;
        for(JsonElement element:recipes) {
            JsonObject recipe=element.getAsJsonObject(),output=recipe.getAsJsonObject("output");
            if(output.has("item") && output.get("item").getAsString().equals("newearth:final_energy_core"))core=recipe;
            if(output.has("field") && output.get("field").getAsString().endsWith("ROCKET_MODULES") && output.get("meta").getAsInt()==7)blackHole=recipe;
        }
        check(core!=null && core.getAsJsonArray("inputs").size()==6,"Core requires all five colors and platinum");
        Set<Integer> colors=new HashSet<>();
        for(JsonElement element:core.getAsJsonArray("inputs")) {
            JsonObject input=element.getAsJsonObject();int meta=input.get("meta").getAsInt();
            if(meta<5)colors.add(meta);else check(meta==6 && input.get("count").getAsInt()==4,"Core consumes four platinum plates");
        }
        check(colors.equals(new HashSet<>(Arrays.asList(0,1,2,3,4))),"No diamond color can be substituted");
        check(blackHole!=null && blackHole.getAsJsonArray("inputs").get(1).getAsJsonObject().get("item").getAsString().equals("newearth:final_energy_core"),"Black-hole engine consumes final core");
        check(ownOutputs.containsAll(Arrays.asList("newearth:assembly_station","newearth:disassembly_station")),"Both workstations have survival recipes");
        check(smelting==2,"Both ordinary and glowing violet logs supply crystal fragments");
        check(wafer!=null && wafer.getAsJsonObject("output").get("meta").getAsInt()==0 && wafer.getAsJsonObject("output").get("count").getAsInt()==3,"Repurposed EP diamond wafer yields three");
        check(wafer.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString().equals("newearth:amethyst_ingot"),"Wafer requires exoplanet amethyst ingot");
        check(plasma!=null && plasma.getAsJsonArray("inputs").size()==2,"Plasma upgrade has exactly two ingredients");
        check(ownOutputs.containsAll(Arrays.asList("newearth:amethyst_ingot","newearth:amethyst_plate","newearth:amethyst_component","newearth:control_panel","newearth:tier9_engine")),"Exoplanet materials feed components and controls");
        ItemStack recycledWafer=new ItemStack(Items.BOAT,3);
        List<ItemStack> oldCircuit=Arrays.asList(new ItemStack(Items.DIAMOND),new ItemStack(Items.FLINT),new ItemStack(Items.FLINT),new ItemStack(Items.REDSTONE),new ItemStack(Items.PAPER));
        micdoodle8.mods.galacticraft.api.recipe.CircuitFabricatorRecipes.addRecipe(recycledWafer,new ArrayList<Object>(oldCircuit));
        check(Manufacturing.processingRecipe("circuit",recycledWafer,Arrays.asList(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.SLIME_BALL,2),new ItemStack(Items.REDSTONE),new ItemStack(Items.PAPER))),"Circuit processing route installed");
        List<net.minecraft.util.NonNullList<Object>> circuits=micdoodle8.mods.galacticraft.api.recipe.CircuitFabricatorRecipes.getRecipes();
        check(circuits.size()==1,"Repurposing removes old wafer production recipe");
        check(((ItemStack)circuits.get(0).get(1)).getItem()==Items.SLIME_BALL && ((ItemStack)circuits.get(0).get(2)).getItem()==Items.SLIME_BALL,"Two resin inputs occupy independent circuit slots");
        check(micdoodle8.mods.galacticraft.api.recipe.CircuitFabricatorRecipes.getOutput(0).getCount()==3,"Circuit output quantity preserved");
        micdoodle8.mods.galacticraft.api.recipe.CircuitFabricatorRecipes.removeRecipe(recycledWafer);
        verifyModels();
        String directory=System.getenv("NEWEARTH_RUNTIME_MODS");
        if(directory!=null) verifyInstalled(new File(directory),recipes);
        else System.out.println("Manufacturing installed binary checks skipped: NEWEARTH_RUNTIME_MODS unset");
        System.out.println("NewEarth manufacturing: "+checks+" checks passed (isolated; no world or flight)");
    }
    private static void verifyModels() throws Exception {
        net.minecraft.client.resources.IResourceManager manager=new net.minecraft.client.resources.IResourceManager() {
            @Override public Set<String> getResourceDomains(){return Collections.singleton("newearth");}
            @Override public net.minecraft.client.resources.IResource getResource(net.minecraft.util.ResourceLocation location) throws IOException {
                InputStream stream=ManufacturingProbe.class.getResourceAsStream("/assets/"+location.getNamespace()+"/"+location.getPath());
                if(stream==null)throw new FileNotFoundException(location.toString());
                return new net.minecraft.client.resources.SimpleResource("probe",location,stream,null,new net.minecraft.client.resources.data.MetadataSerializer());
            }
            @Override public List<net.minecraft.client.resources.IResource> getAllResources(net.minecraft.util.ResourceLocation location) throws IOException {
                return Collections.singletonList(getResource(location));
            }
        };
        for(String name:new String[]{"tier7_rocket","tier9_rocket","tier10_rocket","tier7_workbench"}) {
            net.minecraft.util.ResourceLocation location=new net.minecraft.util.ResourceLocation("newearth","models/"+name+".obj");
            net.minecraft.client.resources.IResource resource=manager.getResource(location);
            net.minecraftforge.client.model.IModel model;
            try { model=new net.minecraftforge.client.model.obj.OBJModel.Parser(resource,manager).parse(); }
            finally {resource.close();}
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite=new net.minecraft.client.renderer.texture.TextureAtlasSprite("probe"){};
            sprite.setIconWidth(16);sprite.setIconHeight(16);sprite.initSprite(16,16,0,0,false);
            net.minecraft.client.renderer.block.model.IBakedModel baked=model.bake(
                    new net.minecraftforge.client.model.obj.OBJModel.OBJState(com.google.common.collect.ImmutableList.of(net.minecraftforge.client.model.obj.OBJModel.Group.ALL),false),
                    net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM,texture -> sprite);
            check(!baked.getQuads(null,null,0).isEmpty(),"Actual Forge OBJ parser and baker produce a visible mesh: "+name);
        }
    }
    private static void registerPart(String id) {
        net.minecraft.util.ResourceLocation key=new net.minecraft.util.ResourceLocation("newearth",id);
        net.minecraft.item.Item item=net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.getValue(key);
        if(item==null || item==net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.AIR)) {
            item=new net.minecraft.item.Item().setRegistryName(key);
            net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(item);
        }
        Manufacturing.ITEMS.put(id,item);
    }
    private static Map<String,Integer> summary(JsonArray specs) {
        Map<String,Integer> counts=new HashMap<>();
        for(JsonElement element:specs)if(!element.isJsonNull()) {
            JsonObject s=element.getAsJsonObject();String key=(s.has("item")?s.get("item"):s.get("field")).getAsString()+":"+(s.has("meta")?s.get("meta").getAsInt():0);
            counts.put(key,counts.getOrDefault(key,0)+(s.has("count")?s.get("count").getAsInt():1));
        }
        return counts;
    }
    private static void verifyInstalled(File directory,JsonArray recipes) throws Exception {
        Set<String> required=new HashSet<>(Arrays.asList("micdoodle8.mods.galacticraft.core.entities.EntitySkeletonBoss","galaxyspace.core.prefab.items.rockets.ItemTier6Rocket","galaxyspace.core.prefab.entities.EntityTier6Rocket","galaxyspace.systems.SolarSystem.planets.overworld.inventory.SlotRocketAssembly","galaxyspace.systems.SolarSystem.planets.overworld.inventory.ContainerRocketAssembler","galaxyspace.systems.SolarSystem.moons.io.entities.EntityBossGhast"));
        Map<String,Set<String>> fields=new HashMap<>();int bosses=0;
        for(File file:directory.listFiles((dir,name)->name.endsWith(".jar"))) try(JarFile jar=new JarFile(file)) {
            for(Enumeration<JarEntry> entries=jar.entries();entries.hasMoreElements();) {
                JarEntry entry=entries.nextElement();String path=entry.getName();
                if(!path.endsWith(".class"))continue;
                String name=path.substring(0,path.length()-6).replace('/','.');
                if(!(required.contains(name)||name.startsWith("com.mjr.extraplanets.entities.bosses.")||name.endsWith("GSItems")||name.endsWith("ExtraPlanets_Items")||name.endsWith("ExtraPlanets_Blocks")))continue;
                byte[] bytes;try(InputStream in=jar.getInputStream(entry)) {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();}
                ClassNode original=new ClassNode();new ClassReader(bytes).accept(original,0);
                Set<String> names=new HashSet<>();for(FieldNode f:original.fields)names.add(f.name);fields.put(name,names);
                byte[] first=new PlanetTransformer().transform(name,name,bytes);
                byte[] patched=new ManufacturingTransformer().transform(name,name,first);
                if(patched==first)continue;
                ClassNode node=new ClassNode();new ClassReader(patched).accept(node,ClassReader.EXPAND_FRAMES);
                if(name.equals("galaxyspace.core.prefab.items.rockets.ItemTier6Rocket")) {
                    int rules=0;
                    for(MethodNode m:node.methods)if(m.name.equals("getAvailableModules"))for(AbstractInsnNode instruction:m.instructions.toArray()) {
                        if(instruction instanceof MethodInsnNode && ((MethodInsnNode)instruction).owner.equals("newearth/ManufacturingHooks") && ((MethodInsnNode)instruction).name.equals("modulesFor"))rules++;
                        check(!(instruction instanceof TypeInsnNode && instruction.getOpcode()==Opcodes.NEW),"No legacy unfiltered module array survives");
                    }
                    check(rules==1,"Installed T6/T7 item uses exactly one shared modification rule");
                }
                for(MethodNode m:node.methods)if((m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,m);
                check(true,"Installed transform verified: "+name);required.remove(name);if(name.startsWith("com.mjr"))bosses++;
            }
        }
        check(required.isEmpty(),"All required installed GS manufacturing targets found: "+required);
        check(bosses>=14,"Both default and EP-specific bosses covered: "+bosses);
        for(JsonElement e:recipes)for(JsonElement input:e.getAsJsonObject().getAsJsonArray("inputs")) {
            JsonObject spec=input.getAsJsonObject();if(!spec.has("field"))continue;
            String field=spec.get("field").getAsString();int dot=field.lastIndexOf('.');String owner=field.substring(0,dot);
            if(fields.containsKey(owner))check(fields.get(owner).contains(field.substring(dot+1)),"Installed material field: "+field);
        }
    }
}
