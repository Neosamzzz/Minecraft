package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Optional client compatibility; Inventory Tweaks is not a build or runtime dependency. */
public final class InventoryTweaksTransformer implements IClassTransformer, Opcodes {
    private static final String HOOK = "newearth/client/DrawerAutoRefill";
    private static final String REFILL_TASK = "invtweaks/InvTweaksHandlerAutoRefill$1";

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
            if (!patchRefillDispatch(method)) {
                System.err.println("[NewEarth] Unsupported Inventory Tweaks refill dispatch; atomic swap not installed");
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            System.out.println("[NewEarth] Inventory Tweaks drawer auto-refill guard installed");
            return writer.toByteArray();
        }
        System.err.println("[NewEarth] Unsupported Inventory Tweaks auto-refill signature; drawer guard not installed");
        return bytes;
    }

    private static boolean patchRefillDispatch(MethodNode method) {
        MethodInsnNode dispatch = null;
        MethodInsnNode queue = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if ("invtweaks/InvTweaks".equals(call.owner)) {
                if ("getInstance".equals(call.name) && "()Linvtweaks/InvTweaks;".equals(call.desc)) dispatch = call;
                if ("addScheduledTask".equals(call.name) && "(Ljava/lang/Runnable;)V".equals(call.desc)) queue = call;
            }
        }
        if (dispatch == null || queue == null) return false;
        AbstractInsnNode init = previousInstruction(queue);
        if (!(init instanceof MethodInsnNode)) return false;
        MethodInsnNode task = (MethodInsnNode) init;
        if (!REFILL_TASK.equals(task.owner) || !"init".equals(task.name)
                || !"(IIZ)Ljava/lang/Runnable;".equals(task.desc)) return false;
        AbstractInsnNode beforeBreak = previousInstruction(task);
        AbstractInsnNode target = previousInstruction(beforeBreak);
        AbstractInsnNode source = previousInstruction(target);
        if (beforeBreak.getOpcode() != ILOAD || target.getOpcode() != ILOAD || source.getOpcode() != ILOAD) return false;
        // The existing final return is already a branch target with a stack-map frame.
        LabelNode exit = null;
        AbstractInsnNode last = queue.getNext();
        while (last != null && last.getOpcode() < 0) {
            if (exit == null && last instanceof LabelNode) exit = (LabelNode) last;
            last = last.getNext();
        }
        if (exit == null || last == null || last.getOpcode() != RETURN) return false;

        InsnList swap = new InsnList();
        swap.add(new VarInsnNode(ILOAD, ((VarInsnNode) source).var));
        swap.add(new VarInsnNode(ILOAD, ((VarInsnNode) target).var));
        swap.add(new VarInsnNode(ILOAD, ((VarInsnNode) beforeBreak).var));
        swap.add(new MethodInsnNode(INVOKESTATIC, HOOK, "refillSelectedStack", "(IIZ)Z", false));
        swap.add(new JumpInsnNode(IFNE, exit));
        method.instructions.insertBefore(dispatch, swap);
        return true;
    }
    private static AbstractInsnNode previousInstruction(AbstractInsnNode at) {
        AbstractInsnNode previous = at.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }
}
