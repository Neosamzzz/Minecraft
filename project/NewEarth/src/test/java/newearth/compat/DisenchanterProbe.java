package newearth.compat;

import java.lang.reflect.*;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.zip.ZipFile;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.*;
import newearth.DisenchanterTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Executes the installed mod in isolated loaders; no server, game instance or save is opened. */
public final class DisenchanterProbe {
    private static final String PREFIX="de.impelon.disenchanter.";
    private static int checks;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
    public static void main(String[] args)throws Exception {
        DisenchanterTransformer transformer=new DisenchanterTransformer();
        byte[] absent={1,2,3};
        check(transformer.transform("other","other",absent)==absent,"Unrelated classes are unchanged");
        check(transformer.transform(PREFIX+"DisenchantingUtils",PREFIX+"DisenchantingUtils",null)==null,"Absent optional mod is harmless");
        String directory=System.getenv("NEWEARTH_RUNTIME_MODS");
        if(directory==null){System.out.println("Disenchanter: "+checks+" optional-mod checks passed; set NEWEARTH_RUNTIME_MODS for installed binary regression");return;}
        Path target=null;byte[] original=null;
        try(java.util.stream.Stream<Path> files=Files.list(Paths.get(directory))) {
            for(Path file:(Iterable<Path>)files.filter(p->p.toString().endsWith(".jar"))::iterator)try(ZipFile jar=new ZipFile(file.toFile())) {
                java.util.zip.ZipEntry entry=jar.getEntry("de/impelon/disenchanter/DisenchantingUtils.class");if(entry==null)continue;
                check(target==null,"Only one Disenchanter is installed");target=file;
                try(java.io.InputStream input=jar.getInputStream(entry)){original=com.google.common.io.ByteStreams.toByteArray(input);}
            }
        }
        check(target!=null,"Installed Disenchanter was found");
        byte[] patched=transformer.transform(PREFIX+"DisenchantingUtils",PREFIX+"DisenchantingUtils",original);
        check(patched!=original,"Actual installed utility class is patched");
        check(transformer.transform(PREFIX+"DisenchantingUtils",PREFIX+"DisenchantingUtils",patched)==patched,"Guard is idempotent");
        ClassNode node=new ClassNode();new ClassReader(patched).accept(node,0);
        for(MethodNode method:node.methods)if((method.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0)
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name,method);
        check(true,"All patched methods pass bytecode verification");
        net.minecraft.launchwrapper.Launch.blackboard=new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment",true);Bootstrap.register();
        Map<String,String> names=new HashMap<>();
        for(String line:Files.readAllLines(Paths.get(System.getProperty("newearth.srgMap"))))if(line.startsWith("\t")) {
            String[] words=line.trim().split("\\s+");names.put(words[words.length-1],words[0]);
        }
        ItemStack[] before,after;
        try(ModLoader loader=new ModLoader(target,original,names)) {
            Class<?> handler=loader.loadClass(PREFIX+"inventory.IDisenchantmentItemHandler");
            Class<?> properties=loader.loadClass(PREFIX+"DisenchantingProperties");
            Method simulate=loader.loadClass(PREFIX+"DisenchantingUtils").getMethod("simulateDisenchantingInInventory",handler,boolean.class,int.class,properties,float.class,Random.class);
            try {simulate.invoke(null,null,true,0,null,0F,new Random(42));throw new AssertionError("Expected original crash");}
            catch(InvocationTargetException error){check(error.getCause() instanceof NullPointerException,"Original installed binary reproduces the reported null-properties crash");}
            before=normal(loader,false);after=normal(loader,true);
            check(before[0].getItem()==Items.ENCHANTED_BOOK&&before[0].hasTagCompound(),"Baseline normal table actually produces an enchanted book");
            check(after[0].isEmpty(),"Baseline persistent table with occupied output refuses another result");
        }
        try(ModLoader loader=new ModLoader(target,patched,names)) {
            Class<?> handler=loader.loadClass(PREFIX+"inventory.IDisenchantmentItemHandler");
            Class<?> properties=loader.loadClass(PREFIX+"DisenchantingProperties");
            Method simulate=loader.loadClass(PREFIX+"DisenchantingUtils").getMethod("simulateDisenchantingInInventory",handler,boolean.class,int.class,properties,float.class,Random.class);
            for(boolean preview:new boolean[]{false,true})check(simulate.invoke(null,null,preview,0,null,0F,new Random(42))==ItemStack.EMPTY,
                    "Missing table safely returns EMPTY before touching inventory, for preview="+preview);
            equal(before,normal(loader,false),"Normal disenchanting");equal(after,normal(loader,true),"Persistent table occupied output");
            final boolean[] armed={false};final int[] callbacks={0};
            ItemStackHandler inventory=new ItemStackHandler(3) {
                @Override protected void onContentsChanged(int slot) {
                    if(armed[0]&&slot!=2)try {
                        callbacks[0]++;ItemStack result=(ItemStack)simulate.invoke(null,null,true,0,null,0F,new Random(42));
                        setStackInSlot(2,result);
                    }catch(ReflectiveOperationException error){throw new AssertionError(error);}
                }
            };
            ItemStack sword=new ItemStack(Items.DIAMOND_SWORD);sword.addEnchantment(Enchantments.SHARPNESS,2);
            inventory.setStackInSlot(0,sword.copy());inventory.setStackInSlot(1,new ItemStack(Items.BOOK,4));
            inventory.setStackInSlot(2,new ItemStack(Items.ENCHANTED_BOOK));armed[0]=true;
            Object extracted=loader.loadClass(PREFIX+"inventory.InventoryUtils").getMethod("extractItemsFromInventory",IItemHandler.class,boolean.class).invoke(null,inventory,false);
            List<?> items=(List<?>)extracted;
            check(callbacks[0]==2,"Actual inventory-draining callback path completes instead of crashing");
            check(ItemStack.areItemStacksEqual(sword,(ItemStack)items.get(0)),"Enchanted source remains intact for the original player-refund routine");
            check(((ItemStack)items.get(1)).getItem()==Items.BOOK&&((ItemStack)items.get(1)).getCount()==4,"All original receiving books are returned");
            check(((ItemStack)items.get(2)).isEmpty()&&inventory.getStackInSlot(2).isEmpty(),"Stale virtual output is cleared rather than refunded as a duplicate enchantment");
            for(int slot=0;slot<3;slot++)check(inventory.getStackInSlot(slot).isEmpty(),"Close drains original slot "+slot+" exactly once");
        }
        System.out.println("Disenchanter: "+checks+" checks passed (actual installed JAR; isolated; no game or save modified)");
    }
    private static ItemStack[] normal(ModLoader loader,boolean persistent)throws Exception {
        Class<?> handler=loader.loadClass(PREFIX+"inventory.IDisenchantmentItemHandler");
        Object inventory=loader.loadClass(PREFIX+"inventory.DisenchantmentItemStackHandler").newInstance();
        ItemStack sword=new ItemStack(Items.DIAMOND_SWORD);sword.addEnchantment(Enchantments.SHARPNESS,2);
        handler.getMethod("setSourceStack",ItemStack.class).invoke(inventory,sword);
        handler.getMethod("setReceiverStack",ItemStack.class).invoke(inventory,new ItemStack(Items.BOOK,4));
        Collection<?> variants=Collections.emptyList();
        if(persistent) {
            Object automatic=loader.loadClass(PREFIX+"block.TableVariant").getField("AUTOMATIC").get(null);variants=Collections.singleton(automatic);
            handler.getMethod("setOutputStack",ItemStack.class).invoke(inventory,new ItemStack(Items.BOOK));
        }
        Class<?> properties=loader.loadClass(PREFIX+"DisenchantingProperties");Object props=properties.getConstructor(Collection.class).newInstance(variants);
        Method simulate=loader.loadClass(PREFIX+"DisenchantingUtils").getMethod("simulateDisenchantingInInventory",handler,boolean.class,int.class,properties,float.class,Random.class);
        ItemStack result=(ItemStack)simulate.invoke(null,inventory,true,0,props,0F,new Random(42));
        return new ItemStack[]{result.copy(),((ItemStack)handler.getMethod("getSourceStack").invoke(inventory)).copy(),
                ((ItemStack)handler.getMethod("getReceiverStack").invoke(inventory)).copy(),((ItemStack)handler.getMethod("getOutputStack").invoke(inventory)).copy()};
    }
    private static void equal(ItemStack[] expected,ItemStack[] actual,String label) {
        for(int i=0;i<expected.length;i++)check(ItemStack.areItemStacksEqual(expected[i],actual[i]),label+" preserves result/inventory "+i);
    }
    private static final class ModLoader extends URLClassLoader {
        final byte[] utility;final Map<String,String> names;
        ModLoader(Path jar,byte[] utility,Map<String,String> names)throws Exception {super(new URL[]{jar.toUri().toURL()},DisenchanterProbe.class.getClassLoader());this.utility=utility;this.names=names;}
        @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
            if(!name.startsWith(PREFIX))return super.loadClass(name,resolve);
            Class<?> type=findLoadedClass(name);
            if(type==null)try(java.io.InputStream input=findResource(name.replace('.','/')+".class").openStream()) {
                byte[] bytes=name.equals(PREFIX+"DisenchantingUtils")?utility:com.google.common.io.ByteStreams.toByteArray(input);
                ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);
                for(MethodNode method:node.methods) {
                    method.name=names.getOrDefault(method.name,method.name);
                    for(AbstractInsnNode instruction:method.instructions.toArray()) {
                        if(instruction instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)instruction;call.name=names.getOrDefault(call.name,call.name);}
                        if(instruction instanceof FieldInsnNode){FieldInsnNode field=(FieldInsnNode)instruction;field.name=names.getOrDefault(field.name,field.name);}
                    }
                }
                ClassWriter writer=new ClassWriter(0);node.accept(writer);bytes=writer.toByteArray();type=defineClass(name,bytes,0,bytes.length);
            }catch(java.io.IOException error){throw new ClassNotFoundException(name,error);}
            if(resolve)resolveClass(type);return type;
        }
    }
}
