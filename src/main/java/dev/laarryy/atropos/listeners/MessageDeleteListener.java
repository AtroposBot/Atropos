package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.exceptions.NotFoundException;
import dev.laarryy.atropos.models.guilds.ServerMessage;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class MessageDeleteListener {

    Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(MessageDeleteEvent event) {

        DatabaseLoader.openConnectionIfClosed();
        ServerMessage serverMessage = ServerMessage.findFirst("message_id_snowflake = ?", event.getMessageId().asLong());

        if (serverMessage == null) {
            return Mono.error(new NotFoundException("No Message"));
        } else {
            serverMessage.setDeleted(true);
            serverMessage.save();
        }
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
