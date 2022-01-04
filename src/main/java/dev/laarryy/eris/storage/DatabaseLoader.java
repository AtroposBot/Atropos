package dev.laarryy.eris.storage;

import dev.laarryy.eris.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.Base;

public class DatabaseLoader {

    private static final Logger logger = LogManager.getLogger(DatabaseLoader.class);

    public static void openConnection() {
        Base.open("com.mysql.cj.jdbc.Driver", ConfigManager.getAddress(), ConfigManager.getUsername(), ConfigManager.getPassword());
    }

    public static void openConnectionIfClosed() {

        if (!Base.hasConnection()) {
            logger.info("Connecting to database!");
            openConnection();
        }
    }

    public static void closeConnectionIfOpen() {
        if (Base.hasConnection()) {
            logger.info("Disconnecting from database!");
            Base.close();
        }
    }
}