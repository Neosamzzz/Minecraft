package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Optional client compatibility; Inventory Tweaks is not a build or runtime dependency. */
public final class InventoryTweaksTransformer implements IClassTransformer, Opcodes {
    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"invtweaks.InvTweaksHandlerAutoRefill".equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES);
        for (MethodNode method : node.methods) {
            if (!"autoRefillSlot".equals(method.name) || !"(ILjava/lang/String;I)V".equals(method.desc)
                    || (method.access & ACC_STATIC) != 0) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray())
                if (instruction instanceof MethodInsnNode
                        && ((MethodInsnNode) instruction).owner.equals("newearth/client/DrawerAutoRefill")) return bytes;
            // Do not queue the refill: InvTweaks will still refresh its selected-stack snapshot normally.
            InsnList guard = new InsnList();
            LabelNode proceed = new LabelNode();
            guard.add(new VarInsnNode(ILOAD, 1));
            guard.add(new VarInsnNode(ALOAD, 2));
            guard.add(new VarInsnNode(ILOAD, 3));
            guard.add(new MethodInsnNode(INVOKESTATIC, "newearth/client/DrawerAutoRefill", "suppress",
                    "(ILjava/lang/String;I)Z", false));
            guard.add(new JumpInsnNode(IFEQ, proceed));
            guard.add(new InsnNode(RETURN));
            guard.add(proceed);
            guard.add(new FrameNode(F_NEW, 4, new Object[] {node.name, INTEGER, "java/lang/String", INTEGER}, 0, new Object[0]));
            method.instructions.insert(guard);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            System.out.println("[NewEarth] Inventory Tweaks drawer auto-refill guard installed");
            return writer.toByteArray();
        }
        System.err.println("[NewEarth] Unsupported Inventory Tweaks auto-refill signature; drawer guard not installed");
        return bytes;
    }
}
