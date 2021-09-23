package dev.laarryy.Icicle.listeners;

import dev.laarryy.Icicle.utils.AddServerToDB;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class MemberJoinListener {

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {
        if (event.getGuild().block() == null) {
            return Mono.empty();
        }
        AddServerToDB.addUserToDatabase(event.getMember(), event.getGuild().block());
        return Mono.empty();
    }
}
