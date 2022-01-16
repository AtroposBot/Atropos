package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.ServerMessage;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Attachment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class MessageCreateListener {

    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(MessageCreateEvent event) {

        if (event.getGuildId().isEmpty() || event.getMember().isEmpty()) {
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
            user = DiscordUser.createIt("user_id_snowflake", userIdSnowflake, "date", Instant.now().toEpochMilli());
        }

        int userId = user.getUserId();
        user.save();
        user.refresh();

        // Create message row in the table
        ServerMessage message = ServerMessage.findOrCreateIt("message_id_snowflake", messageIdSnowflake, "server_id", serverId, "user_id", userId);

        // Populate it

        String content = event.getMessage().getContent();

        if (!event.getMessage().getAttachments().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Attachment attachment : event.getMessage().getAttachments()) {
                sb.append("\n > File: `").append(attachment.getFilename()).append("`:").append(attachment.getProxyUrl());
            }
            content = content + sb;
        }

        if (!event.getMessage().getEmbeds().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Embed embed : event.getMessage().getEmbeds()) {
                if (embed.getTitle().isPresent()) {
                    sb.append("\n > Embed Title: ").append(embed.getTitle()).append("\n");
                }
                if (embed.getDescription().isPresent()) {
                    sb.append("\n > Embed Description: ").append(embed.getDescription()).append("\n");
                }
                if (!embed.getFields().isEmpty()) {
                    for (Embed.Field field : embed.getFields()) {
                        sb.append("\n > Field Title: ").append(field.getName()).append("\n > Field Content: ").append(field.getValue()).append("\n");
                    }
                }
            }
            content = content + sb;
        }

        message.setServerId(serverId);
        message.setServerSnowflake(serverIdSnowflake);
        message.setUserId(userId);
        message.setUserSnowflake(userIdSnowflake);
        message.setDateEpochMilli(Instant.now().toEpochMilli());
        message.setContent(content);
        message.setDeleted(false);

        message.save();

        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
