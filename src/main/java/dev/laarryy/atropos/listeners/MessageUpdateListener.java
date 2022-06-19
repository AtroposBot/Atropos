package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.models.guilds.ServerMessage;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import reactor.core.publisher.Mono;

public class MessageUpdateListener {
    @EventListener
    public Mono<Void> on(MessageUpdateEvent event) {

        return event.getGuild().flatMap(guild -> {
            if (guild == null || event.getGuildId().isEmpty()) {
                return Mono.empty();
            }

            return event.getMessage().flatMap(message -> {

                if (message.getAuthor().isPresent() && message.getAuthor().get().isBot()) {
                    return Mono.empty();
                }

                DatabaseLoader.openConnectionIfClosed();
                long guildId = event.getGuildId().get().asLong();
                long snowflakeId = event.getMessageId().asLong();

                ServerMessage serverMessage = ServerMessage.findFirst("server_id_snowflake = ? and message_id_snowflake = ?", guildId, snowflakeId);

                if (serverMessage == null) {
                    DatabaseLoader.closeConnectionIfOpen();
                    return Mono.empty();
                } else {
                    serverMessage.setContent(message.getContent());
                    serverMessage.save();
                }
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.empty();
            });
        });
    }
}
