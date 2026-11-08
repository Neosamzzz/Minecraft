package newearth.worldgen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Rewrites the vanilla overworld biome provider. Method shapes are matched by pattern rather than by name or
 * descriptor so the patch does not depend on the runtime's obfuscation namespace, and every injected call uses
 * Object arguments for the same reason.
 */
public final class IslandTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "net.minecraft.world.biome.BiomeProvider";
    private static final String WORLD_PROVIDER = "net.minecraft.world.WorldProvider";
    private static final String VILLAGE = "net.minecraft.world.gen.structure.MapGenVillage";
    private static final String HOOKS = "newearth/worldgen/IslandHooks";
    private static final String OBJECT = "java/lang/Object";

    private static final Pattern WORLD_BINDING = Pattern.compile("^\\(L([^;]+);\\)V$");
    private static final Pattern GRID = Pattern.compile("^\\(\\[L([^;]+);IIII\\)\\[L\\1;$");
    private static final Pattern GRID_CACHED = Pattern.compile("^\\(\\[L([^;]+);IIIIZ\\)\\[L\\1;$");
    private static final Pattern LOOKUP = Pattern.compile("^\\(L([^;]+);L([^;]+);\\)L\\2;$");
    private static final Pattern LOOKUP_SINGLE = Pattern.compile("^\\(L([^;]+);\\)L([^;]+);$");

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return bytes;
        if (WORLD_PROVIDER.equals(transformedName)) return patchWorldProvider(bytes);
        if (VILLAGE.equals(transformedName)) return patchVillage(bytes);
        if (!TARGET.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);

        String biomeType = null;
        for (MethodNode method : node.methods) {
            Matcher lookup = LOOKUP.matcher(method.desc);
            if ((method.access & ACC_STATIC) == 0 && lookup.matches()) biomeType = lookup.group(2);
        }
        if (biomeType == null) {
            log("no biome lookup signature; island worldgen disabled for this version");
            return bytes;
        }

        int grids = 0, lookups = 0;
        for (MethodNode method : node.methods) {
            if ((method.access & ACC_STATIC) != 0) continue;
            if (GRID.matcher(method.desc).matches() || GRID_CACHED.matcher(method.desc).matches()) {
                patchGrid(method);
                grids++;
            } else if (LOOKUP.matcher(method.desc).matches() || LOOKUP_SINGLE.matcher(method.desc).matches()) {
                patchLookup(method, biomeType);
                lookups++;
            }
        }
        if (grids != 3 || lookups != 2) {
            log("unexpected layout (grid=" + grids + ", lookup=" + lookups + "); island worldgen disabled");
            return bytes;
        }
        log("biome provider patched");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    /** Blocks vanilla villages whose centre is not on solid island ground. */
    private static byte[] patchVillage(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            if ((method.access & ACC_STATIC) != 0 || !method.desc.equals("(II)Z")) continue;
            FieldInsnNode world = null;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn instanceof FieldInsnNode && insn.getOpcode() == GETFIELD
                        && ((FieldInsnNode) insn).desc.startsWith("L")) {
                    world = (FieldInsnNode) insn;
                    break;
                }
            }
            if (world == null) continue;
            LabelNode proceed = new LabelNode();
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(ALOAD, 0));
            hook.add(new FieldInsnNode(GETFIELD, world.owner, world.name, world.desc));
            hook.add(new VarInsnNode(ILOAD, 1));
            hook.add(new VarInsnNode(ILOAD, 2));
            hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "villageAllowed", "(Ljava/lang/Object;II)Z", false));
            hook.add(new JumpInsnNode(IFNE, proceed));
            hook.add(new InsnNode(ICONST_0));
            hook.add(new InsnNode(IRETURN));
            hook.add(proceed);
            hook.add(new FrameNode(F_SAME, 0, null, 0, null));
            method.instructions.insert(hook);
            patched++;
        }
        if (patched != 1) {
            log("unexpected village layout (" + patched + "); water filter disabled");
            return bytes;
        }
        log("village water filter installed");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
    private static byte[] patchWorldProvider(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            if ((method.access & (ACC_PUBLIC | ACC_FINAL | ACC_STATIC)) != (ACC_PUBLIC | ACC_FINAL)
                    || !WORLD_BINDING.matcher(method.desc).matches()) continue;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() != RETURN) continue;
                InsnList hook = new InsnList();
                hook.add(new VarInsnNode(ALOAD, 1));
                hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "registerWorld", "(Ljava/lang/Object;)V", false));
                method.instructions.insertBefore(insn, hook);
            }
            patched++;
        }
        if (patched != 1) {
            log("unexpected world provider layout (" + patched + "); island registration disabled");
            return bytes;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void patchGrid(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() != ARETURN) continue;
            InsnList hook = new InsnList();
            hook.add(new InsnNode(DUP));
            hook.add(new VarInsnNode(ALOAD, 0));
            hook.add(new InsnNode(SWAP));
            hook.add(new VarInsnNode(ILOAD, 2));
            hook.add(new VarInsnNode(ILOAD, 3));
            hook.add(new VarInsnNode(ILOAD, 4));
            hook.add(new VarInsnNode(ILOAD, 5));
            hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "apply", "(Ljava/lang/Object;Ljava/lang/Object;IIII)V", false));
            method.instructions.insertBefore(insn, hook);
        }
    }

    private static void patchLookup(MethodNode method, String biomeType) {
        LabelNode fallback = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(ALOAD, 0));
        hook.add(new VarInsnNode(ALOAD, 1));
        hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "biomeAt", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
        hook.add(new InsnNode(DUP));
        hook.add(new JumpInsnNode(IFNULL, fallback));
        hook.add(new TypeInsnNode(CHECKCAST, biomeType));
        hook.add(new InsnNode(ARETURN));
        hook.add(fallback);
        hook.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{OBJECT}));
        hook.add(new InsnNode(POP));
        method.instructions.insert(hook);
    }

    private static void log(String message) {
        System.out.println("[NewEarth] island worldgen: " + message);
    }
}
