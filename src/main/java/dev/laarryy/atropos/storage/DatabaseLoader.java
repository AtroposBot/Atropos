package dev.laarryy.atropos.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.laarryy.atropos.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.DB;

import java.util.function.Supplier;


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
        config.addDataSourceProperty("autoReconnect", true);
        ds = new HikariDataSource(config);
    }

    public static Usage use() {
        return new Usage(Base.open(ds));
    }

    public static void use(final Runnable action) {
        try (final var db = Base.open(ds)) {
            action.run();
        }
    }

    public static <T> T use(final Supplier<? extends T> action) {
        try (final var db = Base.open(ds)) {
            return action.get();
        }
    }

    public static void shutdown() {
        ds.close();
    }

    public static final class Usage implements AutoCloseable {

        private final DB db;

        private Usage(final DB db) {
            this.db = db;
        }

        @Override
        public void close() {
            this.db.close();
        }
    }
}
