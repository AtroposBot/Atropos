package dev.laarryy.eris.listeners;

import dev.laarryy.eris.models.guilds.ServerMessage;
import dev.laarryy.eris.storage.DatabaseLoader;
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
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
