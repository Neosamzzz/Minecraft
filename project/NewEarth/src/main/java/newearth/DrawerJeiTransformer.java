package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Hooks for JEI's transfer handler and its initial ingredient list. */
public final class DrawerJeiTransformer implements IClassTransformer,Opcodes {
    public byte[] transform(String name,String transformedName,byte[] bytes) {
        if (bytes != null && ("mezz.jei.plugins.vanilla.ingredients.ItemStackListFactory".equals(transformedName)
                || "mezz.jei.plugins.vanilla.ingredients.item.ItemStackListFactory".equals(transformedName)))
            return orderItems(bytes);
        if (bytes==null || !"mezz.jei.recipes.RecipeRegistry".equals(transformedName)) return bytes;
        ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);int patched=0;
        for (MethodNode method:node.methods) if (method.name.equals("getRecipeTransferHandler")
                && method.desc.endsWith("Lmezz/jei/api/recipe/transfer/IRecipeTransferHandler;")) {
            for (AbstractInsnNode instruction:method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode && ((MethodInsnNode)instruction).owner.equals("newearth/client/DrawerTransferJei")) return bytes;
                if (instruction.getOpcode()!=ARETURN) continue;
                InsnList hook=new InsnList();hook.add(new VarInsnNode(ALOAD,1));hook.add(new VarInsnNode(ALOAD,2));
                hook.add(new MethodInsnNode(INVOKESTATIC,"newearth/client/DrawerTransferJei","wrap",
                        "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",false));
                hook.add(new TypeInsnNode(CHECKCAST,"mezz/jei/api/recipe/transfer/IRecipeTransferHandler"));
                method.instructions.insertBefore(instruction,hook);patched++;
            }
        }
        if (patched==0) throw new IllegalStateException("Unsupported JEI recipe transfer registry");
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);return writer.toByteArray();
    }

    private static byte[] orderItems(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode method : node.methods) {
            if (!method.name.equals("create") || !method.desc.endsWith(")Ljava/util/List;")) continue;
            AbstractInsnNode[] instructions = method.instructions.toArray();
            for (AbstractInsnNode instruction : instructions)
                if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).owner.equals("newearth/ItemCatalogue"))
                    return bytes;
            for (AbstractInsnNode instruction : instructions) {
                if (instruction.getOpcode() == ARETURN)
                    method.instructions.insertBefore(instruction, new MethodInsnNode(INVOKESTATIC, "newearth/ItemCatalogue",
                            "orderJei", "(Ljava/util/List;)Ljava/util/List;", false));
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        }
        return bytes;
    }
}
