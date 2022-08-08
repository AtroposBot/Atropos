package dev.laarryy.atropos.listeners.logging;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.exceptions.NotFoundException;
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
import discord4j.core.object.entity.Message;
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

        return guild.getChannelById(Snowflake.of(logChannelSnowflake)).ofType(TextChannel.class)
                .onErrorMap(t -> {
                    switch (type) {
                        case "member" -> serverProperties.setMemberLogChannelSnowflake(null);
                        case "message" -> serverProperties.setMessageLogChannelSnowflake(null);
                        case "guild" -> serverProperties.setGuildLogChannelSnowflake(null);
                        case "punishment" -> serverProperties.setPunishmentLogChannelSnowflake(null);
                        case "modmail" -> serverProperties.setModMailChannelSnowflake(null);
                    }
                    return new NotFoundException(t.getMessage());
                });
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

    public Mono<Void> onScamMute(MessageCreateEvent event, Punishment punishment) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "modmail"))
                .flatMap(channel -> LogExecutor.logAutoMute(punishment, channel));
    }

    public Mono<Void> onBlacklistMute(MessageCreateEvent event, Punishment punishment) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "modmail"))
                .flatMap(channel -> LogExecutor.logAutoMute(punishment, channel));
    }

    public Mono<Void> onUnban(Guild guild, String reason, Punishment punishment) {
        return getLogChannel(guild, "punishment").flatMap(channel -> LogExecutor.logPunishmentUnban(channel, reason, punishment));
    }

    public Mono<Void> onUnmute(Guild guild, String reason, Punishment punishment) {
        return getLogChannel(guild, "punishment").flatMap(channel -> LogExecutor.logPunishmentUnmute(channel, reason, punishment));
    }

    public Mono<Void> onMutedRoleDelete(Guild guild, Long roleId) {
        return getLogChannel(guild, "guild").flatMap(channel -> LogExecutor.logMutedRoleDelete(roleId, channel));
    }

    public Mono<Void> onMuteNotApplicable(Guild guild, Member memberToMute) {
        return getLogChannel(guild, "punishment").flatMap(channel -> LogExecutor.logMuteNotApplicable(memberToMute, channel));
    }

    public Mono<Void> onNoDehoistPermission(Guild guild, Member memberToDehoist) {
        return getLogChannel(guild, "punishment").flatMap(channel -> LogExecutor.logNoDehoistPermission(memberToDehoist, channel));
    }

    public Mono<Void> onStopJoinsEnable(Guild guild) {
        return getLogChannel(guild, "guild").flatMap(LogExecutor::logStopJoinsEnabled);
    }

    public Mono<Void> onStopJoinsDisable(Guild guild) {
        return getLogChannel(guild, "guild").flatMap(LogExecutor::logStopJoinsDisabled);
    }

    @EventListener
    public Mono<Void> on(MessageDeleteEvent event) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "message"))
                .flatMap(channel -> LogExecutor.logMessageDelete(event, channel));
    }

    @EventListener
    public Mono<Void> on(MessageUpdateEvent event) {

        return event.getMessage()
                .map(Message::getAuthor)
                .filter(author -> !(author.isPresent() && author.get().isBot()))    // ignore bot messages
                .flatMap($ -> event.getGuild())
                .flatMap(guild -> getLogChannel(guild, "message"))
                .flatMap(channel -> LogExecutor.logMessageUpdate(event, channel));
    }

    @EventListener
    public Mono<Void> on(MessageBulkDeleteEvent event) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "message"))
                .flatMap(channel -> LogExecutor.logBulkDelete(event, channel));
    }

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logMemberJoin(event, channel));
    }

    @EventListener
    public Mono<Void> on(MemberLeaveEvent event) {
        return event.getClient().getSelf()
                .map(event.getUser()::equals)
                .filter(isSelf -> !isSelf)
                .flatMap($ -> event.getGuild())
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logMemberLeave(event, channel));
    }

    @EventListener
    public Mono<Void> on(MemberUpdateEvent event) { // Nickname or role updates
        return event.getGuild()
                .flatMap(Guild::getSelfMember)
                .filterWhen(self -> event.getMember().map(self::equals).map(isSelf -> !isSelf))
                .flatMap($ -> event.getGuild())
                .flatMap(guild -> getLogChannel(guild, "member"))
                .flatMap(channel -> LogExecutor.logMemberUpdate(event, channel));
    }

    @EventListener
    public Mono<Void> on(PresenceUpdateEvent event) { // Username, discrim, avatar changes
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "member"))
                .flatMap(channel -> LogExecutor.logPresenceUpdate(event, channel));
    }

    @EventListener
    public Mono<Void> on(InviteCreateEvent event) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logInviteCreate(event, channel));
    }

    @EventListener
    public Mono<Void> on(NewsChannelCreateEvent event) {
        return event.getChannel().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logNewsCreate(event, channel));
    }

    @EventListener
    public Mono<Void> on(NewsChannelDeleteEvent event) {
        return event.getChannel().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logNewsDelete(event, channel));
    }

    @EventListener
    public Mono<Void> on(NewsChannelUpdateEvent event) {
        return event.getCurrent().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logNewsUpdate(event, channel));
    }

    @EventListener
    public Mono<Void> on(StoreChannelCreateEvent event) {
        return event.getChannel().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logStoreCreate(event, channel));
    }

    @EventListener
    public Mono<Void> on(StoreChannelDeleteEvent event) {
        return event.getChannel().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logStoreDelete(event, channel));
    }

    @EventListener
    public Mono<Void> on(StoreChannelUpdateEvent event) {
        return event.getCurrent().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logStoreUpdate(event, channel));
    }

    @EventListener
    public Mono<Void> on(VoiceChannelCreateEvent event) {
        return event.getChannel().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logVoiceCreate(event, channel));
    }

    @EventListener
    public Mono<Void> on(VoiceChannelDeleteEvent event) {
        return event.getChannel().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logVoiceDelete(event, channel));
    }

    @EventListener
    public Mono<Void> on(VoiceChannelUpdateEvent event) {
        return event.getCurrent().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logVoiceUpdate(event, channel));
    }

    @EventListener
    public Mono<Void> on(TextChannelCreateEvent event) {
        return event.getChannel().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logTextCreate(event, channel));
    }

    @EventListener
    public Mono<Void> on(TextChannelDeleteEvent event) {
        return event.getChannel().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logTextDelete(event, channel));
    }

    @EventListener
    public Mono<Void> on(TextChannelUpdateEvent event) {
        return event.getCurrent().getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logTextUpdate(event, channel));
    }

    @EventListener
    public Mono<Void> on(BanEvent event) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logBan(event, channel));
    }

    @EventListener
    public Mono<Void> on(UnbanEvent event) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logUnban(event, channel));
    }

    @EventListener
    public Mono<Void> on(RoleCreateEvent event) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logRoleCreate(event, channel));
    }

    @EventListener
    public Mono<Void> on(RoleDeleteEvent event) {
        return event.getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logRoleDelete(event, channel));
    }

    @EventListener
    public Mono<Void> on(RoleUpdateEvent event) {
        return event.getCurrent()
                .getGuild()
                .flatMap(guild -> getLogChannel(guild, "guild"))
                .flatMap(channel -> LogExecutor.logRoleUpdate(event, channel));
    }
}
