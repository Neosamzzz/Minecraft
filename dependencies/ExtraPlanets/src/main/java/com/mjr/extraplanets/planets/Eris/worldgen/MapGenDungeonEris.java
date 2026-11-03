package com.mjr.extraplanets.planets.Eris.worldgen;

import net.minecraft.world.gen.structure.MapGenStructureIO;

import micdoodle8.mods.galacticraft.core.world.gen.dungeon.DungeonConfiguration;
import micdoodle8.mods.galacticraft.core.world.gen.dungeon.MapGenDungeon;

public class MapGenDungeonEris extends MapGenDungeon {
	private static boolean initialized;

	static {
		try {
			MapGenDungeonEris.initiateStructures();
		} catch (Throwable e) {

		}
	}

	public MapGenDungeonEris(DungeonConfiguration configuration) {
		super(configuration);
	}

	public static void initiateStructures() throws Throwable {
		if (!MapGenDungeonEris.initialized) {
			MapGenStructureIO.registerStructureComponent(RoomBossEris.class, "ErisDungeonBossRoom");
			MapGenStructureIO.registerStructureComponent(RoomTreasureEris.class, "ErisDungeonTreasureRoom");
		}

		MapGenDungeonEris.initialized = true;
	}
}
