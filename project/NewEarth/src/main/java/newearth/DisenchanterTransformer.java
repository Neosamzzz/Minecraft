package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Disenchanter 1.8 can recalculate previews while closing after its table was removed. */
public final class DisenchanterTransformer implements IClassTransformer,Opcodes {
    private static final String TARGET="de.impelon.disenchanter.DisenchantingUtils";
    private static final String DESCRIPTOR="(Lde/impelon/disenchanter/inventory/IDisenchantmentItemHandler;ZILde/impelon/disenchanter/DisenchantingProperties;FLjava/util/Random;)Lnet/minecraft/item/ItemStack;";
    public byte[] transform(String name,String transformedName,byte[] bytes) {
        if(bytes==null||!TARGET.equals(transformedName))return bytes;
        ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);
        for(MethodNode method:node.methods)if(method.name.equals("simulateDisenchantingInInventory")&&method.desc.equals(DESCRIPTOR)) {
            AbstractInsnNode first=nextCode(method.instructions.getFirst());
            AbstractInsnNode second=first==null?null:nextCode(first.getNext());
            if(first instanceof VarInsnNode&&first.getOpcode()==ALOAD&&((VarInsnNode)first).var==3
                    &&second!=null&&second.getOpcode()==IFNONNULL)return bytes;
            FieldInsnNode empty=null;
            for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof FieldInsnNode) {
                FieldInsnNode field=(FieldInsnNode)instruction;
                if(field.getOpcode()==GETSTATIC&&field.owner.equals("net/minecraft/item/ItemStack")
                        &&field.desc.equals("Lnet/minecraft/item/ItemStack;")){empty=field;break;}
            }
            if(empty==null)throw new IllegalStateException("Unsupported Disenchanter empty stack reference");
            LabelNode valid=new LabelNode();InsnList guard=new InsnList();
            guard.add(new VarInsnNode(ALOAD,3));guard.add(new JumpInsnNode(IFNONNULL,valid));
            guard.add(new FieldInsnNode(GETSTATIC,empty.owner,empty.name,empty.desc));guard.add(new InsnNode(ARETURN));
            guard.add(valid);guard.add(new FrameNode(F_SAME,0,null,0,null));method.instructions.insert(guard);
            ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);return writer.toByteArray();
        }
        throw new IllegalStateException("Unsupported Disenchanter inventory simulation signature");
    }
    private static AbstractInsnNode nextCode(AbstractInsnNode node) {
        while(node!=null&&node.getOpcode()<0)node=node.getNext();return node;
    }
}
