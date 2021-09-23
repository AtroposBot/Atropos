package dev.laarryy.Icicle.listeners;

import dev.laarryy.Icicle.models.guilds.ServerMessage;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import reactor.core.publisher.Mono;

public class MessageUpdateListener {
    @EventListener
    public Mono<Void> on(MessageUpdateEvent event) {

        if (event.getGuild().block() == null) {
            return Mono.empty();
        }

        if (event.getMessage().block().getAuthor().isPresent() && event.getMessage().block().getAuthor().get().isBot()) {
            return Mono.empty();
        }

        DatabaseLoader.openConnectionIfClosed();
        long guildId = event.getGuildId().get().asLong();
        long snowflakeId = event.getMessageId().asLong();
        DatabaseLoader.openConnectionIfClosed();

        ServerMessage serverMessage = ServerMessage.findFirst("server_id_snowflake = ? and message_id_snowflake = ?", guildId, snowflakeId);

        if (serverMessage == null) {
            return Mono.empty();
        } else {
            serverMessage.setContent(event.getMessage().block().getContent());
            serverMessage.save();
        }

        return Mono.empty();
    }
}
