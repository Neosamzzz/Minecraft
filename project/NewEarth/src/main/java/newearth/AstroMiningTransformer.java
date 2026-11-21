package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public final class AstroMiningTransformer implements IClassTransformer, Opcodes {
    private static final String ASTEROIDS = "micdoodle8/mods/galacticraft/planets/asteroids/";
    private static final String MINER = ASTEROIDS + "entities/EntityAstroMiner";
    private static final String DOCK = ASTEROIDS + "tile/TileEntityMinerBase";
    private static final String GUI = ASTEROIDS + "client/gui/GuiAstroMinerDock";
    private static final String HOOKS = "newearth/AstroMining";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || transformedName == null) return bytes;
        String target = transformedName.replace('.', '/');
        if (!target.equals(MINER) && !target.equals(DOCK) && !target.equals(GUI)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals(HOOKS)) return bytes;
            }
            if (target.equals(MINER)) changes += patchMiner(node, method);
            else if (target.equals(DOCK) && method.name.equals("updateGUIstate") && method.desc.equals("()V")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() != GETFIELD || !(instruction instanceof FieldInsnNode)) continue;
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    if (field.owner.equals(MINER) && field.name.equals("AIstate") && field.desc.equals("I")) {
                        method.instructions.set(field, hook("dockState", "(L" + MINER + ";)I"));
                        changes++;
                    }
                }
            } else if (target.equals(GUI) && method.name.equals("getStatus") && method.desc.equals("()Ljava/lang/String;")) {
                LabelNode ordinary = new LabelNode();
                InsnList code = new InsnList();
                code.add(new VarInsnNode(ALOAD, 0));
                code.add(new FieldInsnNode(GETFIELD, GUI, "tile", "L" + DOCK + ";"));
                code.add(hook("faultStatus", "(L" + DOCK + ";)Ljava/lang/String;"));
                code.add(new InsnNode(DUP));
                code.add(new JumpInsnNode(IFNULL, ordinary));
                code.add(new VarInsnNode(ALOAD, 0));
                code.add(new InsnNode(ICONST_1));
                code.add(new FieldInsnNode(PUTFIELD, GUI, "extraLines", "Z"));
                code.add(new InsnNode(ARETURN));
                code.add(ordinary);
                code.add(new FrameNode(F_SAME1, 0, null, 1, new Object[] {"java/lang/String"}));
                code.add(new InsnNode(POP));
                method.instructions.insert(code);
                changes++;
            }
        }
        if (changes != (target.equals(MINER) ? 12 : 1))
            throw new IllegalStateException("Unsupported Galacticraft Astro Miner class " + transformedName + ": " + changes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static int patchMiner(ClassNode node, MethodNode method) {
        switch (method.name) {
            case "<init>":
                int speeds = 0;
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction instanceof LdcInsnNode && Double.valueOf(0.022D).equals(((LdcInsnNode) instruction).cst)) {
                        ((LdcInsnNode) instruction).cst = 0.044D;
                        speeds++;
                    }
                }
                return speeds;
            case "freeze":
                if (!method.desc.equals("(I)V")) return 0;
                InsnList fault = new InsnList();
                fault.add(new VarInsnNode(ALOAD, 0));
                fault.add(new VarInsnNode(ILOAD, 1));
                fault.add(hook("recordFault", "(L" + MINER + ";I)V"));
                method.instructions.insert(fault);
                return 1;
            case "setMinePoints":
                if (!method.desc.equals("()V")) return 0;
                AbstractInsnNode done = method.instructions.getLast();
                while (done != null && done.getOpcode() < 0) done = done.getPrevious();
                if (done == null || done.getOpcode() != RETURN) return 0;
                InsnList points = new InsnList();
                points.add(new VarInsnNode(ALOAD, 0));
                points.add(new VarInsnNode(ALOAD, 0));
                points.add(new FieldInsnNode(GETFIELD, node.name, "minePoints", "Ljava/util/LinkedList;"));
                points.add(new VarInsnNode(ALOAD, 0));
                points.add(new FieldInsnNode(GETFIELD, node.name, "baseFacing", "Lnet/minecraft/util/EnumFacing;"));
                points.add(hook("addShafts", "(L" + MINER + ";Ljava/util/List;Lnet/minecraft/util/EnumFacing;)V"));
                method.instructions.insertBefore(done, points);
                return 1;
            case "emptyInventory":
                return patchUnload(method);
            case "prepareMove":
                return patchSliceDelay(method);
            case "moveToPosX":
            case "moveToPosY":
            case "moveToPosZ":
                return patchMovementSpeed(method);
            default:
                return patchDrilling(method);
        }
    }

    private static int patchUnload(MethodNode method) {
        int changes = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (call.owner.equals(DOCK) && call.name.equals("addToInventory") && call.desc.equals("(Lnet/minecraft/item/ItemStack;)Z")) {
                method.instructions.set(call, hook("unload", "(L" + DOCK + ";Lnet/minecraft/item/ItemStack;)Z"));
                changes++;
            }
        }
        return changes;
    }

    private static int patchDrilling(MethodNode method) {
        int changes = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (!(instruction instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode) instruction;
            if (!call.owner.equals(MINER) || !call.name.equals("prepareMove") || !call.desc.equals("(II)Z")) continue;
            AbstractInsnNode distance = previousOpcode(call);
            AbstractInsnNode limit = previousOpcode(distance);
            if (distance != null && limit != null && distance.getOpcode() == ICONST_2 && limit.getOpcode() == ICONST_1) {
                method.instructions.set(limit, new InsnNode(ICONST_4));
                changes++;
            }
        }
        return changes;
    }

    private static int patchMovementSpeed(MethodNode method) {
        int changes = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != GETFIELD || !(instruction instanceof FieldInsnNode)) continue;
            FieldInsnNode field = (FieldInsnNode) instruction;
            if (!field.owner.equals(MINER) || !field.name.equals("speed") || !field.desc.equals("D")) continue;
            method.instructions.insertBefore(field, new InsnNode(DUP));
            method.instructions.insert(field, hook("movementSpeed", "(L" + MINER + ";D)D"));
            changes++;
        }
        return changes;
    }

    private static int patchSliceDelay(MethodNode method) {
        int changes = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() != PUTFIELD || !(instruction instanceof FieldInsnNode)) continue;
            FieldInsnNode field = (FieldInsnNode) instruction;
            AbstractInsnNode delay = previousOpcode(field);
            if (!field.owner.equals(MINER) || !field.name.equals("mineCountDown")
                    || delay == null || delay.getOpcode() != ICONST_3) continue;
            InsnList code = new InsnList();
            code.add(new InsnNode(DUP));
            code.add(hook("nextSliceDelay", "(L" + MINER + ";)I"));
            method.instructions.insertBefore(delay, code);
            method.instructions.remove(delay);
            changes++;
        }
        return changes;
    }

    private static AbstractInsnNode previousOpcode(AbstractInsnNode instruction) {
        if (instruction == null) return null;
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
        return previous;
    }

    private static MethodInsnNode hook(String name, String descriptor) {
        return new MethodInsnNode(INVOKESTATIC, HOOKS, name, descriptor, false);
    }
}
