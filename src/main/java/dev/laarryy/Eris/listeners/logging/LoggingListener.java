package dev.laarryy.Eris.listeners.logging;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Eris.listeners.EventListener;
import dev.laarryy.Eris.managers.PropertiesCacheManager;
import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.models.guilds.ServerBlacklist;
import dev.laarryy.Eris.models.users.Punishment;
import dev.laarryy.Eris.utils.LogExecutor;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.InviteCreateEvent;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.event.domain.channel.NewsChannelCreateEvent;
import discord4j.core.event.domain.channel.NewsChannelDeleteEvent;
import discord4j.core.event.domain.channel.NewsChannelUpdateEvent;
import discord4j.core.event.domain.channel.StoreChannelCreateEvent;
import discord4j.core.event.domain.channel.StoreChannelDeleteEvent;
import discord4j.core.event.domain.channel.StoreChannelUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelCreateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.channel.TextChannelUpdateEvent;
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.channel.VoiceChannelUpdateEvent;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.guild.UnbanEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public final class LoggingListener {
    private final Logger logger = LogManager.getLogger(this);

    LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();

    public LoggingListener() {
    }

    private Mono<TextChannel> getLogChannel(Guild guild, String type) {
        Long guildIdSnowflake = guild.getId().asLong();
        DiscordServerProperties serverProperties = cache.get(guildIdSnowflake);
        if (serverProperties == null) {
            return Mono.empty();
        }

        Long logChannelSnowflake = switch (type) {
            case "member" -> serverProperties.getMemberLogChannelSnowflake();
            case "message" -> serverProperties.getMessageLogChannelSnowflake();
            case "guild" -> serverProperties.getGuildLogChannelSnowflake();
            case "punishment" -> serverProperties.getPunishmentLogChannelSnowflake();
            default -> null;
        };

        if (logChannelSnowflake == null) {
            return Mono.empty();
        }

        TextChannel channel;
        try {
            channel = guild.getChannelById(Snowflake.of(logChannelSnowflake)).ofType(TextChannel.class).block();
        } catch (Exception e) {
            return Mono.empty();
        }

        if (channel == null) {
            return Mono.empty();
        }

        return Mono.just(channel);
    }

    public void onAttemptedInsubordination(SlashCommandEvent event, Member target) {
        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) return;

        getLogChannel(guild, "punishment").subscribeOn(Schedulers.boundedElastic()).subscribe(textChannel -> {
            if (textChannel == null) return;
            LogExecutor.logInsubordination(event, textChannel, target);
        });
    }

    public void onBlacklistTrigger(MessageCreateEvent event, ServerBlacklist blacklist, Punishment punishment) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        getLogChannel(guild, "punishment").subscribeOn(Schedulers.boundedElastic()).subscribe(textChannel -> {
            if (textChannel == null) return;
            LogExecutor.logBlacklistTrigger(event, blacklist, punishment, textChannel);
        });
    }

    public void onPunishment(SlashCommandEvent event, Punishment punishment) {
        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) return;

        getLogChannel(guild, "punishment").subscribeOn(Schedulers.boundedElastic()).subscribe(textChannel -> {
            if (textChannel == null) return;
            LogExecutor.logPunishment(punishment, textChannel);
        });

    }

    public void onPunishment(MessageCreateEvent event, Punishment punishment) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        getLogChannel(guild, "punishment").subscribeOn(Schedulers.boundedElastic()).subscribe(textChannel -> {
            if (textChannel == null) return;
            LogExecutor.logPunishment(punishment, textChannel);
        });

    }

    public void onUnban(Guild guild, String reason, Punishment punishment) {
        if (guild == null)  {
            return;
        }

        getLogChannel(guild, "punishment")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logPunishmentUnban(textChannel, reason, punishment);
                    }
                })
                .subscribe();
    }

    public void onUnmute(Guild guild, String reason, Punishment punishment) {
        if (guild == null)  {
            return;
        }

        getLogChannel(guild, "punishment")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logPunishmentUnmute(textChannel, reason, punishment);
                    }
                })
                .subscribe();
    }

    public void onMutedRoleDelete(Guild guild, Role mutedRole) {
        if (guild == null) {
            return;
        }

        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logMutedRoleDelete(mutedRole, textChannel);
                    }
                });
    }

    @EventListener
    public Mono<Void> on(MessageDeleteEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "message")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logMessageDelete(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MessageUpdateEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        if (event.getMessage().block().getAuthor().isPresent() && event.getMessage().block().getAuthor().get().isBot()) {
            return Mono.empty();
        }

        getLogChannel(guild, "message")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logMessageUpdate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MessageBulkDeleteEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "message")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logBulkDelete(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logMemberJoin(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberLeaveEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logMemberLeave(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberUpdateEvent event) { // Nickname or role updates
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "member")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logMemberUpdate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(PresenceUpdateEvent event) { // Username, discrim, avatar changes
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "member")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logPresenceUpdate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(InviteCreateEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logInviteCreate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(NewsChannelCreateEvent event) {
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logNewsCreate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(NewsChannelDeleteEvent event) {
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logNewsDelete(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(NewsChannelUpdateEvent event) {
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logNewsUpdate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(StoreChannelCreateEvent event) {
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logStoreCreate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(StoreChannelDeleteEvent event) {
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logStoreDelete(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(StoreChannelUpdateEvent event) {
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logStoreUpdate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(VoiceChannelCreateEvent event) {
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logVoiceCreate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(VoiceChannelDeleteEvent event) {
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logVoiceDelete(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(VoiceChannelUpdateEvent event) {
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logVoiceUpdate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(TextChannelCreateEvent event) {
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logTextCreate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(TextChannelDeleteEvent event) {
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logTextDelete(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(TextChannelUpdateEvent event) {
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logTextUpdate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(BanEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logBan(event, textChannel);
                    }
                })
                .subscribe();

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(UnbanEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logUnban(event, textChannel);
                    }
                })
                .subscribe();

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleCreateEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logRoleCreate(event, textChannel);
                    }
                })
                .subscribe();

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleDeleteEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logRoleDelete(event, textChannel);
                    }
                })
                .subscribe();

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(RoleUpdateEvent event) {
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logRoleUpdate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

}
