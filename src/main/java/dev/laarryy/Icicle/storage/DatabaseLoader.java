package dev.laarryy.Icicle.storage;

import dev.laarryy.Icicle.config.ConfigLoader;
import dev.laarryy.Icicle.config.ConfigSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.Base;
import org.spongepowered.configurate.ConfigurateException;

import java.io.File;

public class DatabaseLoader {

    Logger logger = LogManager.getLogger(this);

    String address;
    String username;
    String password;

    public void loadDatabaseConfig() throws ConfigurateException {
        File configFile = new File("/", "config.yml");
        configFile.mkdir();
        ConfigLoader configLoader = new ConfigLoader();
        ConfigSettings configSettings = configLoader.loadConfig("config.yml");

        if (configSettings.getAddress() == null || configSettings.getUsername() == null || configSettings.getPassword() == null) {
            logger.error("Database information not correctly filled out!");
            System.exit(1);
        }

        this.address = configSettings.getAddress();
        this.username = configSettings.getUsername();
        this.password = configSettings.getPassword();

    }

    public void openConnection() {
        Base.open("com.mysql.cj.jdbc.Driver", this.address, this.username, this.password);
    }

    public void openConnectionIfClosed() {
        if (!Base.hasConnection()) {
            logger.info("Connecting to database!");
            openConnection();
        }
    }
}
