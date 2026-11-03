package newearth.dimension;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** DH 3.3.3's 1.12 wrapper has three vanilla-height constants. */
public final class HeightTransformer implements IClassTransformer {
    private static final String TARGET="com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper";
    private static final String DESCRIPTOR="(Lnet/minecraft/world/chunk/Chunk;)I";
    @Override public byte[] transform(String name,String transformedName,byte[] input) {
        if(input==null || !TARGET.equals(transformedName))return input;
        ClassNode node=new ClassNode();new ClassReader(input).accept(node,0);int count=0;
        for(MethodNode method:node.methods) {
            if(!DESCRIPTOR.equals(method.desc) || (method.access & Opcodes.ACC_STATIC)==0)continue;
            String hook;
            switch(method.name) {
                case "getHeight":hook="top";break;
                case "getInclusiveMinBuildHeight":hook="min";break;
                case "getExclusiveMaxBuildHeight":hook="max";break;
                default:continue;
            }
            method.instructions.clear();method.tryCatchBlocks.clear();method.localVariables=null;
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"newearth/dimension/HeightHooks",hook,DESCRIPTOR,false));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));count++;
        }
        if(count!=3)throw new IllegalStateException("Unsupported DH ChunkWrapper: expected three height accessors");
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);return writer.toByteArray();
    }
}
