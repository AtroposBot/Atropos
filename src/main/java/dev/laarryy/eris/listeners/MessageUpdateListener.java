package dev.laarryy.eris.listeners;

import dev.laarryy.eris.models.guilds.ServerMessage;
import dev.laarryy.eris.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import reactor.core.publisher.Mono;

public class MessageUpdateListener {
    @EventListener
    public Mono<Void> on(MessageUpdateEvent event) {

        if (event.getGuild().block() == null) {
            return Mono.empty();
        }

        try {
            event.getMessage().block();
        } catch (Exception e) {
            return Mono.empty();
        }

        if (event.getMessage().block().getAuthor().isPresent() && event.getMessage().block().getAuthor().get().isBot()) {
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
            serverMessage.setContent(event.getMessage().block().getContent());
            serverMessage.save();
        }
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
