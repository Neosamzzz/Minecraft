package newearth.worldgen;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/** Remembers which islands already received their village. */
public final class IslandVillageData extends WorldSavedData {
    private static final String NAME = "newearth_islands";
    private final Set<Long> placed = new HashSet<Long>();

    public IslandVillageData() {
        super(NAME);
    }

    public IslandVillageData(String name) {
        super(name);
    }

    public static IslandVillageData get(World world) {
        WorldSavedData data = world.getPerWorldStorage().getOrLoadData(IslandVillageData.class, NAME);
        if (data == null) {
            data = new IslandVillageData(NAME);
            world.getPerWorldStorage().setData(NAME, data);
        }
        return (IslandVillageData) data;
    }

    public boolean isPlaced(long key) {
        return placed.contains(key);
    }

    public void markPlaced(long key) {
        placed.add(key);
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        placed.clear();
        NBTTagList list = nbt.getTagList("placed", 4);
        for (int i = 0; i < list.tagCount(); i++) {
            placed.add(((NBTTagLong) list.get(i)).getLong());
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Long key : placed) list.appendTag(new NBTTagLong(key));
        nbt.setTag("placed", list);
        return nbt;
    }
}
