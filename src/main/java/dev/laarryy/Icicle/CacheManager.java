package dev.laarryy.Icicle;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;

public class CacheManager {
    private static CacheManager instance;
    private final LoadingCache<Long, DiscordServerProperties> cache;
    private static final Logger logger = LogManager.getLogger(CacheManager.class);


    public CacheManager(LoadingCache<Long, DiscordServerProperties> cache) {
        this.cache = cache;
    }

    public static CacheManager getManager() {
        if (instance == null) {
            instance = new CacheManager(Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .build(aLong -> {
                        DatabaseLoader.openConnectionIfClosed();
                        logger.info("Connecting to database to get properties for log.");
                        return DiscordServerProperties.findFirst("server_id_snowflake = ?", aLong);
                    }));
        }
        return instance;
    }

    public LoadingCache<Long, DiscordServerProperties> getCache() {
        return cache;
    }
}
