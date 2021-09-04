package dev.laarryy.Icicle.listeners;

import discord4j.core.event.domain.role.RoleDeleteEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class RoleDeleteListener {

    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(RoleDeleteEvent event) {
        //TODO: Check if role is guild's muted role and adjust DB to reflect
        return Mono.empty();
    }
}
