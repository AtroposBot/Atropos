package dev.laarryy.Eris.services;

import dev.laarryy.Eris.listeners.logging.LoggingListener;
import dev.laarryy.Eris.managers.LoggingListenerManager;
import dev.laarryy.Eris.models.guilds.DiscordServer;
import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.models.users.DiscordUser;
import dev.laarryy.Eris.models.users.Punishment;
import dev.laarryy.Eris.storage.DatabaseLoader;
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
    }

    private void checkPunishmentEnding(Long l) {

        DatabaseLoader.openConnectionIfClosed();
        LazyList<Punishment> punishmentsLazyList = Punishment.find("end_date_passed = ?", false);

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
                    DatabaseLoader.openConnectionIfClosed();

                    Member member;
                    try { member = guild1.getMemberById(userId).block(); } catch (Exception exception) {
                        member = null;
                    }

                    switch (punishmentType) {
                        case "ban" -> discordUnbanUser(guild1, punishment, userId);
                        case "mute" -> discordUnmuteUser(server, punishment, member, guild1);
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
        punishment.refresh();
        loggingListener.onUnban(guild, "Automatically unbanned on timer.", punishment);
    }

    private void discordUnmuteUser(DiscordServer server, Punishment punishment, Member member, Guild guild) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id = ?", server.getServerId());
        Long mutedRoleSnowflake = serverProperties.getMutedRoleSnowflake();
        if (member != null) {
            member.removeRole(Snowflake.of(mutedRoleSnowflake)).subscribe();
        }
        punishment.setEnded(true);
        punishment.setEndReason("Automatically unmuted on timer.");
        punishment.save();
        loggingListener.onUnmute(guild, "Automatically unmuted on timer.", punishment);
    }

}
