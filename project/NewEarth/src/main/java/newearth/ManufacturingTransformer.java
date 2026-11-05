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
        boolean table="galaxyspace.systems.SolarSystem.planets.overworld.gui.GuiModificationTable".equals(n);
        if(!(boss||item||rocket||table)) return bytes;
        ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);int changes=0;
        for(MethodNode m:c.methods) {
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
                    // EntityTier7Rocket extends EntityTier6Rocket. The original frames still name that type.
                    hook.add(new TypeInsnNode(CHECKCAST,"galaxyspace/core/prefab/entities/EntityTier6Rocket"));
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
            if(table) for(AbstractInsnNode instruction:m.instructions.toArray()) if(instruction instanceof LdcInsnNode && "Mod. count: ".equals(((LdcInsnNode)instruction).cst)) {
                ((LdcInsnNode)instruction).cst="gui.module.count";
                m.instructions.insert(instruction,new MethodInsnNode(INVOKESTATIC,"micdoodle8/mods/galacticraft/core/util/GCCoreUtil","translate","(Ljava/lang/String;)Ljava/lang/String;",false));
                changes++;
            }
        }
        if(table && changes==0) return bytes;
        if(!boss && changes==0) throw new IllegalStateException("Missing manufacturing hooks: "+n);
        if(changes==0) return bytes;
        ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(w);return w.toByteArray();
    }
}
