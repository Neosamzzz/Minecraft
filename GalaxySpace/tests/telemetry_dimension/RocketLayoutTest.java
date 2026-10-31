import galaxyspace.core.telemetry.TelemetryRocketLayout;

public class RocketLayoutTest {
    public static void main(String[] args) {
        for (int tier = 4; tier <= 6; tier++) {
            float height = tier == 4 ? 10F : 11F;
            float offset = TelemetryRocketLayout.verticalOffset("galaxyspace.core.prefab.entities.EntityTier" + tier + "Rocket", height);
            if (height / 4F + offset != 0F) throw new AssertionError("Extra centring still applied: tier " + tier);
        }
        for (String name : new String[]{"micdoodle8.mods.galacticraft.core.entities.EntityTier1Rocket", "net.minecraft.entity.player.EntityPlayer", "galaxyspace.core.prefab.entities.EntityCustomCargoRocket"}) {
            if (TelemetryRocketLayout.verticalOffset(name, 10F) != 0F) throw new AssertionError("Unrelated entity moved");
        }
        System.out.println("ROCKET_LIFT_PASS: cancel only height/4 for GS tiers 4/5/6; other entities unchanged");
    }
}
