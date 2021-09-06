package dev.laarryy.Icicle.commands.punishments;

import dev.laarryy.Icicle.utils.AuditLogger;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.Notifier;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

public final class ManualPunishmentEnder {
    private static final Logger logger = LogManager.getLogger(ManualPunishmentEnder.class);

    private ManualPunishmentEnder() {
    }

    public static void endPunishment(SlashCommandEvent event, ApplicationCommandRequest request) {

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
                    .doOnComplete(() -> event.reply("Done.").withEphemeral(true).subscribe())
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(lo -> databaseEndPunishment(lo, event, reason));
        }

        if (event.getOption("user").isPresent() && event.getOption("user").get().getValue().isPresent()) {
            logger.info("user to unmute found");
            Mono.just(event.getOption("user").get().getValue().get())
                    .filter(returnVal -> returnVal.asUser().block() != null)
                    .flatMap(ApplicationCommandInteractionOptionValue::asUser)
                    .flatMap(user -> user.asMember(event.getInteraction().getGuildId().get()))
                    .filter(member -> discordUnmute(member, event, reason))
                    .doOnSuccess(s -> event.reply("Done.").withEphemeral(true).subscribe())
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(member -> databaseEndPunishment(member.getId().asLong(), event, reason));
            logger.info("unmute mono subbed");
        }
    }

    private static boolean discordUnban(Guild guild, Long aLong, String reason) {
        try {
            guild.unban(Snowflake.of(aLong), reason);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private static boolean discordUnmute(Member member, SlashCommandEvent event, String reason) {
        logger.info("preparing to unmute discord-side");
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
                logger.info("Unmuting discord-side");
                member.removeRole(Snowflake.of(mutedRoleId), reason).block();
                AuditLogger.addCommandToDB(event, true);
                return true;
            } else {
                Notifier.notifyCommandUserOfError(event, "userNotMuted");
                AuditLogger.addCommandToDB(event, false);
                return false;
            }

    }

    private static boolean databaseEndPunishment(Long aLong, SlashCommandEvent event, String reason) {
        DatabaseLoader.openConnectionIfClosed();

        String commandName = event.getCommandName();
        logger.info("Command name - " + commandName);
        String punishmentType = switch (commandName) {
            case "unban" -> "ban";
            case "unmute" -> "mute";
            default -> "unknown";
        };

        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", aLong);
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());

        if (discordUser != null && discordServer != null) {
            DatabaseLoader.openConnectionIfClosed();
            int serverId = discordServer.getServerId();
            LazyList<Punishment> punishmentLazyList = Punishment.find("server_id = ? and user_id_punished = ? and punishment_type = ? and end_date_passed = ?",
                    serverId,
                    discordUser.getUserId(),
                    punishmentType,
                    false);

            logger.info("punishment list size: " + punishmentLazyList.size());

            Flux.fromIterable(punishmentLazyList)
                    .filter(punishment -> {
                                if (punishment != null) {
                                    logger.info("non-null punishment");
                                    return true;
                                } else return false;
                            }
                    )
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(punishment -> {
                        logger.info("ending DB punishment: " + punishment.getPunishmentType());
                        punishment.setEnded(true);
                        punishment.setEndDate(Instant.now().toEpochMilli());
                        punishment.setEndReason(reason);
                        punishment.save();
                    });
            logger.info("database punishment updated as ended.");
            return true;
        } else {
            return false;
        }
    }
}