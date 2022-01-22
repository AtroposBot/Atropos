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

import java.time.Duration;
import java.time.Instant;

public class ScheduledTaskDoer {

    private final Logger logger = LogManager.getLogger(this);
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    GatewayDiscordClient client;

    public ScheduledTaskDoer(GatewayDiscordClient client) {
        if (client != null) {
            this.client = client;
        } else {
            logger.error("Client is null.");
        }

        DatabaseLoader.openConnectionIfClosed();
        Flux.interval(Duration.ofMinutes(1))
                .doOnNext(this::checkPunishmentEnding)
                .subscribe();

        Flux.interval(Duration.ofDays(1))
                .doFirst(() -> wipeOldData(0L))
                .doOnNext(this::wipeOldData)
                .subscribe();
    }

    private void wipeOldData(Long l) {
        DatabaseLoader.openConnectionIfClosed();
        Instant thirtyDaysAgoInstant = Instant.now().minus(Duration.ofDays(30));
        Long thirtyDaysAgo = thirtyDaysAgoInstant.toEpochMilli();
        LazyList<ServerMessage> messageLazyList = ServerMessage.find("date < ?", thirtyDaysAgo);

        Flux.fromIterable(messageLazyList)
                .doFinally(msg -> logger.info("+++++++++++ Old Messages Wiped"))
                .subscribe(serverMessage -> serverMessage.delete(true));
    }

    private void checkPunishmentEnding(Long l) {

        DatabaseLoader.openConnectionIfClosed();
        LazyList<Punishment> punishmentsLazyList = Punishment.find("end_date_passed = ? and permanent = ?", false, false);

        Flux.fromIterable(punishmentsLazyList)
                .filter(this::checkIfOverDue)
                .doOnNext(pun -> logger.info("handling overdue punishment"))
                .subscribe(this::endPunishment);
        DatabaseLoader.closeConnectionIfOpen();
    }

    private boolean checkIfOverDue(Punishment punishment) {
        DatabaseLoader.openConnectionIfClosed();
        Long endDate = punishment.getEndDate();
        if (endDate == null) return false;
        Instant endInstant = Instant.ofEpochMilli(endDate);
        Instant nowInstant = Instant.now();

        DatabaseLoader.closeConnectionIfOpen();
        return endInstant.isBefore(nowInstant);
    }

    private void endPunishment(Punishment punishment) {

        DatabaseLoader.openConnectionIfClosed();

        String punishmentType = punishment.getPunishmentType();
        DiscordUser punishedUser = DiscordUser.findById(punishment.getPunishedUserId());
        DiscordServer server = DiscordServer.findById(punishment.getServerId());
        Snowflake userId = Snowflake.of(punishedUser.getUserIdSnowflake());

        client.getGuildById(Snowflake.of(server.getServerSnowflake()))
                .onErrorReturn(Exception.class, null)
                
                .subscribe(guild1 -> {
                    if (guild1 == null) {
                        // Ensure bot is still in guild - if not, nothing more is required.
                        punishment.setEnded(true);
                        punishment.setAutomaticEnd(true);
                        punishment.save();
                        punishment.refresh();
                        DatabaseLoader.closeConnectionIfOpen();
                        return;
                    }

                    Member member;
                    try {
                        member = guild1.getMemberById(userId).block();
                    } catch (Exception exception) {
                        member = null;
                    }

                    switch (punishmentType) {
                        case "ban" -> autoUnbanUser(guild1, punishment, userId);
                        case "mute" -> autoUnmuteUser(server, punishment, member, guild1);
                        default -> punishment.setEnded(true);
                    }
                    punishment.save();
                });
        DatabaseLoader.closeConnectionIfOpen();
    }

    private void autoUnbanUser(Guild guild, Punishment punishment, Snowflake userId) {
        guild.unban(userId).subscribe();
        DatabaseLoader.openConnectionIfClosed();
        Member self = guild.getSelfMember().block();
        DiscordUser selfDiscordUser = DiscordUser.findFirst("user_id_snowflake = ?", self.getId().asLong());
        punishment.setEnded(true);
        punishment.setAutomaticEnd(true);
        punishment.setPunishmentEnder(selfDiscordUser.getUserId());
        punishment.setPunishmentEnderName(self.getUsername());
        punishment.setPunishmentEnderDiscrim(self.getDiscriminator());
        punishment.setEndReason("Automatically unbanned on timer.");
        punishment.save();
        punishment.refresh();
        loggingListener.onUnban(guild, "Automatically unbanned on timer.", punishment);
    }

    private void autoUnmuteUser(DiscordServer server, Punishment punishment, Member member, Guild guild) {
        DatabaseLoader.openConnectionIfClosed();
        Member self = guild.getSelfMember().block();

        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id = ?", server.getServerId());
        Long mutedRoleSnowflake = serverProperties.getMutedRoleSnowflake();

        if (member != null) {
            member.removeRole(Snowflake.of(mutedRoleSnowflake)).subscribe();
        }

        DiscordUser selfDiscordUser = DiscordUser.findFirst("user_id_snowflake = ?", self.getId().asLong());
        punishment.setEnded(true);
        punishment.setAutomaticEnd(true);
        punishment.setPunishmentEnder(selfDiscordUser.getUserId());
        punishment.setPunishmentEnderName(self.getUsername());
        punishment.setPunishmentEnderDiscrim(self.getDiscriminator());
        punishment.setEndReason("Automatically unmuted on timer.");
        punishment.save();
        punishment.refresh();
        loggingListener.onUnmute(guild, "Automatically unmuted on timer.", punishment);
    }

}
