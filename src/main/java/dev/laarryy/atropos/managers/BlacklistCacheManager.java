package dev.laarryy.atropos.managers;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import dev.laarryy.atropos.jooq.tables.records.ServerBlacklistRecord;
import dev.laarryy.atropos.models.guilds.Blacklist;
import discord4j.common.util.Snowflake;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collection;

import static dev.laarryy.atropos.jooq.Tables.SERVERS;
import static dev.laarryy.atropos.jooq.Tables.SERVER_BLACKLIST;
import static dev.laarryy.atropos.storage.DatabaseLoader.sqlContext;

public class BlacklistCacheManager {
    private static BlacklistCacheManager instance;
    private final AsyncLoadingCache<Snowflake, Collection<Blacklist>> cache;
    private static final Logger logger = LogManager.getLogger(PropertiesCacheManager.class);


    public BlacklistCacheManager(AsyncLoadingCache<Snowflake, Collection<Blacklist>> cache) {
        this.cache = cache;
    }

    public static BlacklistCacheManager getManager() {
        if (instance == null) {
            instance = new BlacklistCacheManager(Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .scheduler(Scheduler.systemScheduler())
                    .buildAsync(AsyncCacheLoader.bulk((keys, executor) -> // ignore executor, we aren't defining any in the cache builder
                            Flux.from(sqlContext.select(SERVERS.SERVER_ID.as("server_snowflake"), SERVER_BLACKLIST.asterisk())
                                            .from(SERVER_BLACKLIST)
                                            .join(SERVERS)
                                            .on(SERVER_BLACKLIST.SERVER_ID.eq(SERVERS.ID))
                                            .and(SERVERS.SERVER_ID.in(keys)))
                                    .collectMultimap(
                                            result -> result.get("server_snowflake", Snowflake.class),
                                            result -> {
                                                final ServerBlacklistRecord blacklist = new ServerBlacklistRecord();
                                                blacklist.fromMap(result.intoMap());
                                                return new Blacklist(blacklist);
                                            }
                                    )
                                    .toFuture()
                    )));
        }
        return instance;
    }

    public AsyncLoadingCache<Snowflake, Collection<Blacklist>> getBlacklistCache() {
        return cache;
    }
}
