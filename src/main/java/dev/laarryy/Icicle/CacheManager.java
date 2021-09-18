package dev.laarryy.Icicle;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;

import java.time.Duration;

public class CacheManager {
    private static CacheManager instance;
    private final LoadingCache<Long, DiscordServerProperties> cache;

    public CacheManager(LoadingCache<Long, DiscordServerProperties> cache) {
        this.cache = cache;
    }

    public static CacheManager getManager() {
        if (instance == null) {
            instance = new CacheManager(Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .build(aLong -> {
                        DatabaseLoader.openConnectionIfClosed();
                        return DiscordServerProperties.findFirst("server_id_snowflake = ?", aLong);
                    }));
        }
        return instance;
    }

    public LoadingCache<Long, DiscordServerProperties> getCache() {
        return cache;
    }
}
