package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Small version-checked patches; GS and EP binaries remain external dependencies. */
public final class PlanetTransformer implements IClassTransformer, Opcodes {
    private static final String HOOKS = "newearth/PlanetHooks";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        String n = transformedName;
        boolean registry = "micdoodle8.mods.galacticraft.api.GalacticraftRegistry".equals(n);
        boolean core = "micdoodle8.mods.galacticraft.core.GalacticraftCore".equals(n);
        boolean gui = "micdoodle8.mods.galacticraft.core.client.gui.screen.GuiCelestialSelection".equals(n);
        boolean epGui = "com.mjr.extraplanets.client.gui.screen.CustomCelestialSelection".equals(n);
        boolean gsBodies = "galaxyspace.systems.SolarSystem.SolarSystemBodies".equals(n);
        boolean world = "micdoodle8.mods.galacticraft.core.util.WorldUtil".equals(n);
        boolean item = "galaxyspace.core.prefab.items.rockets.ItemTier6Rocket".equals(n);
        boolean rocket = "galaxyspace.core.prefab.entities.EntityTier6Rocket".equals(n);
        boolean orbit = "com.mjr.extraplanets.planets.Saturn.spacestation.WorldProviderSaturnOrbit".equals(n)
                || "com.mjr.extraplanets.planets.Neptune.spacestation.WorldProviderNeptuneOrbit".equals(n)
                || "com.mjr.extraplanets.planets.Pluto.spacestation.WorldProviderPlutoOrbit".equals(n)
                || "com.mjr.extraplanets.planets.Eris.spacestation.WorldProviderErisOrbit".equals(n);
        boolean provider = n != null && (n.equals("com.mjr.extraplanets.planets.Neptune.WorldProviderNeptune")
                || n.equals("com.mjr.extraplanets.planets.Pluto.WorldProviderPluto")
                || n.equals("com.mjr.extraplanets.planets.Eris.WorldProviderEris"));
        if (!(registry || core || gui || epGui || gsBodies || world || item || rocket || provider || orbit)) return bytes;
        ClassNode c = new ClassNode();
        new ClassReader(bytes).accept(c, epGui ? ClassReader.EXPAND_FRAMES : 0);
        int changes = 0;
        for (MethodNode m : c.methods) {
            if (registry && m.name.equals("getTeleportTypeForDimension")) {
                InsnList hook = new InsnList(); LabelNode original = new LabelNode();
                hook.add(new VarInsnNode(ALOAD, 0));
                hook.add(new MethodInsnNode(INVOKESTATIC, "newearth/dimension/EndDimension", "teleportTypeFor",
                        "(Ljava/lang/Class;)Lmicdoodle8/mods/galacticraft/api/world/ITeleportType;", false));
                hook.add(new InsnNode(DUP)); hook.add(new JumpInsnNode(IFNULL, original)); hook.add(new InsnNode(ARETURN));
                hook.add(original); hook.add(new FrameNode(F_SAME1, 0, null, 1,
                        new Object[]{"micdoodle8/mods/galacticraft/api/world/ITeleportType"})); hook.add(new InsnNode(POP));
                m.instructions.insert(hook); changes++;
            }
            if (gsBodies && m.name.equals("registerDummyMoon")) {
                InsnList hook = new InsnList(); LabelNode proceed = new LabelNode();
                hook.add(new VarInsnNode(ALOAD, 1));
                hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "hasReachableMoonCounterpart", "(Ljava/lang/String;)Z", false));
                hook.add(new JumpInsnNode(IFEQ, proceed)); hook.add(new InsnNode(ACONST_NULL)); hook.add(new InsnNode(ARETURN));
                hook.add(proceed); hook.add(new FrameNode(F_SAME, 0, null, 0, null)); m.instructions.insert(hook); changes++;
            }
            if (epGui && m.name.equals("drawCustomButtons") && m.desc.equals("(II)V")) {
                patchDetails(m); changes++;
            }
            if (core && m.name.equals("makeDummyPlanet")) {
                InsnList hook = new InsnList(); LabelNode proceed = new LabelNode();
                hook.add(new VarInsnNode(ALOAD, 1));
                hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "isReplacedDummy", "(Ljava/lang/String;)Z", false));
                hook.add(new JumpInsnNode(IFEQ, proceed)); hook.add(new InsnNode(ACONST_NULL)); hook.add(new InsnNode(ARETURN));
                hook.add(proceed); hook.add(new FrameNode(F_SAME, 0, null, 0, null));
                m.instructions.insert(hook); changes++;
            }
            if (gui) changes += patchCatalogue(m);
            if ((gui || epGui) && m.name.equals("drawButtons") && m.desc.equals("(II)V")) {
                patchMapButtons(m); changes++;
            }
            if (provider && m.name.equals("canSpaceshipTierPass") && m.desc.equals("(I)Z")) {
                // EP 0.8.0 source snapshots and installed jars differ here. Accept
                // either the known constant gate or a body-tier lookup, then emit
                // the same explicit six-tier comparison for both variants.
                boolean knownGate = false;
                int previous = n.contains("Neptune") ? 7 : n.contains("Pluto") ? 8 : 9;
                for (AbstractInsnNode i : m.instructions.toArray()) {
                    if (i instanceof IntInsnNode && ((IntInsnNode) i).operand == previous) knownGate = true;
                    if (i instanceof MethodInsnNode && ((MethodInsnNode) i).name.equals("getTierRequirement")) knownGate = true;
                }
                if (!knownGate) throw new IllegalStateException("Unsupported EP tier gate: " + n);
                m.instructions.clear(); m.tryCatchBlocks.clear();
                if (m.localVariables != null) m.localVariables.clear();
                LabelNode fail = new LabelNode();
                m.instructions.add(new VarInsnNode(ILOAD, 1));
                m.instructions.add(new IntInsnNode(BIPUSH, 6));
                m.instructions.add(new JumpInsnNode(IF_ICMPLT, fail));
                m.instructions.add(new InsnNode(ICONST_1)); m.instructions.add(new InsnNode(IRETURN));
                m.instructions.add(fail); m.instructions.add(new FrameNode(F_SAME, 0, null, 0, null));
                m.instructions.add(new InsnNode(ICONST_0)); m.instructions.add(new InsnNode(IRETURN));
                changes++;
            }
            if (orbit && m.name.equals("canSpaceshipTierPass") && m.desc.equals("(I)Z")) {
                int patched = 0;
                for (AbstractInsnNode i : m.instructions.toArray()) if (i.getOpcode() == IF_ICMPLE) {
                    ((JumpInsnNode) i).setOpcode(IF_ICMPLT); patched++;
                }
                if (patched != 1) throw new IllegalStateException("Unsupported EP orbit gate: " + n);
                changes++;
            }
            if (item && m.name.equals("getAvailableModules")) {
                m.instructions.clear(); m.tryCatchBlocks.clear();
                if (m.localVariables != null) m.localVariables.clear();
                m.instructions.add(new VarInsnNode(ALOAD, 0));
                m.instructions.add(new MethodInsnNode(INVOKESTATIC, "newearth/ManufacturingHooks", "modulesFor",
                        "(Lnet/minecraft/item/Item;)Ljava/lang/Object;", false));
                m.instructions.add(new TypeInsnNode(CHECKCAST, "[Lgalaxyspace/core/prefab/items/modules/ItemModule;"));
                m.instructions.add(new InsnNode(ARETURN)); changes++;
            }
            if (rocket && m.name.equals("onTeleport")) {
                for (AbstractInsnNode i : m.instructions.toArray()) if (i.getOpcode() == RETURN) {
                    InsnList hook = new InsnList(); hook.add(new VarInsnNode(ALOAD, 0)); hook.add(new VarInsnNode(ALOAD, 1));
                    hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "preserveEngine", "(Ljava/lang/Object;Ljava/lang/Object;)V", false));
                    m.instructions.insertBefore(i, hook);
                }
                changes++;
            }
            if (world && m.name.equals("getPossibleDimensionsForSpaceshipTier")) {
                for (AbstractInsnNode i : m.instructions.toArray()) if (i.getOpcode() == ARETURN) {
                    InsnList hook = new InsnList(); hook.add(new VarInsnNode(ALOAD, 1));
                    hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "filterDestinations", "(Ljava/util/List;Ljava/lang/Object;)Ljava/util/List;", false));
                    m.instructions.insertBefore(i, hook);
                }
                changes++;
            }
            if (world && m.name.equals("transferEntityToDimension") && Type.getArgumentTypes(m.desc).length == 5) {
                InsnList hook = new InsnList(); LabelNode proceed = new LabelNode();
                hook.add(new VarInsnNode(ALOAD, 0)); hook.add(new VarInsnNode(ILOAD, 1)); hook.add(new VarInsnNode(ALOAD, 4));
                hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "allowTransfer", "(Ljava/lang/Object;ILjava/lang/Object;)Z", false));
                hook.add(new JumpInsnNode(IFNE, proceed)); hook.add(new InsnNode(ACONST_NULL)); hook.add(new InsnNode(ARETURN)); hook.add(proceed); hook.add(new FrameNode(F_SAME, 0, null, 0, null));
                m.instructions.insert(hook); changes++;
            }
        }
        int expected = gui ? 6 : epGui ? 2 : world ? 2 : 1;
        if (changes != expected) throw new IllegalStateException("Unsupported solar-system patch target " + n + ": " + changes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        c.accept(writer);
        return writer.toByteArray();
    }

    private static void patchDetails(MethodNode m) {
        String client = "newearth/client/CatalogueHooks";
        JumpInsnNode guard = null;
        for (AbstractInsnNode i : m.instructions.toArray()) if (i instanceof FieldInsnNode
                && ((FieldInsnNode) i).name.equals("selectedBody")) {
            AbstractInsnNode next = i.getNext();
            while (next != null && next.getOpcode() < 0) next = next.getNext();
            if (next != null && next.getOpcode() == IFNULL) { guard = (JumpInsnNode) next; break; }
        }
        if (guard == null) throw new IllegalStateException("Unsupported EP detail panel guard");
        // Preserve the galaxy selector below this block; only suppress the legacy details.
        m.instructions.insertBefore(guard, new MethodInsnNode(INVOKESTATIC,
                "newearth/client/CatalogueHooks", "legacyDetails", "(Ljava/lang/Object;)Z", false));
        guard.setOpcode(IFEQ);
    }

    private static void patchMapButtons(MethodNode m) {
        int hints = 0;
        for (AbstractInsnNode i : m.instructions.toArray()) {
            if (!(i instanceof FieldInsnNode) || !((FieldInsnNode) i).name.equals("SELECTED")) continue;
            AbstractInsnNode jump = i.getNext();
            while (jump != null && jump.getOpcode() < 0) jump = jump.getNext();
            if (jump == null || jump.getOpcode() != IF_ACMPNE)
                throw new IllegalStateException("Unsupported map zoom hint guard");
            InsnList disabled = new InsnList(); disabled.add(new InsnNode(POP2)); disabled.add(new InsnNode(ICONST_0));
            m.instructions.insertBefore(jump, disabled); ((JumpInsnNode) jump).setOpcode(IFEQ); hints++;
        }
        if (hints != 1) throw new IllegalStateException("Unsupported map zoom hint count " + hints);
        for (AbstractInsnNode i : m.instructions.toArray()) if (i.getOpcode() == RETURN) {
            InsnList render = new InsnList(); render.add(new VarInsnNode(ALOAD, 0));
            render.add(new MethodInsnNode(INVOKESTATIC, "newearth/client/CatalogueHooks", "drawFacts", "(Ljava/lang/Object;)V", false));
            m.instructions.insertBefore(i, render);
        }
    }

    private static int patchCatalogue(MethodNode m) {
        String client = "newearth/client/CatalogueHooks";
        if ((m.name.equals("drawScreen") || m.name.equals("func_73863_a")) && m.desc.equals("(IIF)V")) {
            InsnList head = new InsnList(); head.add(new VarInsnNode(ALOAD, 0));
            head.add(new VarInsnNode(ILOAD, 1)); head.add(new VarInsnNode(ILOAD, 2));
            head.add(new MethodInsnNode(INVOKESTATIC, client, "frame", "(Ljava/lang/Object;II)V", false));
            m.instructions.insert(head);
            int wheels = 0;
            for (AbstractInsnNode i : m.instructions.toArray()) if (i instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) i;
                if (call.owner.equals("org/lwjgl/input/Mouse") && call.name.equals("getDWheel")) {
                    m.instructions.insertBefore(i, new VarInsnNode(ALOAD, 0));
                    m.instructions.set(i, new MethodInsnNode(INVOKESTATIC, client, "wheel", "(Ljava/lang/Object;)I", false)); wheels++;
                }
            }
            if (wheels != 1) throw new IllegalStateException("Unsupported GC catalogue wheel count " + wheels);
            return 1;
        }
        if (m.name.equals("drawChildren") && m.desc.equals("(Ljava/util/List;IIZ)I")) {
            InsnList head = new InsnList(); head.add(new VarInsnNode(ALOAD, 0));
            head.add(new VarInsnNode(ILOAD, 3)); head.add(new VarInsnNode(ILOAD, 4));
            head.add(new MethodInsnNode(INVOKESTATIC, client, "begin", "(Ljava/lang/Object;IZ)I", false));
            head.add(new VarInsnNode(ISTORE, 3)); m.instructions.insert(head);
            for (AbstractInsnNode i : m.instructions.toArray()) if (i.getOpcode() == IRETURN) {
                InsnList end = new InsnList(); end.add(new VarInsnNode(ALOAD, 0)); end.add(new VarInsnNode(ILOAD, 4));
                end.add(new MethodInsnNode(INVOKESTATIC, client, "end", "(Ljava/lang/Object;Z)V", false));
                m.instructions.insertBefore(i, end);
            }
            return 1;
        }
        if (m.name.equals("testClicked")) {
            InsnList head = new InsnList(); head.add(new VarInsnNode(ALOAD, 0)); head.add(new VarInsnNode(ILOAD, 3));
            head.add(new VarInsnNode(ILOAD, 4)); head.add(new VarInsnNode(ILOAD, 5));
            head.add(new MethodInsnNode(INVOKESTATIC, client, "rowY", "(Ljava/lang/Object;III)I", false));
            head.add(new VarInsnNode(ISTORE, 3)); m.instructions.insert(head); return 1;
        }
        boolean click = (m.name.equals("mouseClicked") || m.name.equals("func_73864_a")) && m.desc.equals("(III)V");
        boolean drag = (m.name.equals("mouseClickMove") || m.name.equals("func_146273_a")) && m.desc.equals("(IIIJ)V");
        if (click || drag) {
            InsnList head = new InsnList(); LabelNode proceed = new LabelNode(); head.add(new VarInsnNode(ALOAD, 0));
            if (click) {
                for (int i = 1; i <= 3; i++) head.add(new VarInsnNode(ILOAD, i));
                head.add(new MethodInsnNode(INVOKESTATIC, client, "click", "(Ljava/lang/Object;III)Z", false));
            } else head.add(new MethodInsnNode(INVOKESTATIC, client, "dragging", "(Ljava/lang/Object;)Z", false));
            head.add(new JumpInsnNode(IFEQ, proceed)); head.add(new InsnNode(RETURN));
            head.add(proceed); head.add(new FrameNode(F_SAME, 0, null, 0, null)); m.instructions.insert(head); return 1;
        }
        return 0;
    }
}
