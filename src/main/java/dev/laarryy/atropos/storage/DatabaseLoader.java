package dev.laarryy.atropos.storage;

import dev.laarryy.atropos.config.ConfigManager;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.mariadb.r2dbc.MariadbConnectionConfiguration;
import org.mariadb.r2dbc.MariadbConnectionFactory;
import reactor.core.publisher.Mono;

public class DatabaseLoader {

    private static final ConnectionPool pool;
    public static final DSLContext sqlContext;

    static {
        pool = new ConnectionPool(
                ConnectionPoolConfiguration.builder()
                        .connectionFactory(new MariadbConnectionFactory(
                                MariadbConnectionConfiguration.builder()
                                        .host(ConfigManager.getAddress())
                                        .username(ConfigManager.getUsername())
                                        .password(ConfigManager.getPassword())
                                        .build()
                        ))
                        .build()
        );

        sqlContext = DSL.using(pool);
    }

    public static Mono<Void> shutdown() {
        return pool.close();
    }
}
