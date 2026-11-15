package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** JEI chooses vanilla and mod handlers in one place; wrapping here avoids plugin registration order conflicts. */
public final class DrawerJeiTransformer implements IClassTransformer,Opcodes {
    public byte[] transform(String name,String transformedName,byte[] bytes) {
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
}
