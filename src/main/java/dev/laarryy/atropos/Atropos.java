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

        ScheduledTaskDoer scheduledTaskDoer = new ScheduledTaskDoer(client);
        AddServerToDB addServerToDB = new AddServerToDB();

        // Register all guilds and users in them to database

        DatabaseLoader.openConnectionIfClosed();

        List<Guild> unregisteredGuilds = client.getGuilds()
                .filter(guild -> DiscordServer.findFirst("server_id = ?", guild.getId().asLong()) == null)
                .collectList().block();

        for (Guild guild: unregisteredGuilds) {
            logger.info("Registering + " + guild.getId().asLong());
            addServerToDB.addServerToDatabase(guild);
        }


        client.onDisconnect().block();
    }
}
