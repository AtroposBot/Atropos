package dev.laarryy.Icicle.listeners;

import discord4j.core.event.domain.InviteCreateEvent;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.event.domain.channel.PinsUpdateEvent;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.guild.UnbanEvent;
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import reactor.core.publisher.Mono;

public class LoggingListener {

    @EventListener
    public Mono<Void> on(MessageDeleteEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MessageUpdateEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MessageBulkDeleteEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberLeaveEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberUpdateEvent event) { // Nickname or role updates

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(PresenceUpdateEvent event) { // Username, discrim, avatar changes

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(InviteCreateEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(PinsUpdateEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(BanEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(UnbanEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleCreateEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleDeleteEvent event) {

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleUpdateEvent event) {

        return Mono.empty();
    }

}
