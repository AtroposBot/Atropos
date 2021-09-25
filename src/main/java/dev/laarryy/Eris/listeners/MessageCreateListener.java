package dev.laarryy.Eris.listeners;

import dev.laarryy.Eris.models.guilds.DiscordServer;
import dev.laarryy.Eris.models.guilds.ServerMessage;
import dev.laarryy.Eris.models.users.DiscordUser;
import dev.laarryy.Eris.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageCreateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class MessageCreateListener {

    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(MessageCreateEvent event) {

        if (event.getGuildId().isEmpty() || event.getMember().isEmpty()) {
            logger.info("Guild or Member Empty");
            return Mono.empty();
        }

        if (event.getMember().get().isBot()) {
            return Mono.empty();
        }

        DatabaseLoader.openConnectionIfClosed();

        long messageIdSnowflake = event.getMessage().getId().asLong();
        long serverIdSnowflake = event.getGuildId().get().asLong();
        long userIdSnowflake = event.getMember().get().getId().asLong();

        DiscordServer server = DiscordServer.findFirst("server_id = ?", serverIdSnowflake);
        int serverId = server.getServerId();
        server.saveIt();

        DiscordUser user = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);

        if (user == null) {
            return Mono.empty();
        }

        int userId = user.getUserId();
        user.saveIt();

        // Create message row in the table
        ServerMessage message = ServerMessage.findOrCreateIt("message_id_snowflake", messageIdSnowflake, "server_id", serverId, "user_id", userId);

        // Populate it

        String content = event.getMessage().getContent();

        message.setServerId(serverId);
        message.setServerSnowflake(serverIdSnowflake);
        message.setUserId(userId);
        message.setUserSnowflake(userIdSnowflake);
        message.setDateEpochMilli(Instant.now().toEpochMilli());
        message.setContent(content);
        message.setDeleted(false);

        logger.info("Saving Message to DB.");

        message.saveIt();

        return Mono.empty();
    }
}
