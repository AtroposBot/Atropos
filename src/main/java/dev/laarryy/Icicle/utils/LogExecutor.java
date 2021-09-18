package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
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
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;

import java.time.Instant;

public final class LogExecutor {
    private LogExecutor() {}

    public static void logPunishment(Punishment punishment, TextChannel logChannel) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser punishedUser = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        DiscordUser punishingUser = DiscordUser.findFirst("id = ?", punishment.getPunishingUserId());
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("New Punishment: ID #" + punishment.getPunishmentId())
                .addField("Punished User", "`" + punishedUser.getUserIdSnowflake() + "`:<@" + punishedUser.getUserIdSnowflake() + ">", false)
                .addField("Punishing User", "`" + punishingUser.getUserIdSnowflake() + "`:<@" + punishingUser.getUserIdSnowflake() + ">", false)
                .addField("Type", punishment.getPunishmentType(), false)
                .addField("Reason", punishment.getPunishmentMessage(), false)
                .footer("For more information, run /inf search case <id>", "")
                .timestamp(Instant.now())
                .build();
        logChannel.createMessage("A punishment has been recorded").subscribe();
        logChannel.createMessage(embed).subscribe();
    }

    public static void logMessageDelete(MessageDeleteEvent event, TextChannel logChannel) {
        if (event.getMessage().isPresent()) {

        }
        logChannel.createMessage("MESSAGE DELETED!").subscribe();

    }

    public static void logMessageUpdate(MessageUpdateEvent event, TextChannel logChannel) {

    }

    public static void logBulkDelete(MessageBulkDeleteEvent event, TextChannel logChannel) {

    }

    public static void logMemberJoin(MemberJoinEvent event, TextChannel logChannel) {

    }

    public static void logMemberLeave(MemberLeaveEvent event, TextChannel logChannel) {

    }

    public static void logMemberUpdate(MemberUpdateEvent event, TextChannel logChannel) {

    }

    public static void logPresenceUpdate(PresenceUpdateEvent event, TextChannel logChannel) {

    }

    public static void logInviteCreate(InviteCreateEvent event, TextChannel logChannel) {

    }

    public static void logNewsCreate(NewsChannelCreateEvent event, TextChannel logChannel) {

    }

    public static void logNewsDelete(NewsChannelDeleteEvent event, TextChannel logChannel) {

    }

    public static void logNewsUpdate(NewsChannelUpdateEvent event, TextChannel logChannel) {

    }

    public static void logStoreCreate(StoreChannelCreateEvent event, TextChannel logChannel) {

    }

    public static void logStoreDelete(StoreChannelDeleteEvent event, TextChannel logChannel) {

    }

    public static void logStoreUpdate(StoreChannelUpdateEvent event, TextChannel logChannel) {

    }

    public static void logVoiceCreate(VoiceChannelCreateEvent event, TextChannel logChannel) {

    }

    public static void logVoiceDelete(VoiceChannelDeleteEvent event, TextChannel logChannel) {

    }

    public static void logVoiceUpdate(VoiceChannelUpdateEvent event, TextChannel logChannel) {

    }

    public static void logTextCreate(TextChannelCreateEvent event, TextChannel logChannel) {

    }

    public static void logTextDelete(TextChannelDeleteEvent event, TextChannel logChannel) {

    }

    public static void logTextUpdate(TextChannelUpdateEvent event, TextChannel logChannel) {

    }

    public static void logBan(BanEvent event, TextChannel logChannel) {

    }

    public static void logUnban(UnbanEvent event, TextChannel logChannel) {

    }

    public static void logRoleCreate(RoleCreateEvent event, TextChannel logChannel) {

    }

    public static void logRoleDelete(RoleDeleteEvent event, TextChannel logChannel) {

    }

    public static void logRoleUpdate(RoleUpdateEvent event, TextChannel logChannel) {

    }

    public static void logPunishmentUnban(Long unBannedUserId, TextChannel logChannel, String reason) {

    }

    public static void logPunishmentUnmute(Long unMutedUserId, TextChannel logChannel, String reason) {

    }

}
