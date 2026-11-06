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
    private static int checks;
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);checks++;}
    static void run() throws Exception {
        try (Reader reader=new InputStreamReader(ManufacturingProbe.class.getResourceAsStream(
                "/assets/newearth/models/item/engine_station.json"), java.nio.charset.StandardCharsets.UTF_8)) {
            net.minecraft.client.renderer.block.model.ModelBlock model=
                    net.minecraft.client.renderer.block.model.ModelBlock.deserialize(reader);
            check(model.getAllTransforms().getTransform(net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.GUI).scale.x>0,
                    "Actual Minecraft parser accepts the engine station inventory model");
            check(model.getAllTransforms().getTransform(net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.GROUND).scale.x>0,
                    "Engine station dropped-item transform remains visible");
        }
        net.minecraft.item.Item six=new net.minecraft.item.Item().setRegistryName("galaxyspace:rocket_tier_6");
        net.minecraft.item.Item seven=new net.minecraft.item.Item().setRegistryName("newearth:tier7_rocket");
        check(ManufacturingHooks.modulesFor(six)[0] instanceof Tier6IonModule,"T6 modification table permits ion only");
        check(ManufacturingHooks.modulesFor(seven).length==0,"T7 has no engine modifications");
        for (net.minecraft.item.Item rocketItem : Arrays.asList(six, seven)) {
            ItemStack standard = new ItemStack(rocketItem);
            ItemStack preinstalled = standard.copy();
            net.minecraft.nbt.NBTTagCompound engine = new net.minecraft.nbt.NBTTagCompound();
            engine.setBoolean(asmodeuscore.core.astronomy.SpaceData.Engine_Type.SUBLIGHT_ENGINE.getName(), true);
            preinstalled.setTagCompound(engine);
            List<ItemStack> variants = new ArrayList<>(Arrays.asList(standard, preinstalled));
            ManufacturingHooks.filterRocketVariants(rocketItem, variants);
            check(variants.size()==1 && variants.get(0)==standard,"Creative and JEI preserve the standard rocket and drop the preset");
        }
        InventoryBasic inv=new InventoryBasic("test",false,27);
        inv.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,2));
        inv.setInventorySlotContents(1,new ItemStack(Items.IRON_INGOT,3));
        Manufacturing.Batch loose=new Manufacturing.Batch("test",new ItemStack(Items.GOLD_INGOT,2),Arrays.asList(new ItemStack(Items.IRON_INGOT,4)));
        int[] debit=loose.debit(inv,18);
        check(debit!=null&&debit[0]==2&&debit[1]==2,"Split stacks fund a whole batch");
        check(inv.getStackInSlot(0).getCount()==2,"Matching never consumes inventory");
        inv.setInventorySlotContents(1,new ItemStack(Items.IRON_INGOT,1));
        check(loose.debit(inv,18)==null,"Insufficient inputs fail atomically");
        inv.setInventorySlotContents(18,new ItemStack(Items.IRON_INGOT,64));
        check(loose.debit(inv,18)==null,"Output slots cannot fund inputs");
        inv.setInventorySlotContents(1,new ItemStack(Items.IRON_INGOT,3));
        inv.setInventorySlotContents(2,new ItemStack(Items.DIAMOND));
        check(loose.debit(inv,18)==null,"Unexpected ingredient rejects the batch");
        Manufacturing.Batch batch=new Manufacturing.Batch("material_station",new ItemStack(Items.GOLD_INGOT,2),Arrays.asList(new ItemStack(Items.IRON_INGOT,4)));
        InventoryBasic cells=new InventoryBasic("cells",false,9);
        cells.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,4));
        int[] placed=batch.debit(cells,9);
        check(placed!=null&&placed[0]==4,"Material recipe takes its stack from the shown cell");
        cells.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,2));
        cells.setInventorySlotContents(1,new ItemStack(Items.IRON_INGOT,2));
        check(batch.debit(cells,9)==null,"Material recipe does not gather one ingredient from another cell");
        Manufacturing.Batch repeated=new Manufacturing.Batch("test",new ItemStack(Items.PAPER),Arrays.asList(new ItemStack(Items.IRON_INGOT,3),new ItemStack(Items.IRON_INGOT,3)));
        inv.setInventorySlotContents(2,ItemStack.EMPTY);
        check(repeated.debit(inv,18)==null,"Repeated ingredient cannot double spend a stack");
        Manufacturing.Batch upgrade=new Manufacturing.Batch("engine_station",new ItemStack(Items.EMERALD),Arrays.asList(new ItemStack(Items.IRON_INGOT),new ItemStack(Items.PAPER)));
        InventoryBasic engineSlots=new InventoryBasic("engine",false,2);
        engineSlots.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT));
        engineSlots.setInventorySlotContents(1,new ItemStack(Items.PAPER));
        int[] order=upgrade.debit(engineSlots,2);
        check(order!=null&&order[0]==1&&order[1]==1,"Engine station takes the engine from the upper slot");
        engineSlots.setInventorySlotContents(0,new ItemStack(Items.PAPER));
        engineSlots.setInventorySlotContents(1,new ItemStack(Items.IRON_INGOT));
        check(upgrade.debit(engineSlots,2)==null,"Engine station rejects the two inputs swapped");
        class GcCell extends net.minecraft.item.Item implements micdoodle8.mods.galacticraft.api.item.IItemElectric {
            public float recharge(ItemStack s, float e, boolean d) { return 0; }
            public float discharge(ItemStack s, float e, boolean d) { return 0; }
            public float getElectricityStored(ItemStack s) { return 0; }
            public float getMaxElectricityStored(ItemStack s) { return 0; }
            public void setElectricity(ItemStack s, float j) { }
            public float getTransfer(ItemStack s) { return 0; }
            public int getTierGC(ItemStack s) { return 1; }
        }
        check(ManufacturingTile.battery(new ItemStack(new GcCell())),"Material station accepts a GC electric item");
        check(ManufacturingTile.forgeEnergyFromJoules(0F)==0,"A GC battery is not drained when it converts to no FE");
        check(ManufacturingTile.forgeEnergyFromJoules(micdoodle8.mods.galacticraft.core.energy.EnergyConfigHandler.RF_RATIO)==1,"One RF of Galacticraft energy becomes one FE");
        check(!ManufacturingTile.battery(new ItemStack(Items.STICK)),"Plain items are not batteries");
        check(ManufacturingTile.powerSide(net.minecraft.util.EnumFacing.SOUTH) == net.minecraft.util.EnumFacing.WEST, "South front takes power from the player's left");
        check(ManufacturingTile.powerSide(net.minecraft.util.EnumFacing.NORTH) == net.minecraft.util.EnumFacing.EAST, "North front takes power from the player's left");
        net.minecraft.tileentity.TileEntity.register("newearth:manufacturing", ManufacturingTile.class);
        ManufacturingTile tile=new ManufacturingTile();
        ManufacturingContainer powerGui=new ManufacturingContainer(new net.minecraft.entity.player.InventoryPlayer(null),tile);
        for(int charge:new int[]{32767,32768,50000}) {
            powerGui.updateProgressBar(0,(short)charge);
            check(tile.getField(0)==charge,"GUI energy sync preserves unsigned 16-bit charge "+charge);
        }
        Manufacturing.BATCHES.add(batch);
        ManufacturingTile crafting=new ManufacturingTile();
        crafting.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,8));
        crafting.setField(0,8000);
        for(int tick=0;tick<50;tick++)crafting.process("material_station");
        ManufacturingTile resumed=new ManufacturingTile();
        resumed.readFromNBT(crafting.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        check(resumed.getField(1)==50 && resumed.getField(0)==6000,"Processing progress and stored energy survive reload");
        for(int tick=0;tick<149;tick++)resumed.process("material_station");
        check(resumed.getStackInSlot(ManufacturingTile.OUTPUT_SLOT).isEmpty(),"Saved progress does not complete early");
        resumed.process("material_station");
        check(resumed.getStackInSlot(0).getCount()==4 && resumed.getStackInSlot(ManufacturingTile.OUTPUT_SLOT).getCount()==2,"Saved progress completes on the remaining tick");
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
        net.minecraft.item.Item modules=new net.minecraft.item.Item().setRegistryName("galaxyspace:rocket_modules").setHasSubtypes(true);
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
            if(tier==7)check(!rocket.hasTagCompound(),"T7 assembly yields a rocket without engine state");
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
        ItemStack sevenReady=new ItemStack(Manufacturing.ITEMS.get("tier7_rocket"));
        check(PlanetHooks.readyErisStack(7,sevenReady.getItem(),sevenReady),"T7 is eligible for Eris without module NBT");
        for(int tier:new int[]{9,10}) {
            ItemStack rocket=AssemblyStationTile.output(tier);
            check(PlanetHooks.readyErisStack(tier,rocket.getItem(),rocket),"T"+tier+" recovered ship remains eligible for Eris");
        }
        check(ItemTier9Rocket.completePad("galacticraftcore:landing_pad_full",0),"Complete GC launch pad accepted");
        check(ItemTier9Rocket.completePad("galaxyspace:advanced_landing_pad_full",0),"Complete GS launch pad accepted");
        check(!ItemTier9Rocket.completePad("galacticraftcore:landing_pad",0),"Individual pad pieces rejected");
        check(!ItemTier9Rocket.completePad("galacticraftcore:landing_pad_full",1),"Buggy pad rejected");
        micdoodle8.mods.galacticraft.core.util.ConfigManagerCore.rocketFuelFactor=1;
        EntityTier7Rocket sevenHull=new EntityTier7Rocket(null);
        check(sevenHull.getRocketTier()==7,"Unmodified T7 retains its flight tier");
        double sevenHeight=10.3157D*EntityTier7Rocket.HULL_SCALE_Y;
        double sevenWidth=5.9794D*EntityTier7Rocket.HULL_SCALE_XZ;
        check(sevenHeight>12.1862D && sevenHeight<17.6751D,"T7 visible height lies between GS T6 and EP new T9");
        check(sevenWidth>3.9900D && sevenWidth<6.3192D,"T7 visible width lies between GS T6 and EP new T9");
        check(sevenHull.height>=sevenHeight && sevenHull.getOnPadYOffset()==0D,"T7 physical height encloses the hull and preserves pad detection");
        check(Math.abs(-0.0153D*EntityTier7Rocket.HULL_SCALE_Y-sevenHull.getRenderOffsetY())<0.001D,"EP T7 nozzle base aligns with the entity origin");

        EntityTier9Rocket ship=new EntityTier9Rocket(null);
        check(ship.getMaxSeats()==6 && ship.getRocketTier()==9 && ship.fuelTank.getCapacity()==1500,"T9 concrete entity has six seats and a usable fuel tank");
        check(ship.getRotateOffset()==4.0F && ship.getOnPadYOffset()==0D && ship.getRenderOffsetY()==1.1F,"T9 keeps the EP camera pivot and model offset with a dockable physical origin");
        check(ItemTier9Rocket.placedY(ship,64D)==64D+0.4D,"T9 physical origin rests on the 0.4 pad base");
        check(EntityTier9Rocket.class.getDeclaredMethod("spawnParticles",boolean.class).getDeclaringClass()==EntityTier9Rocket.class,"T9 uses the EP four-nozzle flame");
        net.minecraft.util.NonNullList<ItemStack> creative=net.minecraft.util.NonNullList.create();
        ItemTier9Rocket tier9Item=new ItemTier9Rocket();
        tier9Item.getSubItems(tier9Item.getCreativeTab(),creative);
        check(creative.size()==micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType.values().length,"T9 lists every GC rocket variant, saw "+creative.size());
        creative.clear();
        ItemTier10Rocket tier10Item=new ItemTier10Rocket();
        tier10Item.getSubItems(net.minecraft.creativetab.CreativeTabs.SEARCH,creative);
        check(creative.size()==micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType.values().length,"T10 lists every GC rocket variant, saw "+creative.size());
        ship.setRocketType(micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType.INVENTORY54.getIndex());
        check(ship.getSizeInventory()==micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType.INVENTORY54.getInventorySpace(),"Cargo variant uses the GC inventory size");
        ship.setRocketType(micdoodle8.mods.galacticraft.api.entity.IRocketType.EnumRocketType.PREFUELED.getIndex());
        check(ship.getType().getPreFueled() && tier9Item instanceof micdoodle8.mods.galacticraft.api.item.IHoldableItem && tier10Item instanceof micdoodle8.mods.galacticraft.api.item.IHoldableItem,"Creative variant is pre-fueled and both items use the GC hold pose");
        check(new Tier6IonModule().getForrbidenModules()==null,"Tier 6 modification list hides the sublight conflict icon");
        ship.setRocketType(0);
        check(ship.isDockValid(new micdoodle8.mods.galacticraft.core.tile.TileEntityLandingPad()),"T9 docks on a real GC launch pad");
        check(ship.isDockValid(new galaxyspace.systems.SolarSystem.planets.overworld.tile.TileEntityAdvLandingPad()),"T9 docks on a real GS advanced pad");
        EntityTier10Rocket finalShip=new EntityTier10Rocket(null);
        check(finalShip.getRocketTier()==10 && finalShip.recoveredItem().getItem()==Manufacturing.ITEMS.get("tier10_rocket"),"T10 recovers its own item");
        check(finalShip.getRotateOffset()==5.8F && finalShip.getOnPadYOffset()==0D && finalShip.getRenderOffsetY()==1.1F,"T10 keeps the EP camera pivot and model offset with a dockable physical origin");
        check(ItemTier9Rocket.placedY(finalShip,64D)==64D+0.4D,"T10 physical origin rests on the 0.4 pad base");
        net.minecraft.util.math.AxisAlignedBB padScan = new net.minecraft.util.math.AxisAlignedBB(-0.5D,64D,-0.5D,0.5D,65D,0.5D);
        for (EntityTier9Rocket parked : new EntityTier9Rocket[]{ship,finalShip}) {
            parked.setPosition(0.5D,ItemTier9Rocket.placedY(parked,64D),0.5D);
            check(parked.getEntityBoundingBox().intersects(padScan),"T"+parked.getRocketTier()+" is rediscoverable by the real GC/GS pad scan");
            parked.setLaunchPhase(micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase.EnumLaunchPhase.UNIGNITED);
            check(!parked.inFlight(),"Parked ship holds the pad");
            parked.setLaunchPhase(micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase.EnumLaunchPhase.LAUNCHED);
            check(parked.inFlight(),"Launched ship releases the pad even while crossing its scan");
            parked.setLaunchPhase(micdoodle8.mods.galacticraft.api.prefab.entity.EntitySpaceshipBase.EnumLaunchPhase.UNIGNITED);
            check(parked.getMountedYOffset(parked)==parked.getHullYOffset(),"Pilot seat follows the visible hull elevation");
        }
        boolean ownFlame=true;try{EntityTier10Rocket.class.getDeclaredMethod("spawnParticles",boolean.class);}catch(NoSuchMethodException ignored){ownFlame=false;}
        check(!ownFlame,"T10 keeps the shared EP flame");
        for(int meta=0;meta<4;meta++) {
            ship.setRocketType(meta);
            check(ship.recoveredItem().getMetadata()==meta,"Recovery preserves cargo variant "+meta);
        }
        check(ManufacturingHooks.blueprint("com.mjr.extraplanets.planets.Uranus.WorldProviderUranus").equals("blueprint_engine_station"),"Uranus unlocks the engine station");
        check("blueprint_plasma".equals(ManufacturingHooks.blueprint("galaxyspace.systems.BarnardsSystem.moons.barnarda_c1.dimension.WorldProviderBarnarda_C1_WE")),"C1 actual provider grants plasma blueprint");
        check(ManufacturingHooks.blueprint("galaxyspace.systems.SolarSystem.moons.io.dimension.WorldProviderIo")==null,"Shared chest tier never grants EP blueprint to GS Io");
        String[] licensePlanets={"Mercury","Jupiter","Saturn"};
        for(int i=0;i<licensePlanets.length;i++) {
            String provider="com.mjr.extraplanets.planets."+licensePlanets[i]+".WorldProvider"+licensePlanets[i];
            check(ManufacturingHooks.licenseMeta(provider)==8+i,"EP dungeon grants its matching GS licence");
            check(ManufacturingHooks.blueprint(provider)==null,"Licence dungeon does not request a NewEarth blueprint");
        }
        check(ManufacturingHooks.licenseMeta("galaxyspace.systems.SolarSystem.planets.mercury.dimension.WorldProviderMercury")<0,"GS Mercury does not share the EP licence reward");
        check(ManufacturingHooks.licenseMeta("com.mjr.extraplanets.planets.Pluto.WorldProviderPluto")<0,"Later dungeons keep their blueprint rewards");
        for(String part:Manufacturing.PARTS)check(!part.matches("blueprint_t[456]"),"Retired paper blueprints are not registered");
        JsonArray recipes=new JsonParser().parse(new InputStreamReader(ManufacturingProbe.class.getResourceAsStream("/assets/newearth/manufacturing.json"),"UTF-8")).getAsJsonArray();
        check(!recipes.toString().matches("(?s).*newearth:blueprint_t[456].*"),"Manufacturing data has no retired paper blueprint conversions");
        Set<String> machines=new HashSet<>();
        for(JsonElement e:recipes) {
            JsonObject r=e.getAsJsonObject();String machine=r.get("machine").getAsString();machines.add(machine);
            int slots=0;for(JsonElement i:r.getAsJsonArray("inputs"))slots+=i.getAsJsonObject().has("count")?i.getAsJsonObject().get("count").getAsInt():1;
            if(machine.equals("AssemblyRecipes"))check(slots<=9,"GS component recipe fits nine input slots");
            if(r.has("layout"))check(summary(r.getAsJsonArray("layout")).equals(summary(r.getAsJsonArray("inputs"))),"Shaped layout preserves ingredient quantities");
            check(!machine.equals("RocketAssemblyRecipes"),"NewEarth rockets are assembled only at the shared station");
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
        check(machines.equals(new HashSet<>(Arrays.asList("AssemblyRecipes", "circuit", "compressor", "engine_station", "material_station", "smelting"))),
                "Manufacturing data contains only the supported part and material machines");
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
                if(stream==null && location.getNamespace().equals("extraplanets"))
                    stream=new FileInputStream(new File(System.getProperty("newearth.epResources"),"assets/"+location.getNamespace()+"/"+location.getPath()));
                if(stream==null)throw new FileNotFoundException(location.toString());
                return new net.minecraft.client.resources.SimpleResource("probe",location,stream,null,new net.minecraft.client.resources.data.MetadataSerializer());
            }
            @Override public List<net.minecraft.client.resources.IResource> getAllResources(net.minecraft.util.ResourceLocation location) throws IOException {
                return Collections.singletonList(getResource(location));
            }
        };
        for(String name:new String[]{"tier7_rocket","tier9_rocket","tier10_rocket","tier7_workbench"}) {
            net.minecraft.util.ResourceLocation location=name.equals("tier7_rocket")
                    ? new net.minecraft.util.ResourceLocation("extraplanets","models/rocket_t7.obj")
                    : new net.minecraft.util.ResourceLocation("newearth","models/"+name+".obj");
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
        Set<String> required=new HashSet<>(Arrays.asList("micdoodle8.mods.galacticraft.core.entities.EntitySkeletonBoss","galaxyspace.core.prefab.items.rockets.ItemTier6Rocket","galaxyspace.core.prefab.entities.EntityTier6Rocket","galaxyspace.systems.SolarSystem.moons.io.entities.EntityBossGhast"));
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
