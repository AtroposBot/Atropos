package dev.laarryy.atropos.services;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import dev.laarryy.atropos.jooq.tables.records.ServerPropertiesRecord;
import discord4j.common.util.Snowflake;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static dev.laarryy.atropos.jooq.Tables.SERVER_PROPERTIES;
import static dev.laarryy.atropos.storage.DatabaseLoader.sqlContext;

public class CacheMaintainer {
    private final Logger logger = LogManager.getLogger(this);

    public Mono<Void> startCacheRefresh(AsyncLoadingCache<Snowflake, ServerPropertiesRecord> cache) {
        Mono<Void> startRefreshing = Flux.interval(Duration.ofMinutes(3))
                .doFirst(() -> {
                    logger.info("Starting Cache Refresh");
                })
                .flatMap(l -> refreshPropertiesCache(cache))
                .then();

        return Mono.when(startRefreshing);
    }

    private Mono<Void> refreshPropertiesCache(AsyncLoadingCache<Snowflake, ServerPropertiesRecord> cache) {
        return Flux.from(sqlContext.select(SERVER_PROPERTIES.SERVER_ID_SNOWFLAKE).from(SERVER_PROPERTIES))
                .doOnNext(result -> cache.asMap().remove(result.value1()))
                .then();
    }
}
