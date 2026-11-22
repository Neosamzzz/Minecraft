package newearth.compat.ae2;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Configuration merging and pinned UEL bytecode regression; no game or save is opened. */
public final class Ae2Probe {
    private enum Feature { CHANNELS, PROCESSORS, CRAFTING_TOASTS }
    private static int checks;
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label); checks++;
    }
    public static void main(String[] args) throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard = new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        java.lang.reflect.Field home = net.minecraftforge.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true); home.set(null, new File(".").getAbsoluteFile());
        Configuration config = new Configuration(new File("merge.cfg"));
        Property channels = config.get("features.networkfeatures", "Channels", true);
        Property processors = config.get("features.craftingcomponents", "Processors", false);
        Property toasts = config.get("features.craftingfeatures", "CraftingToasts", false);
        config.get("client", "showPlacementPreview", true).set(true);
        config.get("client", "SEARCH_MODE", "MANUAL_SEARCH").set("JEI_MANUAL_SEARCH");
        config.get("battery", "wirelessTerminal", 1600000).set(123456);
        config.save();
        byte[] originalConfig = Files.readAllBytes(Paths.get("merge.cfg"));
        check(!newearth.ContentTransformer.readAe2Feature(channels, true, Feature.CHANNELS), "Runtime closes channels");
        check(newearth.ContentTransformer.readAe2Feature(processors, true, Feature.PROCESSORS), "Runtime opens processors");
        check(!newearth.ContentTransformer.readAe2Feature(toasts, true, Feature.CRAFTING_TOASTS), "Runtime keeps the toast preference");
        config.save();
        check(Arrays.equals(originalConfig, Files.readAllBytes(Paths.get("merge.cfg"))),
                "Even a later AE2 save writes the same configuration values");
        Configuration reread = new Configuration(new File("merge.cfg")); reread.load();
        check(reread.get("features.networkfeatures", "Channels", false).getBoolean(), "File keeps its original channel setting");
        check(!reread.get("features.craftingcomponents", "Processors", true).getBoolean(), "File keeps its original processor setting");
        check(reread.get("client", "showPlacementPreview", false).getBoolean(), "File keeps its original preview setting");
        check(reread.get("client", "SEARCH_MODE", "").getString().equals("JEI_MANUAL_SEARCH"), "Existing user search setting preserved");
        check(reread.get("battery", "wirelessTerminal", 0).getInt() == 123456, "Existing battery settings preserved");
        Files.delete(Paths.get("merge.cfg"));
        for (int color = 0; color < 17; color++) {
            check(!Ae2Integration.retired("part", color), "Basic cable retained " + color);
            check(!Ae2Integration.retired("part", 20 + color), "Covered cable retained " + color);
            check(Ae2Integration.retired("part", 40 + color), "Smart cable retired " + color);
            check(Ae2Integration.retired("part", 60 + color), "Dense smart cable retired " + color);
            check(Ae2Integration.retired("part", 500 + color), "Dense covered cable retired " + color);
        }
        for (int meta : new int[]{220,240,260,340,341,360,380,440,480})
            check(!Ae2Integration.retired("part", meta), "Core storage and crafting part retained " + meta);
        check(Ae2Integration.retired("material",59), "Quantum wireless card retired");
        check(!Ae2Integration.retired("material",53), "Crafting card retained until replacement exists");
        check(Ae2Integration.retired("biometric_card",0), "Identity card removed from acquisition");
        byte[] absent = {1,2,3};
        newearth.ContentTransformer transformer = new newearth.ContentTransformer();
        check(transformer.transform("unrelated","unrelated",absent) == absent, "Optional mod absence leaves unrelated classes unchanged");
        check(transformer.transform("unrelated","unrelated",null) == null, "Absent class safe");
        String directory = System.getenv("NEWEARTH_RUNTIME_MODS");
        String explicitJar = System.getenv("NEWEARTH_AE2_JAR");
        if (directory == null && explicitJar == null) throw new AssertionError("Set NEWEARTH_AE2_JAR or NEWEARTH_RUNTIME_MODS");
        File runtimeJar = explicitJar != null ? new File(explicitJar) : Paths.get(directory,"ae2-uel-v0.56.6.jar").toFile();
        try (ZipFile jar = new ZipFile(runtimeJar)) {
            byte[] original = read(jar,"appeng/core/AEConfig.class");
            byte[] patched = transformer.transform("appeng.core.AEConfig","appeng.core.AEConfig",original);
            check(!Arrays.equals(original,patched), "Existing early feature hook patches pinned UEL");
            check(transformer.transform("appeng.core.AEConfig","appeng.core.AEConfig",patched) == patched, "Early feature hook is idempotent");
            ClassNode settings = node(patched);
            for (MethodNode method : settings.methods) if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0)
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(settings.name,method);
            MethodNode preview = settings.methods.stream().filter(m -> m.name.equals("showPlacementPreview") && m.desc.equals("()Z"))
                    .findFirst().get();
            check(preview.instructions.size() == 2 && preview.instructions.getFirst().getOpcode() == Opcodes.ICONST_0,
                    "Placement preview is disabled by its runtime getter");
            Set<String> features = new HashSet<>();
            for (FieldNode field : node(read(jar,"appeng/core/features/AEFeature.class")).fields) features.add(field.name);
            for (String feature : new String[]{"CHANNELS","QUANTUM_NETWORK_BRIDGE","QUANTUM_LINKING_CARD","SPATIAL_IO",
                    "CERTUS_ORE","CHARGED_CERTUS_ORE","CERTUS_QUARTZ_WORLD_GEN","METEORITE_WORLD_GEN","SPAWN_PRESSES_IN_METEORITES","METEORITE_COMPASS",
                    "SMART_CABLES","DENSE_CABLES","PORTABLE_CELL","MATTER_CANNON","CHARGED_STAFF","ENTROPY_MANIPULATOR",
                    "COLOR_APPLICATOR","PAINT_BALLS","POWER_GEN","PANELS"}) {
                check(features.contains(feature), "Retired feature exists in pinned version: " + feature);
                check(newearth.ContentTransformer.retiredAe2Feature(feature), "Selected feature disabled: " + feature);
            }
            for (String feature : new String[]{"STORAGE_CELLS","CRAFTING_CPU","PATTERNS","ENERGY_ACCEPTOR","WIRELESS_ACCESS_TERMINAL"})
                check(!newearth.ContentTransformer.retiredAe2Feature(feature), "Essential feature retained: " + feature);
            for (String recipe : new String[]{"tools/network_tool", "network/parts/terminals", "network/parts/terminals_interface"}) {
                String originalJson = new String(read(jar,"assets/appliedenergistics2/recipes/" + recipe + ".json"),"UTF-8");
                com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(originalJson).getAsJsonObject();
                String output = json.get("result").toString();
                Ae2Integration.rewriteRecipe(json,"appliedenergistics2");
                check(!json.toString().contains("itemIlluminatedPanel") && json.toString().contains("minecraft:glass"), "Actual core recipe survives panel removal: " + recipe);
                check(json.get("result").toString().equals(output), "Core recipe output unchanged: " + recipe);
                com.google.gson.JsonObject other = new com.google.gson.JsonParser().parse(originalJson).getAsJsonObject();
                Ae2Integration.rewriteRecipe(other,"other_mod");
                check(other.toString().contains("itemIlluminatedPanel"), "Unrelated mod recipes not rewritten");
            }
            for (String name : new String[]{"network/blocks/controller", "misc/grindstone_crank", "tools/paintballs_lumen_pink"}) {
                com.google.gson.JsonObject recipe = new com.google.gson.JsonParser().parse(new String(
                        read(jar, "assets/appliedenergistics2/recipes/" + name + ".json"), "UTF-8")).getAsJsonObject();
                check(Ae2Integration.removedMaterialOutput(recipe, "appliedenergistics2"),
                        "Retired recipe is skipped before ingredient lookup: " + name);
            }
            com.google.gson.JsonArray constants = new com.google.gson.JsonParser().parse(new String(
                    read(jar, "assets/appliedenergistics2/recipes/_constants.json"), "UTF-8")).getAsJsonArray();
            for (com.google.gson.JsonElement constant : constants) {
                com.google.gson.JsonObject definition = constant.getAsJsonObject();
                Ae2Integration.rewriteRecipe(definition, "appliedenergistics2");
                if ("appliedenergistics2:knife".equals(definition.get("name").getAsString())) {
                    com.google.gson.JsonArray knives = definition.getAsJsonArray("ingredient");
                    check(knives.size() == 1 && "appliedenergistics2:nether_quartz_cutting_knife".equals(
                            knives.get(0).getAsJsonObject().get("item").getAsString()),
                            "Constants only resolve the registered cutting knife");
                }
            }
            Set<String> originalKeys = keys(new String(read(jar, "assets/appliedenergistics2/lang/en_us.lang"), "UTF-8"));
            Path resource = Paths.get(System.getProperty("newearth.resourceRoot"));
            String chinese = new String(Files.readAllBytes(resource.resolve("assets/appliedenergistics2/lang/zh_cn.lang")), "UTF-8");
            Set<String> translated = keys(chinese);
            for (String key : translated) {
                if (key.endsWith(".newearth.desc")) {
                    check(originalKeys.contains(key.replace(".newearth.desc", ".name")), "Hover refers to an existing UEL item " + key);
                } else check(originalKeys.contains(key), "Name or GUI translation exists in UEL " + key);
            }
            for (String feature : new String[]{"INSCRIBER","PRINTED_CIRCUITS","PROCESSORS","STORAGE_MONITOR","PART_CONVERSION_MONITOR"})
                check(features.contains(feature) && !newearth.ContentTransformer.retiredAe2Feature(feature),
                        "Restored feature: " + feature);
            String[][] consumers = {
                {"network/parts/terminals", "logic"},
                {"network/parts/terminals_crafting", "calculation"},
                {"network/blocks/storage_drive", "engineering"}
            };
            for (String[] consumer : consumers) {
                com.google.gson.JsonObject recipe = new com.google.gson.JsonParser().parse(new String(read(jar,
                        "assets/appliedenergistics2/recipes/" + consumer[0] + ".json"), "UTF-8")).getAsJsonObject();
                Ae2Integration.rewriteRecipe(recipe, "appliedenergistics2");
                com.google.gson.JsonElement ingredients = recipe.has("key") ? recipe.get("key") : recipe.get("ingredients");
                check(ingredients.toString().contains("material." + consumer[1] + "_processor"),
                        "Original processor ingredient retained: " + consumer[0]);
            }
            check(!Ae2Integration.retired("part", 400) && !Ae2Integration.retired("part", 420)
                    && !Ae2Integration.retired("crafting_monitor", 0), "Three monitors remain obtainable");
            com.google.gson.JsonObject monitor = new com.google.gson.JsonParser().parse(new String(read(jar,
                    "assets/appliedenergistics2/recipes/network/parts/monitors_storage.json"), "UTF-8")).getAsJsonObject();
            Ae2Integration.rewriteRecipe(monitor, "appliedenergistics2");
            check(monitor.get("ingredients").toString().contains("material.silicon_print"), "Silicon plate has a monitor recipe");
            net.minecraft.init.Bootstrap.register();
            checkBusPorts(jar, transformer);
            try (java.net.URLClassLoader upstream = new java.net.URLClassLoader(
                    new java.net.URL[]{runtimeJar.toURI().toURL()}, Ae2Probe.class.getClassLoader())) {
                checkPressingRecipes(upstream);
                checkPairingPlayerSlots(upstream);
            }
            checkOreDefinitions(jar);
            checkPairingPermissions(jar,transformer);
            checkReplacementRecipes(resource);
            String loader = "appeng.recipes.AERecipeLoader";
            byte[] processing = read(jar, loader.replace('.', '/') + ".class");
            byte[] disabled = transformer.transform(loader, loader, processing);
            MethodNode entry = node(disabled).methods.stream()
                    .filter(m -> m.name.equals("loadProcessingRecipes") && m.desc.equals("()Z"))
                    .findFirst().get();
            check(entry.instructions.size() == 2 && entry.instructions.getFirst().getOpcode() == Opcodes.ICONST_0
                    && entry.instructions.getLast().getOpcode() == Opcodes.IRETURN,
                    "AE2's separate processing loader cannot register removed ingredients");
            new Analyzer<BasicValue>(new BasicVerifier()).analyze(loader, entry);
            String materialType = "appeng.items.materials.ItemMaterial";
            byte[] patchedMaterials = transformer.transform(materialType, materialType, read(jar, materialType.replace('.', '/') + ".class"));
            check(transformer.transform(materialType, materialType, patchedMaterials) == patchedMaterials, "Subtype filter installed once");
            for (MethodNode method : node(patchedMaterials).methods) if (method.name.equals("createMaterial"))
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(materialType, method);

            actualLocale(runtimeJar, resource);
        }
        System.out.println("AE2: " + checks + " checks passed (actual UEL v0.56.6; no game or save modified)");
    }

    private static void checkPressingRecipes(ClassLoader upstream) throws Exception {
        ItemStack gcSilicon = new ItemStack(Items.COAL, 1, 2);
        List<Object> recipes = Ae2Integration.pressingRecipes(upstream, gcSilicon, Items.STICK, true);
        check(recipes.size() == 7 && Ae2Integration.pressingRecipes(upstream, gcSilicon, Items.STICK, false).size() == 4,
                "Inscriber restores four boards and three optional processors");
        ItemStack[] inputs = {gcSilicon, new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.REDSTONE),
                new ItemStack(Items.QUARTZ), new ItemStack(Items.REDSTONE), new ItemStack(Items.DIAMOND), new ItemStack(Items.REDSTONE)};
        int[] outputs = {20, 18, 22, 16, 23, 17, 24};
        Class<?> type = recipes.get(0).getClass();
        for (int i = 0; i < recipes.size(); i++) {
            Object recipe = recipes.get(i);
            List<?> middle = (List<?>) type.getMethod("getInputs").invoke(recipe);
            List<?> top = (List<?>) type.getMethod("getTopInputs").invoke(recipe);
            List<?> bottom = (List<?>) type.getMethod("getBottomInputs").invoke(recipe);
            check(middle.size() == 1 && ItemStack.areItemStacksEqual(inputs[i], (ItemStack) middle.get(0)),
                    "Inscriber middle ingredient for output " + outputs[i]);
            check(ItemStack.areItemStacksEqual(new ItemStack(Items.STICK, 1, outputs[i]),
                    (ItemStack) type.getMethod("getOutput").invoke(recipe)), "Inscriber output " + outputs[i]);
            String mode = type.getMethod("getProcessType").invoke(recipe).toString();
            if (outputs[i] < 22) {
                check(top.isEmpty() && bottom.isEmpty(), "Board pressing has no mold inputs");
                check(mode.equals("INSCRIBE"), "Board pressing preserves a stray stack in a side slot");
            } else {
                check(mode.equals("PRESS"), "Processor assembly consumes both boards");
                check(top.size() == 1 && ItemStack.areItemStacksEqual(new ItemStack(Items.STICK, 1, outputs[i - 1]),
                        (ItemStack) top.get(0)), "Processor uses its board");
                check(bottom.size() == 1 && ItemStack.areItemStacksEqual(new ItemStack(Items.STICK, 1, 20),
                        (ItemStack) bottom.get(0)), "Processor uses a silicon plate");
            }
        }
    }
    private static void checkBusPorts(ZipFile jar, newearth.ContentTransformer transformer) throws Exception {
        for (String target : Arrays.asList("appeng.parts.automation.PartSharedItemBus",
                "appeng.fluids.parts.PartFluidImportBus", "appeng.fluids.parts.PartFluidExportBus")) {
            byte[] original = read(jar, target.replace('.', '/') + ".class");
            byte[] patched = transformer.transform(target, target, original);
            check(patched != original && transformer.transform(target, target, patched) == patched,
                    "Port routing is installed once: " + target);
            ClassNode type = node(patched);
            for (MethodNode method : type.methods)
                if (method.name.equals("getHandler") || method.name.equals("doBusWork"))
                    new Analyzer<BasicValue>(new BasicVerifier()).analyze(type.name, method);
        }

        net.minecraft.item.ItemStack[] stacks = {
                new net.minecraft.item.ItemStack(net.minecraft.init.Items.COAL, 4),
                new net.minecraft.item.ItemStack(net.minecraft.init.Items.IRON_INGOT, 3), ItemStack.EMPTY};
        int[] faceQueries = {0};
        boolean[] outputMoved = {false};
        net.minecraft.inventory.ISidedInventory inventory = (net.minecraft.inventory.ISidedInventory) java.lang.reflect.Proxy.newProxyInstance(
                Ae2Probe.class.getClassLoader(), new Class<?>[]{net.minecraft.inventory.ISidedInventory.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getSizeInventory": return stacks.length;
                        case "getInventoryStackLimit": return 64;
                        case "getSlotsForFace":
                            faceQueries[0]++;
                            return args[0] == net.minecraft.util.EnumFacing.DOWN ? new int[]{outputMoved[0] ? 2 : 1} : new int[]{0};
                        case "getStackInSlot": return stacks[(Integer) args[0]];
                        case "canExtractItem": return (Integer) args[0] != 0;
                        case "isItemValidForSlot":
                        case "canInsertItem": return (Integer) args[0] == 0 && ((net.minecraft.item.ItemStack) args[1]).getItem() == net.minecraft.init.Items.COAL;
                        case "decrStackSize":
                            ItemStack removed = stacks[(Integer) args[0]].splitStack((Integer) args[1]);
                            outputMoved[0] = true;
                            return removed;
                        case "setInventorySlotContents": stacks[(Integer) args[0]] = (net.minecraft.item.ItemStack) args[1]; return null;
                        case "markDirty": return null;
                        default: throw new AssertionError(method.getName());
                    }
                });
        net.minecraft.tileentity.TileEntity machine = new net.minecraft.tileentity.TileEntity() {
            @SuppressWarnings("unchecked")
            @Override public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,
                                                  net.minecraft.util.EnumFacing side) {
                if (net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY == capability)
                    return (T) new net.minecraftforge.items.wrapper.SidedInvWrapper(inventory, side);
                return null;
            }
        };
        net.minecraft.util.EnumFacing[] sides = net.minecraft.util.EnumFacing.VALUES;
        Ae2BusPorts.SidedItems intake = new Ae2BusPorts.SidedItems(machine, inventory, sides, true);
        for (int i = 0; i < 64; i++) check(intake.getStackInSlot(2).isEmpty(), "Unexposed physical slot stays hidden");
        check(faceQueries[0] == sides.length, "Repeated empty-slot scans use six face queries instead of 384");
        check(intake.getSlots() == 3 && intake.getStackInSlot(0).isEmpty(), "Input aliases stay hidden from the item collector");
        check(intake.extractItem(1, 6, true).getCount() == 3 && stacks[1].getCount() == 3,
                "Another face can simulate extraction from the one physical output slot");
        check(intake.extractItem(1, 2, false).getCount() == 2 && stacks[1].getCount() == 1 && stacks[0].getCount() == 4,
                "Actual extraction leaves the input untouched");
        stacks[2] = new ItemStack(Items.IRON_INGOT, 2);
        check(intake.getStackInSlot(1).isEmpty() && intake.extractItem(2, 2, true).getCount() == 2,
                "Committed transfer refreshes the sided layout before the next transfer");
        Ae2BusPorts.SidedItems feeder = new Ae2BusPorts.SidedItems(machine, inventory, sides, false);
        check(feeder.insertItem(1, stacks[0].copy(), true).getCount() == 4
                && feeder.insertItem(0, stacks[0].copy(), true).isEmpty(), "Delivery respects the machine's slot permissions");

        net.minecraftforge.fluids.FluidTank water = new net.minecraftforge.fluids.FluidTank(net.minecraftforge.fluids.FluidRegistry.WATER, 800, 1000);
        net.minecraftforge.fluids.FluidTank lava = new net.minecraftforge.fluids.FluidTank(net.minecraftforge.fluids.FluidRegistry.LAVA, 900, 1000);
        net.minecraftforge.fluids.FluidTank filter = new net.minecraftforge.fluids.FluidTank(net.minecraftforge.fluids.FluidRegistry.LAVA, 1, 1000);
        int[] fluidQueries = new int[2];
        net.minecraft.tileentity.TileEntity tanks = new net.minecraft.tileentity.TileEntity() {
            @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> capability,
                                                   net.minecraft.util.EnumFacing side) {
                fluidQueries[0]++;
                return side == net.minecraft.util.EnumFacing.NORTH || side == net.minecraft.util.EnumFacing.DOWN;
            }

            @SuppressWarnings("unchecked")
            @Override public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,
                                                  net.minecraft.util.EnumFacing side) {
                fluidQueries[1]++;
                return (T) (side == net.minecraft.util.EnumFacing.NORTH ? water : lava);
            }
        };
        net.minecraftforge.common.capabilities.Capability<?> fluidCapability =
                net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
        for (int i = 0; i < 256; i++) {
            net.minecraftforge.fluids.capability.IFluidHandler port =
                    Ae2BusPorts.fluidPort(tanks, fluidCapability, net.minecraft.util.EnumFacing.NORTH, null);
            check(port.fill(new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.WATER, 100), false) == 100,
                    "Repeated fluid delivery uses the preferred port");
        }
        check(fluidQueries[0] == 256 && fluidQueries[1] == 256, "Preferred success performs one capability check and one lookup per bus operation");
        Arrays.fill(fluidQueries, 0);
        net.minecraftforge.fluids.capability.IFluidHandler fluidPorts =
                Ae2BusPorts.fluidPort(tanks, fluidCapability, net.minecraft.util.EnumFacing.NORTH, filter);
        net.minecraftforge.fluids.FluidStack fluid = fluidPorts.drain(500, false);
        check(fluid != null && fluid.getFluid() == net.minecraftforge.fluids.FluidRegistry.LAVA
                && water.getFluidAmount() == 800 && lava.getFluidAmount() == 900, "Fluid filter selects an alternate output port without withdrawing");
        fluid.amount = 300;
        check(fluidPorts.drain(fluid, true).amount == 300 && lava.getFluidAmount() == 600 && water.getFluidAmount() == 800,
                "Commit drains the sampled port and only the accepted amount");
        check(fluidQueries[0] == 2 && fluidQueries[1] == 2, "Fallback checks only the needed faces and reuses the sampled handler on commit");
        check(fluidPorts.fill(new net.minecraftforge.fluids.FluidStack(net.minecraftforge.fluids.FluidRegistry.LAVA, 150), true) == 150
                && lava.getFluidAmount() == 750 && water.getFluidAmount() == 800, "Delivery chooses the port that accepts the fluid");
        check(fluidQueries[0] == 2 && fluidQueries[1] == 2, "Resolved ports reuse their handlers within the same bus operation");
    }

    private static void checkOreDefinitions(ZipFile jar) throws Exception {
        ClassNode blocks = node(read(jar,"appeng/core/api/definitions/ApiBlocks.class"));
        Map<String,Set<String>> guards = new HashMap<>(); String current = null;
        for (MethodNode method : blocks.methods) if (method.name.equals("<init>"))
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof LdcInsnNode && ((LdcInsnNode)instruction).cst instanceof String) {
                    current = (String)((LdcInsnNode)instruction).cst;
                    if (current.equals("quartz_ore") || current.equals("charged_quartz_ore")) guards.put(current,new HashSet<>());
                }
                if (instruction instanceof FieldInsnNode && guards.containsKey(current)) {
                    FieldInsnNode field = (FieldInsnNode)instruction;
                    if (field.owner.equals("appeng/core/features/AEFeature")) guards.get(current).add(field.name);
                }
            }
        check(guards.containsKey("quartz_ore") && guards.get("quartz_ore").contains("CERTUS_ORE"), "Actual quartz ore definition is guarded by the disabled registration feature");
        check(guards.containsKey("charged_quartz_ore") && guards.get("charged_quartz_ore").contains("CHARGED_CERTUS_ORE"), "Actual charged ore definition is guarded before block/item registration");
    }
    private static void checkPairingPermissions(ZipFile jar, newearth.ContentTransformer transformer) throws Exception {
        String type = "appeng.tile.misc.TileSecurityStation";
        byte[] original = read(jar,type.replace('.','/')+".class");
        byte[] patched = transformer.transform(type,type,original);
        check(!Arrays.equals(original,patched), "Actual permission provider disabled");
        check(transformer.transform(type,type,patched) == patched, "Permission hook is idempotent");
        ClassNode after = node(patched); boolean found = false;
        for (MethodNode method : after.methods) {
            if ((method.access & (Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE)) == 0)
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(after.name,method);
            if (method.name.equals("isSecurityEnabled")) {
                found = true; check(method.instructions.size()==2 && method.instructions.getFirst().getOpcode()==Opcodes.ICONST_0,"Pairing station cannot enable player permission enforcement");
            }
        }
        check(found,"Expected permission-provider method found");
        check(after.methods.stream().anyMatch(m->m.name.equals("getSecurityKey")),"Wireless binding identifier retained");
        boolean cacheUsesProvider = false;
        for (MethodNode method : node(read(jar,"appeng/me/cache/SecurityCache.class")).methods)
            if (method.name.equals("isAvailable")) for (AbstractInsnNode i : method.instructions.toArray())
                if (i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("isSecurityEnabled")) cacheUsesProvider=true;
        check(cacheUsesProvider,"Actual security grid consults the disabled provider");
    }
    private static void checkPairingPlayerSlots(ClassLoader upstream) throws Exception {
        net.minecraft.inventory.Container container = new net.minecraft.inventory.Container() {
            @Override public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) { return true; }
        };
        String prefix = "appeng.container.slot.";
        Class<?> handler = net.minecraftforge.items.IItemHandler.class;
        net.minecraftforge.items.IItemHandler player = new net.minecraftforge.items.ItemStackHandler(36);
        for (int index = 0; index < 36; index++) {
            String type = index < 9 ? "SlotPlayerHotBar" : "SlotPlayerInv";
            Class<?> slot = Class.forName(prefix + type, true, upstream);
            container.inventorySlots.add((net.minecraft.inventory.Slot) slot.getConstructor(handler, int.class, int.class, int.class)
                    .newInstance(player, index, 0, 0));
        }
        Class<?> restricted = Class.forName(prefix + "SlotRestrictedInput", true, upstream);
        Class<?> placeable = Class.forName(prefix + "SlotRestrictedInput$PlacableItemType", true, upstream);
        net.minecraftforge.items.IItemHandler encoder = new net.minecraftforge.items.ItemStackHandler(2);
        net.minecraft.inventory.Slot input = (net.minecraft.inventory.Slot) restricted
                .getConstructor(placeable, handler, int.class, int.class, int.class, net.minecraft.entity.player.InventoryPlayer.class)
                .newInstance(placeable.getField("ENCODABLE_ITEM").get(null), encoder, 0, 0, 0, null);
        net.minecraft.inventory.Slot output = (net.minecraft.inventory.Slot) Class.forName(prefix + "SlotOutput", true, upstream)
                .getConstructor(handler, int.class, int.class, int.class, int.class).newInstance(encoder, 1, 0, 0, -1);
        container.inventorySlots.add(input);
        container.inventorySlots.add(output);
        new GuiWirelessPairing(container);
        for (int index = 0; index < 36; index++) {
            net.minecraft.inventory.Slot slot = container.inventorySlots.get(index);
            check(slot.xPos == 8 + index % 9 * 18 && slot.yPos == (index < 9 ? 142 : 84 + (index / 9 - 1) * 18),
                    "Actual AE2 player slot lands on the matching inventory tile: " + index);
        }
        check(input.xPos == 53 && output.xPos == 107, "Pairing input and output remain in the encoder area");
    }
    private static void checkReplacementRecipes(Path resources) throws Exception {
        for (String name : new String[]{"certus_crystal","fluix_crystal","logic_processor","calculation_processor","engineering_processor"}) {
            check(!Files.exists(resources.resolve("assets/newearth/recipes/ae2_"+name+".json")), "Obsolete material recipe removed: "+name);
        }
    }
    private static void actualLocale(File ae2, Path resources) throws Exception {
        net.minecraft.client.resources.data.MetadataSerializer serializer = new net.minecraft.client.resources.data.MetadataSerializer();
        serializer.registerMetadataSectionType(new net.minecraft.client.resources.data.PackMetadataSectionSerializer(),
                net.minecraft.client.resources.data.PackMetadataSection.class);
        net.minecraft.client.resources.IResourcePack upstream = new net.minecraft.client.resources.FileResourcePack(ae2);
        net.minecraft.client.resources.data.PackMetadataSection metadata = upstream.getPackMetadata(serializer,"pack");
        if (metadata == null || metadata.getPackFormat() < 3) upstream = new net.minecraft.client.resources.LegacyV2Adapter(upstream);
        net.minecraft.client.resources.SimpleReloadableResourceManager manager = new net.minecraft.client.resources.SimpleReloadableResourceManager(serializer);
        manager.reloadResources(Arrays.asList(upstream,new net.minecraft.client.resources.FolderResourcePack(resources.toFile())));
        net.minecraft.client.resources.Locale locale = new net.minecraft.client.resources.Locale();
        locale.loadLocaleDataFiles(manager,Arrays.asList("en_us","zh_cn"));
        Properties expected = new Properties();
        try (Reader reader = Files.newBufferedReader(resources.resolve("assets/appliedenergistics2/lang/zh_cn.lang"),java.nio.charset.StandardCharsets.UTF_8)) { expected.load(reader); }
        for (String key : expected.stringPropertyNames())
            check(locale.formatMessage(key,new Object[0]).equals(expected.getProperty(key)), "Actual Minecraft Locale loads AE2 override: " + key);
        Set<String> ae2Keys = new HashSet<>(expected.stringPropertyNames());
        try (ZipFile jar = new ZipFile(ae2)) {
            Properties upstreamChinese = new Properties();
            upstreamChinese.load(new StringReader(new String(read(jar, "assets/appliedenergistics2/lang/zh_cn.lang"), "UTF-8")));
            for (String key : upstreamChinese.stringPropertyNames()) {
                if (key.matches("item\\.appliedenergistics2\\.multi_part\\..*(formation|annihilation)_plane\\.name")
                        || key.matches("gui\\.appliedenergistics2\\.(Fluid)?FormationPlane")) {
                    check(locale.formatMessage(key, new Object[0]).equals(upstreamChinese.getProperty(key)),
                            "Formation and annihilation panels keep their upstream wording: " + key);
                }
            }
            for (String language : Arrays.asList("en_us", "zh_cn")) {
                ae2Keys.addAll(keys(new String(read(jar, "assets/appliedenergistics2/lang/" + language + ".lang"), "UTF-8")));
            }
        }
        try (java.util.stream.Stream<String> lines = Files.lines(resources.resolve("assets/newearth/lang/zh_cn.lang"))) {
            lines.filter(line -> line.startsWith("newearth.ae2.") && line.contains("="))
                    .map(line -> line.substring(0, line.indexOf('='))).forEach(ae2Keys::add);
        }
        for (String key : ae2Keys)
            check(locale.formatMessage(key, new Object[0]).indexOf(0x4e0d) < 0,
                    "Merged Chinese AE2 text contains no disallowed wording: " + key);
    }
    private static Set<String> keys(String text) {
        Set<String> keys = new HashSet<>();
        for (String line : text.split("\\r?\\n")) if (!line.startsWith("#") && !line.startsWith("//") && line.contains("=")) {
            String key = line.substring(0,line.indexOf('='));
            check(keys.add(key), "No duplicate localization key " + key);
        }
        return keys;
    }
    private static byte[] read(ZipFile jar, String path) throws IOException {
        try (InputStream input = jar.getInputStream(jar.getEntry(path))) { return com.google.common.io.ByteStreams.toByteArray(input); }
    }
    private static ClassNode node(byte[] bytes) { ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node,0); return node; }
}
