package dev.laarryy.Eris;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Eris.commands.punishments.PunishmentManager;
import dev.laarryy.Eris.config.ConfigManager;
import dev.laarryy.Eris.config.EmojiManager;
import dev.laarryy.Eris.managers.BlacklistCacheManager;
import dev.laarryy.Eris.managers.ClientManager;
import dev.laarryy.Eris.managers.CommandManager;
import dev.laarryy.Eris.managers.ListenerManager;
import dev.laarryy.Eris.managers.PropertiesCacheManager;
import dev.laarryy.Eris.managers.PunishmentManagerManager;
import dev.laarryy.Eris.models.guilds.Blacklist;
import dev.laarryy.Eris.models.guilds.DiscordServer;
import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.services.AutoPunishmentEnder;
import dev.laarryy.Eris.storage.DatabaseLoader;
import dev.laarryy.Eris.utils.AddServerToDB;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;

import java.util.List;

public class Eris {

    private static final Logger logger = LogManager.getLogger(Eris.class);

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

        LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();
        LoadingCache<Long, List<Blacklist>> blacklistCache = BlacklistCacheManager.getManager().getBlacklistCache();

        CommandManager commandManager = new CommandManager();
        commandManager.registerCommands(client);

        ListenerManager listenerManager = new ListenerManager();
        listenerManager.registerListeners(client);

        PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();

        AutoPunishmentEnder autoPunishmentEnder = new AutoPunishmentEnder(client);
        AddServerToDB addServerToDB = new AddServerToDB();

        // Register all guilds and users in them to database

        DatabaseLoader.openConnectionIfClosed();

        List<Guild> unregisteredGuilds = client.getGuilds()
                .filter(guild -> DiscordServer.findFirst("server_id = ?", guild.getId().asLong()) != null)
                .collectList().block();

        for (Guild guild: unregisteredGuilds) {
            logger.info("Registering + " + guild.getId().asLong());
            addServerToDB.addServerToDatabase(guild);
        }


        client.onDisconnect().block();
    }
}
