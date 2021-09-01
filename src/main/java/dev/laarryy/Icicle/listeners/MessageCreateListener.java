package dev.laarryy.Icicle.listeners;

import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public class MessageCreateListener {

    @EventListener
    public Mono<Void> on(MessageCreateEvent event) {
        DatabaseLoader.openConnectionIfClosed();


        return Mono.empty();
    }
}
