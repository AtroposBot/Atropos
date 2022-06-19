package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.commands.punishments.ManualPunishmentEnder;
import dev.laarryy.atropos.commands.punishments.PunishmentManager;
import dev.laarryy.atropos.exceptions.NoMutedRoleException;
import dev.laarryy.atropos.exceptions.NoPermissionsException;
import dev.laarryy.atropos.exceptions.NoUserException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.exceptions.UserNotMutedExcception;
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

        return event.getInteraction().getGuild().flatMap(guild -> {
            if (event.getInteraction().getMember().isEmpty()) {
                return Mono.error(new NoUserException("Member Not Found"));
            }

            Member member = event.getInteraction().getMember().get();
            if (guild == null) {
                return Mono.error(new NullServerException("No Server"));
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
                return banUser(punishmentId, userId, guild, mod, event);
            }

            if (kick.matches()) {
                DatabaseLoader.openConnectionIfClosed();
                String punishmentId = kick.group(1);
                String userId = kick.group(2);
                return kickUser(punishmentId, userId, guild, mod, event);
            }

            if (unmute.matches()) {
                DatabaseLoader.openConnectionIfClosed();
                String punishmentId = unmute.group(1);
                String userId = unmute.group(2);
                return unmuteUser(punishmentId, userId, guild, mod, event);
            }

            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        });
    }

    private Mono<Void> banUser(String punishmentId, String userId, Guild guild, Member mod, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        DiscordUser moderator = DiscordUser.findFirst("user_id_snowflake = ?", mod.getId().asLong());

        String auditString = "Button: Ban " + discordUser.getUserIdSnowflake() + " for case " + punishmentId;

        return CommandChecks.commandChecks(event, "ban").flatMap(aBoolean -> {
            if (!aBoolean) {
                AuditLogger.addCommandToDB(event, auditString, false);
                return Mono.error(new NoPermissionsException("No Permission"));
            }

            Member punisher = event.getInteraction().getMember().get();

            return guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).flatMap(punished ->
                    punishmentManager.checkIfPunisherHasHighestRole(punisher, punished, guild, event).flatMap(theBool -> {

                        if (!theBool) {
                            AuditLogger.addCommandToDB(event, auditString, false);
                            return Mono.error(new NoPermissionsException("No Permission")).then();
                        }

                        return event.deferReply().flatMap(unused -> {
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
                            AuditLogger.addCommandToDB(event, auditString, true);
                            DatabaseLoader.closeConnectionIfOpen();

                            return loggingListener.onPunishment(event, punishment)
                                    .then(Notifier.notifyPunisherOfBan(event, punishment, punishment.getPunishmentMessage()))
                                    .then(Notifier.notifyPunished(guild, punishment, reason))
                                    .then(punishmentManager.discordBanUser(guild, discordUser.getUserIdSnowflake(), 1, reason))
                                    .then(event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.danger("it-worked", "User Banned").disabled())))
                                    .then();
                        });
                    }));
        });
    }

    private Mono<Void> kickUser(String punishmentId, String userId, Guild guild, Member mod, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        DiscordUser moderator = DiscordUser.findFirst("user_id_snowflake = ?", mod.getId().asLong());

        String auditString = "Button: Kick " + discordUser.getUserIdSnowflake() + " for case " + punishmentId;

        return CommandChecks.commandChecks(event, "kick").flatMap(aBoolean -> {

            if (!aBoolean) {
                AuditLogger.addCommandToDB(event, auditString, false);
                return Mono.error(new NoPermissionsException("No Permission"));
            }


            Member punisher = event.getInteraction().getMember().get();
            return guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).flatMap(punished ->
                    punishmentManager.checkIfPunisherHasHighestRole(punisher, punished, guild, event).flatMap(theBool -> {
                        if (!theBool) {
                            AuditLogger.addCommandToDB(event, auditString, false);
                            return Mono.error(new NoPermissionsException("No Permission")).then();
                        }

                        return event.deferReply().flatMap(unused -> {
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
                            DatabaseLoader.closeConnectionIfOpen();
                            AuditLogger.addCommandToDB(event, auditString, true);

                            return loggingListener.onPunishment(event, punishment)
                                    .then(Notifier.notifyPunisherOfKick(event, punishment, punishment.getPunishmentMessage()))
                                    .then(Notifier.notifyPunished(guild, punishment, reason))
                                    .then(punishmentManager.discordKickUser(guild, discordUser.getUserIdSnowflake(), reason))
                                    .then(event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.danger("it-worked", "User Kicked").disabled())))
                                    .then();
                        });
                    }));
        });
    }

    private Mono<Void> unmuteUser(String punishmentId, String userId, Guild guild, Member moderator, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        Punishment punishment = getPunishmentFromId(punishmentId);

        return guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).flatMap(mutedUser -> {
            String reason = "Unmuted by moderators after review of case `" + punishment.getPunishmentId() + "`, with original reason:\n > " + punishment.getPunishmentMessage();
            DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
            Long mutedRoleId = serverProperties.getMutedRoleSnowflake();

            String auditString = "Button: Unmute " + discordUser.getUserIdSnowflake() + " for case " + punishment.getPunishmentId();

            return CommandChecks.commandChecks(event, "unmute").flatMap(aBoolean -> {
                if (!aBoolean) {
                    AuditLogger.addCommandToDB(event, auditString, false);
                    return Mono.error(new NoPermissionsException("No Permission"));
                }

                Member punisher = event.getInteraction().getMember().get();

                return punishmentManager.checkIfPunisherHasHighestRole(punisher, mutedUser, guild, event).flatMap(theBool -> {
                    if (!theBool) {
                        AuditLogger.addCommandToDB(event, auditString, false);
                        return Mono.error(new NoPermissionsException("No Permission"));
                    }

                    if (mutedRoleId == null) {
                        AuditLogger.addCommandToDB(event, auditString, false);
                        DatabaseLoader.closeConnectionIfOpen();
                        return Mono.error(new NoMutedRoleException("No Muted Role"));
                    }

                    return guild.getRoleById(Snowflake.of(mutedRoleId)).flatMap(mutedRole -> event.deferReply().flatMap(unused -> mutedUser.getRoles().any(role -> role.equals(mutedRole)).flatMap(theBoolean -> {
                        if (mutedRole != null && theBoolean) {
                            return mutedUser.removeRole(Snowflake.of(mutedRoleId), reason).flatMap(unused1 -> {
                                AuditLogger.addCommandToDB(event, auditString, true);
                                return manualPunishmentEnder.databaseEndPunishment(discordUser.getUserIdSnowflake(), guild, "unmute", reason, moderator, mutedUser)
                                        .then(Notifier.notifyModOfUnmute(event, mutedUser.getDisplayName(), reason))
                                        .then(event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.success("it-worked", "User Unmuted").disabled())))
                                        .then();
                            });
                        } else {
                            AuditLogger.addCommandToDB(event, auditString, false);
                            return Mono.error(new UserNotMutedExcception("User Not Muted"));
                        }
                    })));
                });
            });
        });
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
