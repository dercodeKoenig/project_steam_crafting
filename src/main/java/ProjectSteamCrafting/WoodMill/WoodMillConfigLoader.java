package ProjectSteamCrafting.WoodMill;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class WoodMillConfigLoader {

    public static WoodMillConfig loadConfig() {
        String filename = "woodmill.json";
        Path configDir = Paths.get(FMLPaths.CONFIGDIR.get().toString(), "projectsteam_crafting");

        WoodMillConfig config = new WoodMillConfig();
        config.recipes = new ArrayList<>();

        Path filePath = configDir.resolve(filename);

        try {
            // Create the config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            // Create a default config file if it doesn't exist
            if (!Files.exists(filePath)) {
                Files.copy(WoodMillConfigLoader.class.getResourceAsStream("/assets/projectsteam_crafting/config/" + filename), filePath);
                System.out.println("Default config file created: " + filePath);
            }

            // Load JSON from the file
            String jsonContent = Files.readString(filePath);
            Gson gson = new Gson();
            config = gson.fromJson(jsonContent, WoodMillConfig.class);
        } catch (JsonSyntaxException e) {
            System.err.println("Failed to parse JSON: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return config;
    }
}
