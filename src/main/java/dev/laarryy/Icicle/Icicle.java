package dev.laarryy.Icicle;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.commands.punishments.PunishmentManager;
import dev.laarryy.Icicle.config.ConfigManager;
import dev.laarryy.Icicle.config.EmojiManager;
import dev.laarryy.Icicle.managers.CacheManager;
import dev.laarryy.Icicle.managers.ClientManager;
import dev.laarryy.Icicle.managers.CommandManager;
import dev.laarryy.Icicle.managers.ListenerManager;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.services.AutoPunishmentEnder;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.AddServerToDB;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Icicle {

    private static final Logger logger = LogManager.getLogger(Icicle.class);
    private final PunishmentManager punishmentManager = new PunishmentManager();

    public PunishmentManager getPunishmentManager() {
        return this.punishmentManager;
    }


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

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    final User self = event.getSelf();
                    logger.info("Logged in as: " + self.getUsername() + "#" + self.getDiscriminator());
                });

        logger.debug("Connected!");

        // Connect to DB

        logger.debug("Connecting to Database...");

        DatabaseLoader.openConnection();

        logger.info("Connected to Database!");

        LoadingCache<Long, DiscordServerProperties> cache = CacheManager.getManager().getCache();

        CommandManager commandManager = new CommandManager();
        commandManager.registerCommands(client);

        ListenerManager listenerManager = new ListenerManager();
        listenerManager.registerListeners(client);

        AutoPunishmentEnder autoPunishmentEnder = new AutoPunishmentEnder(client);

        // Register all guilds and users in them to database

        // TODO: Shelf this to only when joining servers. Don't need to do it every startup.
        client.getGuilds()
                .map(AddServerToDB::addServerToDatabase)
                .doOnError(logger::error)
                .subscribe();

        client.onDisconnect().block();
    }
}
