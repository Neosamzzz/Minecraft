package newearth;

import galaxyspace.core.prefab.items.modules.ItemModule;
import galaxyspace.systems.SolarSystem.planets.overworld.items.modules.IonEngine;

/** Tier 6 only offers the ion engine, without the sublight conflict icon. */
public final class Tier6IonModule extends IonEngine {
    @Override public ItemModule[] getForrbidenModules() { return null; }
}
