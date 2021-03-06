package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.commands.punishments.ManualPunishmentEnder;
import dev.laarryy.atropos.commands.punishments.PunishmentManager;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import dev.laarryy.atropos.managers.PunishmentManagerManager;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.CommandChecks;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ButtonUseListener {

    PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    ManualPunishmentEnder manualPunishmentEnder = new ManualPunishmentEnder();
    PermissionChecker permissionChecker = new PermissionChecker();
    private final Logger logger = LogManager.getLogger(this);
    private static final Pattern BAN = Pattern.compile("(.*)-atropos-ban-(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);
    private static final Pattern UNMUTE = Pattern.compile("(.*)-atropos-unmute-(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);
    private static final Pattern KICK = Pattern.compile("(.*)-atropos-kick-(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);


    @EventListener
    public Mono<Void> on(ButtonInteractionEvent event) {

        if (event.getInteraction().getGuild().blockOptional().isEmpty()
                || event.getInteraction().getMember().isEmpty()) {
            return Mono.empty();
        }

        Guild guild = event.getInteraction().getGuild().block();

        if (guild == null) {
            return Mono.empty();
        }

        Member mod = event.getInteraction().getMember().get();
        String id = event.getCustomId();
        Matcher ban = BAN.matcher(id);
        Matcher kick = KICK.matcher(id);
        Matcher unmute = UNMUTE.matcher(id);


        if (ban.matches()) {
            DatabaseLoader.openConnectionIfClosed();
            String punishmentId = ban.group(1);
            String userId = ban.group(2);
            banUser(punishmentId, userId, guild, mod, event);
        }

        if (kick.matches()) {
            DatabaseLoader.openConnectionIfClosed();
            String punishmentId = kick.group(1);
            String userId = kick.group(2);
            kickUser(punishmentId, userId, guild, mod, event);
        }

        if (unmute.matches()) {
            DatabaseLoader.openConnectionIfClosed();
            String punishmentId = unmute.group(1);
            String userId = unmute.group(2);
            unmuteUser(punishmentId, userId, guild, mod, event);
        }

        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }

    private void banUser(String punishmentId, String userId, Guild guild, Member mod, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        DiscordUser moderator = DiscordUser.findFirst("user_id_snowflake = ?", mod.getId().asLong());

        String auditString = "Button: Ban " + discordUser.getUserIdSnowflake() + " for case " + punishmentId;

        if (!CommandChecks.commandChecks(event, "ban")) {
            AuditLogger.addCommandToDB(event, auditString, false);
            return;
        }

        Member punisher = event.getInteraction().getMember().get();
        Member punished = guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).block();

        if (!punishmentManager.checkIfPunisherHasHighestRole(punisher, punished, guild, event)) {
            AuditLogger.addCommandToDB(event, auditString, false);
            return;
        }

        event.deferReply().block();

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
        Punishment initialMute = getPunishmentFromId(punishmentId);

        String reason = "Banned after moderator review. " +
                "Original case number `" + initialMute.getPunishmentId() + "` with reason:\n > " + initialMute.getPunishmentMessage();

        Punishment punishment = Punishment.create(
                "user_id_punished", discordUser.getUserId(),
                "name_punished", punished.getUsername(),
                "discrim_punished", punished.getDiscriminator(),
                "user_id_punisher", moderator.getUserId(),
                "name_punisher", punisher.getUsername(),
                "discrim_punisher", punisher.getDiscriminator(),
                "server_id", discordServer.getServerId(),
                "punishment_type", "ban",
                "punishment_date", Instant.now().toEpochMilli(),
                "punishment_message", reason,
                "did_dm", true,
                "end_date_passed", false,
                "permanent", true,
                "automatic", false,
                "punishment_end_reason", "No reason provided.");
        punishment.save();
        punishment.refresh();

        loggingListener.onPunishment(event, punishment);
        Notifier.notifyPunisherOfBan(event, punishment, punishment.getPunishmentMessage());
        Notifier.notifyPunished(guild, punishment, reason);
        AuditLogger.addCommandToDB(event, auditString, true);

        punishmentManager.discordBanUser(guild, discordUser.getUserIdSnowflake(), 1, reason);
        DatabaseLoader.closeConnectionIfOpen();
        event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.danger("it-worked", "User Banned").disabled())).block();
    }

    private void kickUser(String punishmentId, String userId, Guild guild, Member mod, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        DiscordUser moderator = DiscordUser.findFirst("user_id_snowflake = ?", mod.getId().asLong());

        String auditString = "Button: Kick " + discordUser.getUserIdSnowflake() + " for case " + punishmentId;

        if (!CommandChecks.commandChecks(event, "kick")) {
            AuditLogger.addCommandToDB(event, auditString, false);
            return;
        }

        Member punisher = event.getInteraction().getMember().get();
        Member punished = guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).block();

        if (!punishmentManager.checkIfPunisherHasHighestRole(punisher, punished, guild, event)) {
            AuditLogger.addCommandToDB(event, auditString, false);
            return;
        }

        event.deferReply().block();

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
        Punishment initialMute = getPunishmentFromId(punishmentId);

        String reason = "Kicked after moderator review. " +
                "Original case number `" + initialMute.getPunishmentId() + "` with reason:\n > " + initialMute.getPunishmentMessage();

        Punishment punishment = Punishment.create(
                "user_id_punished", discordUser.getUserId(),
                "name_punished", punished.getUsername(),
                "discrim_punished", punished.getDiscriminator(),
                "user_id_punisher", moderator.getUserId(),
                "name_punisher", punisher.getUsername(),
                "discrim_punisher", punisher.getDiscriminator(),
                "server_id", discordServer.getServerId(),
                "punishment_type", "kick",
                "punishment_date", Instant.now().toEpochMilli(),
                "punishment_message", reason,
                "did_dm", true,
                "end_date_passed", true,
                "permanent", true,
                "automatic", false,
                "punishment_end_reason", "No reason provided.");
        punishment.save();
        punishment.refresh();

        loggingListener.onPunishment(event, punishment);
        Notifier.notifyPunisherOfKick(event, punishment, punishment.getPunishmentMessage());
        Notifier.notifyPunished(guild, punishment, reason);
        AuditLogger.addCommandToDB(event, auditString, true);

        punishmentManager.discordKickUser(guild, discordUser.getUserIdSnowflake(), reason);
        DatabaseLoader.closeConnectionIfOpen();
        event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.danger("it-worked", "User Kicked").disabled())).block();
    }

    private void unmuteUser(String punishmentId, String userId, Guild guild, Member moderator, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        Punishment punishment = getPunishmentFromId(punishmentId);

        Member mutedUser = guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).block();
        String reason = "Unmuted by moderators after review of case `" + punishment.getPunishmentId() + "`, with original reason:\n > " + punishment.getPunishmentMessage();
        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        Long mutedRoleId = serverProperties.getMutedRoleSnowflake();

        String auditString = "Button: Unmute " + discordUser.getUserIdSnowflake() + " for case " + punishment.getPunishmentId();

        if (!CommandChecks.commandChecks(event, "unmute")) {
            AuditLogger.addCommandToDB(event, auditString, false);
            return;
        }

        Member punisher = event.getInteraction().getMember().get();

        if (!punishmentManager.checkIfPunisherHasHighestRole(punisher, mutedUser, guild, event)) {
            AuditLogger.addCommandToDB(event, auditString, false);
            return;
        }

        if (mutedRoleId == null) {
            Notifier.notifyCommandUserOfError(event, "noMutedRole");
            AuditLogger.addCommandToDB(event, auditString, false);
            DatabaseLoader.closeConnectionIfOpen();
            return;
        }

        Role mutedRole;
        try {
            mutedRole = guild.getRoleById(Snowflake.of(mutedRoleId)).block();
        } catch (NullPointerException exception) {
            Notifier.notifyCommandUserOfError(event, "noMutedRole");
            AuditLogger.addCommandToDB(event, auditString, false);
            DatabaseLoader.closeConnectionIfOpen();
            return;
        }

        event.deferReply().block();

        if (mutedRole != null && mutedUser.getRoles().any(role -> role.equals(mutedRole)).block()) {
            mutedUser.removeRole(Snowflake.of(mutedRoleId), reason).block();
            manualPunishmentEnder.databaseEndPunishment(discordUser.getUserIdSnowflake(), guild, "unmute", reason, moderator, mutedUser);
            AuditLogger.addCommandToDB(event, auditString, true);
            Notifier.notifyModOfUnmute(event, mutedUser.getDisplayName(), reason);
            event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.success("it-worked", "User Unmuted").disabled())).block();
        } else {
            Notifier.notifyCommandUserOfError(event, "userNotMuted");
            AuditLogger.addCommandToDB(event, auditString, false);
        }

        DatabaseLoader.closeConnectionIfOpen();
    }


    private Punishment getPunishmentFromId(String punishmentId) {
        DatabaseLoader.openConnectionIfClosed();
        return Punishment.findFirst("id = ?", Integer.valueOf(punishmentId));
    }

    private DiscordUser getDiscordUserFromId(String userId) {
        DatabaseLoader.openConnectionIfClosed();
        return DiscordUser.findFirst("id = ?", Integer.valueOf(userId));
    }
}
