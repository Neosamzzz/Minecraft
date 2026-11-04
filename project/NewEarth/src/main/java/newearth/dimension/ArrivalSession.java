package newearth.dimension;

import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/** Server-authoritative first-arrival sessions, persisted in the destination save. */
@Mod.EventBusSubscriber(modid = "newearth")
public final class ArrivalSession extends WorldSavedData {
    private static final String KEY = "newearth_arrival", HOLD = "NewEarthArrivalHold";
    private ArrivalLoadBudget load = new ArrivalLoadBudget(40, 60);
    private final Set<UUID> completed = new HashSet<>();
    private final Map<String, Group> groups = new LinkedHashMap<>();

    private static final class Group {
        String id;
        Set<UUID> members = new HashSet<>(), ready = new HashSet<>(), received = new HashSet<>();
        int cursor, ticks, descent;
        BlockPos landing, center;
        Set<ChunkPos> extra = new HashSet<>();
        Group(String id) { this.id = id; }
    }

    public ArrivalSession(String name) { super(name); }

    private static ArrivalSession data(WorldServer world) {
        ArrivalSession data = (ArrivalSession) world.getPerWorldStorage().getOrLoadData(ArrivalSession.class, KEY);
        if (data == null) { data = new ArrivalSession(KEY); world.getPerWorldStorage().setData(KEY, data); }
        return data;
    }

    private Group pending(UUID player) {
        for (Group group : groups.values()) if (group.members.contains(player)) return group;
        return null;
    }

    public static void enter(EntityPlayerMP player, boolean transition) {
        WorldServer world = player.getServerWorld();
        ArrivalSession saved = data(world);
        if (saved.completed.contains(player.getUniqueID())) {
            if (transition || player.getEntityData().getBoolean("NewEarthArrivalPending")) {
                restore(player);
                land(player, EndDimension.landing(world));
            }
            return;
        }
        Group group = saved.pending(player.getUniqueID());
        if (group == null) {
            group = new Group(UUID.randomUUID().toString());
            // Only an actual common mount joins players; unrelated server players stay independent.
            Entity ship = player.getLowestRidingEntity();
            for (EntityPlayerMP other : world.getMinecraftServer().getPlayerList().getPlayers()) {
                if (other.world == world && other.getLowestRidingEntity() == ship
                        && !saved.completed.contains(other.getUniqueID()) && saved.pending(other.getUniqueID()) == null)
                    group.members.add(other.getUniqueID());
            }
            group.members.add(player.getUniqueID());
            if (saved.groups.isEmpty()) saved.load = new ArrivalLoadBudget(40, 60);
            saved.groups.put(group.id, group);
            saved.markDirty();
        }
        player.getEntityData().setBoolean("NewEarthArrivalPending", true);
        // Entry handlers can run before the client is ready. The server repeats the state periodically.
        hold(player, world.getSpawnPoint(), world.getActualHeight() - 4);
        group.received.remove(player.getUniqueID());
        ArrivalNetwork.send(player, group.id, group.ready.contains(player.getUniqueID()) ? ArrivalNetwork.WAIT : ArrivalNetwork.POEM, group.landing);
    }

    public static int loadLevel(WorldServer world) { return data(world).load.level(System.nanoTime()); }

    public static void ready(EntityPlayerMP player, String id, boolean region) {
        if (!EndDimension.isDestination(player.world)) return;
        ArrivalSession saved = data(player.getServerWorld());
        Group group = saved.groups.get(id);
        if (group == null || !group.members.contains(player.getUniqueID())) return;
        if (region) {
            if (group.landing != null) group.received.add(player.getUniqueID());
        } else if (group.ready.add(player.getUniqueID())) {
            saved.markDirty();
            ArrivalNetwork.send(player, group.id, ArrivalNetwork.WAIT, group.landing);
        }
    }

