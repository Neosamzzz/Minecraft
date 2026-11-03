package com.mjr.extraplanets.client.event;

import com.mjr.extraplanets.client.gui.screen.CustomCelestialSelection;

import net.minecraftforge.fml.common.eventhandler.Event;

public class ClientCustomCelestialGUIEvent extends Event {

	public ClientCustomCelestialGUIEvent() {
	}

	public static class PostRendering extends ClientCustomCelestialGUIEvent {
		public CustomCelestialSelection instance;

		public PostRendering(CustomCelestialSelection instance) {
			this.instance = instance;
		}
	}
}