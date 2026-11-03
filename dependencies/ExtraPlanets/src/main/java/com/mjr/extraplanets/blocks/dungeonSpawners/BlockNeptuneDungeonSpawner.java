package com.mjr.extraplanets.blocks.dungeonSpawners;

import com.mjr.extraplanets.Config;
import com.mjr.extraplanets.tileEntities.dungeonSpawners.TileEntityDungeonSpawnerNeptune;
import com.mjr.extraplanets.tileEntities.dungeonSpawners.TileEntityDungeonSpawnerNeptuneDefault;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import micdoodle8.mods.galacticraft.core.blocks.BlockBossSpawner;

public class BlockNeptuneDungeonSpawner extends BlockBossSpawner {
	public BlockNeptuneDungeonSpawner(String name) {
		super(name);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		if (Config.USE_DEFAULT_BOSSES)
			return new TileEntityDungeonSpawnerNeptuneDefault();
		else
			return new TileEntityDungeonSpawnerNeptune();
	}
}