package dev.laarryy.atropos;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.commands.punishments.PunishmentManager;
import dev.laarryy.atropos.config.ConfigManager;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.managers.BlacklistCacheManager;
import dev.laarryy.atropos.managers.ClientManager;
import dev.laarryy.atropos.managers.CommandManager;
import dev.laarryy.atropos.managers.ListenerManager;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.managers.PunishmentManagerManager;
import dev.laarryy.atropos.models.guilds.Blacklist;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.services.CacheMaintainer;
import dev.laarryy.atropos.services.ScheduledTaskDoer;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AddServerToDB;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.javalite.activejdbc.DBException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.ReactorBlockHoundIntegration;

import java.util.List;

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

        // Connect to DB

        logger.debug("Connecting to Database...");

        DatabaseLoader.openConnection();

        logger.info("Connected to Database!");

        LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();
        LoadingCache<Long, List<Blacklist>> blacklistCache = BlacklistCacheManager.getManager().getBlacklistCache();

        CommandManager commandManager = new CommandManager();
        Mono<Void> commandRegistration = commandManager.registerCommands(client)
                .doFinally(signalType -> logger.info("Commands Registered"));

        ListenerManager listenerManager = new ListenerManager();
        Mono<Void> listenerRegistration = listenerManager.registerListeners(client)
                .doFinally(signalType -> logger.info("Listeners Registered"));

        PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();

        Mono<Void> scheduledTaskDoer = new ScheduledTaskDoer().startTasks(client);

        Mono<Void> startCacheRefresh = new CacheMaintainer().startCacheRefresh(cache).then();

        // Register all guilds and users in them to database

        Mono<Void> addServersToDB = Flux.from(client.getGuilds())
                .filter(guild -> DiscordServer.findFirst("server_id = ?", guild.getId().asLong()) == null)
                .flatMap(guild -> {
                    AddServerToDB addServerToDB1 = new AddServerToDB();
                    return addServerToDB1.addServerToDatabase(guild);
                })
                .doFinally(signalType -> logger.info("Servers Added to Database."))
                .then();

        DatabaseLoader.openConnectionIfClosed();

        Mono.when(
                        ready,
                        commandRegistration,
                        listenerRegistration,
                        scheduledTaskDoer,
                        startCacheRefresh,
                        addServersToDB,
                        client.onDisconnect()
                )
                .onErrorContinue((throwable, o) -> {
                    logger.error("-- Error in Bot --");
                    logger.error("stinky", throwable);
                })
                .block();
    }
}
