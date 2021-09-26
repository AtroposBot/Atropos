package dev.laarryy.Eris.services;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.storage.DatabaseLoader;
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
    }

    private void refreshPropertiesCache(AsyncLoadingCache<Long, DiscordServerProperties> cache) {
        DatabaseLoader.openConnectionIfClosed();
        LazyList<DiscordServerProperties> propertiesList = DiscordServerProperties.findAll();
        Flux.fromIterable(propertiesList)
                .subscribe(property -> {
                });
    }
}
