package newearth;

import java.util.List;
import micdoodle8.mods.galacticraft.api.vector.BlockVec3;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import micdoodle8.mods.galacticraft.planets.asteroids.entities.EntityAstroMiner;
import micdoodle8.mods.galacticraft.planets.asteroids.tile.TileEntityMinerBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = NewEarth.MOD_ID)
public final class AstroMining {
    private static final String DISCARD = "NewEarthDiscard";
    private static final String FAULT = "NewEarthMinerFault";
    private static final int MAX_FILTERS = 12;
    private static final int FAULT_STATE_BASE = -20;

    private AstroMining() { }

    public static double movementSpeed(EntityAstroMiner miner, double speed) {
        return miner.AIstate == EntityAstroMiner.AISTATE_MINING ? speed * 2.5D : speed;
    }

    public static int nextSliceDelay(EntityAstroMiner miner) {
        return miner.AIstate == EntityAstroMiner.AISTATE_MINING ? 0 : 3;
    }

    public static void addShafts(EntityAstroMiner miner, List<BlockVec3> points, EnumFacing facing, BlockVec3 target) {
        AstroMiningRoutes.addAsteroidFaces(miner, points, facing, target);
    }

    public static void startRoute(TileEntityMinerBase dock, List<BlockVec3> points, EnumFacing facing) {
        AstroMiningRoutes.start(dock, points, facing);
    }

    public static void refillRoute(TileEntityMinerBase dock, List<BlockVec3> points, EnumFacing facing) {
        AstroMiningRoutes.refill(dock, points, facing);
    }

    public static void tickDock(TileEntityMinerBase dock) { AstroMiningChunks.tickDock(dock); }

    public static void releaseDock(TileEntityMinerBase dock) { AstroMiningChunks.releaseDock(dock); }

    public static void tickMiner(EntityAstroMiner miner, EnumFacing facing) { AstroMiningChunks.tickMiner(miner, facing, true); }

    public static void updateMinerFacing(EntityAstroMiner miner, EnumFacing facing) { AstroMiningChunks.tickMiner(miner, facing, false); }

    public static void releaseMiner(EntityAstroMiner miner) { AstroMiningChunks.releaseMiner(miner); }

    public static boolean unload(TileEntityMinerBase dock, ItemStack stack) {
        if (findSample(samples(dock), stack) < 0) return dock.addToInventory(stack);
        // GC treats an emptied stack as a completed unload and marks the miner dirty.
        stack.setCount(0);
        return true;
    }

    private static NBTTagList samples(TileEntityMinerBase dock) {
        return dock.getTileData().getTagList(DISCARD, Constants.NBT.TAG_COMPOUND);
    }

    static int findSample(NBTTagList samples, ItemStack stack) {
        for (int i = 0; i < samples.tagCount(); i++) {
            ItemStack sample = new ItemStack(samples.getCompoundTagAt(i));
            if (!sample.isEmpty() && ItemStack.areItemsEqual(sample, stack)
                    && ItemStack.areItemStackTagsEqual(sample, stack)) return i;
        }
        return -1;
    }

    @SubscribeEvent
    public static void configureDiscard(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != EnumHand.MAIN_HAND || !event.getEntityPlayer().isSneaking()) return;
        TileEntity tile = event.getWorld().getTileEntity(event.getPos());
        if (!(tile instanceof TileEntityMinerBase)) return;
        TileEntityMinerBase piece = (TileEntityMinerBase) tile;
        TileEntityMinerBase dock = piece.isMaster ? piece : piece.getMaster();
        if (dock == null) return;
        if (event.getWorld().isRemote) return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.util.EnumActionResult.SUCCESS);

        ItemStack held = event.getItemStack();
        NBTTagList samples = samples(dock);
        if (held.isEmpty()) {
            TextComponentTranslation message = new TextComponentTranslation(samples.tagCount() == 0
                    ? "newearth.miner.discard.empty" : "newearth.miner.discard.list");
            for (int i = 0; i < samples.tagCount(); i++) {
                message.appendText(i == 0 ? " " : ", ");
                ItemStack sample = new ItemStack(samples.getCompoundTagAt(i));
                message.appendSibling(new TextComponentString(sample.getDisplayName()).setStyle(sample.getTextComponent().getStyle()));
            }
            event.getEntityPlayer().sendMessage(message);
            return;
        }

        if (dock.linkedMinerID != null && (dock.linkedMiner == null
                || dock.linkedMiner.playerMP != event.getEntityPlayer()) && !event.getEntityPlayer().canUseCommand(2, "")) {
            event.getEntityPlayer().sendMessage(new TextComponentTranslation("newearth.miner.discard.denied"));
            return;
        }
        int index = findSample(samples, held);
        String key;
        if (index >= 0) {
            samples.removeTag(index);
            key = "newearth.miner.discard.removed";
        } else if (samples.tagCount() < MAX_FILTERS) {
            ItemStack sample = held.copy();
            sample.setCount(1);
            samples.appendTag(sample.writeToNBT(new NBTTagCompound()));
            key = "newearth.miner.discard.added";
        } else {
            event.getEntityPlayer().sendMessage(new TextComponentTranslation("newearth.miner.discard.full"));
            return;
        }
        dock.getTileData().setTag(DISCARD, samples);
        dock.markDirty();
        event.getEntityPlayer().sendMessage(new TextComponentTranslation(key).appendText(" ").appendSibling(held.getTextComponent()));
    }

    public static void recordFault(EntityAstroMiner miner, int cause) {
        miner.getEntityData().setInteger(FAULT, cause);
    }

    public static int dockState(EntityAstroMiner miner) {
        if (miner.AIstate != EntityAstroMiner.AISTATE_STUCK) return miner.AIstate;
        int cause = miner.getEntityData().getInteger(FAULT);
        return cause == 0 ? miner.AIstate : FAULT_STATE_BASE - cause;
    }

    public static String faultStatus(TileEntityMinerBase dock) {
        int cause = FAULT_STATE_BASE - dock.linkedMinerDataAIState;
        String key;
        switch (cause) {
            case EntityAstroMiner.FAIL_BASEDESTROYED: key = "base"; break;
            case EntityAstroMiner.FAIL_OUTOFENERGY: key = "energy"; break;
            case EntityAstroMiner.FAIL_RETURNPATHBLOCKED: key = "path"; break;
            case EntityAstroMiner.FAIL_ANOTHERWASLINKED: key = "link"; break;
            default: return null;
        }
        return "\u00a7c" + GCCoreUtil.translate("newearth.miner.fault." + key);
    }
}
