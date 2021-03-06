package dev.laarryy.atropos.commands.punishments;

import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.Notifier;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public final class ManualPunishmentEnder {
    private static final Logger logger = LogManager.getLogger(ManualPunishmentEnder.class);
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();

    public ManualPunishmentEnder() {}

    public void endPunishment(ChatInputInteractionEvent event) {

        DatabaseLoader.openConnectionIfClosed();

        if (event.getInteraction().getGuild().block() == null || event.getInteraction().getGuildId().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        String reason;
        if (event.getOption("reason").isPresent() && event.getOption("reason").get().getValue().isPresent()) {
            reason = event.getOption("reason").get().getValue().get().asString();
        } else reason = "No reason provided.";

        if (event.getOption("id").isPresent() && event.getOption("id").get().getValue().isPresent()) {
            Flux.fromArray(event.getOption("id").get().getValue().get().asString().split(" "))
                    .map(Long::valueOf)
                    .onErrorReturn(Exception.class, 0L)
                    .filter(aLong -> aLong != 0)
                    .filter(aLong -> discordUnban(event.getInteraction().getGuild().block(), aLong, reason))
                    
                    .subscribe(lo -> {
                        databaseEndPunishment(lo, event.getInteraction().getGuild().block(), event.getCommandName(), reason, event.getInteraction().getUser(), event.getClient().getUserById(Snowflake.of(lo)).block());
                        Notifier.notifyModOfUnban(event, reason, lo);
                    });
        }

        if (event.getOption("user").isPresent() && event.getOption("user").get().getValue().isPresent()) {
            Mono.just(event.getOption("user").get().getValue().get())
                    .filter(returnVal -> returnVal.asUser().block() != null)
                    .flatMap(ApplicationCommandInteractionOptionValue::asUser)
                    .flatMap(user -> user.asMember(event.getInteraction().getGuildId().get()))
                    .filter(member -> discordUnmute(member, event, reason))
                    
                    .subscribe(member -> databaseEndPunishment(member.getId().asLong(), event.getInteraction().getGuild().block(), event.getCommandName(), reason, event.getInteraction().getUser(), member));
        }
        DatabaseLoader.closeConnectionIfOpen();
    }

    public boolean discordUnban(Guild guild, Long unbannedUserSnowflake, String reason) {
        try {
            guild.getClient().getUserById(Snowflake.of(unbannedUserSnowflake));
            guild.unban(Snowflake.of(unbannedUserSnowflake), reason).block();
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean discordUnmute(Member member, ChatInputInteractionEvent event, String reason) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", event.getInteraction().getGuildId().get().asLong());
        Long mutedRoleId = serverProperties.getMutedRoleSnowflake();
        if (mutedRoleId == null) {
            Notifier.notifyCommandUserOfError(event, "noMutedRole");
            AuditLogger.addCommandToDB(event, false);
            return false;
        }
        Role mutedRole;
        try {
            mutedRole = event.getInteraction().getGuild().block().getRoleById(Snowflake.of(mutedRoleId)).block();
        } catch (NullPointerException exception) {
            Notifier.notifyCommandUserOfError(event, "noMutedRole");
            AuditLogger.addCommandToDB(event, false);
            return false;
        }
            if (mutedRole != null && member.getRoles().any(role -> role.equals(mutedRole)).block()) {
                member.removeRole(Snowflake.of(mutedRoleId), reason).block();
                AuditLogger.addCommandToDB(event, true);
                Notifier.notifyModOfUnmute(event, member.getDisplayName(), reason);
                DatabaseLoader.closeConnectionIfOpen();
                return true;
            } else {
                Notifier.notifyCommandUserOfError(event, "userNotMuted");
                AuditLogger.addCommandToDB(event, false);
                DatabaseLoader.closeConnectionIfOpen();
                return false;
            }
    }

    public boolean discordUnmute(Member member, ButtonInteractionEvent event, String reason, String entry) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", event.getInteraction().getGuildId().get().asLong());
        Long mutedRoleId = serverProperties.getMutedRoleSnowflake();
        if (mutedRoleId == null) {
            Notifier.notifyCommandUserOfError(event, "noMutedRole");
            AuditLogger.addCommandToDB(event, entry, false);
            return false;
        }
        Role mutedRole;
        try {
            mutedRole = event.getInteraction().getGuild().block().getRoleById(Snowflake.of(mutedRoleId)).block();
        } catch (NullPointerException exception) {
            Notifier.notifyCommandUserOfError(event, "noMutedRole");
            AuditLogger.addCommandToDB(event, entry, false);
            return false;
        }
        if (mutedRole != null && member.getRoles().any(role -> role.equals(mutedRole)).block()) {
            member.removeRole(Snowflake.of(mutedRoleId), reason).block();
            AuditLogger.addCommandToDB(event, entry, true);
            Notifier.notifyModOfUnmute(event, member.getDisplayName(), reason);
            DatabaseLoader.closeConnectionIfOpen();
            return true;
        } else {
            Notifier.notifyCommandUserOfError(event, "userNotMuted");
            AuditLogger.addCommandToDB(event, entry,false);
            DatabaseLoader.closeConnectionIfOpen();
            return false;
        }
    }

    public boolean databaseEndPunishment(Long userIdSnowflake, Guild guild, String commandName, String reason, User punishmentEnder, User punishedUser) {
        DatabaseLoader.openConnectionIfClosed();

        String punishmentType = switch (commandName) {
            case "unban" -> "ban";
            case "unmute" -> "mute";
            default -> "unknown";
        };

        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
        DiscordUser punishmentEnderUser = DiscordUser.findFirst("user_id_snowflake = ?", punishmentEnder.getId().asLong());

        if (discordUser != null && discordServer != null) {
            DatabaseLoader.openConnectionIfClosed();
            int serverId = discordServer.getServerId();
            LazyList<Punishment> punishmentLazyList = Punishment.find("server_id = ? and user_id_punished = ? and punishment_type = ? and end_date_passed = ?",
                    serverId,
                    discordUser.getUserId(),
                    punishmentType,
                    false);
            LazyList<Punishment> punishmentLazyList2 = Punishment.findBySQL("select * from punishments where server_id = ? and user_id_punished = ? and punishment_type = ? and punishment_end_date is NULL and end_date_passed = ?",
                    serverId,
                    discordUser.getUserId(),
                    punishmentType,
                    true);

            punishmentLazyList.addAll(punishmentLazyList2);

            Flux.fromIterable(punishmentLazyList)
                    .filter(punishment -> {
                                if (punishment != null) {
                                    return true;
                                } else return false;
                            }
                    )
                    
                    .subscribe(punishment -> {
                        DatabaseLoader.openConnectionIfClosed();
                        punishment.setEnded(true);
                        punishment.setEndDate(Instant.now().toEpochMilli());
                        punishment.setEndReason(reason);
                        punishment.setPunishmentEnder(punishmentEnderUser.getUserId());
                        punishment.setPunishmentEnderName(punishmentEnder.getUsername());
                        punishment.setPunishmentEnderDiscrim(punishmentEnder.getDiscriminator());
                        punishment.setAutomaticEnd(false);
                        punishment.save();
                        punishment.refresh();

                        if (punishmentType.equals("mute")) {
                            loggingListener.onUnmute(guild, reason, punishment);
                        }
                        if (punishmentType.equals("ban")) {
                            loggingListener.onUnban(guild, reason, punishment);
                        }
                    });
            DatabaseLoader.closeConnectionIfOpen();
            return true;
        } else {
            DatabaseLoader.closeConnectionIfOpen();
            return false;
        }
    }
}