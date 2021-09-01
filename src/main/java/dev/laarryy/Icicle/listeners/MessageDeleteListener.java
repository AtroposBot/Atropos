package dev.laarryy.Icicle.listeners;

import discord4j.core.event.domain.message.MessageDeleteEvent;
import reactor.core.publisher.Mono;

public class MessageDeleteListener {

    @EventListener
    public Mono<Void> on(MessageDeleteEvent event) {

        return Mono.empty();
    }
}
