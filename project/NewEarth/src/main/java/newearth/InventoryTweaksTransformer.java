package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Optional client compatibility; Inventory Tweaks is not a build or runtime dependency. */
public final class InventoryTweaksTransformer implements IClassTransformer, Opcodes {
    private static final String HOOK = "newearth/client/DrawerAutoRefill";
    private static final String REFILL_TASK = "invtweaks.InvTweaksHandlerAutoRefill$1";
    private static final String MANAGER = "invtweaks/container/ContainerSectionManager";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes != null && REFILL_TASK.equals(transformedName)) return patchRefillTask(bytes);
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

    private static byte[] patchRefillTask(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES);
        if (!hasField(node, "containerMgr", "L" + MANAGER + ";") || !hasField(node, "i", "I")
                || !hasField(node, "targetedSlot", "I") || !hasField(node, "expectedItemId", "Ljava/lang/String;")
                || !hasField(node, "refillBeforeBreak", "Z")) return bytes;
        for (MethodNode method : node.methods) {
            if (!"run".equals(method.name) || !"()V".equals(method.desc)) continue;
            boolean hasLegacyMove = false;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (HOOK.equals(call.owner)) return bytes;
                hasLegacyMove |= MANAGER.equals(call.owner) && "move".equals(call.name) && "(II)Z".equals(call.desc);
            }
            if (!hasLegacyMove) return bytes;

            LabelNode legacy = new LabelNode();
            InsnList guard = new InsnList();
            loadField(guard, node, "containerMgr", "L" + MANAGER + ";");
            guard.add(new MethodInsnNode(INVOKEVIRTUAL, MANAGER, "getContainer", "()Lnet/minecraft/inventory/Container;", false));
            loadField(guard, node, "i", "I");
            loadField(guard, node, "targetedSlot", "I");
            loadField(guard, node, "expectedItemId", "Ljava/lang/String;");
            loadField(guard, node, "refillBeforeBreak", "Z");
            guard.add(new MethodInsnNode(INVOKESTATIC, HOOK, "refillEmptyHand",
                    "(Ljava/lang/Object;IILjava/lang/String;Z)Z", false));
            guard.add(new JumpInsnNode(IFEQ, legacy));
            guard.add(new InsnNode(RETURN));
            guard.add(legacy);
            guard.add(new FrameNode(F_NEW, 1, new Object[]{node.name}, 0, new Object[0]));
            method.instructions.insert(guard);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        }
        return bytes;
    }

    private static boolean hasField(ClassNode owner, String name, String descriptor) {
        for (FieldNode field : owner.fields) {
            if (name.equals(field.name) && descriptor.equals(field.desc) && (field.access & ACC_STATIC) == 0) return true;
        }
        return false;
    }

    private static void loadField(InsnList code, ClassNode owner, String name, String descriptor) {
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new FieldInsnNode(GETFIELD, owner.name, name, descriptor));
    }
}
