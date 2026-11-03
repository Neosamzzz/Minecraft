package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Prevent retired IDs from entering EP's registry, and skip JSON before item resolution. */
public final class ContentTransformer implements IClassTransformer, Opcodes {
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        boolean items = "com.mjr.extraplanets.items.ExtraPlanets_Items".equals(transformedName);
        boolean recipes = "net.minecraftforge.common.crafting.CraftingHelper".equals(transformedName);
        boolean meta = "com.mjr.mjrlegendslib.item.ItemBasicMeta".equals(transformedName);
        boolean entities = "com.mjr.mjrlegendslib.util.RegisterUtilities".equals(transformedName);
        boolean schematics = "micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry".equals(transformedName);
        boolean gear = "micdoodle8.mods.galacticraft.api.GalacticraftRegistry".equals(transformedName);
        if (!(items || recipes || meta || entities || schematics || gear)) return bytes;
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            if (gear && method.name.equals("addGearObject") && method.desc.equals(
                    "(ILmicdoodle8/mods/galacticraft/api/item/EnumExtendedInventorySlot;Ljava/lang/Object;)V")) {
                InsnList head = new InsnList(); LabelNode keep = new LabelNode(); head.add(new VarInsnNode(ALOAD, 2));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/ContentPruning", "allowGear", "(Ljava/lang/Object;)Z", false));
                head.add(new JumpInsnNode(IFNE, keep)); head.add(new InsnNode(RETURN));
                head.add(keep); head.add(new FrameNode(F_SAME, 0, null, 0, null)); method.instructions.insert(head); changes++;
            }
            if (entities && method.name.equals("registerNonMobEntity") && method.desc.equals(
                    "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/String;IIZ)V")) {
                InsnList head = new InsnList(); LabelNode keep = new LabelNode();
                head.add(new VarInsnNode(ALOAD, 2));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/ContentPruning", "allowEntityRegistration", "(Ljava/lang/Class;)Z", false));
                head.add(new JumpInsnNode(IFNE, keep)); head.add(new InsnNode(RETURN));
                head.add(keep); head.add(new FrameNode(F_SAME, 0, null, 0, null)); method.instructions.insert(head); changes++;
            }
            if (schematics && method.name.equals("registerSchematicRecipe") && method.desc.equals(
                    "(Lmicdoodle8/mods/galacticraft/api/recipe/ISchematicPage;)V")) {
                InsnList head = new InsnList(); LabelNode keep = new LabelNode(); head.add(new VarInsnNode(ALOAD, 0));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/ContentPruning", "allowSchematic",
                        "(Lmicdoodle8/mods/galacticraft/api/recipe/ISchematicPage;)Z", false));
                head.add(new JumpInsnNode(IFNE, keep)); head.add(new InsnNode(RETURN));
                head.add(keep); head.add(new FrameNode(F_SAME, 0, null, 0, null)); method.instructions.insert(head); changes++;
            }
            if (items && method.name.equals("registerItem")
                    && method.desc.equals("(Lnet/minecraft/item/Item;Ljava/lang/String;)V")) {
                InsnList head = new InsnList(); LabelNode keep = new LabelNode();
                head.add(new VarInsnNode(ALOAD, 0)); head.add(new VarInsnNode(ALOAD, 1));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/ContentPruning", "allowRegistration",
                        "(Lnet/minecraft/item/Item;Ljava/lang/String;)Z", false));
                head.add(new JumpInsnNode(IFNE, keep)); head.add(new InsnNode(RETURN));
                head.add(keep); head.add(new FrameNode(F_SAME, 0, null, 0, null)); method.instructions.insert(head); changes++;
            }
            if (recipes && method.name.equals("processConditions") && method.desc.equals(
                    "(Lcom/google/gson/JsonObject;Ljava/lang/String;Lnet/minecraftforge/common/crafting/JsonContext;)Z")) {
                InsnList head = new InsnList(); LabelNode keep = new LabelNode();
                head.add(new VarInsnNode(ALOAD, 0)); head.add(new VarInsnNode(ALOAD, 2));
                head.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraftforge/common/crafting/JsonContext", "getModId", "()Ljava/lang/String;", false));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/ContentPruning", "skipRecipe", "(Lcom/google/gson/JsonObject;Ljava/lang/String;)Z", false));
                head.add(new JumpInsnNode(IFEQ, keep)); head.add(new InsnNode(ICONST_0)); head.add(new InsnNode(IRETURN));
                head.add(keep); head.add(new FrameNode(F_SAME, 0, null, 0, null)); method.instructions.insert(head); changes++;
            }
            if (meta && (method.name.equals("getSubItems") || method.name.equals("func_150895_a"))
                    && method.desc.equals("(Lnet/minecraft/creativetab/CreativeTabs;Lnet/minecraft/util/NonNullList;)V")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction.getOpcode() == RETURN) {
                    InsnList tail = new InsnList(); tail.add(new VarInsnNode(ALOAD, 2));
                    tail.add(new MethodInsnNode(INVOKESTATIC, "newearth/ContentPruning", "filterSubItems", "(Ljava/util/List;)V", false));
                    method.instructions.insertBefore(instruction, tail);
                }
                changes++;
            }
        }
        if (changes != 1) throw new IllegalStateException("Unsupported content pruning target " + transformedName + ": " + changes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer); return writer.toByteArray();
    }
}
