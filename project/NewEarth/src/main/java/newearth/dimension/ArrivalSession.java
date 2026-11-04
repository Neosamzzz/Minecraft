package newearth.dimension;

import java.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;
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
    private final Set<UUID> completed = new HashSet<>();
    private final Map<String, Group> groups = new LinkedHashMap<>();

    private static final class Group {
        String id;
        Set<UUID> members = new HashSet<>(), ready = new HashSet<>(), received = new HashSet<>();
        int cursor, ticks, descent;
        BlockPos landing;
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
            saved.groups.put(group.id, group);
            saved.markDirty();
        }
        player.getEntityData().setBoolean("NewEarthArrivalPending", true);
        // Entry handlers can run before the client is ready. The server repeats the state periodically.
        hold(player, world.getSpawnPoint(), world.getActualHeight() - 4);
        group.received.remove(player.getUniqueID());
        ArrivalNetwork.send(player, group.id, group.ready.contains(player.getUniqueID()) ? ArrivalNetwork.WAIT : ArrivalNetwork.POEM, group.landing);
    }

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
        // Share one bounded generation budget across groups in this world.
        long deadline = System.nanoTime() + 25_000_000L;
        Iterator<Group> iterator = saved.groups.values().iterator();
        while (iterator.hasNext()) {
            Group group = iterator.next();
            List<EntityPlayerMP> online = new ArrayList<>();
            for (UUID member : group.members) {
                EntityPlayerMP player = world.getMinecraftServer().getPlayerList().getPlayerByUUID(member);
                if (player != null && player.world == world) online.add(player);
            }
            if (online.isEmpty()) continue;
            group.ticks++;
            BlockPos center = world.getSpawnPoint();
            if (group.landing == null && group.cursor >= 121) group.landing = EndDimension.landing(world);
            if (group.landing == null) {
                int count = 0;
                while (group.cursor < 121 && count++ < 4 && System.nanoTime() < deadline) {
                    int index = group.cursor++;
                    world.getChunkProvider().provideChunk((center.getX() >> 4) + index % 11 - 5,
                            (center.getZ() >> 4) + index / 11 - 5);
                }
            }
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
                for (EntityPlayerMP player : online) { restore(player); land(player, group.landing); }
                iterator.remove();
            }
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
