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
    private static final String FACING = "Lnet/minecraft/util/EnumFacing;";
    private static final String VECTOR = "Lmicdoodle8/mods/galacticraft/api/vector/BlockVec3;";

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
            else if (target.equals(DOCK)) changes += patchDock(method);
            else if (method.name.equals("getStatus") && method.desc.equals("()Ljava/lang/String;")) {
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
        if (changes != (target.equals(MINER) ? 15 : target.equals(DOCK) ? 5 : 1))
            throw new IllegalStateException("Unsupported Galacticraft Astro Miner class " + transformedName + ": " + changes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static int patchDock(MethodNode method) {
        if (method.desc.equals("()V")) {
            switch (method.name) {
                case "findTargetPoints":
                    method.instructions.clear();
                    method.tryCatchBlocks.clear();
                    if (method.localVariables != null) method.localVariables.clear();
                    method.instructions.add(routeHook("startRoute"));
                    method.instructions.add(new InsnNode(RETURN));
                    return 1;
                case "update":
                case "func_73660_a":
                    return prependOwnerHook(method, DOCK, "tickDock");
                case "invalidate":
                case "func_145843_s":
                    return prependOwnerHook(method, DOCK, "releaseDock");
                case "updateGUIstate":
                    for (AbstractInsnNode instruction : method.instructions.toArray()) {
                        if (!(instruction instanceof FieldInsnNode) || instruction.getOpcode() != GETFIELD) continue;
                        FieldInsnNode field = (FieldInsnNode) instruction;
                        if (field.owner.equals(MINER) && field.name.equals("AIstate") && field.desc.equals("I")) {
                            method.instructions.set(field, hook("dockState", "(L" + MINER + ";)I"));
                            return 1;
                        }
                    }
                    return 0;
                default: return 0;
            }
        }
        if (method.name.equals("findNextTarget") && method.desc.equals("()" + VECTOR)) {
            method.instructions.insert(routeHook("refillRoute"));
            return 1;
        }
        return 0;
    }

    private static InsnList routeHook(String name) {
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new FieldInsnNode(GETFIELD, DOCK, "targetPoints", "Ljava/util/LinkedList;"));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new FieldInsnNode(GETFIELD, DOCK, "facing", FACING));
        code.add(hook(name, "(L" + DOCK + ";Ljava/util/List;" + FACING + ")V"));
        return code;
    }

    private static int prependOwnerHook(MethodNode method, String owner, String name) {
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(hook(name, "(L" + owner + ";)V"));
        method.instructions.insert(code);
        return 1;
    }

    private static void prependMinerFacingHook(MethodNode method, String name) {
        InsnList code = new InsnList();
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new VarInsnNode(ALOAD, 0));
        code.add(new FieldInsnNode(GETFIELD, MINER, "facingAI", FACING));
        code.add(hook(name, "(L" + MINER + ";" + FACING + ")V"));
        method.instructions.insert(code);
    }

    private static int patchMiner(ClassNode node, MethodNode method) {
        int stateHook = 0;
        switch (method.name) {
            case "onUpdate":
            case "func_70071_h_":
                if (!method.desc.equals("()V")) return 0;
                prependMinerFacingHook(method, "tickMiner");
                stateHook = 1;
                break;
            case "setDead":
            case "func_70106_y":
                return method.desc.equals("()V") ? prependOwnerHook(method, MINER, "releaseMiner") : 0;
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
                points.add(new FieldInsnNode(GETFIELD, node.name, "baseFacing", FACING));
                points.add(new VarInsnNode(ALOAD, 0));
                points.add(new FieldInsnNode(GETFIELD, node.name, "posTarget", VECTOR));
                points.add(hook("addShafts", "(L" + MINER + ";Ljava/util/List;" + FACING + VECTOR + ")V"));
                method.instructions.insertBefore(done, points);
                return 1;
            case "emptyInventory":
                return patchUnload(method);
            case "prepareMove":
                if (!method.desc.equals("(II)Z")) return 0;
                prependMinerFacingHook(method, "updateMinerFacing");
                return patchSliceDelay(method) + 1;
            case "moveToPosX":
            case "moveToPosY":
            case "moveToPosZ":
                return patchMovementSpeed(method);
            default:
                break;
        }
        return patchDrilling(method) + stateHook;
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
                method.instructions.set(limit, new IntInsnNode(BIPUSH, 12));
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
