package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Applies the reserve at Storage Drawers automation boundaries and manual bulk withdrawal. */
public final class DrawerTakeTransformer implements IClassTransformer, Opcodes {
    private static final String BLOCK = "com.jaquadro.minecraft.storagedrawers.block.BlockDrawers";
    private static final String DRAWER = "com/jaquadro/minecraft/storagedrawers/api/storage/IDrawer";
    private static final String TILE = "com/jaquadro/minecraft/storagedrawers/block/tile/TileEntityDrawers";
    private static final String HOOK = "newearth/storage/DrawerTransfer";
    private static final String AUTOMATION = "newearth/storage/DrawerAutomation";
    private static final String MOD = "com.jaquadro.minecraft.storagedrawers.";
    private static final String HANDLER = MOD + "capabilities.DrawerItemHandler";
    private static final String REPOSITORY = MOD + "capabilities.DrawerItemRepository";
    private static final String CONTROLLER_REPOSITORY = MOD + "block.tile.TileEntityController$ItemRepository";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        if (HANDLER.equals(transformedName) || REPOSITORY.equals(transformedName) || CONTROLLER_REPOSITORY.equals(transformedName))
            return reserveAutomation(bytes, transformedName);
        if (!BLOCK.equals(transformedName)) return bytes;
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
    private static byte[] reserveAutomation(byte[] bytes, String type) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            boolean extraction = method.name.equals("extractItem");
            boolean stockList = type.equals(REPOSITORY) && method.name.equals("getAllItems");
            boolean stockCount = type.equals(REPOSITORY) && method.name.equals("getStoredItemCount");
            boolean stockStack = type.equals(HANDLER) && method.name.equals("getStackInSlot");
            if (!extraction && !stockList && !stockCount && !stockStack) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (AUTOMATION.equals(call.owner)) return bytes;
                if (!DRAWER.equals(call.owner)) continue;

                String hook = null;
                String descriptor = null;
                if (extraction && call.name.equals("adjustStoredItemCount") && call.desc.equals("(I)I")) {
                    method.instructions.set(call, new MethodInsnNode(INVOKESTATIC, AUTOMATION, "extract",
                            "(Ljava/lang/Object;I)I", false));
                    changes++;
                    continue;
                }
                if ((extraction || stockList || stockCount) && call.name.equals("getStoredItemCount") && call.desc.equals("()I")) {
                    hook = "available";
                    descriptor = "(Ljava/lang/Object;I)I";
                } else if (stockList && call.name.equals("isEmpty") && call.desc.equals("()Z")) {
                    hook = "empty";
                    descriptor = "(Ljava/lang/Object;Z)Z";
                } else if (stockStack && call.name.equals("getPublicItemStack")
                        && call.desc.equals("()Lnet/minecraft/item/ItemStack;")) {
                    hook = "visibleStack";
                    descriptor = "(Ljava/lang/Object;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;";
                }
                if (hook != null) {
                    method.instructions.insertBefore(call, new InsnNode(DUP));
                    method.instructions.insert(call, new MethodInsnNode(INVOKESTATIC, AUTOMATION, hook, descriptor, false));
                    changes++;
                }
            }
        }
        int expected = type.equals(HANDLER) ? 4 : type.equals(REPOSITORY) ? 5 : 4;
        if (changes != expected) throw new IllegalStateException("Unsupported Storage Drawers automation: " + type + " (" + changes + ")");
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
