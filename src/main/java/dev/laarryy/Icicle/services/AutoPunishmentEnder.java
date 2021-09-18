package dev.laarryy.Icicle.services;

import dev.laarryy.Icicle.LoggingListenerManager;
import dev.laarryy.Icicle.listeners.logging.LoggingListener;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;

public class AutoPunishmentEnder {

    private final Logger logger = LogManager.getLogger(this);
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    GatewayDiscordClient client;

    public AutoPunishmentEnder(GatewayDiscordClient client) {
        if (client != null) {
            this.client = client;
        } else {
            logger.error("Client is null.");
        }

        DatabaseLoader.openConnectionIfClosed();
        Flux.interval(Duration.ofMinutes(1))
                .doOnNext(this::checkPunishmentEnding)
                .subscribe();

        logger.info("Constructor called, Flux should be started.");
    }

    private void checkPunishmentEnding(Long l) {
        logger.info("Checking punishment ending.");

        logger.info("Opening connection.");
        DatabaseLoader.openConnectionIfClosed();
        logger.info("Populating LazyList");
        LazyList<Punishment> punishmentsLazyList = Punishment.find("end_date_passed = ?", false);
        logger.info("LazyList Populated, doing Flux.");

        Flux.fromIterable(punishmentsLazyList)
                .filter(this::checkIfOverDue)
                .doOnNext(pun -> logger.info("handling overdue punishment"))
                .subscribe(this::endPunishment);

    }

    private boolean checkIfOverDue(Punishment punishment) {
        DatabaseLoader.openConnectionIfClosed();
        Long endDate = punishment.getEndDate();
        if (endDate == null) return false;
        Instant endInstant = Instant.ofEpochMilli(endDate);
        Instant nowInstant = Instant.now();

        return endInstant.isBefore(nowInstant);
    }

    private void endPunishment(Punishment punishment) {

        DatabaseLoader.openConnectionIfClosed();

        logger.info("Ending punishment of type: " + punishment.getPunishmentType());

        String punishmentType = punishment.getPunishmentType();
        DiscordUser punishedUser = DiscordUser.findById(punishment.getPunishedUserId());
        DiscordServer server = DiscordServer.findById(punishment.getServerId());
        Snowflake userId = Snowflake.of(punishedUser.getUserIdSnowflake());

        // Ensure bot is still in guild - if not, nothing more is required.


        client.getGuildById(Snowflake.of(server.getServerSnowflake()))
                .onErrorReturn(Exception.class, null)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(guild1 -> {
                    if (guild1 == null) return;
                    logger.info("Ending punishment - should be subbed to boundedElastic");
                    DatabaseLoader.openConnectionIfClosed();

                    Member member;
                    try { member = guild1.getMemberById(userId).block(); } catch (Exception exception) {
                        member = null;
                    }

                    switch (punishmentType) {
                        case "ban" -> discordUnbanUser(guild1, punishment, userId);
                        case "mute" -> discordUnmuteUser(server, punishment, member);
                        default -> punishment.setEnded(true);
                    }
                });
    }

    private void discordUnbanUser(Guild guild, Punishment punishment, Snowflake userId) {
        guild.unban(userId).subscribe();
        DatabaseLoader.openConnectionIfClosed();
        punishment.setEnded(true);
        punishment.setEndReason("Automatically unbanned on timer.");
        punishment.save();
        loggingListener.onUnban(guild, userId.asLong(), "Automatically unbanned on timer.");
    }

    private void discordUnmuteUser(DiscordServer server, Punishment punishment, Member member) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id = ?", server.getServerId());
        Long mutedRoleSnowflake = serverProperties.getMutedRoleSnowflake();
        if (member != null) {
            member.removeRole(Snowflake.of(mutedRoleSnowflake)).subscribe();
        }
        punishment.setEnded(true);
        punishment.setEndReason("Automatically unmuted on timer.");
        punishment.save();
        loggingListener.onUnmute(member, member.getId().asLong(), "Automatically unmuted on timer.");
    }

}
