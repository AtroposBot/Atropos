package dev.laarryy.atropos.managers;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import dev.laarryy.atropos.jooq.tables.records.ServerPropertiesRecord;
import discord4j.common.util.Snowflake;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static dev.laarryy.atropos.jooq.Tables.SERVERS;
import static dev.laarryy.atropos.jooq.Tables.SERVER_PROPERTIES;
import static dev.laarryy.atropos.storage.DatabaseLoader.sqlContext;
import static org.jooq.impl.DSL.select;

public class PropertiesCacheManager {
    private static PropertiesCacheManager instance;
    private final AsyncLoadingCache<Snowflake, ServerPropertiesRecord> cache;
    private static final Logger logger = LogManager.getLogger(PropertiesCacheManager.class);


    public PropertiesCacheManager(AsyncLoadingCache<Snowflake, ServerPropertiesRecord> cache) {
        this.cache = cache;
    }

    public static PropertiesCacheManager getManager() {
        if (instance == null) {
            instance = new PropertiesCacheManager(Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .scheduler(Scheduler.systemScheduler())
                    .buildAsync(AsyncCacheLoader.bulk((keys, executor) -> // ignore executor, we aren't defining any in the cache builder
                            Flux.from(sqlContext.selectFrom(SERVER_PROPERTIES)
                                            .where(SERVER_PROPERTIES.SERVER_ID.in(select(SERVERS.ID).from(SERVERS).where(SERVERS.SERVER_ID.in(keys)))))
                                    .collectMap(ServerPropertiesRecord::getServerIdSnowflake)
                                    .toFuture()
                    )));
        }
        return instance;
    }

    public AsyncLoadingCache<Snowflake, ServerPropertiesRecord> getPropertiesCache() {
        return cache;
    }
}
