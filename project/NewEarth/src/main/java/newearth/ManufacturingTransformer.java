package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Patches verified GS/EP manufacturing entry points without replacing their jars. */
public final class ManufacturingTransformer implements IClassTransformer, Opcodes {
    private static final String H="newearth/ManufacturingHooks";
    @Override public byte[] transform(String name,String n,byte[] bytes) {
        if(bytes==null) return null;
        boolean boss=n!=null && (n.equals("micdoodle8.mods.galacticraft.core.entities.EntitySkeletonBoss")
                || n.startsWith("com.mjr.extraplanets.entities.bosses.")
                || n.equals("galaxyspace.systems.SolarSystem.moons.io.entities.EntityBossGhast"));
        boolean item="galaxyspace.core.prefab.items.rockets.ItemTier6Rocket".equals(n);
        boolean rocket="galaxyspace.core.prefab.entities.EntityTier6Rocket".equals(n);
        boolean slot="galaxyspace.systems.SolarSystem.planets.overworld.inventory.SlotRocketAssembly".equals(n);
        boolean container="galaxyspace.systems.SolarSystem.planets.overworld.inventory.ContainerRocketAssembler".equals(n);
        if(!(boss||item||rocket||slot||container)) return bytes;
        ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);int changes=0;
        for(MethodNode m:c.methods) {
            if(container && m.name.equals("<init>")) for(AbstractInsnNode instruction:m.instructions.toArray()) if(instruction.getOpcode()==RETURN) {
                InsnList hook=new InsnList();hook.add(new VarInsnNode(ALOAD,0));hook.add(new VarInsnNode(ALOAD,2));
                hook.add(new MethodInsnNode(INVOKESTATIC,H,"assemblyContainer","(Lnet/minecraft/inventory/Container;Ljava/lang/Object;)V",false));m.instructions.insertBefore(instruction,hook);changes++;
            }
            if(container && (m.name.equals("transferStackInSlot")||m.name.equals("func_82846_b"))) {
                InsnList hook=new InsnList();LabelNode original=new LabelNode();hook.add(new VarInsnNode(ALOAD,0));hook.add(new VarInsnNode(ALOAD,1));hook.add(new VarInsnNode(ILOAD,2));
                hook.add(new MethodInsnNode(INVOKESTATIC,H,"transferAssemblyEngine","(Lnet/minecraft/inventory/Container;Lnet/minecraft/entity/player/EntityPlayer;I)Lnet/minecraft/item/ItemStack;",false));
                hook.add(new InsnNode(DUP));hook.add(new JumpInsnNode(IFNULL,original));hook.add(new InsnNode(ARETURN));hook.add(original);
                hook.add(new FrameNode(F_SAME1,0,null,1,new Object[]{"net/minecraft/item/ItemStack"}));hook.add(new InsnNode(POP));m.instructions.insert(hook);changes++;
            }

            if(boss && m.name.equals("getGuaranteedLoot") && m.desc.equals("(Ljava/util/Random;)Lnet/minecraft/item/ItemStack;")) {
                InsnList hook=new InsnList();LabelNode original=new LabelNode();
                hook.add(new VarInsnNode(ALOAD,0));hook.add(new VarInsnNode(ALOAD,1));
                hook.add(new MethodInsnNode(INVOKESTATIC,H,"reward","(Ljava/lang/Object;Ljava/util/Random;)Lnet/minecraft/item/ItemStack;",false));
                hook.add(new InsnNode(DUP));hook.add(new JumpInsnNode(IFNULL,original));hook.add(new InsnNode(ARETURN));
                hook.add(original);hook.add(new FrameNode(F_SAME1,0,null,1,new Object[]{"net/minecraft/item/ItemStack"}));hook.add(new InsnNode(POP));
                m.instructions.insert(hook);changes++;
            }
            if(item && m.name.equals("placeRocketOnPad")) {
                for(AbstractInsnNode i:m.instructions.toArray()) if(i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("<init>") && ((MethodInsnNode)i).owner.equals("galaxyspace/core/prefab/entities/EntityTier6Rocket") && ((MethodInsnNode)i).desc.startsWith("(Lnet/minecraft/world/World;DDD")) {
                    InsnList hook=new InsnList();hook.add(new VarInsnNode(ALOAD,1));
                    hook.add(new MethodInsnNode(INVOKESTATIC,H,"swapIfTier7","(Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/Entity;",false));
                    m.instructions.insert(i,hook);changes++;
                }
            }
            if(item && m.name.equals("placeRocketOnPad")) {
                int found=0;
                for(AbstractInsnNode i:m.instructions.toArray()) if(i instanceof MethodInsnNode && (((MethodInsnNode)i).name.equals("spawnEntity")||((MethodInsnNode)i).name.equals("func_72838_d"))) {
                    // Stack is World,Entity. Leave both operands for the original call.
                    InsnList hook=new InsnList();hook.add(new InsnNode(DUP));hook.add(new VarInsnNode(ALOAD,1));
                    hook.add(new MethodInsnNode(INVOKESTATIC,H,"placed","(Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V",false));m.instructions.insertBefore(i,hook);found++;
                }
                if(found!=1) throw new IllegalStateException("GS rocket placement changed");changes++;
            }
            if(rocket) {
                if(m.name.equals("igniteCheckingCooldown")) {
                    InsnList hook=new InsnList();LabelNode original=new LabelNode();hook.add(new VarInsnNode(ALOAD,0));
                    hook.add(new MethodInsnNode(INVOKESTATIC,H,"canIgnite","(Lnet/minecraft/entity/Entity;)Z",false));
                    hook.add(new JumpInsnNode(IFNE,original));hook.add(new InsnNode(RETURN));hook.add(original);hook.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insert(hook);changes++;
                }
                if(m.name.equals("getRocketTier")) {reset(m);m.instructions.add(new VarInsnNode(ALOAD,0));m.instructions.add(new MethodInsnNode(INVOKESTATIC,H,"rocketTier","(Lnet/minecraft/entity/Entity;)I",false));m.instructions.add(new InsnNode(IRETURN));changes++;}
                for(AbstractInsnNode i:m.instructions.toArray()) if(i instanceof FieldInsnNode && i.getOpcode()==GETSTATIC && ((FieldInsnNode)i).owner.equals("galaxyspace/core/GSItems") && ((FieldInsnNode)i).name.equals("ROCKET_TIER_6")) {
                    InsnList hook=new InsnList();hook.add(new VarInsnNode(ALOAD,0));hook.add(new MethodInsnNode(INVOKESTATIC,H,"rocketItem","(Lnet/minecraft/entity/Entity;)Lnet/minecraft/item/Item;",false));m.instructions.insertBefore(i,hook);m.instructions.remove(i);changes++;
                }
                if(m.name.equals("onTeleport")) for(AbstractInsnNode i:m.instructions.toArray()) if(i.getOpcode()==RETURN) {
                    InsnList hook=new InsnList();hook.add(new VarInsnNode(ALOAD,0));hook.add(new VarInsnNode(ALOAD,1));hook.add(new MethodInsnNode(INVOKESTATIC,H,"recover","(Lnet/minecraft/entity/Entity;Ljava/lang/Object;)V",false));m.instructions.insertBefore(i,hook);changes++;
                }
                if(m.name.equals("getItemsDropped")) for(AbstractInsnNode i:m.instructions.toArray()) if(i.getOpcode()==ARETURN) {
                    m.instructions.insertBefore(i,new MethodInsnNode(INVOKESTATIC,H,"dropped","(Ljava/util/List;)Ljava/util/List;",false));changes++;
                }
            }
            if(slot && (m.name.equals("isItemValid")||m.name.equals("func_75214_a"))) {
                InsnList hook=new InsnList();LabelNode original=new LabelNode();
                hook.add(new VarInsnNode(ALOAD,0));hook.add(new FieldInsnNode(GETFIELD,c.name,"index","I"));hook.add(new VarInsnNode(ALOAD,1));
                hook.add(new MethodInsnNode(INVOKESTATIC,H,"validRocketSlot","(ILnet/minecraft/item/ItemStack;)Z",false));
                hook.add(new JumpInsnNode(IFEQ,original));hook.add(new InsnNode(ICONST_1));hook.add(new InsnNode(IRETURN));hook.add(original);hook.add(new FrameNode(F_SAME,0,null,0,null));m.instructions.insert(hook);changes++;
            }
        }
        if(!boss && changes==0) throw new IllegalStateException("Missing manufacturing hooks: "+n);
        if(changes==0) return bytes;
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();
    }
    private static void reset(MethodNode m) {m.instructions.clear();m.tryCatchBlocks.clear();if(m.localVariables!=null)m.localVariables.clear();}
}
