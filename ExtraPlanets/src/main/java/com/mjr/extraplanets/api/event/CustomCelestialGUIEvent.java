package com.mjr.extraplanets.api.event;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.fml.common.eventhandler.Event;

public class CustomCelestialGUIEvent extends Event {

	public CustomCelestialGUIEvent() {
	}

	public static class PreLoadingGalaxies extends CustomCelestialGUIEvent {
		public final List<String> solarSystemUnlocalizedNamesToIgnore = new ArrayList<>();
		public final List<String> solarSystemNamesToIgnore = new ArrayList<>();
		public final List<String> galaxiesUnlocalizedNamesToIgnore = new ArrayList<>();

		public PreLoadingGalaxies() {
		}
	}

	public static class PreLoadingCelestialBodies extends CustomCelestialGUIEvent {
		public final List<String> bodyUnlocalizedNamesToIgnore = new ArrayList<>();
		public final List<String> bodyNamesToIgnore = new ArrayList<>();

		public PreLoadingCelestialBodies() {
		}
	}
}