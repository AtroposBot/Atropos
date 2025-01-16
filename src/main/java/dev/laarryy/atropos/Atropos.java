package dev.laarryy.atropos;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import dev.laarryy.atropos.commands.punishments.PunishmentManager;
import dev.laarryy.atropos.config.ConfigManager;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.jooq.tables.records.ServerPropertiesRecord;
import dev.laarryy.atropos.managers.BlacklistCacheManager;
import dev.laarryy.atropos.managers.ClientManager;
import dev.laarryy.atropos.managers.CommandManager;
import dev.laarryy.atropos.managers.ListenerManager;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.managers.PunishmentManagerManager;
import dev.laarryy.atropos.models.guilds.Blacklist;
import dev.laarryy.atropos.services.CacheMaintainer;
import dev.laarryy.atropos.services.ScheduledTaskDoer;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AddServerToDB;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

import static dev.laarryy.atropos.jooq.Tables.SERVERS;
import static dev.laarryy.atropos.storage.DatabaseLoader.sqlContext;

public class Atropos {

    private static final Logger logger = LogManager.getLogger(Atropos.class);

    public static void main(String[] args) throws Exception {

        // Print token and other args to console
        for (String arg : args) {
            logger.debug(arg);
        }

        // Load Config
        ConfigManager manager = new ConfigManager();
        manager.loadDatabaseConfig();

        EmojiManager emojiManager = new EmojiManager();
        emojiManager.loadEmojiConfig();

        logger.info("Loaded Config");
        logger.info("Connecting to Discord!");

        GatewayDiscordClient client = ClientManager.getManager().getClient();

        Mono<Void> ready = client.getEventDispatcher().on(ReadyEvent.class)
                .flatMap(event -> {
                    final User self = event.getSelf();
                    logger.info("Logged in as: " + self.getUsername() + "#" + self.getDiscriminator());
                    return Mono.empty();
                })
                .doFinally(signalType -> logger.info("Done Logging in."))
                .then();

        logger.debug("Connected!");

        // Connect to DB, if something goes wrong it'll end up in an erred mono
        final Mono<Void> databaseConnectivityTest =
                Mono.fromDirect(sqlContext.selectOne())
                        .doOnSubscribe(s -> logger.debug("Connecting to Database..."))
                        .doOnSuccess(i -> logger.info("Connected to Database!"))
                        .then();

        AsyncLoadingCache<Snowflake, ServerPropertiesRecord> serverPropertiesCache = PropertiesCacheManager.getManager().getPropertiesCache();
        AsyncLoadingCache<Snowflake, Collection<Blacklist>> blacklistCache = BlacklistCacheManager.getManager().getBlacklistCache();

        CommandManager commandManager = new CommandManager();
        Mono<Void> commandRegistration = commandManager.registerCommands(client)
                .doAfterTerminate(() -> logger.info("Commands Registered"));

        ListenerManager listenerManager = new ListenerManager();
        Mono<Void> listenerRegistration = listenerManager.registerListeners(client)
                .doAfterTerminate(() -> logger.info("Listeners Registered"));

        PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();

        Mono<Void> scheduledTaskDoer = new ScheduledTaskDoer().startTasks(client);

        Mono<Void> startCacheRefresh = new CacheMaintainer().startCacheRefresh(serverPropertiesCache).then();

        // Register all guilds and users in them to database

        Mono<Void> addServersToDB = Flux.from(client.getGuilds())
                .filterWhen(guild ->
                        Mono.fromDirect(sqlContext.selectOne()
                                        .from(SERVERS)
                                        .where(SERVERS.SERVER_ID_SNOWFLAKE.eq(guild.getId())))
                                .hasElement()
                                .transform(BooleanUtils::not)
                )
                .flatMap(AddServerToDB::addServerToDatabase)
                .doAfterTerminate(() -> logger.info("Servers Added to Database."))
                .then();

        Mono.when(
                        ready,
                        databaseConnectivityTest,
                        commandRegistration,
                        listenerRegistration,
                        scheduledTaskDoer,
                        startCacheRefresh,
                        addServersToDB
                )
                .onErrorContinue((throwable, o) -> {
                    logger.error("-- Error in Bot --");
                    logger.error("stinky", throwable);
                })
                .takeUntilOther(client.onDisconnect())
                .onErrorResume(error -> DatabaseLoader.shutdown().then(Mono.error(error)))
                .then(DatabaseLoader.shutdown())
                .block();
    }
}
