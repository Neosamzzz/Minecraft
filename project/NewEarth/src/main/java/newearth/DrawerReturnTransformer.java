package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Runs before subclass workbenches clear their grids, and observes each inventory interaction. */
public final class DrawerReturnTransformer implements IClassTransformer,Opcodes {
    private static final String HOOK="newearth/storage/DrawerReturns";
    public byte[] transform(String name,String transformedName,byte[] bytes) {
        if(bytes==null||!"net.minecraft.inventory.Container".equals(transformedName))return bytes;
        ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);int patched=0;
        for(MethodNode method:node.methods) {
            boolean close=(method.name.equals("onContainerClosed")||method.name.equals("func_75134_a"))&&Type.getArgumentTypes(method.desc).length==1;
            boolean click=(method.name.equals("slotClick")||method.name.equals("func_184996_a"))&&Type.getArgumentTypes(method.desc).length==4;
            boolean detect=(method.name.equals("detectAndSendChanges")||method.name.equals("func_75142_b"))&&method.desc.equals("()V");
            if(!close&&!click&&!detect)continue;
            for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode&&((MethodInsnNode)instruction).owner.equals(HOOK))return bytes;
            InsnList start=new InsnList();start.add(new VarInsnNode(ALOAD,0));
            if(close)start.add(new VarInsnNode(ALOAD,1));
            start.add(new MethodInsnNode(INVOKESTATIC,HOOK,close?"close":"observe",close?"(Ljava/lang/Object;Ljava/lang/Object;)V":"(Ljava/lang/Object;)V",false));
            method.instructions.insert(start);
            if(click)for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction.getOpcode()==ARETURN) {
                InsnList end=new InsnList();end.add(new VarInsnNode(ALOAD,0));
                end.add(new MethodInsnNode(INVOKESTATIC,HOOK,"observe","(Ljava/lang/Object;)V",false));method.instructions.insertBefore(instruction,end);
            }
            patched++;
        }
        if(patched!=3)throw new IllegalStateException("Unsupported Container lifecycle for drawer returns: "+patched);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);return writer.toByteArray();
    }
}
