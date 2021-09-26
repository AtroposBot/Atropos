package dev.laarryy.Eris.managers;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Eris.models.guilds.Blacklist;
import dev.laarryy.Eris.models.guilds.DiscordServer;
import dev.laarryy.Eris.models.guilds.ServerBlacklist;
import dev.laarryy.Eris.storage.DatabaseLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BlacklistCacheManager {
    private static BlacklistCacheManager instance;
    private final LoadingCache<Long, List<Blacklist>> cache;
    private static final Logger logger = LogManager.getLogger(PropertiesCacheManager.class);


    public BlacklistCacheManager(LoadingCache<Long, List<Blacklist>> cache) {
        this.cache = cache;
    }

    public static BlacklistCacheManager getManager() {
        if (instance == null) {
            instance = new BlacklistCacheManager(Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .build(aLong -> {
                        DatabaseLoader.openConnectionIfClosed();
                        DiscordServer server = DiscordServer.findFirst("server_id = ?", aLong);
                        List<ServerBlacklist> serverBlacklistList = ServerBlacklist.find("server_id = ?", server.getServerId());
                        List<Blacklist> blacklistList = new ArrayList<>();
                        for (ServerBlacklist serverBlacklist : serverBlacklistList) {
                            blacklistList.add(new Blacklist(serverBlacklist));
                        }
                        return blacklistList;
                    }));
        }
        return instance;
    }

    public LoadingCache<Long, List<Blacklist>> getBlacklistCache() {
        return cache;
    }
}

