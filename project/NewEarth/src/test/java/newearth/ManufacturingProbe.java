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
        net.minecraft.item.Item six=new net.minecraft.item.Item().setRegistryName("galaxyspace:rocket_tier_6");
        net.minecraft.item.Item seven=new net.minecraft.item.Item().setRegistryName("newearth:tier7_rocket");
        check(ManufacturingHooks.moduleType(six).equals("IonEngine"),"T6 modification table permits ion only");
        check(ManufacturingHooks.moduleType(seven).equals("SublightEngine"),"T7 modification table permits sublight only");
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
        ManufacturingTile tile=new ManufacturingTile();
        Manufacturing.BATCHES.add(batch);
        tile.setInventorySlotContents(0,new ItemStack(Items.IRON_INGOT,8));
        for(int i=18;i<27;i++)tile.setInventorySlotContents(i,new ItemStack(Items.GOLD_INGOT,64));
        check(!tile.process("material_station")&&tile.getStackInSlot(0).getCount()==8,"Full output does not consume inputs");
        tile.setInventorySlotContents(18,ItemStack.EMPTY);
        check(tile.process("material_station")&&tile.getStackInSlot(0).getCount()==4&&tile.getStackInSlot(18).getCount()==2,"Successful batch consumes once and produces exact quantity");
        check(!tile.process("engine_station"),"Wrong station cannot run a material batch");
        check(!tile.canInsertItem(18,new ItemStack(Items.IRON_INGOT),net.minecraft.util.EnumFacing.UP),"Automation cannot insert into output");
        Manufacturing.BATCHES.clear();
        InventoryBasic matrix=new InventoryBasic("rocket",false,14);
        net.minecraft.item.Item[] parts={Items.DIAMOND,Items.IRON_INGOT,Items.IRON_INGOT,Items.REDSTONE,Items.FLINT,Items.FLINT,Items.GOLD_INGOT,Items.GOLD_INGOT,Items.PAPER};
        Object[] required=new Object[parts.length];
        for(int i=0;i<parts.length;i++){required[i]=new ItemStack(parts[i]);matrix.setInventorySlotContents(i+2,new ItemStack(parts[i]));}
        galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.addShapelessRecipe(new ItemStack(Items.BOAT),required);
        check(!galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.findMatchingRecipe(matrix,null).isEmpty(),"Real GS matcher accepts complete nine-part rocket assembly");
        matrix.setInventorySlotContents(5,ItemStack.EMPTY);
        check(galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.findMatchingRecipe(matrix,null).isEmpty(),"Real GS matcher rejects rocket assembly without the engine");
        galaxyspace.systems.SolarSystem.planets.overworld.recipes.RocketAssemblyRecipes.getRecipeList().clear();
        check(ManufacturingHooks.blueprint("com.mjr.extraplanets.planets.Uranus.WorldProviderUranus").equals("blueprint_engine_station"),"Uranus unlocks the engine station");
        check(ManufacturingHooks.blueprint("galaxyspace.systems.SolarSystem.moons.io.dimension.WorldProviderIo")==null,"Shared chest tier never grants EP blueprint to GS Io");
        JsonArray recipes=new JsonParser().parse(new InputStreamReader(ManufacturingProbe.class.getResourceAsStream("/assets/newearth/manufacturing.json"),"UTF-8")).getAsJsonArray();
        check(recipes.size()==16,"Complete manufacturing recipe data");
        Set<String> machines=new HashSet<>();
        for(JsonElement e:recipes) {
            JsonObject r=e.getAsJsonObject();String machine=r.get("machine").getAsString();machines.add(machine);
            int slots=0;for(JsonElement i:r.getAsJsonArray("inputs"))slots+=i.getAsJsonObject().has("count")?i.getAsJsonObject().get("count").getAsInt():1;
            if(machine.equals("AssemblyRecipes"))check(slots<=9,"GS component recipe fits nine input slots");
            if(r.has("layout"))check(summary(r.getAsJsonArray("layout")).equals(summary(r.getAsJsonArray("inputs"))),"Shaped layout preserves ingredient quantities");
            if(machine.equals("RocketAssemblyRecipes"))check(slots==9,"T7 assembly requires a structural engine as well as its licence");
        }
        check(machines.size()==4,"Both stations and both GS assemblers are connected");
        String directory=System.getenv("NEWEARTH_RUNTIME_MODS");
        if(directory!=null) verifyInstalled(new File(directory),recipes);
        else System.out.println("Manufacturing installed binary checks skipped: NEWEARTH_RUNTIME_MODS unset");
        System.out.println("NewEarth manufacturing: "+checks+" checks passed (isolated; no world or flight)");
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
        Set<String> required=new HashSet<>(Arrays.asList("galaxyspace.core.prefab.items.rockets.ItemTier6Rocket","galaxyspace.core.prefab.entities.EntityTier6Rocket","galaxyspace.systems.SolarSystem.planets.overworld.inventory.SlotRocketAssembly","galaxyspace.systems.SolarSystem.moons.io.entities.EntityBossGhast"));
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
