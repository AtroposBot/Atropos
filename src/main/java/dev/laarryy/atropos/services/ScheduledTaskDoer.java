package dev.laarryy.atropos.services;

import dev.laarryy.atropos.jooq.tables.records.PunishmentsRecord;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.rest.http.client.ClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import static dev.laarryy.atropos.jooq.Tables.PUNISHMENTS;
import static dev.laarryy.atropos.jooq.Tables.SERVERS;
import static dev.laarryy.atropos.jooq.Tables.SERVER_MESSAGES;
import static dev.laarryy.atropos.jooq.Tables.SERVER_PROPERTIES;
import static dev.laarryy.atropos.jooq.Tables.USERS;
import static dev.laarryy.atropos.storage.DatabaseLoader.sqlContext;
import static org.jooq.impl.DSL.select;

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
        Instant thirtyDaysAgo = Instant.now().minus(Duration.ofDays(30));
        return Mono.fromDirect(sqlContext.deleteFrom(SERVER_MESSAGES).where(SERVER_MESSAGES.DATE.lessThan(thirtyDaysAgo)))
                .doOnSuccess(i -> logger.info("+++++++++++ Old Messages Wiped"))
                .then();
    }

    private Mono<Void> checkPunishmentEnding(Long l) {
        return Flux.from(sqlContext.selectFrom(PUNISHMENTS).where(PUNISHMENTS.END_DATE_PASSED.eq(false)).and(PUNISHMENTS.PERMANENT.eq(false)))
                .doOnSubscribe(s -> logger.info("Checking punishment ending"))
                .filter(this::checkIfOverDue)
                .flatMap(this::endPunishment)
                .then();
    }

    private boolean checkIfOverDue(PunishmentsRecord punishment) {
        logger.info("overdue check starting");
        @Nullable Instant endDate = punishment.getPunishmentEndDate();
        if (endDate == null) return false;
        Instant now = Instant.now();

        logger.info("overDue Check: " + endDate.isBefore(now));

        return endDate.isBefore(now);
    }

    private Mono<Void> endPunishment(PunishmentsRecord punishment) {

        logger.info("Ending punishment!");


        String punishmentType = punishment.getPunishmentType();

        return Mono.fromDirect(sqlContext.select(SERVERS.SERVER_ID).from(SERVERS).where(SERVERS.ID.eq(punishment.getServerId())))
                .flatMap(result -> client.getGuildById(result.value1()))
                // if bot isn't in guild (ClientException w/ 404), nothing more is required.
                .onErrorResume(
                        ClientException.isStatusCode(404),
                        error -> Mono.fromDirect(sqlContext.update(PUNISHMENTS)
                                        .set(PUNISHMENTS.END_DATE_PASSED, true)
                                        .set(PUNISHMENTS.AUTOMATIC_END, true)
                                        .set(PUNISHMENTS.PUNISHMENT_END_REASON, "Bot not in guild, punishment expired anyways.")
                                        .where(PUNISHMENTS.ID.eq(punishment.getId())))
                                .doOnSubscribe(s -> logger.info("null guild"))
                                .then(Mono.empty())
                )
                // otherwise...
                .flatMap(guild ->
                        Mono.fromDirect(sqlContext.select(USERS.USER_ID_SNOWFLAKE).from(USERS).where(USERS.ID.eq(punishment.getUserIdPunished())))
                                .map(result -> result.value1())
                                .flatMap(userId -> {
                                    logger.info("Guild not null, auto-unpunishing");
                                    return switch (punishmentType) {
                                        case "ban" -> autoUnbanUser(guild, punishment, userId);
                                        case "mute" -> autoUnmuteUser(punishment, userId, guild);
                                        default -> Mono.fromDirect(sqlContext.update(PUNISHMENTS)
                                                        .set(PUNISHMENTS.END_DATE_PASSED, true)
                                                        .set(PUNISHMENTS.PUNISHMENT_END_REASON, "Punishment ended but IDK what is")
                                                        .where(PUNISHMENTS.ID.eq(punishment.getId())))
                                                .then();
                                    };
                                }))
                .then();

    }

    private Mono<Void> autoUnbanUser(Guild guild, PunishmentsRecord punishment, Snowflake userId) {
        return guild.getSelfMember().flatMap(selfMember ->
                Mono.fromDirect(sqlContext.update(PUNISHMENTS)
                                .set(PUNISHMENTS.END_DATE_PASSED, true)
                                .set(PUNISHMENTS.AUTOMATIC_END, true)
                                .set(PUNISHMENTS.NAME_PUNISHMENT_ENDER, selfMember.getUsername())
                                .set(PUNISHMENTS.DISCRIM_PUNISHMENT_ENDER, selfMember.getDiscriminator())
                                .set(PUNISHMENTS.PUNISHMENT_END_REASON, "Automatically unbanned on timer.")
                                .set(PUNISHMENTS.PUNISHMENT_ENDER, select(USERS.ID).from(USERS).where(USERS.USER_ID_SNOWFLAKE.eq(selfMember.getId())))
                                .where(PUNISHMENTS.ID.eq(punishment.getId()))
                                .returning())
                        .flatMap(p -> loggingListener.onUnban(guild, "Automatically unbanned on timer.", p))
                        .then(guild.unban(userId))
        );
    }

    private Mono<Void> autoUnmuteUser(PunishmentsRecord punishment, Snowflake memberId, Guild guild) {

        return guild.getSelfMember().flatMap(self ->
                Mono.fromDirect(sqlContext.update(PUNISHMENTS)
                                .set(PUNISHMENTS.END_DATE_PASSED, true)
                                .set(PUNISHMENTS.AUTOMATIC_END, true)
                                .set(PUNISHMENTS.PUNISHMENT_ENDER, select(USERS.ID).from(USERS).where(USERS.USER_ID_SNOWFLAKE.eq(self.getId())))
                                .set(PUNISHMENTS.NAME_PUNISHMENT_ENDER, self.getUsername())
                                .set(PUNISHMENTS.DISCRIM_PUNISHMENT_ENDER, self.getDiscriminator())
                                .set(PUNISHMENTS.PUNISHMENT_END_REASON, "Automatically unmuted on timer.")
                                .where(PUNISHMENTS.ID.eq(punishment.getId()))
                                .returning())
                        .doOnSubscribe(s -> logger.info("Auto Unmuting user: " + punishment.getNamePunished()))
                        .flatMap(p -> loggingListener.onUnmute(guild, "Automatically unmuted on timer.", p))
                        .then(guild.getMemberById(memberId)
                                .onErrorComplete(ClientException.isStatusCode(404)) // member not found, skip
                                .flatMap(member ->
                                        Mono.fromDirect(sqlContext.select(SERVER_PROPERTIES.MUTED_ROLE_ID_SNOWFLAKE)
                                                        .from(SERVER_PROPERTIES)
                                                        .where(SERVER_PROPERTIES.SERVER_ID.eq(punishment.getServerId())))
                                                .flatMap(result -> member.removeRole(result.value1()))
                                ))
        );

    }

}
