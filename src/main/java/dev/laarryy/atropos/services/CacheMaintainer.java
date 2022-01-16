package dev.laarryy.atropos.services;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.storage.DatabaseLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class CacheMaintainer {
    private final Logger logger = LogManager.getLogger(this);

    public CacheMaintainer(AsyncLoadingCache<Long, DiscordServerProperties> cache) {
        DatabaseLoader.openConnectionIfClosed();
        Flux.interval(Duration.ofMinutes(3))
                .doOnNext(l -> refreshPropertiesCache(cache))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
        DatabaseLoader.closeConnectionIfOpen();
    }

    private void refreshPropertiesCache(AsyncLoadingCache<Long, DiscordServerProperties> cache) {
        DatabaseLoader.openConnectionIfClosed();
        LazyList<DiscordServerProperties> propertiesList = DiscordServerProperties.findAll();
        Flux.fromIterable(propertiesList)
                .subscribe(property -> {
                });
        DatabaseLoader.closeConnectionIfOpen();
    }
}
