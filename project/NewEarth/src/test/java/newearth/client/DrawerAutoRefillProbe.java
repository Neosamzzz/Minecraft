package newearth.client;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipFile;
import newearth.InventoryTweaksTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public final class DrawerAutoRefillProbe {
    private static int checks;
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        Object player = new Object(), world = new Object();
        DrawerAutoRefill.PendingDeposit pending = new DrawerAutoRefill.PendingDeposit();
        check(!pending.consume(27, "minecraft:stone", 0, true), "Ordinary depletion refills");
        pending.record(player, world, 0, "minecraft:stone", 0, 0);
        pending.observe(player, world, 0, "minecraft:stone", 0, 1_000_000L);
        check(!pending.consume(27, "minecraft:stone", 0, false), "Partial deposits do not suppress a refill");
        pending.observe(player, world, 0, null, 0, 2_000_000_000L);
        check(pending.consume(27, "minecraft:stone", 0, true), "Delayed server empty-slot update suppresses deposit refill");
        check(!pending.consume(27, "minecraft:stone", 0, true), "Suppression is consumed once");
        pending.record(player, world, 4, "minecraft:wool", 3, 0);
        check(!pending.consume(31, "minecraft:wool", 4, true), "Other metadata remains unaffected");
        check(!pending.consume(30, "minecraft:wool", 3, true), "Other slot remains unaffected");
        check(pending.consume(31, "minecraft:wool", 3, true), "Selected hotbar slot and metadata match");
        for (int scenario = 0; scenario < 7; scenario++) {
            pending.record(player, world, 0, "minecraft:stone", 0, 0);
            switch (scenario) {
                case 0: pending.clear(); break; // Other interaction or GUI.
                case 1: pending.observe(new Object(), world, 0, null, 0, 1); break;
                case 2: pending.observe(player, new Object(), 0, null, 0, 1); break;
                case 3: pending.observe(player, world, 1, null, 0, 1); break;
                case 4: pending.observe(player, world, 0, "minecraft:dirt", 0, 1); break;
                case 5: pending.observe(player, world, 0, "minecraft:stone", 1, 1); break;
                case 6: pending.observe(player, world, 0, null, 0, 5_000_000_001L); break;
            }
            check(!pending.consume(27, "minecraft:stone", 0, true), "Context reset " + scenario);
        }
        String mods = System.getenv("NEWEARTH_RUNTIME_MODS");
        if (mods == null) {
            System.out.println("Drawer auto-refill: " + checks + " state checks passed; set NEWEARTH_RUNTIME_MODS for installed binary checks");
            return;
        }
        byte[] input = null;
        Path inventoryJar = null;
        try (java.util.stream.Stream<Path> files = Files.list(Paths.get(mods))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".jar"))::iterator) {
                try (ZipFile jar = new ZipFile(file.toFile())) {
                    java.util.zip.ZipEntry entry = jar.getEntry("invtweaks/InvTweaksHandlerAutoRefill.class");
                    if (entry != null) {
                        check(input == null, "Only one Inventory Tweaks is installed");
                        inventoryJar = file;
                        try (java.io.InputStream stream = jar.getInputStream(entry)) {
                            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                            byte[] buffer = new byte[8192]; int count;
                            while ((count = stream.read(buffer)) != -1) out.write(buffer, 0, count);
                            input = out.toByteArray();
                        }
                    }
                }
            }
        }
        check(input != null, "Installed Inventory Tweaks class found");
        InventoryTweaksTransformer transformer = new InventoryTweaksTransformer();
        String refillTask = "invtweaks.InvTweaksHandlerAutoRefill$1";
        try (ZipFile jar = new ZipFile(inventoryJar.toFile());
             java.io.InputStream stream = jar.getInputStream(jar.getEntry(refillTask.replace('.', '/') + ".class"))) {
            byte[] originalTask = com.google.common.io.ByteStreams.toByteArray(stream);
            byte[] patchedTask = transformer.transform(refillTask, refillTask, originalTask);
            check(patchedTask != originalTask, "Installed delayed refill task receives the single-swap hook");
            check(transformer.transform(refillTask, refillTask, patchedTask) == patchedTask, "Delayed refill patch is idempotent");
            ClassNode taskNode = new ClassNode();
            new ClassReader(patchedTask).accept(taskNode, 0);
            for (MethodNode method : taskNode.methods) {
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(taskNode.name, method);
            }
            check(true, "Patched delayed refill passes JVM stack and local-variable checks");
        }
        check(transformer.transform("unrelated", "unrelated", input) == input, "Other classes unchanged");
        check(transformer.transform("invtweaks.InvTweaksHandlerAutoRefill", "invtweaks.InvTweaksHandlerAutoRefill", null) == null,
                "Missing optional mod is harmless");
        byte[] output = transformer.transform("invtweaks.InvTweaksHandlerAutoRefill", "invtweaks.InvTweaksHandlerAutoRefill", input);
        check(output != input, "Installed class is patched");
        check(transformer.transform("invtweaks.InvTweaksHandlerAutoRefill", "invtweaks.InvTweaksHandlerAutoRefill", output) == output,
                "Repeated transformation does not duplicate the guard");
        ClassNode node = new ClassNode(); new ClassReader(output).accept(node, ClassReader.EXPAND_FRAMES);
        int guards = 0;
        for (MethodNode method : node.methods) {
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0)
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(node.name, method);
            for (AbstractInsnNode instruction : method.instructions.toArray())
                if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("newearth/client/DrawerAutoRefill") && call.name.equals("suppress")) {
                        guards++;
                        check(method.name.equals("autoRefillSlot"), "Guard precedes refill scheduling only");
                    }
                }
        }
        check(guards == 1, "Exactly one suppression guard installed");
        // Load and execute the actual patched class with a true guard, without creating a game or inventory.
        ClassWriter hook = new ClassWriter(0);
        hook.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "newearth/client/DrawerAutoRefill", null, "java/lang/Object", null);
        MethodVisitor stub = hook.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "suppress", "(ILjava/lang/String;I)Z", null, null);
        stub.visitCode(); stub.visitInsn(Opcodes.ICONST_1); stub.visitInsn(Opcodes.IRETURN); stub.visitMaxs(1, 3); stub.visitEnd();
        hook.visitEnd();
        final byte[] guardClass = hook.toByteArray(), targetClass = output;
        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {inventoryJar.toUri().toURL()},
                DrawerAutoRefillProbe.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals("newearth.client.DrawerAutoRefill") || name.startsWith("invtweaks.")) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null) {
                        byte[] bytes = name.startsWith("newearth.") ? guardClass : targetClass;
                        if (name.startsWith("invtweaks.") && !name.equals("invtweaks.InvTweaksHandlerAutoRefill")) {
                            try (java.io.InputStream stream = findResource(name.replace('.', '/') + ".class").openStream()) {
                                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                                byte[] buffer = new byte[8192]; int count;
                                while ((count = stream.read(buffer)) != -1) out.write(buffer, 0, count);
                                bytes = out.toByteArray();
                            } catch (java.io.IOException error) { throw new ClassNotFoundException(name, error); }
                        }
                        type = defineClass(name, bytes, 0, bytes.length);
                    }
                    if (resolve) resolveClass(type);
                    return type;
                }
                return super.loadClass(name, resolve);
            }
        }) {
            Class<?> type = loader.loadClass("invtweaks.InvTweaksHandlerAutoRefill");
            java.lang.reflect.Field unsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            Object instance = ((sun.misc.Unsafe) unsafe.get(null)).allocateInstance(type);
            type.getMethod("autoRefillSlot", int.class, String.class, int.class).invoke(instance, 27, "minecraft:stone", 0);
            check(true, "Actual patched class verifies and returns before inventory access or deferred task creation");
        }
        System.out.println("Drawer auto-refill compatibility: " + checks + " checks passed (no game or save modified)");
    }
}
