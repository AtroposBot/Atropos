package dev.laarryy.Icicle;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.commands.punishments.PunishmentManager;
import dev.laarryy.Icicle.config.ConfigManager;
import dev.laarryy.Icicle.config.EmojiManager;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.joins.ServerUser;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.services.AutoPunishmentEnder;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;

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

        client.getGuilds()
                .map(Icicle::addServerToDatabase)
                .doOnError(logger::error)
                .subscribe();

        client.onDisconnect().block();
    }

    public static boolean addServerToDatabase(Guild guild) {

        long serverIdSnowflake = guild.getId().asLong();
        DatabaseLoader.openConnectionIfClosed();
        DiscordServer server = DiscordServer.findOrCreateIt("server_id", serverIdSnowflake);
        server.save();
        server.refresh();

        if (server.getDateEntry() == 0) {
            server.setDateEntry(Instant.now().toEpochMilli());
            server.save();
        }

        int serverId = server.getServerId();

        DiscordServerProperties properties = DiscordServerProperties.findOrCreateIt("server_id", serverId, "server_id_snowflake", serverIdSnowflake);

        properties.setServerName(guild.getName());
        properties.save();
        properties.refresh();

        if (properties.getMembersOnFirstJoin() == 0) {
            properties.setMembersOnFirstJoin(guild.getMemberCount());
            properties.save();
        }

        guild.getMembers()
                .map(member -> Icicle.addUserToDatabase(member, guild))
                .doOnError(logger::error)
                .subscribe();

        return true;
    }

    public static boolean addUserToDatabase(Member member, Guild guild) {

        if (member.isBot()) {
            return false;
        }

        DatabaseLoader.openConnectionIfClosed();

        long userIdSnowflake = member.getId().asLong();
        long serverIdSnowflake = guild.getId().asLong();

        DiscordUser user = DiscordUser.findOrCreateIt("user_id_snowflake", userIdSnowflake);
        user.save();
        user.refresh();

        if (user.getDateEntry() == 0) {
            user.setDateEntry(Instant.now().toEpochMilli());
            user.save();
        }

        DiscordServer server = DiscordServer.findOrCreateIt("server_id", serverIdSnowflake);

        int serverId = server.getServerId();
        int userId = user.getUserId();

        ServerUser serverUser = ServerUser.findOrCreateIt("user_id", userId, "server_id", serverId);
        serverUser.save();
        serverUser.refresh();

        if (serverUser.getDate() == 0) {
            serverUser.setDate(Instant.now().toEpochMilli());
            serverUser.save();
        }

        return true;
    }
}