    @SubscribeEvent public static void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || !EndDimension.isDestination(event.world)) return;
        WorldServer world = (WorldServer) event.world;
        ArrivalSession saved = data(world);
        if (saved.groups.isEmpty()) return;
        boolean active = false;
        for (EntityPlayerMP player : world.getMinecraftServer().getPlayerList().getPlayers())
            if (player.world == world && saved.pending(player.getUniqueID()) != null) { active = true; break; }
        if (!active) {
            for (Group group : saved.groups.values()) releaseChunks(world, group, true);
            saved.load = new ArrivalLoadBudget(40, 60);
            return;
        }
        long now = System.nanoTime();
        net.minecraft.server.MinecraftServer server = world.getMinecraftServer();
        long previousTick = server.tickTimeArray[Math.floorMod(server.getTickCounter() - 1, server.tickTimeArray.length)];
        saved.load.sample(now, previousTick == 0 ? 20 : previousTick / 1_000_000.0);
        // Slow ramp with a world-wide budget; a long tick pauses extra generation before retrying.
        long deadline = now + saved.load.nanos(now);
        int remaining = saved.load.attempts(now);
        // Rotate independent groups so one viewer cannot monopolize the shared generation budget.
        if (saved.groups.size() > 1) {
            String first = saved.groups.keySet().iterator().next();
            Group head = saved.groups.remove(first);
            saved.groups.put(first, head);
        }
        Iterator<Group> iterator = saved.groups.values().iterator();
        while (iterator.hasNext()) {
            Group group = iterator.next();
            List<EntityPlayerMP> online = new ArrayList<>();
            for (UUID member : group.members) {
                EntityPlayerMP player = world.getMinecraftServer().getPlayerList().getPlayerByUUID(member);
                if (player != null && player.world == world) online.add(player);
            }
            if (online.isEmpty()) { releaseChunks(world, group, true); continue; }
            group.ticks++;
            if (group.center == null) group.center = world.getSpawnPoint();
            BlockPos center = group.center;
            // Near-to-far traversal continues through poem, waiting and descent, without a radius cap.
            while (remaining-- > 0 && System.nanoTime() < deadline) {
                BlockPos offset = preparationOffset(group.cursor++);
                int x = (center.getX() >> 4) + offset.getX(), z = (center.getZ() >> 4) + offset.getZ();
                if (!world.getWorldBorder().contains(new BlockPos(x * 16, 64, z * 16))) continue;
                boolean loaded = world.getChunkProvider().getLoadedChunk(x, z) != null;
                world.getChunkProvider().provideChunk(x, z);
                if (!loaded && !world.getPlayerChunkMap().contains(x, z)) group.extra.add(new ChunkPos(x, z));
            }
            if (group.landing == null && group.cursor >= 121) group.landing = EndDimension.landing(world);
            if (group.ticks % 100 == 0) { saved.markDirty(); releaseChunks(world, group, false); }
            boolean ready = group.landing != null;
            for (EntityPlayerMP player : online) ready &= group.ready.contains(player.getUniqueID())
                    && group.received.contains(player.getUniqueID());
            if (ready && group.descent == 0) {
                // Recheck the natural site after population; the vertical path must see the sky.
                group.landing = EndDimension.landing(world);
                group.descent = 1;
                for (EntityPlayerMP player : online) ArrivalNetwork.send(player, group.id, ArrivalNetwork.RELEASE, group.landing);
            }
            for (EntityPlayerMP player : online) {
                if (group.descent > 0) {
                    double y = group.landing.getY() + Math.max(0, 40 - group.descent);
                    hold(player, group.landing, y);
                    if (group.ticks % 20 == 0) ArrivalNetwork.send(player, group.id, ArrivalNetwork.RELEASE, group.landing);
                } else {
                    hold(player, group.landing == null ? center : group.landing, world.getActualHeight() - 4);
                    if (group.ticks % 20 == 0) ArrivalNetwork.send(player, group.id,
                            group.ready.contains(player.getUniqueID()) ? ArrivalNetwork.WAIT : ArrivalNetwork.POEM, group.landing);
                }
            }
            if (group.descent > 0 && ++group.descent > 41) {
                saved.completed.addAll(group.members);
                saved.markDirty();
                for (EntityPlayerMP player : online) {
                    restore(player); land(player, group.landing);
                    ArrivalNetwork.send(player, group.id, ArrivalNetwork.LANDED, group.landing);
                }
                releaseChunks(world, group, true);
                org.apache.logging.log4j.LogManager.getLogger("NewEarth").info(
                        "Arrival landed; extra preparation stopped after {} chunk positions", group.cursor);
                iterator.remove();
            }
        }
    }

    /** Square spiral, starting at the center; each complete ring covers every nearby chunk. */
    public static BlockPos preparationOffset(int index) {
        if (index == 0) return BlockPos.ORIGIN;
        int radius = (int) ((Math.sqrt(index) + 1) / 2);
        int side = radius * 2;
        long start = (long) (side - 1) * (side - 1);
        int offset = (int) (index - start);
        if (offset < side) return new BlockPos(radius, 0, -radius + 1 + offset);
        if (offset < side * 2) return new BlockPos(radius - 1 - (offset - side), 0, radius);
        if (offset < side * 3) return new BlockPos(-radius, 0, radius - 1 - (offset - side * 2));
        return new BlockPos(-radius + 1 + (offset - side * 3), 0, -radius);
    }

    private static void releaseChunks(WorldServer world, Group group, boolean all) {
        if (group.center == null) return;
        BlockPos offset = preparationOffset(Math.max(0, group.cursor - 1));
        int radius = Math.max(Math.abs(offset.getX()), Math.abs(offset.getZ()));
        Iterator<ChunkPos> iterator = group.extra.iterator();
        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();
            Chunk chunk = world.getChunkProvider().getLoadedChunk(pos.x, pos.z);
            int distance = Math.max(Math.abs(pos.x - (group.center.getX() >> 4)), Math.abs(pos.z - (group.center.getZ() >> 4)));
            if (chunk == null || world.getPlayerChunkMap().contains(pos.x, pos.z)) { iterator.remove(); continue; }
            // Keep the generation frontier for adjacent population, then let vanilla save/unload it.
            if (all || (distance < radius - 1 && chunk.isTerrainPopulated())) {
                world.getChunkProvider().queueUnload(chunk);
                iterator.remove();
            }
        }
    }

    public static final class ReplayCommand extends net.minecraft.command.CommandBase {
        @Override public String getName() { return "neweartharrival"; }
        @Override public String getUsage(net.minecraft.command.ICommandSender sender) { return "/neweartharrival"; }
        @Override public int getRequiredPermissionLevel() { return 2; }
        @Override public void execute(net.minecraft.server.MinecraftServer server,
                net.minecraft.command.ICommandSender sender, String[] args) throws net.minecraft.command.CommandException {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            if (!EndDimension.isDestination(player.world))
                throw new net.minecraft.command.CommandException("请先进入终章维度");
            ArrivalSession saved = data(player.getServerWorld());
            if (saved.pending(player.getUniqueID()) != null)
                throw new net.minecraft.command.CommandException("抵达流程正在进行中");
            saved.completed.remove(player.getUniqueID());
            saved.markDirty();
            enter(player, false);
        }
    }

    private static void hold(EntityPlayerMP player, BlockPos at, double y) {
        if (player.isRiding()) player.dismountRidingEntity();
        if (!player.getEntityData().hasKey(HOLD)) {
            NBTTagCompound old = new NBTTagCompound();
            old.setBoolean("AllowFlight", player.capabilities.allowFlying);
            old.setBoolean("Flying", player.capabilities.isFlying);
            old.setBoolean("NoGravity", player.hasNoGravity());
            player.getEntityData().setTag(HOLD, old);
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
        }
        player.setNoGravity(true);
        player.motionX = player.motionY = player.motionZ = 0;
        player.fallDistance = 0;
        if (player.getDistanceSq(at.getX() + .5, y, at.getZ() + .5) > .01)
            player.setPositionAndUpdate(at.getX() + .5, y, at.getZ() + .5);
    }

    private static void land(EntityPlayerMP player, BlockPos at) {
        player.getEntityData().removeTag("NewEarthArrivalPending");
        player.setPositionAndUpdate(at.getX() + .5, at.getY(), at.getZ() + .5);
        player.motionX = player.motionY = player.motionZ = 0;
        player.fallDistance = 0;
    }

    public static void restore(EntityPlayerMP player) {
        if (!player.getEntityData().hasKey(HOLD)) return;
        NBTTagCompound old = player.getEntityData().getCompoundTag(HOLD);
        player.capabilities.allowFlying = old.getBoolean("AllowFlight");
        player.capabilities.isFlying = old.getBoolean("Flying");
        player.setNoGravity(old.getBoolean("NoGravity"));
        player.getEntityData().removeTag(HOLD);
        player.sendPlayerAbilities();
        player.fallDistance = 0;
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) restore((EntityPlayerMP) event.player);
    }
    @SubscribeEvent public static void attack(LivingAttackEvent event) {
        if (event.getEntityLiving().getEntityData().hasKey(HOLD)) event.setCanceled(true);
    }
    @SubscribeEvent public static void fall(LivingFallEvent event) {
        if (event.getEntityLiving().getEntityData().hasKey(HOLD)) event.setCanceled(true);
    }

    @Override public void readFromNBT(NBTTagCompound nbt) {
        completed.clear(); groups.clear();
        NBTTagList done = nbt.getTagList("Completed", 8);
        for (int i = 0; i < done.tagCount(); i++) completed.add(UUID.fromString(done.getStringTagAt(i)));
        NBTTagList sessions = nbt.getTagList("Sessions", 10);
        for (int i = 0; i < sessions.tagCount(); i++) {
            NBTTagCompound entry = sessions.getCompoundTagAt(i);
            Group group = new Group(entry.getString("Id"));
            group.cursor = Math.max(0, entry.getInteger("Cursor"));
            if (entry.hasKey("Center")) group.center = BlockPos.fromLong(entry.getLong("Center"));
            NBTTagList members = entry.getTagList("Members", 10);
            for (int j = 0; j < members.tagCount(); j++) {
                NBTTagCompound member = members.getCompoundTagAt(j);
                UUID id = member.getUniqueId("Player");
                group.members.add(id);
                if (member.getBoolean("Ready")) group.ready.add(id);
            }
            groups.put(group.id, group);
        }
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList done = new NBTTagList(), sessions = new NBTTagList();
        for (UUID id : completed) done.appendTag(new NBTTagString(id.toString()));
        for (Group group : groups.values()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("Id", group.id);
            entry.setInteger("Cursor", group.cursor);
            if (group.center != null) entry.setLong("Center", group.center.toLong());
            NBTTagList members = new NBTTagList();
            for (UUID id : group.members) {
                NBTTagCompound member = new NBTTagCompound();
                member.setUniqueId("Player", id); member.setBoolean("Ready", group.ready.contains(id));
                members.appendTag(member);
            }
            entry.setTag("Members", members); sessions.appendTag(entry);
        }
        nbt.setTag("Completed", done); nbt.setTag("Sessions", sessions);
        return nbt;
    }
}
