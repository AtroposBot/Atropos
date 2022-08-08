package dev.laarryy.atropos.listeners;

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

        try (final var usage = DatabaseLoader.use()) {
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
}
