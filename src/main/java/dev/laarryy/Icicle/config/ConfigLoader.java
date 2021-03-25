package dev.laarryy.Icicle.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;

public class ConfigLoader {

    private final Logger logger = LogManager.getLogger(this);

    public ConfigSettings loadConfig(final String configPath) throws ConfigurateException {
        final YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(Path.of(configPath))
                .build();

        final CommentedConfigurationNode node = loader.load();

        final ConfigSettings config;

        try {
            config = node.get(ConfigSettings.class);
        } catch (SerializationException e) {
            logger.error("Serialization Exception in loading config! Here it is: " + e.getMessage());
            System.exit(1);
            return null;
        }

        try {
            loader.save(node);
        } catch (final ConfigurateException e) {
            logger.error("There was an error saving the configuration! Here's what it is: " + e.getMessage());
            System.exit(1);
        }

        return config;
    }

}
