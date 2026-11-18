package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Adjusts the manual full-stack branch before Storage Drawers removes stock. */
public final class DrawerManualTakeTransformer implements IClassTransformer, Opcodes {
    private static final String BLOCK = "com.jaquadro.minecraft.storagedrawers.block.BlockDrawers";
    private static final String DRAWER = "com/jaquadro/minecraft/storagedrawers/api/storage/IDrawer";
    private static final String TILE = "com/jaquadro/minecraft/storagedrawers/block/tile/TileEntityDrawers";
    private static final String HOOK = "newearth/storage/DrawerTransfer";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !BLOCK.equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode method : node.methods) {
            if (!"extractItem".equals(method.name)) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode size = (MethodInsnNode) instruction;
                if (!DRAWER.equals(size.owner) || !"getStoredItemStackSize".equals(size.name) || !"()I".equals(size.desc)) continue;
                AbstractInsnNode next = size.getNext();
                while (next != null && next.getOpcode() < 0) next = next.getNext();
                if (!(next instanceof MethodInsnNode)) continue;
                MethodInsnNode take = (MethodInsnNode) next;
                if (!TILE.equals(take.owner) || !"takeItemsFromSlot".equals(take.name)
                        || !"(II)Lnet/minecraft/item/ItemStack;".equals(take.desc)) continue;

                // Keep a reference to the selected drawer while obtaining its normal stack size.
                method.instructions.insertBefore(size, new InsnNode(DUP));
                InsnList adjust = new InsnList();
                adjust.add(new InsnNode(SWAP));
                adjust.add(new MethodInsnNode(INVOKEINTERFACE, DRAWER, "getStoredItemCount", "()I", true));
                adjust.add(new MethodInsnNode(INVOKESTATIC, HOOK, "manualStackAmount", "(II)I", false));
                method.instructions.insert(size, adjust);
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                node.accept(writer);
                return writer.toByteArray();
            }
        }
        return bytes;
    }
}
