/* SPDX-License-Identifier: MIT; adapted from ReTerraForged. See META-INF/LICENSE-ReTerraForged.txt. */
package newearth.terrain.world.worldgen.cell.biome.type;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class BiomeTypeColors {
    private static final BiomeTypeColors INSTANCE = new BiomeTypeColors();
    private Map<String, Color> colors;
    
    private BiomeTypeColors() {
        this.colors = new HashMap<>();
        try (InputStream inputStream = BiomeType.class.getResourceAsStream("/newearth/terrain/biomes.txt")) {
            Properties properties = new Properties();
            properties.load(inputStream);
            for (Map.Entry<?, ?> entry : properties.entrySet()) {
                Color color = Color.decode("#" + entry.getValue().toString());
                this.colors.put(entry.getKey().toString(), color);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Color getColor(String name, Color defaultColor) {
        return this.colors.getOrDefault(name, defaultColor);
    }
    
    public static BiomeTypeColors getInstance() {
        return BiomeTypeColors.INSTANCE;
    }
    
}
