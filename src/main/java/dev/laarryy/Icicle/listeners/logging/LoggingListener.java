package dev.laarryy.Icicle.listeners.logging;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.listeners.EventListener;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.LogExecutor;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.InviteCreateEvent;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.guild.UnbanEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Objects;

public final class LoggingListener {
    private final Logger logger = LogManager.getLogger(this);
    LoadingCache<Long, DiscordServerProperties> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(aLong -> {
                DatabaseLoader.openConnectionIfClosed();
                return DiscordServerProperties.findFirst("server_id_snowflake = ?", aLong);
            });

    public LoggingListener() {

    }

    private Mono<TextChannel> getLogChannel(Guild guild, String type) {
        DatabaseLoader.openConnectionIfClosed();
        Long guildIdSnowflake = guild.getId().asLong();
        DiscordServerProperties serverProperties = cache.get(guildIdSnowflake);
        if (serverProperties == null) {
            logger.info("serverProperties is null");
            return null;
        }

        Long logChannelSnowflake = switch (type) {
            case "member" -> serverProperties.getMemberLogChannelSnowflake();
            case "message" -> serverProperties.getMessageLogChannelSnowflake();
            case "guild" -> serverProperties.getGuildLogChannelSnowflake();
            case "punishment" -> serverProperties.getPunishmentLogChannelSnowflake();
            default -> null;
        };

        if (logChannelSnowflake == null) {
            logger.info("logChannelSnowflake is null");
            return null;
        }

        GuildChannel channel = guild.getChannelById(Snowflake.of(logChannelSnowflake)).block();

        if (!(channel instanceof TextChannel)) {
            logger.info("channel isn't a textchannel");
            return null;
        }

        return Mono.just((TextChannel) channel);
    }

    public void onPunishment(SlashCommandEvent event, Punishment punishment) {
        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) return;

        getLogChannel(guild, "punishment")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logPunishment(punishment, textChannel);
                });
    }

    public void onUnban(Guild guild, Long unBannedId, String reason) {
        if (guild == null) return;

        getLogChannel(guild, "punishment")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logPunishmentUnban(unBannedId, textChannel, reason);
                });
    }

    public void onUnmute(Member member, Long unMutedId, String reason) {
        Guild guild = member.getGuild().block();
        if (guild == null) return;

        getLogChannel(guild, "punishment")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    LogExecutor.logPunishmentUnmute(unMutedId, textChannel, reason);
                });
    }

    @EventListener
    public Mono<Void> on(MessageDeleteEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "message")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logMessageDelete(event, textChannel);
                });
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MessageUpdateEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "message")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logMessageUpdate(event, textChannel);
                });
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MessageBulkDeleteEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "message")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logBulkDelete(event, textChannel);
                });
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logMemberJoin(event, textChannel);
                });
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberLeaveEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logMemberLeave(event, textChannel);
                });
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberUpdateEvent event) { // Nickname or role updates
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "member")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logMemberUpdate(event, textChannel);
                });
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(PresenceUpdateEvent event) { // Username, discrim, avatar changes
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "member")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logPresenceUpdate(event, textChannel);
                });
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(InviteCreateEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logInviteCreate(event, textChannel);
                });
        return Mono.empty();
    }

    // TODO: Look into logging channel stuff (hopefully the events thing in d4j is resolved kek)

    @EventListener
    public Mono<Void> on(BanEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logBan(event, textChannel);
                });

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(UnbanEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logUnban(event, textChannel);
                });

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleCreateEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logRoleCreate(event, textChannel);
                });

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleDeleteEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logRoleDelete(event, textChannel);
                });

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleUpdateEvent event) {
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .filter(Objects::nonNull)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(textChannel -> {
                    if (textChannel == null) return;
                    LogExecutor.logRoleUpdate(event, textChannel);
                });
        return Mono.empty();
    }

}
