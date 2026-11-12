package newearth.server;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Early return before tick counters, world updates, and Forge server tick events. */
public final class PauseTransformer implements IClassTransformer, Opcodes {
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.server.MinecraftServer".equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            if (!(method.name.equals("tick") || method.name.equals("func_71217_p")) || !method.desc.equals("()V")) continue;
            InsnList hook = new InsnList();
            LabelNode normal = new LabelNode();
            hook.add(new VarInsnNode(ALOAD, 0));
            hook.add(new MethodInsnNode(INVOKESTATIC, "newearth/server/PauseHooks", "pause", "(Lnet/minecraft/server/MinecraftServer;)Z", false));
            hook.add(new JumpInsnNode(IFEQ, normal));
            hook.add(new InsnNode(RETURN));
            hook.add(normal);
            hook.add(new FrameNode(F_SAME, 0, null, 0, null));
            method.instructions.insert(hook);
            changes++;
        }
        if (changes != 1) throw new IllegalStateException("Unsupported MinecraftServer tick entry points: " + changes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
