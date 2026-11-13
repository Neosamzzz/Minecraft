import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Minimal server fix for the pinned upstream 0.2 binary. */
public final class Patch implements Opcodes {
    public static void main(String[] args) throws Exception {
        int ageCalls = 0, defaults = 0;
        try (JarFile input = new JarFile(args[0]);
             JarOutputStream output = new JarOutputStream(Files.newOutputStream(Paths.get(args[1])))) {
            Enumeration<JarEntry> entries = input.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream stream = input.getInputStream(entry)) {
                    byte[] block = new byte[8192];
                    int n;
                    while ((n = stream.read(block)) != -1) buffer.write(block, 0, n);
                }
                byte[] bytes = buffer.toByteArray();
                if (entry.getName().equals("cool/muyucloud/saplanting/events/EntityItemEvent.class")) {
                    ClassNode node = new ClassNode();
                    new ClassReader(bytes).accept(node, 0);
                    for (MethodNode method : node.methods) {
                        for (AbstractInsnNode instruction : method.instructions.toArray()) {
                            if (!(instruction instanceof MethodInsnNode)) continue;
                            MethodInsnNode call = (MethodInsnNode) instruction;
                            if (call.owner.equals("net/minecraft/entity/item/EntityItem") && call.name.equals("func_174872_o") && call.desc.equals("()I")) {
                                method.instructions.set(call, new MethodInsnNode(INVOKESTATIC, node.name, "saplanting$getAge", "(Lnet/minecraft/entity/item/EntityItem;)I", false));
                                ageCalls++;
                            }
                        }
                    }
                    // Read the actual persisted age; ticksExisted is a separate counter.
                    MethodVisitor getter = node.visitMethod(ACC_PRIVATE | ACC_STATIC, "saplanting$getAge", "(Lnet/minecraft/entity/item/EntityItem;)I", null, null);
                    getter.visitCode();
                    getter.visitLdcInsn(Type.getObjectType("net/minecraft/entity/item/EntityItem"));
                    getter.visitVarInsn(ALOAD, 0);
                    getter.visitLdcInsn("age");
                    getter.visitLdcInsn("field_70292_b");
                    getter.visitMethodInsn(INVOKESTATIC, "net/minecraftforge/fml/relauncher/ReflectionHelper", "getPrivateValue", "(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false);
                    getter.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                    getter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                    getter.visitInsn(IRETURN);
                    getter.visitMaxs(4, 1);
                    getter.visitEnd();
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    node.accept(writer);
                    bytes = writer.toByteArray();
                } else if (entry.getName().equals("cool/muyucloud/saplanting/util/Config.class")) {
                    ClassNode node = new ClassNode();
                    new ClassReader(bytes).accept(node, 0);
                    for (MethodNode method : node.methods) {
                        for (AbstractInsnNode instruction : method.instructions.toArray()) {
                            if (instruction instanceof LdcInsnNode && "multiThread".equals(((LdcInsnNode) instruction).cst)) {
                                AbstractInsnNode next = instruction.getNext();
                                if (next != null && next.getOpcode() == ICONST_1) {
                                    method.instructions.set(next, new InsnNode(ICONST_0));
                                    defaults++;
                                }
                            }
                        }
                    }
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(writer);
                    bytes = writer.toByteArray();
                }
                JarEntry replacement = new JarEntry(entry.getName());
                replacement.setTime(0);
                output.putNextEntry(replacement);
                output.write(bytes);
                output.closeEntry();
            }
        }
        if (ageCalls != 2 || defaults != 1) {
            Files.deleteIfExists(Paths.get(args[1]));
            throw new IllegalStateException("Unexpected upstream binary: age=" + ageCalls + ", defaults=" + defaults);
        }
        System.out.println("Patched both age calls; multithreading disabled by default.");
    }
}
