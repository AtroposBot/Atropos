package dev.laarryy.eris.listeners;

import dev.laarryy.eris.commands.punishments.ManualPunishmentEnder;
import dev.laarryy.eris.commands.punishments.PunishmentManager;
import dev.laarryy.eris.listeners.logging.LoggingListener;
import dev.laarryy.eris.managers.LoggingListenerManager;
import dev.laarryy.eris.managers.PunishmentManagerManager;
import dev.laarryy.eris.models.guilds.DiscordServer;
import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import dev.laarryy.eris.models.users.DiscordUser;
import dev.laarryy.eris.models.users.Punishment;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.AuditLogger;
import dev.laarryy.eris.utils.CommandChecks;
import dev.laarryy.eris.utils.Notifier;
import dev.laarryy.eris.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ButtonUseListener {

    PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    ManualPunishmentEnder manualPunishmentEnder = new ManualPunishmentEnder();
    PermissionChecker permissionChecker = new PermissionChecker();
    private static final Pattern BAN = Pattern.compile("(.*)-eris-scamban-(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);
    private static final Pattern UNMUTE = Pattern.compile("(.*)-eris-unmute-(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);


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
        Matcher unmute = UNMUTE.matcher(id);


        if (ban.matches()) {
            DatabaseLoader.openConnectionIfClosed();
            String punishmentId = ban.group(1);
            String userId = ban.group(2);
            banUser(punishmentId, userId, guild, mod, event);
        }

        if (unmute.matches()) {
            DatabaseLoader.openConnectionIfClosed();
            String punishmentId = unmute.group(1);
            String userId = unmute.group(2);
            unmuteUser(punishmentId, userId, guild, event);
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

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
        Punishment initialMute = getPunishmentFromId(punishmentId);

        punishmentManager.discordBanUser(guild, discordUser.getUserIdSnowflake(), 0, "Scam Links");

        Punishment punishment = Punishment.create(
                "user_id_punished", discordUser.getUserId(),
                "user_id_punisher", moderator.getUserId(),
                "server_id", discordServer.getServerId(),
                "punishment_type", "mute",
                "punishment_date", Instant.now().toEpochMilli(),
                "punishment_message", "Banned for scam link. Original reason: " + initialMute.getPunishmentMessage(),
                "did_dm", false,
                "end_date_passed", true,
                "punishment_end_reason", "No reason provided.");
        punishment.save();
        punishment.refresh();

        loggingListener.onPunishment(event, punishment);
        Notifier.notifyPunisherOfBan(event, punishment, punishment.getPunishmentMessage());
        AuditLogger.addCommandToDB(event, auditString, true);
        DatabaseLoader.closeConnectionIfOpen();
        event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.danger("it-worked", "User Banned").disabled())).block();

    }

    private void unmuteUser(String punishmentId, String userId, Guild guild, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        Punishment mute = getPunishmentFromId(punishmentId);
        Member mutedUser = guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).block();
        String reason = "Unmuted by moderators after scam-link review of case `" + mute.getPunishmentId() + "`, with original reason " + mute.getPunishmentMessage();
        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        Long mutedRoleId = serverProperties.getMutedRoleSnowflake();

        String auditString = "Button: Unmute " + discordUser.getUserIdSnowflake() + " for case " + mute.getPunishmentId();

        if (!CommandChecks.commandChecks(event, "unmute")) {
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
        if (mutedRole != null && mutedUser.getRoles().any(role -> role.equals(mutedRole)).block()) {
            mutedUser.removeRole(Snowflake.of(mutedRoleId), reason).block();
            manualPunishmentEnder.databaseEndPunishment(discordUser.getUserIdSnowflake(), guild, "unmute", reason);
            AuditLogger.addCommandToDB(event, auditString, true);
            Notifier.notifyModOfUnmute(event, mutedUser.getDisplayName(), reason);
            event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.success("it-worked", "User Unmuted").disabled())).block();
            DatabaseLoader.closeConnectionIfOpen();
            return;
        } else {
            Notifier.notifyCommandUserOfError(event, "userNotMuted");
            AuditLogger.addCommandToDB(event, auditString, false);
            DatabaseLoader.closeConnectionIfOpen();
            return;
        }
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
