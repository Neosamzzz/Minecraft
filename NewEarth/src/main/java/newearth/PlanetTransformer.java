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
        boolean ep = "com.mjr.extraplanets.Config".equals(n);
        boolean gs = "galaxyspace.core.configs.GSConfigDimensions".equals(n);
        boolean registry = "micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry".equals(n);
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
        if (!(ep || gs || registry || world || item || rocket || provider || orbit)) return bytes;
        ClassNode c = new ClassNode();
        new ClassReader(bytes).accept(c, 0);
        int changes = 0;
        for (MethodNode m : c.methods) {
            if ((ep && m.name.equals("load") && m.desc.equals("()V"))
                    || (gs && m.name.equals("syncConfig") && m.desc.equals("(Z)V"))) {
                for (AbstractInsnNode i : m.instructions.toArray()) if (i.getOpcode() == RETURN) {
                    InsnList hook = new InsnList();
                    hook.add(new LdcInsnNode(Type.getObjectType(c.name)));
                    hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, ep ? "configureEP" : "configureGS", "(Ljava/lang/Class;)V", false));
                    m.instructions.insertBefore(i, hook);
                }
                changes++;
            }
            if (registry && m.name.equals("registerPlanet") && m.desc.endsWith(")Z")) {
                InsnList hook = new InsnList();
                LabelNode proceed = new LabelNode();
                hook.add(new VarInsnNode(ALOAD, 0));
                hook.add(new MethodInsnNode(INVOKESTATIC, HOOKS, "keepPlanet", "(Ljava/lang/Object;)Z", false));
                hook.add(new JumpInsnNode(IFNE, proceed));
                hook.add(new InsnNode(ICONST_0)); hook.add(new InsnNode(IRETURN)); hook.add(proceed); hook.add(new FrameNode(F_SAME, 0, null, 0, null));
                m.instructions.insert(hook); changes++;
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
                String base = "galaxyspace/core/prefab/items/modules/ItemModule";
                m.instructions.add(new InsnNode(ICONST_2));
                m.instructions.add(new TypeInsnNode(ANEWARRAY, base));
                int index = 0;
                for (String module : new String[] {"IonEngine", "SublightEngine"}) {
                    String type = "galaxyspace/systems/SolarSystem/planets/overworld/items/modules/" + module;
                    m.instructions.add(new InsnNode(DUP)); m.instructions.add(new InsnNode(ICONST_0 + index++));
                    m.instructions.add(new TypeInsnNode(NEW, type)); m.instructions.add(new InsnNode(DUP));
                    m.instructions.add(new MethodInsnNode(INVOKESPECIAL, type, "<init>", "()V", false));
                    m.instructions.add(new InsnNode(AASTORE));
                }
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
        int expected = world ? 2 : 1;
        if (changes != expected) throw new IllegalStateException("Unsupported solar-system patch target " + n + ": " + changes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        c.accept(writer);
        return writer.toByteArray();
    }
}
