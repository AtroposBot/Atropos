package dev.laarryy.atropos.services;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.storage.DatabaseLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class CacheMaintainer {
    private final Logger logger = LogManager.getLogger(this);

    public Mono<Void> startCacheRefresh(LoadingCache<Long, DiscordServerProperties> cache) {
        Mono<Void> startRefreshing = Flux.interval(Duration.ofMinutes(3))
                .doFirst(() -> logger.info("Starting Cache Refresh"))
                .doOnNext(l -> refreshPropertiesCache(cache)).then();

        return Mono.when(startRefreshing);
    }

    private Mono<Void> refreshPropertiesCache(LoadingCache<Long, DiscordServerProperties> cache) {
        DatabaseLoader.openConnectionIfClosed();
        LazyList<DiscordServerProperties> propertiesList = DiscordServerProperties.findAll();
        DatabaseLoader.closeConnectionIfOpen();
        return Flux.fromIterable(propertiesList)
                .doOnNext(property -> cache.invalidate(property.getServerIdSnowflake())).then();
    }
}
