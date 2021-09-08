package dev.laarryy.Icicle.services;

import dev.laarryy.Icicle.cache.GenericCache;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

public class CacheMaintainer {
    private final Logger logger = LogManager.getLogger(this);

    public CacheMaintainer(GenericCache<Long, DiscordServerProperties> cache) {
        DatabaseLoader.openConnectionIfClosed();
        Flux.interval(Duration.ofMinutes(3))
                .doOnNext(l -> refreshPropertiesCache(cache))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void refreshPropertiesCache(GenericCache<Long, DiscordServerProperties> cache) {
        DatabaseLoader.openConnectionIfClosed();
        LazyList<DiscordServerProperties> propertiesList = DiscordServerProperties.findAll();
        logger.info("refreshing properties cache");
        Flux.fromIterable(propertiesList)
                .subscribe(property -> {
                    cache.clean();
                    cache.put(property.getServerIdSnowflake(), property);
                });
    }
}
