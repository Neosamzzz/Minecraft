package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class EnvironmentTransformer implements IClassTransformer, Opcodes {
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        boolean handler = "com.mjr.extraplanets.handlers.MainHandlerServer".equals(transformedName);
        boolean modules = "com.mjr.extraplanets.items.armor.modules.ExtraPlanets_Modules".equals(transformedName);
        boolean provider = transformedName != null && transformedName.matches(
                "com\\.mjr\\.extraplanets\\.(planets|moons)\\.[A-Za-z0-9]+\\.WorldProvider[A-Za-z0-9]+");
        if (!(handler || modules || provider)) return bytes;
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            if ((modules && method.name.equals("init") && method.desc.equals("()V"))
                    || (handler && method.name.equals("onCommandEvent") && method.desc.equals("(Lnet/minecraftforge/event/CommandEvent;)V"))) {
                method.instructions.clear(); method.tryCatchBlocks.clear();
                if (method.localVariables != null) method.localVariables.clear();
                method.instructions.add(new InsnNode(RETURN)); changes++;
            }
            if (handler && method.name.equals("isValidSpaceSuit") && method.desc.equals(
                    "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Ljava/util/List;Z)Z")) {
                InsnList head = new InsnList(); LabelNode original = new LabelNode();
                for (int slot = 2; slot <= 5; slot++) head.add(new VarInsnNode(ALOAD, slot));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/EnvironmentHooks", "gsSuit",
                        "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z", false));
                head.add(new JumpInsnNode(IFEQ, original)); head.add(new InsnNode(ICONST_1)); head.add(new InsnNode(IRETURN));
                head.add(original); head.add(new FrameNode(F_SAME, 0, null, 0, null)); method.instructions.insert(head); changes++;
            }
            if (handler && method.name.equals("checkRadiation") && method.desc.equals(
                    "(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingUpdateEvent;Lnet/minecraft/entity/player/EntityPlayerMP;ILjava/util/List;)V")) {
                InsnList head = new InsnList(); LabelNode original = new LabelNode(); head.add(new VarInsnNode(ALOAD, 2));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/EnvironmentHooks", "protectedByGS", "(Lnet/minecraft/entity/player/EntityPlayer;)Z", false));
                head.add(new JumpInsnNode(IFEQ, original)); head.add(new InsnNode(RETURN));
                head.add(original); head.add(new FrameNode(F_SAME, 0, null, 0, null)); method.instructions.insert(head); changes++;
            }
            if (provider && method.name.equals("getThermalLevelModifier") && method.desc.equals("()F")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction.getOpcode() == FRETURN)
                    method.instructions.insertBefore(instruction, new MethodInsnNode(INVOKESTATIC,
                            "newearth/EnvironmentHooks", "thermal", "(F)F", false));
                changes++;
            }
        }
        if (handler && changes != 3 || modules && changes != 1)
            throw new IllegalStateException("Unsupported EP environment target " + transformedName + ": " + changes);
        if (changes == 0) return bytes; // inherited provider temperature is adapted in body metadata
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer); return writer.toByteArray();
    }
}
