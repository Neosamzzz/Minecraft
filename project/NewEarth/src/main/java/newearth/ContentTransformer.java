package newearth;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Prevent retired IDs from entering EP's registry, and skip JSON before item resolution. */
public final class ContentTransformer implements IClassTransformer, Opcodes {
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        if ("com.mjr.extraplanets.recipes.ExtraPlanets_Recipes".equals(transformedName)
                || "com.mjr.extraplanets.ExtraPlanets".equals(transformedName))
            return unifyMaterials(bytes, transformedName);
        boolean items = "com.mjr.extraplanets.items.ExtraPlanets_Items".equals(transformedName);
        boolean recipes = "net.minecraftforge.common.crafting.CraftingHelper".equals(transformedName);
        boolean meta = "com.mjr.mjrlegendslib.item.ItemBasicMeta".equals(transformedName);
        boolean entities = "com.mjr.mjrlegendslib.util.RegisterUtilities".equals(transformedName);
        boolean schematics = "micdoodle8.mods.galacticraft.api.recipe.SchematicRegistry".equals(transformedName);
        boolean gear = "micdoodle8.mods.galacticraft.api.GalacticraftRegistry".equals(transformedName);
        boolean client = "com.mjr.mjrlegendslib.util.ClientUtilities".equals(transformedName);
        boolean blocks = "com.mjr.extraplanets.blocks.ExtraPlanets_Blocks".equals(transformedName);
        boolean storage = "com.mjr.extraplanets.blocks.planetAndMoonBlocks.BlockBasicSaturn".equals(transformedName)
                || "com.mjr.extraplanets.blocks.planetAndMoonBlocks.BlockBasicJupiter".equals(transformedName);
        boolean pad = "com.mjr.extraplanets.blocks.BlockCustomLandingPad".equals(transformedName);
        boolean jei = "mezz.jei.plugins.vanilla.ingredients.item.ItemStackListFactory".equals(transformedName);
        if (!(items || recipes || meta || entities || schematics || gear || client || blocks || storage || pad || jei)) return bytes;
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            if (items || blocks) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.owner.equals("com/mjr/mjrlegendslib/util/RegisterUtilities") && call.name.equals("registerOre")
                            && call.desc.equals("(Ljava/lang/String;Lnet/minecraft/item/ItemStack;)V")) {
                        call.owner = "newearth/MaterialUnification";
                        if (blocks) changes++;
                    }
                }
            }
            if (jei && method.name.equals("addItemStack") && method.desc.equals(
                    "(Lmezz/jei/startup/StackHelper;Lnet/minecraft/item/ItemStack;Ljava/util/List;Ljava/util/Set;)V")) {
                InsnList head = new InsnList(); LabelNode keep = new LabelNode();
                head.add(new VarInsnNode(ALOAD, 2));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/MaterialUnification", "visible", "(Lnet/minecraft/item/ItemStack;)Z", false));
                head.add(new JumpInsnNode(IFNE, keep)); head.add(new InsnNode(RETURN));
                head.add(keep); head.add(new FrameNode(F_SAME, 0, null, 0, null)); method.instructions.insert(head); changes++;
            }
            if (client && method.name.equals("addVariants") && method.desc.equals(
                    "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)V")) {
                InsnList head = new InsnList(); LabelNode keep = new LabelNode();
                head.add(new VarInsnNode(ALOAD, 0)); head.add(new VarInsnNode(ALOAD, 1));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/ContentPruning", "allowVariants",
                        "(Ljava/lang/String;Ljava/lang/String;)Z", false));
                head.add(new JumpInsnNode(IFNE, keep)); head.add(new InsnNode(RETURN));
                head.add(keep); head.add(new FrameNode(F_SAME, 0, null, 0, null));
                method.instructions.insert(head); changes++;
            }
            if (client && method.name.equals("registerModel") && method.desc.equals(
                    "(Lnet/minecraft/item/Item;ILnet/minecraft/client/renderer/block/model/ModelResourceLocation;)V")) {
                InsnList head = new InsnList(); LabelNode keep = new LabelNode(); head.add(new VarInsnNode(ALOAD, 0));
                head.add(new MethodInsnNode(INVOKESTATIC, "newearth/ContentPruning", "allowGear", "(Ljava/lang/Object;)Z", false));
                head.add(new JumpInsnNode(IFNE, keep)); head.add(new InsnNode(RETURN));
                head.add(keep); head.add(new FrameNode(F_SAME, 0, null, 0, null));
                method.instructions.insert(head); changes++;
            }
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
            if (((meta && (method.name.equals("getSubItems") || method.name.equals("func_150895_a")))
                    || ((storage || pad) && (method.name.equals("getSubBlocks") || method.name.equals("func_149666_a"))))
                    && method.desc.equals("(Lnet/minecraft/creativetab/CreativeTabs;Lnet/minecraft/util/NonNullList;)V")) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) if (instruction.getOpcode() == RETURN) {
                    InsnList tail = new InsnList(); tail.add(new VarInsnNode(ALOAD, 2));
                    tail.add(new MethodInsnNode(INVOKESTATIC, "newearth/ContentPruning", "filterSubItems", "(Ljava/util/List;)V", false));
                    method.instructions.insertBefore(instruction, tail);
                }
                changes++;
            }
        }
        // Forge's side transformer removes creative-list methods before us on a dedicated server.
        if ((storage || pad || meta) && changes == 0
                && net.minecraftforge.fml.relauncher.FMLLaunchHandler.side() == net.minecraftforge.fml.relauncher.Side.SERVER) return bytes;
        if (blocks ? changes == 0 : changes != (client ? 2 : 1)) throw new IllegalStateException("Unsupported content pruning target " + transformedName + ": " + changes);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer); return writer.toByteArray();
    }

    private static byte[] unifyMaterials(byte[] bytes, String name) {
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0);
        int changes = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == INVOKESPECIAL && call.name.equals("<init>")
                        && call.owner.equals("net/minecraft/item/ItemStack")
                        && (call.desc.equals("(Lnet/minecraft/item/Item;)V")
                            || call.desc.equals("(Lnet/minecraft/item/Item;I)V")
                            || call.desc.equals("(Lnet/minecraft/item/Item;II)V"))) {
                    method.instructions.insert(call, new MethodInsnNode(INVOKESTATIC,
                            "newearth/MaterialUnification", "canonical",
                            "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;", false));
                    changes++;
                }
                if (call.owner.equals("com/mjr/extraplanets/util/GCRecipeUtilities")
                        && call.name.equals("addCompresssorShapelessRecipe")
                        && call.desc.equals("(Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V")) {
                    call.owner = "newearth/MaterialUnification"; call.name = "addCompressor";
                }
            }
        }
        if (changes == 0) throw new IllegalStateException("Unsupported material registration target " + name);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer); return writer.toByteArray();
    }
}
