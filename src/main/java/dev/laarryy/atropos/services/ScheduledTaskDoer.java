package dev.laarryy.atropos.services;

import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.models.guilds.ServerMessage;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;

public class ScheduledTaskDoer {

    private final Logger logger = LogManager.getLogger(this);
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    GatewayDiscordClient client;

    public Mono<Void> startTasks(GatewayDiscordClient client) {
        if (client != null) {
            this.client = client;
        } else {
            logger.error("Client is null.");
        }

        logger.info("Starting tasks");


        Mono<Void> startInterval1 = Flux.interval(Duration.ofMinutes(1))
                .doOnNext(l -> logger.info("CHECKING PUN. ENDING"))
                .flatMap(this::checkPunishmentEnding)
                .then();

        Mono<Void> startInterval2 = Flux.interval(Duration.ofDays(1))
                .doOnNext(this::wipeOldData)
                .then();

        return Mono.when(
                startInterval1,
                startInterval2);
    }

    private Mono<Void> wipeOldData(Long l) {
        Instant thirtyDaysAgoInstant = Instant.now().minus(Duration.ofDays(30));
        Long thirtyDaysAgo = thirtyDaysAgoInstant.toEpochMilli();
        LazyList<ServerMessage> messageLazyList = DatabaseLoader.use(() -> ServerMessage.find("date < ?", thirtyDaysAgo));

        return Flux.fromIterable(messageLazyList)
                .doFinally(msg -> logger.info("+++++++++++ Old Messages Wiped"))
                .doOnNext(serverMessage -> serverMessage.delete(true)).then();
    }

    private Mono<Void> checkPunishmentEnding(Long l) {

        LazyList<Punishment> punishmentsLazyList = DatabaseLoader.use(() -> Punishment.find("end_date_passed = ? and permanent = ?", false, false));

        logger.info("Checking punishment ending");

        return Flux.fromIterable(punishmentsLazyList)
                .filter(this::checkIfOverDue)
                .flatMap(p -> {
                    logger.info("Ending punishment");
                    return endPunishment(p);
                })
                .then();
    }

    private boolean checkIfOverDue(Punishment punishment) {
        logger.info("overdue check starting");
        Long endDate = punishment.getEndDate();
        if (endDate == null) return false;
        Instant endInstant = Instant.ofEpochMilli(endDate);
        Instant nowInstant = Instant.now();

        logger.info("overDue Check: " + endInstant.isBefore(nowInstant));

        return endInstant.isBefore(nowInstant);
    }

    private Mono<Void> endPunishment(Punishment punishment) {

        logger.info("Ending punishment!");


        String punishmentType = punishment.getPunishmentType();
        DiscordUser punishedUser = DatabaseLoader.use(() -> DiscordUser.findById(punishment.getPunishedUserId()));
        DiscordServer server = DatabaseLoader.use(() -> DiscordServer.findById(punishment.getServerId()));
        Snowflake userId = Snowflake.of(punishedUser.getUserIdSnowflake());

        return client.getGuildById(Snowflake.of(server.getServerSnowflake()))
                .flatMap(guild -> {
                    if (guild == null) {
                        // Ensure bot is still in guild - if not, nothing more is required.
                        logger.info("null guild");
                        try (final var usage = DatabaseLoader.use()) {
                            punishment.setEnded(true);
                            punishment.setAutomaticEnd(true);
                            punishment.setEndReason("Bot not in guild, punishment expired anyways.");
                            punishment.save();
                            punishment.refresh();
                        }
                        return Mono.empty();
                    } else {
                        return guild.getMemberById(userId).flatMap(member -> {
                            logger.info("Guild not null, auto-unpunishing");
                            switch (punishmentType) {
                                case "ban" -> {
                                    return autoUnbanUser(guild, punishment, userId);
                                }
                                case "mute" -> {
                                    return autoUnmuteUser(server, punishment, member, guild);
                                }
                                default -> {
                                    try (final var usage = DatabaseLoader.use()) {
                                        punishment.setEnded(true);
                                        punishment.setEndReason("Punishment ended but IDK what is");
                                        punishment.save();
                                    }
                                    return Mono.empty();
                                }
                            }
                        });
                    }
                })
                .then();

    }

    private Mono<Void> autoUnbanUser(Guild guild, Punishment punishment, Snowflake userId) {
        return guild.getSelfMember()
                .flatMap(selfMember -> {
                    DiscordUser selfDiscordUser = DatabaseLoader.use(() -> DiscordUser.findFirst("user_id_snowflake = ?", selfMember.getId().asLong()));

                    try (final var usage = DatabaseLoader.use()) {
                        punishment.setEnded(true);
                        punishment.setAutomaticEnd(true);
                        punishment.setPunishmentEnder(selfDiscordUser.getUserId());
                        punishment.setPunishmentEnderName(selfMember.getUsername());
                        punishment.setPunishmentEnderDiscrim(selfMember.getDiscriminator());
                        punishment.setEndReason("Automatically unbanned on timer.");
                        punishment.save();
                        punishment.refresh();
                        return guild.unban(userId).then(loggingListener.onUnban(guild, "Automatically unbanned on timer.", punishment));
                    }
                });
    }

    private Mono<Void> autoUnmuteUser(DiscordServer server, Punishment punishment, Member member, Guild guild) {

        return guild.getSelfMember()
                .flatMap(self -> {
                    DiscordServerProperties serverProperties = DatabaseLoader.use(() -> DiscordServerProperties.findFirst("server_id = ?", server.getServerId()));
                    Long mutedRoleSnowflake = serverProperties.getMutedRoleSnowflake();

                    logger.info("Auto Unmuting user: " + punishment.getPunishedUserName());

                    try (final var usage = DatabaseLoader.use()) {
                        DiscordUser selfDiscordUser = DiscordUser.findFirst("user_id_snowflake = ?", self.getId().asLong());
                        punishment.setEnded(true);
                        punishment.setAutomaticEnd(true);
                        punishment.setPunishmentEnder(selfDiscordUser.getUserId());
                        punishment.setPunishmentEnderName(self.getUsername());
                        punishment.setPunishmentEnderDiscrim(self.getDiscriminator());
                        punishment.setEndReason("Automatically unmuted on timer.");
                        punishment.save();
                        punishment.refresh();
                    }
                        if (member != null && mutedRoleSnowflake != null) {
                            return member.removeRole(Snowflake.of(mutedRoleSnowflake))
                                    .then(loggingListener.onUnmute(guild, "Automatically unmuted on timer.", punishment));
                        } else {
                            return loggingListener.onUnmute(guild, "Automatically unmuted on timer.", punishment);
                        }


                });

    }

}
