package dev.laarryy.atropos.commands.punishments;

import dev.laarryy.atropos.exceptions.NoMutedRoleException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.exceptions.UserNotMutedException;
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
import java.util.Objects;

public final class ManualPunishmentEnder {
    private static final Logger logger = LogManager.getLogger(ManualPunishmentEnder.class);
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();

    public ManualPunishmentEnder() {
    }

    public Mono<Void> endPunishment(ChatInputInteractionEvent event) {

        Mono<Guild> guildMono = event.getInteraction().getGuild();

        return guildMono.flatMap(guild -> {
                    if (guild == null) {
                        return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NullServerException("No Server")));
                    }

                    String reason;
                    if (event.getOption("reason").isPresent() && event.getOption("reason").get().getValue().isPresent()) {
                        reason = event.getOption("reason").get().getValue().get().asString();
                    } else reason = "No reason provided.";

                    if (event.getOption("id").isPresent() && event.getOption("id").get().getValue().isPresent()) {
                        return Flux.fromArray(event.getOption("id").get().getValue().get().asString().split(" "))
                                .map(Long::valueOf)
                                .onErrorReturn(Exception.class, 0L)
                                .filter(aLong -> aLong != 0)
                                .flatMap(lo -> event.getClient().getUserById(Snowflake.of(lo)).flatMap(user ->
                                        databaseEndPunishment(lo, guild, event.getCommandName(), reason, event.getInteraction().getUser(), user)
                                                .flatMap(unused -> guild.unban(Snowflake.of(lo), reason))
                                                .then(Notifier.notifyModOfUnban(event, reason, lo))))
                                .then();
                    }

                    if (event.getOption("user").isPresent() && event.getOption("user").get().getValue().isPresent()) {
                        return Mono.just(event.getOption("user").get().getValue().get())
                                .flatMap(ApplicationCommandInteractionOptionValue::asUser)
                                .filter(Objects::nonNull)
                                .flatMap(user -> user.asMember(guild.getId()))
                                .flatMap(member -> Mono.when(
                                        discordUnmute(member, event, reason),
                                        databaseEndPunishment(member.getId().asLong(), guild, event.getCommandName(), reason, event.getInteraction().getUser(), member)
                                ));
                    }


                    return Mono.empty();
                });
    }

    public Mono<Boolean> discordUnmute(Member member, ChatInputInteractionEvent event, String reason) {
        return Mono.defer(() -> {
            Long mutedRoleId;
            try (final var usage = DatabaseLoader.use()) {
                DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", event.getInteraction().getGuildId().get().asLong());
                mutedRoleId = serverProperties.getMutedRoleSnowflake();
            }

            if (mutedRoleId == null) {
                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NoMutedRoleException("No Muted Role")));
            }

            Mono<Role> mutedRole = event.getInteraction().getGuild().flatMap(guild -> guild.getRoleById(Snowflake.of(mutedRoleId)));

            return mutedRole.flatMap(role -> {
                if (role == null) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NoMutedRoleException("No Muted Role")));
                } else {
                    return member.getRoles().any(arole -> arole.equals(role))
                            .flatMap(aBoolean -> {

                                if (!aBoolean) {
                                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new UserNotMutedException("User Not Muted")));
                                }
                                return member.removeRole(Snowflake.of(mutedRoleId))
                                        .then(Notifier.notifyModOfUnmute(event, member.getDisplayName(), reason))
                                        .then(AuditLogger.addCommandToDB(event, true));
                            })
                            .thenReturn(true)
                            .onErrorReturn(Exception.class, false);
                }
            });
        });
    }

    public Mono<Void> databaseEndPunishment(Long userIdSnowflake, Guild guild, String commandName, String reason, User punishmentEnder, User punishedUser) {
        return Mono.defer(() -> {
            String punishmentType = switch (commandName) {
                case "unban" -> "ban";
                case "unmute" -> "mute";
                default -> "unknown";
            };

            DiscordUser discordUser;
            DiscordServer discordServer;
            DiscordUser punishmentEnderUser;
            try (final var usage = DatabaseLoader.use()) {
                discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);
                discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
                punishmentEnderUser = DiscordUser.findFirst("user_id_snowflake = ?", punishmentEnder.getId().asLong());
            }

            if (discordUser != null && discordServer != null) {
                return DatabaseLoader.use(() -> {
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

                    return Flux.fromIterable(punishmentLazyList)
                            .filter(Objects::nonNull)
                            .flatMap(punishment -> {
                                DatabaseLoader.use(() -> {
                                    punishment.setEnded(true);
                                    punishment.setEndDate(Instant.now().toEpochMilli());
                                    punishment.setEndReason(reason);
                                    punishment.setPunishmentEnder(punishmentEnderUser.getUserId());
                                    punishment.setPunishmentEnderName(punishmentEnder.getUsername());
                                    punishment.setPunishmentEnderDiscrim(punishmentEnder.getDiscriminator());
                                    punishment.setAutomaticEnd(false);
                                    punishment.save();
                                    punishment.refresh();
                                });

                                if (punishmentType.equals("mute")) {
                                    return loggingListener.onUnmute(guild, reason, punishment);
                                }
                                if (punishmentType.equals("ban")) {
                                    return loggingListener.onUnban(guild, reason, punishment);
                                }
                                return Mono.empty();
                            }).then();
                });
            } else {
                return Mono.empty();
            }
        });
    }
}
