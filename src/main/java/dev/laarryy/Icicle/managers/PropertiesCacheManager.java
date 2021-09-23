package dev.laarryy.Icicle.managers;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

public class PropertiesCacheManager {
    private static PropertiesCacheManager instance;
    private final LoadingCache<Long, DiscordServerProperties> cache;
    private static final Logger logger = LogManager.getLogger(PropertiesCacheManager.class);


    public PropertiesCacheManager(LoadingCache<Long, DiscordServerProperties> cache) {
        this.cache = cache;
    }

    public static PropertiesCacheManager getManager() {
        if (instance == null) {
            instance = new PropertiesCacheManager(Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .build(aLong -> {
                        DatabaseLoader.openConnectionIfClosed();
                        logger.info("Connecting to database to get properties for log.");
                        return DiscordServerProperties.findFirst("server_id_snowflake = ?", aLong);
                    }));
        }
        return instance;
    }

    public LoadingCache<Long, DiscordServerProperties> getPropertiesCache() {
        return cache;
    }
}
