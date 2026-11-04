package newearth;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.api.world.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.*;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/** Registry, equipment and installed-provider checks without creating a world or save. */
public final class EnvironmentProbe implements Opcodes {
    private static int checks;
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }
    private static ItemStack piece(String id) { return new ItemStack(new Item().setRegistryName(id)); }
    private static ItemStack suit(String slot) { return piece("galaxyspace:space_suit_" + slot); }

    public static void main(String[] args) throws Exception {
        net.minecraft.launchwrapper.Launch.blackboard = new HashMap<>();
        net.minecraft.launchwrapper.Launch.blackboard.put("fml.deobfuscatedEnvironment", true);
        Bootstrap.register();
        com.google.gson.JsonObject filter = new com.google.gson.JsonParser().parse("{\"result\":{\"item\":\"extraplanets:tools\",\"data\":3},\"key\":{\"C\":{\"item\":\"extraplanets:cloth\",\"data\":0}}}").getAsJsonObject();
        check(!ContentPruning.skipRecipe(filter, "extraplanets"), "Retained machine filter keeps its recipe");
        check("minecraft:wool".equals(filter.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString())
                && filter.getAsJsonObject("key").getAsJsonObject("C").get("data").getAsInt() == 32767,
                "Machine filter accepts any wool instead of retired spacesuit cloth");
        ItemStack head = suit("head"), chest = suit("chest"), legs = suit("legs"), feet = suit("feet");
        check(EnvironmentHooks.gsSuit(head, chest, legs, feet), "Full GS suit protects");
        check(EnvironmentHooks.gsSuit(piece("galaxyspace:space_suit_light_head"), chest, legs, feet), "Light and ordinary GS pieces can mix");
        check(!EnvironmentHooks.gsSuit(head, chest, legs, ItemStack.EMPTY), "Missing boots must not protect");
        check(!EnvironmentHooks.gsSuit(feet, chest, legs, head), "Wrong slot must not protect");
        check(!EnvironmentHooks.gsSuit(piece("othermod:space_suit_head"), chest, legs, feet), "Matching path in another mod must not protect");
        check(!EnvironmentHooks.protectedByGS(null), "No actor is not protected");
        check(EnvironmentHooks.thermal(2) == 2 && EnvironmentHooks.thermal(-5) == -5, "Preserve ordinary GC-scale environments");
        for (float raw : new float[] {-150, -140, -130, -120, -115, 20, 40, 70, 80, 90, 100, 150}) {
            float adjusted = EnvironmentHooks.thermal(raw);
            check(Math.signum(adjusted) == Math.signum(raw), "Preserve thermal direction " + raw);
            check(adjusted == EnvironmentHooks.thermal(adjusted), "Thermal conversion is idempotent " + raw);
            check(thermallyStable(adjusted, 4), "GS T4 can normalise retained EP extremes " + raw);
        }
        check(thermallyStable(8, 3) && !thermallyStable(8, 2), "Saturn requires GS T3 across day and night");
        check(thermallyStable(-14, 4) && !thermallyStable(-14, 3), "Neptune extreme requires GS T4");
        Planet body = new Planet("environment_probe");
        AtmosphereInfo atmosphere = new AtmosphereInfo(true, true, false, -150, 2, 0.7F);
        atmosphere.composition.add(EnumAtmosphericGas.OXYGEN); body.setAtmosphere(atmosphere);
        EnvironmentHooks.adaptAtmosphere(body);
        check(body.atmosphere.thermalLevel() == -15, "Inherited provider atmosphere is converted");
        check(body.atmosphere.isBreathable() && body.atmosphere.hasPrecipitation()
                && body.atmosphere.windLevel() == 2 && body.atmosphere.relativeDensity() == 0.7F
                && body.atmosphere.isGasPresent(EnumAtmosphericGas.OXYGEN), "Atmosphere retains air, weather, wind and density");
        verifyPreDimensionMetadata();
        for (String id : new String[] {"tier0_space_suit_helmet", "tier4_space_suit_jetpack_chest", "tier1_un_prepared_space_suit_boots",
                "tier1_pressure_layer", "tier4_radiation_layer", "tier3_armor_layer", "thermal_cloth", "tier5_thermal_padding",
                "cloth", "gravity_controller", "module_items", "enviro_emergency_kit", "tier5_kit"})
            check(ContentPruning.retiredItem(id), "Retire EP protection item " + id);
        for (String id : new String[] {"iodide_salt", "potassium_iodide", "geiger_counter", "anti_radiation", "oxygen_tank_very_heavy_full"})
            check(!ContentPruning.retiredItem(id), "Retain useful environment processing " + id);
        exerciseHandler(head, chest, legs, feet);
        verifyInstalled();
        arrivalPersistence();
        System.out.println("NewEarth environment: " + checks + " checks passed (isolated; no world or survival session)");
    }

    private static void verifyPreDimensionMetadata() {
        // Provider type exists before GC registers the runtime dimension. It has
        // no constructor, so this also guards against accidental instantiation.
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V1_8, ACC_PUBLIC, "com/mjr/extraplanets/probe/MetadataProvider", null,
                "net/minecraft/world/WorldProviderSurface", null);
        writer.visitEnd();
        Class<? extends net.minecraft.world.WorldProvider> provider =
                define("com.mjr.extraplanets.probe.MetadataProvider", writer.toByteArray())
                        .asSubclass(net.minecraft.world.WorldProvider.class);
        Planet ep = new Planet("metadataep");
        ep.setDimensionInfo(-16, provider);
        ep.setAtmosphere(new AtmosphereInfo(false, false, false, -150, 0, 1));
        Planet other = new Planet("othermetadataep");
        other.setDimensionInfo(-9001, net.minecraft.world.WorldProviderSurface.class);
        other.setAtmosphere(new AtmosphereInfo(false, false, false, -150, 0, 1));
        Planet missing = new Planet("missingmetadataep");
        missing.setDimensionInfo(-9002, null);
        missing.setAtmosphere(new AtmosphereInfo(false, false, false, -150, 0, 1));
        micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry.registerPlanet(ep);
        micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry.registerPlanet(other);
        micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry.registerPlanet(missing);
        check(!net.minecraftforge.common.DimensionManager.isDimensionRegistered(-16), "EP dimension is not registered at post-init fixture");
        EnvironmentHooks.adaptMetadata();
        check(ep.atmosphere.thermalLevel() == -15, "EP metadata adapts before runtime dimension registration");
        check(!net.minecraftforge.common.DimensionManager.isDimensionRegistered(-16), "Metadata adaptation never creates a runtime dimension");
        check(other.atmosphere.thermalLevel() == -150 && missing.atmosphere.thermalLevel() == -150,
                "Other-mod and missing provider types are skipped");
        EnvironmentHooks.adaptMetadata();
        check(ep.atmosphere.thermalLevel() == -15, "Post-init metadata adaptation remains idempotent");
    }

    // Normalisation/damage cadence from GS's GCPlayerHandler overwrite. Equality
    // means a complete set can cancel each thermal step rather than accumulate heat.
    private static boolean thermallyStable(float raw, float strength) {
        float temperature = Math.abs(raw);
        int base = (int) Math.floor(200 / temperature);
        int normalise = Math.max(1, (int) Math.floor(Math.floor(150 / strength) / temperature
                * (Math.max(1, temperature / strength) / 1.5F)));
        int harmful = Math.max(1, (int) Math.floor(base * 5 / (temperature / Math.max(1, strength / 2))));
        return harmful >= normalise;
    }

    public static final class HandlerFixture {
        public static int exposed, commands;
        public boolean isValidSpaceSuit(EntityPlayer player, ItemStack a, ItemStack b, ItemStack c, ItemStack d, List<String> list, boolean pressure) { return false; }
        public void checkRadiation(LivingEvent.LivingUpdateEvent event, EntityPlayerMP player, int amount, List<String> list) { exposed++; }
        public void onCommandEvent(CommandEvent event) { commands++; }
    }

    private static void exerciseHandler(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) throws Exception {
        EnvironmentTransformer transformer = new EnvironmentTransformer();
        String name = "newearth.EnvironmentProbe$HandlerFixture";
        byte[] patched;
        try (InputStream input = EnvironmentProbe.class.getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
            patched = transformer.transform(name, "com.mjr.extraplanets.handlers.MainHandlerServer", read(input));
        }
        Class<?> type = define(name, patched); Object handler = type.newInstance();
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        EntityPlayerMP player = (EntityPlayerMP) ((sun.misc.Unsafe) field.get(null)).allocateInstance(EntityPlayerMP.class);
        player.inventory = new InventoryPlayer(player);
        player.inventory.armorInventory.set(3, head); player.inventory.armorInventory.set(2, chest);
        player.inventory.armorInventory.set(1, legs); player.inventory.armorInventory.set(0, feet);
        java.lang.reflect.Method valid = type.getMethod("isValidSpaceSuit", EntityPlayer.class, ItemStack.class, ItemStack.class,
                ItemStack.class, ItemStack.class, List.class, boolean.class);
        check((Boolean) valid.invoke(handler, player, head, chest, legs, feet, Collections.emptyList(), true), "Transformed pressure check accepts GS suit");
        java.lang.reflect.Method radiation = type.getMethod("checkRadiation", LivingEvent.LivingUpdateEvent.class, EntityPlayerMP.class, int.class, List.class);
        radiation.invoke(handler, null, player, 100, Collections.emptyList());
        check(type.getField("exposed").getInt(null) == 0, "GS suit bypasses EP exposure and accumulated-radiation damage branch");
        player.inventory.armorInventory.set(0, ItemStack.EMPTY);
        radiation.invoke(handler, null, player, 100, Collections.emptyList());
        check(type.getField("exposed").getInt(null) == 1, "Missing equipment still uses EP exposure branch");
        type.getMethod("onCommandEvent", CommandEvent.class).invoke(handler, new Object[] {null});
        check(type.getField("commands").getInt(null) == 0, "EP emergency-kit command injection is removed");
    }

    private static void verifyInstalled() throws Exception {
        String runtime = System.getenv("NEWEARTH_RUNTIME_MODS");
        if (runtime == null || runtime.isEmpty()) { System.out.println("Installed EP environment checks skipped; set NEWEARTH_RUNTIME_MODS"); return; }
        boolean found = false; int thermalMethods = 0, handlerMethods = 0;
        for (File file : new File(runtime).listFiles()) if (file.getName().endsWith(".jar")) try (JarFile jar = new JarFile(file)) {
            if (jar.getJarEntry("com/mjr/extraplanets/ExtraPlanets.class") == null) continue;
            found = true; EnvironmentTransformer transformer = new EnvironmentTransformer();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement(); if (!entry.getName().endsWith(".class")) continue;
                String name = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                byte[] original; try (InputStream input = jar.getInputStream(entry)) { original = read(input); }
                byte[] patched = transformer.transform(name, name, original);
                if (patched == original) continue;
                ClassNode node = new ClassNode(); new ClassReader(patched).accept(node, ClassReader.EXPAND_FRAMES);
                for (MethodNode method : node.methods) {
                    if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) == 0) new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
                    if (method.name.equals("getThermalLevelModifier") && method.desc.equals("()F")) thermalMethods++;
                }
                if (name.endsWith("MainHandlerServer")) handlerMethods++;
                check(true, "Installed environment bytecode verified " + name);
                if (name.endsWith("WorldProviderJupiter")) executeThermal(node, 10, 9);
                if (name.endsWith("WorldProviderSaturn")) executeThermal(node, 8, 7);
                if (name.endsWith("WorldProviderUranus")) executeThermal(node, -12, -11.5F);
                if (name.endsWith("WorldProviderNeptune") || name.endsWith("WorldProviderTriton")) executeThermal(node, -14, -13);
            }
        }
        check(found && thermalMethods >= 10 && handlerMethods == 1, "Installed EP temperature and player handlers covered");
    }

    private static void executeThermal(ClassNode source, float day, float night) throws Exception {
        String name = "newearth/probe/Thermal" + source.name.substring(source.name.lastIndexOf('/') + 1);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        writer.visit(V1_8, ACC_PUBLIC, name, null, "java/lang/Object", null);
        writer.visitField(ACC_PUBLIC, "day", "Z", null, null).visitEnd();
        MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null); init.visitCode();
        init.visitVarInsn(ALOAD, 0); init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN); init.visitMaxs(0, 0); init.visitEnd();
        MethodVisitor daylight = writer.visitMethod(ACC_PUBLIC, "isDaytime", "()Z", null, null); daylight.visitCode();
        daylight.visitVarInsn(ALOAD, 0); daylight.visitFieldInsn(GETFIELD, name, "day", "Z");
        daylight.visitInsn(IRETURN); daylight.visitMaxs(0, 0); daylight.visitEnd();
        for (MethodNode method : source.methods) if (method.name.equals("getThermalLevelModifier") && method.desc.equals("()F")) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FrameNode) method.instructions.remove(instruction);
                else if (instruction instanceof FieldInsnNode && ((FieldInsnNode) instruction).name.equals("THERMAL_PADDINGS"))
                    method.instructions.set(instruction, new InsnNode(ICONST_1));
                else if (instruction instanceof MethodInsnNode) {
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (call.name.equals("isServer")) method.instructions.set(call, new InsnNode(ICONST_1));
                    else if (call.name.equals("isDaytime") || call.name.equals("func_76569_d")) { call.owner = name; call.name = "isDaytime"; }
                }
            }
            method.access = ACC_PUBLIC; method.accept(writer);
        }
        writer.visitEnd(); Class<?> type = define(name.replace('/', '.'), writer.toByteArray()); Object provider = type.newInstance();
        type.getField("day").setBoolean(provider, true);
        check((Float) type.getMethod("getThermalLevelModifier").invoke(provider) == day, "Installed day temperature " + source.name);
        type.getField("day").setBoolean(provider, false);
        check((Float) type.getMethod("getThermalLevelModifier").invoke(provider) == night, "Installed night temperature " + source.name);
    }

    private static void arrivalPersistence() {
        net.minecraft.nbt.NBTTagCompound input = new net.minecraft.nbt.NBTTagCompound();
        net.minecraft.nbt.NBTTagList done = new net.minecraft.nbt.NBTTagList();
        UUID finished = UUID.randomUUID(), waiting = UUID.randomUUID();
        done.appendTag(new net.minecraft.nbt.NBTTagString(finished.toString()));
        input.setTag("Completed", done);
        net.minecraft.nbt.NBTTagCompound group = new net.minecraft.nbt.NBTTagCompound();
        group.setString("Id", "saved-arrival");
        group.setInteger("Cursor", 6000);
        group.setLong("Center", new net.minecraft.util.math.BlockPos(-512, 70, 256).toLong());
        net.minecraft.nbt.NBTTagList members = new net.minecraft.nbt.NBTTagList();
        net.minecraft.nbt.NBTTagCompound member = new net.minecraft.nbt.NBTTagCompound();
        member.setUniqueId("Player", waiting); member.setBoolean("Ready", true); members.appendTag(member);
        group.setTag("Members", members);
        net.minecraft.nbt.NBTTagList groups = new net.minecraft.nbt.NBTTagList(); groups.appendTag(group);
        input.setTag("Sessions", groups);
        newearth.dimension.ArrivalSession saved = new newearth.dimension.ArrivalSession("probe");
        saved.readFromNBT(input);
        net.minecraft.nbt.NBTTagCompound output = saved.writeToNBT(new net.minecraft.nbt.NBTTagCompound());
        check(output.getTagList("Completed", 8).getStringTagAt(0).equals(finished.toString()), "Completed arrival survives save reload");
        net.minecraft.nbt.NBTTagCompound session = output.getTagList("Sessions", 10).getCompoundTagAt(0);
        check(session.getString("Id").equals("saved-arrival"), "Reconnect retains session identity");
        check(session.getInteger("Cursor") == 6000 && session.getLong("Center") == group.getLong("Center"), "Preparation frontier resumes across restart");
        net.minecraft.nbt.NBTTagCompound restored = session.getTagList("Members", 10).getCompoundTagAt(0);
        check(restored.getUniqueId("Player").equals(waiting) && restored.getBoolean("Ready"), "Offline ready participant survives restart without replay");
        Set<net.minecraft.util.math.BlockPos> spiral = new HashSet<>();
        boolean adjacent = true;
        net.minecraft.util.math.BlockPos previous = net.minecraft.util.math.BlockPos.ORIGIN;
        for (int i = 0; i < 6561; i++) {
            net.minecraft.util.math.BlockPos pos = newearth.dimension.ArrivalSession.preparationOffset(i);
            adjacent &= i == 0 || Math.abs(pos.getX() - previous.getX()) + Math.abs(pos.getZ() - previous.getZ()) == 1;
            spiral.add(pos); previous = pos;
            if (i == 120) check(spiral.size() == 121 && spiral.contains(new net.minecraft.util.math.BlockPos(-5, 0, -5))
                    && spiral.contains(new net.minecraft.util.math.BlockPos(5, 0, 5)), "Essential landing square is prepared first");
        }
        check(spiral.size() == 6561 && adjacent, "Preparation expands continuously without duplicate chunks");
        check(spiral.contains(new net.minecraft.util.math.BlockPos(-40, 0, -40))
                && spiral.contains(new net.minecraft.util.math.BlockPos(40, 0, 40)), "Preparation grows beyond ordinary 20-chunk view distance");
        newearth.dimension.ArrivalNetwork.State sent = new newearth.dimension.ArrivalNetwork.State();
        sent.session = "saved-arrival"; sent.dimension = 1020; sent.stage = 1;
        sent.landing = new net.minecraft.util.math.BlockPos(-129, 203, 514);
        io.netty.buffer.ByteBuf bytes = io.netty.buffer.Unpooled.buffer();
        try {
            sent.toBytes(bytes);
            newearth.dimension.ArrivalNetwork.State received = new newearth.dimension.ArrivalNetwork.State(); received.fromBytes(bytes);
            check(received.session.equals(sent.session) && received.dimension == 1020 && received.stage == 1
                    && received.landing.equals(sent.landing), "Arrival packet retains dimension, session and signed landing coordinates");
        } finally { bytes.release(); }
    }

    private static Class<?> define(String name, byte[] bytes) {
        return new ClassLoader(EnvironmentProbe.class.getClassLoader()) { Class<?> load() { return defineClass(name, bytes, 0, bytes.length); } }.load();
    }
    private static byte[] read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        return output.toByteArray();
    }
}
