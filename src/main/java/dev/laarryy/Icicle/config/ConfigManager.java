package dev.laarryy.Icicle.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.ConfigurateException;

import java.io.File;

public class ConfigManager {

    private static final Logger logger = LogManager.getLogger(ConfigManager.class);
    String address;
    String username;
    String password;

    private static final ConfigLoader configLoader = new ConfigLoader();
    private static ConfigSettings configSettings;


    static {
        try {
            configSettings = configLoader.loadConfig("config.yml");
        } catch (ConfigurateException e) {
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
        }
    }

    public void loadDatabaseConfig() {

        File configFile = new File("/", "config.yml");
        configFile.mkdir();

        this.address = configSettings.getAddress();
        this.username = configSettings.getUsername();
        this.password = configSettings.getPassword();
    }

    public static String getAddress() {
        return configSettings.getAddress();
    }

    public static String getUsername() {
        return configSettings.getUsername();
    }

    public static String getPassword() {
        return configSettings.getPassword();
    }
}
