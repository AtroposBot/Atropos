package dev.laarryy.Icicle.listeners;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class ChannelDeleteListener {

    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(TextChannelDeleteEvent event) {
        //TODO: Check if channel is guild's logging channel
        return Mono.empty();
    }
}
