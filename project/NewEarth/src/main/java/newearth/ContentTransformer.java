package newearth;

import java.util.EnumSet;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Apply pack content rules before optional mods register their features. */
public final class ContentTransformer implements IClassTransformer, Opcodes {
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) return null;
        if ("appeng.items.materials.ItemMaterial".equals(transformedName)) return filterAe2Materials(bytes);
        if ("appeng.core.AEConfig".equals(transformedName)) return filterAe2Features(bytes);
        if ("appeng.items.storage.AbstractStorageCell".equals(transformedName))
            return disableBooleanMethod(bytes, "disassembleDrive", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;)Z");
        if ("appeng.tile.misc.TileSecurityStation".equals(transformedName)) return disableBooleanMethod(bytes, "isSecurityEnabled", "()Z");
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

    public static boolean allowAe2Material(Enum<?> material) {
        String name = material.name();
        return name.equals("BLANK_PATTERN") || name.equals("WIRELESS_BOOSTER")
                || name.startsWith("CARD_") && !name.equals("CARD_QUANTUM_LINK");
    }

    private static byte[] filterAe2Materials(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode method : node.methods) if (method.name.equals("createMaterial")) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode && ((MethodInsnNode) instruction).name.equals("allowAe2Material")) return bytes;
                if (instruction.getOpcode() == ICONST_1 && instruction.getNext().getOpcode() == ISTORE) {
                    method.instructions.insertBefore(instruction, new VarInsnNode(ALOAD, 1));
                    method.instructions.set(instruction, new MethodInsnNode(INVOKESTATIC, "newearth/ContentTransformer",
                            "allowAe2Material", "(Ljava/lang/Enum;)Z", false));
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(writer);
                    return writer.toByteArray();
                }
            }
        }
        throw new IllegalStateException("Unsupported AE2 material registration");
    }

    private static byte[] filterAe2Features(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int replaced = 0;
        int installed = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("<init>") || !method.desc.equals("(Ljava/io/File;)V")) continue;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.owner.equals("java/util/EnumSet") && call.name.equals("add")
                        && call.desc.equals("(Ljava/lang/Object;)Z")
                        && call.getPrevious().getPrevious() instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) call.getPrevious().getPrevious();
                    if (!field.owner.equals(node.name) || !field.name.equals("featureFlags")) continue;
                    method.instructions.set(call, new MethodInsnNode(INVOKESTATIC, "newearth/ContentTransformer",
                            "addAe2Feature", "(Ljava/util/EnumSet;Ljava/lang/Enum;)Z", false));
                    replaced++;
                } else if (call.owner.equals("newearth/ContentTransformer") && call.name.equals("addAe2Feature")) {
                    installed++;
                }
            }
        }
        if (installed == 2 && replaced == 0) return bytes;
        if (replaced != 2 || installed != 0) throw new IllegalStateException("Unsupported AE2 feature initialization");
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] disableBooleanMethod(byte[] bytes, String name, String descriptor) {
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node,0);
        for (MethodNode method : node.methods) if (method.name.equals(name) && method.desc.equals(descriptor)) {
            if (method.instructions.size() == 2 && method.instructions.getFirst().getOpcode() == ICONST_0) return bytes;
            method.instructions.clear(); method.tryCatchBlocks.clear();
            if (method.localVariables != null) method.localVariables.clear();
            method.instructions.add(new InsnNode(ICONST_0)); method.instructions.add(new InsnNode(IRETURN));
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer); return writer.toByteArray();
        }
        throw new IllegalStateException("Unsupported AE2 method " + name);
    }

    public static <F extends Enum<F>> boolean addAe2Feature(EnumSet<F> features, F feature) {
        return !retiredAe2Feature(feature.name()) && features.add(feature);
    }

    public static boolean retiredAe2Feature(String feature) {
        switch (feature) {
            case "CERTUS":
            case "FLUIX":
            case "SILICON":
            case "DUSTS":
            case "NUGGETS":
            case "QUARTZ_GLASS":
            case "SKY_STONE":
            case "PURE_CRYSTALS":
            case "CRYSTAL_SEEDS":
            case "PROCESSORS":
            case "PRINTED_CIRCUITS":
            case "PRESSES":
            case "MATTER_BALL":
            case "CORES":
            case "GRIND_STONE":
            case "INSCRIBER":
            case "CRYSTAL_GROWTH_ACCELERATOR":
            case "DECORATIVE_BLOCKS":
            case "DECORATIVE_LIGHTS":
            case "SKY_STONE_CHESTS":
            case "FLOUR":
            case "TINY_TNT":
            case "IN_WORLD_FLUIX":
            case "IN_WORLD_PURIFICATION":
            case "IN_WORLD_SINGULARITY":
            case "ENABLE_DISASSEMBLY_CRAFTING":
            case "CONDENSER":
            case "CERTUS_QUARTZ_TOOLS":
            case "CERTUS_ORE":
            case "CHARGED_CERTUS_ORE":
            case "CERTUS_QUARTZ_WORLD_GEN":
            case "METEORITE_WORLD_GEN":
            case "SPAWN_PRESSES_IN_METEORITES":
            case "METEORITE_COMPASS":
            case "QUANTUM_NETWORK_BRIDGE":
            case "QUANTUM_LINKING_CARD":
            case "SPATIAL_IO":
            case "CHANNELS":
            case "SMART_CABLES":
            case "DENSE_CABLES":
            case "PORTABLE_CELL":
            case "MATTER_CANNON":
            case "CHARGED_STAFF":
            case "ENTROPY_MANIPULATOR":
            case "COLOR_APPLICATOR":
            case "PAINT_BALLS":
            case "POWER_GEN":
            case "PANELS":
            case "STORAGE_MONITOR":
            case "PART_CONVERSION_MONITOR":
                return true;
            default:
                return false;
        }
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
