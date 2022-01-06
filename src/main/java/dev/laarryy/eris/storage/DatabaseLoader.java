package dev.laarryy.eris.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.laarryy.eris.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.Base;


public class DatabaseLoader {

    private static final Logger logger = LogManager.getLogger(DatabaseLoader.class);

    private static final HikariConfig config = new HikariConfig();
    private static final HikariDataSource ds;

    static {
            config.setJdbcUrl(ConfigManager.getAddress());
            config.setUsername(ConfigManager.getUsername());
            config.setPassword(ConfigManager.getPassword());
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            ds = new HikariDataSource(config);
    }

    public static void openConnection() {
        logger.info("Connecting to Database");
        Base.open(ds);
    }

    public static void openConnectionIfClosed() {
        if (!Base.hasConnection()) {
            openConnection();
        }
    }

    public static void closeConnectionIfOpen() {
        if (Base.hasConnection()) {
            logger.info("Disconnecting from Database");
            Base.close();
        }
    }
}
