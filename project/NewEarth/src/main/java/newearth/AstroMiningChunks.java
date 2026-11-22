package newearth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityAstroMiner;
import micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityMinerBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Separate Forge tickets keep overlapping base and miner requests independent. */
@Mod.EventBusSubscriber(modid = NewEarth.MOD_ID)
public final class AstroMiningChunks {
    private static final String DOCK_POS = "AstroMinerDock";
    private static final Map<World, Map<BlockPos, Ticket>> DOCKS = new IdentityHashMap<>();
    private static final Map<World, Map<EntityAstroMiner, MinerTicket>> MINERS = new IdentityHashMap<>();

    private static final class MinerTicket {
        final Ticket ticket;
        int chunkX, chunkZ;
        EnumFacing facing;
        boolean initialized;

        MinerTicket(Ticket ticket) { this.ticket = ticket; }
    }

    private AstroMiningChunks() { }

    static void register(NewEarth mod) {
        ForgeChunkManager.setForcedChunkLoadingCallback(mod, AstroMiningChunks::restore);
    }

    static void tickDock(TileEntityMinerBase dock) {
        World world = dock.getWorld();
        if (world == null || world.isRemote || !dock.isMaster || dock.isInvalid()) return;
        Map<BlockPos, Ticket> tickets = DOCKS.computeIfAbsent(world, key -> new HashMap<>());
        BlockPos pos = dock.getPos();
        if (tickets.containsKey(pos) || world.getTotalWorldTime() % 20 != 0) return;
        Ticket ticket = ForgeChunkManager.requestTicket(NewEarth.instance, world, Type.NORMAL);
        if (ticket == null) return;
        ticket.getModData().setLong(DOCK_POS, pos.toLong());
        tickets.put(pos, ticket);
        retain(world, ticket, dockChunks(new ChunkPos(pos)));
    }

    static void tickMiner(EntityAstroMiner miner, EnumFacing facing, boolean create) {
        World world = miner.world;
        if (world == null || world.isRemote || miner.isDead) return;
        Map<EntityAstroMiner, MinerTicket> tickets = MINERS.computeIfAbsent(world, key -> new IdentityHashMap<>());
        MinerTicket state = tickets.get(miner);
        if (state == null) {
            if (!create || (miner.ticksExisted > 1 && miner.ticksExisted % 20 != 0)) return;
            Ticket ticket = ForgeChunkManager.requestTicket(NewEarth.instance, world, Type.ENTITY);
            if (ticket == null) return;
            ticket.bindEntity(miner);
            state = new MinerTicket(ticket);
            tickets.put(miner, state);
        }
        int x = MathHelper.floor(miner.posX) >> 4, z = MathHelper.floor(miner.posZ) >> 4;
        if (state.initialized && x == state.chunkX && z == state.chunkZ && facing == state.facing
                && miner.ticksExisted % 20 != 0) return;
        Set<ChunkPos> needed = minerChunks(new ChunkPos(x, z), facing);
        if (!state.ticket.getChunkList().equals(needed)) retain(world, state.ticket, needed);
        state.chunkX = x;
        state.chunkZ = z;
        state.facing = facing;
        state.initialized = true;
    }

    static void releaseDock(TileEntityMinerBase dock) {
        Map<BlockPos, Ticket> tickets = DOCKS.get(dock.getWorld());
        if (tickets == null || !dock.isMaster) return;
        Ticket ticket = tickets.remove(dock.getPos());
        if (ticket != null) ForgeChunkManager.releaseTicket(ticket);
    }

    static void releaseMiner(EntityAstroMiner miner) {
        Map<EntityAstroMiner, MinerTicket> tickets = MINERS.get(miner.world);
        if (tickets == null) return;
        MinerTicket state = tickets.remove(miner);
        if (state != null) ForgeChunkManager.releaseTicket(state.ticket);
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event) {
        DOCKS.remove(event.getWorld());
        MINERS.remove(event.getWorld());
    }

    static Set<ChunkPos> dockChunks(ChunkPos centre) {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        chunks.add(centre);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) chunks.add(new ChunkPos(centre.x + x, centre.z + z));
        }
        return chunks;
    }

    static Set<ChunkPos> minerChunks(ChunkPos centre, EnumFacing facing) {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        chunks.add(centre);
        if (facing != null && facing.getAxis().isHorizontal())
            chunks.add(new ChunkPos(centre.x + facing.getXOffset(), centre.z + facing.getZOffset()));
        return chunks;
    }

    private static void retain(World world, Ticket ticket, Set<ChunkPos> needed) {
        int limit = ticket.getChunkListDepth();
        if (limit > 0 && limit < needed.size()) {
            // Keep the anchor first even if the host configured a smaller ticket.
            Set<ChunkPos> bounded = new LinkedHashSet<>();
            for (ChunkPos chunk : needed) {
                if (bounded.size() == limit) break;
                bounded.add(chunk);
            }
            needed = bounded;
        }
        for (ChunkPos chunk : needed) {
            if (ticket.getChunkList().contains(chunk)) continue;
            ForgeChunkManager.forceChunk(ticket, chunk);
            world.getChunkProvider().provideChunk(chunk.x, chunk.z);
        }
        for (ChunkPos chunk : new ArrayList<>(ticket.getChunkList())) {
            if (!needed.contains(chunk)) ForgeChunkManager.unforceChunk(ticket, chunk);
        }
        // A full ticket may have evicted an anchor while switching chunks.
        for (ChunkPos chunk : needed) {
            if (!ticket.getChunkList().contains(chunk)) ForgeChunkManager.forceChunk(ticket, chunk);
        }
    }

    private static void restore(List<Ticket> saved, World world) {
        Map<BlockPos, Ticket> docks = DOCKS.computeIfAbsent(world, key -> new HashMap<>());
        Map<EntityAstroMiner, MinerTicket> miners = MINERS.computeIfAbsent(world, key -> new IdentityHashMap<>());
        for (Ticket ticket : saved) {
            if (ticket.getEntity() instanceof EntityAstroMiner) {
                EntityAstroMiner miner = (EntityAstroMiner) ticket.getEntity();
                if (miners.putIfAbsent(miner, new MinerTicket(ticket)) == null) {
                    tickMiner(miner, null, false);
                    continue;
                }
            } else if (ticket.getModData().hasKey(DOCK_POS)) {
                BlockPos pos = BlockPos.fromLong(ticket.getModData().getLong(DOCK_POS));
                world.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);
                TileEntity tile = world.getTileEntity(pos);
                if (tile instanceof TileEntityMinerBase && ((TileEntityMinerBase) tile).isMaster
                        && docks.putIfAbsent(pos, ticket) == null) {
                    retain(world, ticket, dockChunks(new ChunkPos(pos)));
                    continue;
                }
            }
            ForgeChunkManager.releaseTicket(ticket);
        }
    }
}
