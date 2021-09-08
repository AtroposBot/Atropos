package dev.laarryy.Icicle.listeners.logging;

import dev.laarryy.Icicle.Icicle;
import dev.laarryy.Icicle.cache.GenericCache;
import dev.laarryy.Icicle.listeners.EventListener;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
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
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

public class LoggingListener {

    GenericCache<Long, DiscordServerProperties> cache = Icicle.getGenericCache();
    boolean messageLogsEnabled;
    boolean guildLogsEnabled;
    boolean memberLogsEnabled;
    boolean punishmentLogEnabled;

    @EventListener
    public Mono<Void> on(MessageDeleteEvent event) {

        if (!messageLogsEnabled) {
            return Mono.empty();
        }

        DatabaseLoader.openConnectionIfClosed();
        DiscordServer server = DiscordServer.findFirst("server_id = ?", event.getGuild().block().getId().asLong());

        if (server == null) {
            return Mono.empty();
        }

        DiscordServerProperties properties = DiscordServerProperties.findFirst("id = ?", server.getServerId());

        if (properties == null) {
            return Mono.empty();
        }



        return Mono.empty();
    }

    public void logMessageDelete(MessageDeleteEvent event) {

    }

    @EventListener
    public Mono<Void> on(MessageUpdateEvent event) {

        if (!messageLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MessageBulkDeleteEvent event) {

        if (!messageLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {

        if (!memberLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberLeaveEvent event) {

        if (!memberLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberUpdateEvent event) { // Nickname or role updates

        if (!memberLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(PresenceUpdateEvent event) { // Username, discrim, avatar changes

        if (!memberLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(InviteCreateEvent event) {

        if (!guildLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(PinsUpdateEvent event) {

        if (!guildLogsEnabled) {
            return Mono.empty();
        }

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

        if (!guildLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleDeleteEvent event) {

        if (!guildLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleUpdateEvent event) {

        if (!guildLogsEnabled) {
            return Mono.empty();
        }

        return Mono.empty();
    }

}
