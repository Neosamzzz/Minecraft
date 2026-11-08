package newearth.worldgen;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Rewrites the vanilla overworld biome provider. Members are matched by descriptor so the patch works
 * with both MCP and SRG runtime names.
 */
public final class IslandTransformer implements IClassTransformer, Opcodes {
    private static final String TARGET = "net.minecraft.world.biome.BiomeProvider";
    private static final String HOOKS = "newearth/worldgen/IslandHooks";
    private static final String BIOME = "net/minecraft/world/biome/Biome";
    private static final String BIOME_DESC = "L" + BIOME + ";";
    private static final String GRID_DESC = "([" + BIOME_DESC + "IIII)[" + BIOME_DESC;
    private static final String GRID_CACHED_DESC = "([" + BIOME_DESC + "IIIIZ)[" + BIOME_DESC;
    private static final String POS_DESC = "Lnet/minecraft/util/math/BlockPos;";
    private static final String GET_BIOME_DESC = "(" + POS_DESC + ")" + BIOME_DESC;
    private static final String GET_BIOME_FALLBACK_DESC = "(" + POS_DESC + BIOME_DESC + ")" + BIOME_DESC;

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int patched = 0;
        for (MethodNode method : node.methods) {
            if (method.name.equals("<init>") && method.desc.equals("(Lnet/minecraft/world/storage/WorldInfo;)V")) {
                patchConstructor(method);
                patched++;
            } else if ((method.desc.equals(GRID_DESC) || method.desc.equals(GRID_CACHED_DESC))
                    && (method.access & ACC_STATIC) == 0) {
                patchGrid(method);
                patched++;
            } else if ((GET_BIOME_DESC.equals(method.desc) || GET_BIOME_FALLBACK_DESC.equals(method.desc)) && (method.access & ACC_STATIC) == 0) {
                patchPositionLookup(method);
                patched++;
            }
        }
        if (patched != 6) throw new IllegalStateException("Unsupported BiomeProvider layout, patched " + patched);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void patchConstructor(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() != RETURN) continue;
            InsnList hook = new InsnList();
            hook.add(new VarInsnNode(ALOAD, 0));
            hook.add(new VarInsnNode(ALOAD, 1));
            hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "register",
                    "(Ljava/lang/Object;Lnet/minecraft/world/storage/WorldInfo;)V", false));
            method.instructions.insertBefore(insn, hook);
        }
    }

    private static void patchGrid(MethodNode method) {
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn.getOpcode() != ARETURN) continue;
            InsnList hook = new InsnList();
            hook.add(new InsnNode(DUP));
            hook.add(new VarInsnNode(ILOAD, 2));
            hook.add(new VarInsnNode(ILOAD, 3));
            hook.add(new VarInsnNode(ILOAD, 4));
            hook.add(new VarInsnNode(ILOAD, 5));
            hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "apply", "([" + BIOME_DESC + "IIII)V", false));
            method.instructions.insertBefore(insn, hook);
        }
    }

    private static void patchPositionLookup(MethodNode method) {
        LabelNode fallback = new LabelNode();
        InsnList hook = new InsnList();
        hook.add(new VarInsnNode(ALOAD, 1));
        hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "biomeAt", "(" + POS_DESC + ")" + BIOME_DESC, false));
        hook.add(new InsnNode(DUP));
        hook.add(new JumpInsnNode(IFNULL, fallback));
        hook.add(new InsnNode(ARETURN));
        hook.add(fallback);
        hook.add(new FrameNode(F_SAME1, 0, null, 1, new Object[]{BIOME}));
        hook.add(new InsnNode(POP));
        method.instructions.insert(hook);
    }
}
