package newearth.worldgen;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;

/** Remembers that the main island already received its village. */
public final class IslandVillageData extends WorldSavedData {
    private static final String NAME = "newearth_islands";
    private boolean placed;

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

    public boolean isPlaced() {
        return placed;
    }

    public void setPlaced() {
        placed = true;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        placed = nbt.getBoolean("villagePlaced");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("villagePlaced", placed);
        return nbt;
    }
}
