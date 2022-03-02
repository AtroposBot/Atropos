package dev.laarryy.atropos.listeners.logging;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.listeners.EventListener;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.models.guilds.ServerBlacklist;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.utils.LogExecutor;
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
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

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
            case "modmail" -> serverProperties.getModMailChannelSnowflake();
            default -> null;
        };

        if (logChannelSnowflake == null) {
            return Mono.empty();
        }

        return guild.getChannelById(Snowflake.of(logChannelSnowflake)).ofType(TextChannel.class);
    }

    public Mono<Void> onAttemptedInsubordination(ChatInputInteractionEvent event, Member target) {
        return event.getInteraction().getGuild()
            .flatMap(guild -> getLogChannel(guild, "punishment"))
            .flatMap(channel -> LogExecutor.logInsubordination(event, channel, target));
    }

    public Mono<Void> onAttemptedInsubordination(ButtonInteractionEvent event, Member target) {
        return event.getInteraction().getGuild()
            .flatMap(guild -> getLogChannel(guild, "punishment"))
            .flatMap(channel -> LogExecutor.logInsubordination(event, channel, target));
    }

    public Mono<Void> onBlacklistTrigger(MessageCreateEvent event, ServerBlacklist blacklist, Punishment punishment) {
        return event.getGuild()
            .flatMap(guild -> getLogChannel(guild, "punishment"))
            .flatMap(channel -> LogExecutor.logBlacklistTrigger(event, blacklist, punishment, channel));
    }

    public Mono<Void> onPunishment(ChatInputInteractionEvent event, Punishment punishment) {
        return event.getInteraction().getGuild()
            .flatMap(guild -> getLogChannel(guild, "punishment"))
            .flatMap(channel -> LogExecutor.logPunishment(punishment, channel));
    }

    public Mono<Void> onPunishment(MessageCreateEvent event, Punishment punishment) {
        return event.getGuild()
            .flatMap(guild -> getLogChannel(guild, "punishment"))
            .flatMap(channel -> LogExecutor.logPunishment(punishment, channel));
    }

    public Mono<Void> onPunishment(ButtonInteractionEvent event, Punishment punishment) {
        return event.getInteraction().getGuild()
            .flatMap(guild -> getLogChannel(guild, "punishment"))
            .flatMap(channel -> LogExecutor.logPunishment(punishment, channel));
    }

    public void onScamMute(MessageCreateEvent event, Punishment punishment) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        getLogChannel(guild, "modmail").subscribe(textChannel -> {
            if (textChannel == null) return;
            LogExecutor.logAutoMute(punishment, textChannel);
        });

    }

    public void onBlacklistMute(MessageCreateEvent event, Punishment punishment) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        getLogChannel(guild, "modmail").subscribe(textChannel -> {
            if (textChannel == null) return;
            LogExecutor.logAutoMute(punishment, textChannel);
        });

    }

    public void onUnban(Guild guild, String reason, Punishment punishment) {
        if (guild == null)  {
            return;
        }

        getLogChannel(guild, "punishment")
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
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logPunishmentUnmute(textChannel, reason, punishment);
                    }
                })
                .subscribe();
    }

    public void onMutedRoleDelete(Guild guild, Long roleId) {
        if (guild == null) {
            return;
        }

        getLogChannel(guild, "guild")
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logMutedRoleDelete(roleId, textChannel);
                    }
                })
                .subscribe();
    }

    public void onMuteNotApplicable(Guild guild, Member memberToMute) {
        if (guild == null) {
            return;
        }

        getLogChannel(guild, "punishment")
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logMuteNotApplicable(memberToMute, textChannel);
                    }
                })
                .subscribe();
    }

    public void onStopJoinsEnable(Guild guild) {
        if (guild == null) {
            return;
        }
        getLogChannel(guild, "guild")
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logStopJoinsEnabled(textChannel);
                    }
                })
                .subscribe();
    }

    public void onStopJoinsDisable(Guild guild) {
        if (guild == null) {
            return;
        }

        getLogChannel(guild, "guild")
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logStopJoinsDisabled(textChannel);
                    }
                })
                .subscribe();
    }

    @EventListener
    public Mono<Void> on(MessageDeleteEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "message")
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

        try {
            event.getMessage().block();
        } catch (Exception e) {
            return Mono.empty();
        }

        if (event.getMessage().block().getAuthor().isPresent() && event.getMessage().block().getAuthor().get().isBot()) {
            return Mono.empty();
        }

        getLogChannel(guild, "message")
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
        wait(2000);
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
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
        if (event.getUser().equals(event.getClient().getSelf().block())) {
            return Mono.empty();
        }
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
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
        if (event.getMember().block().equals(event.getGuild().block().getSelfMember().block())) {
            return Mono.empty();
        }
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "member")
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
        wait(2000);
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();
        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
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
        wait(2000);
        Guild guild = event.getCurrent().getGuild().block();
        if (guild == null) return Mono.empty();

        getLogChannel(guild, "guild")
                .doOnSuccess(textChannel -> {
                    if (textChannel != null) {
                        LogExecutor.logRoleUpdate(event, textChannel);
                    }
                })
                .subscribe();
        return Mono.empty();
    }

}
