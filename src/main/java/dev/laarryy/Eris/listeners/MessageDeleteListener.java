package dev.laarryy.Eris.listeners;

import dev.laarryy.Eris.models.guilds.ServerMessage;
import dev.laarryy.Eris.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import reactor.core.publisher.Mono;

public class MessageDeleteListener {

    @EventListener
    public Mono<Void> on(MessageDeleteEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        ServerMessage serverMessage = ServerMessage.findFirst("message_id_snowflake = ?", event.getMessageId().asLong());

        if (serverMessage == null) {
            return Mono.empty();
        } else {
            serverMessage.setDeleted(true);
            serverMessage.save();
        }

        return Mono.empty();
    }
}
