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

        DatabaseLoader.openConnectionIfClosed();

        Mono<Void> startInterval1 = Flux.interval(Duration.ofMinutes(1))
                .doOnNext(this::checkPunishmentEnding)
                .then();

        Mono<Void> startInterval2 = Flux.interval(Duration.ofDays(1))
                .doOnNext(this::wipeOldData)
                .then();

        return Mono.when(
                startInterval1,
                startInterval2);
    }

    private Mono<Void> wipeOldData(Long l) {
        DatabaseLoader.openConnectionIfClosed();
        Instant thirtyDaysAgoInstant = Instant.now().minus(Duration.ofDays(30));
        Long thirtyDaysAgo = thirtyDaysAgoInstant.toEpochMilli();
        LazyList<ServerMessage> messageLazyList = ServerMessage.find("date < ?", thirtyDaysAgo);

        return Flux.fromIterable(messageLazyList)
                .doFinally(msg -> logger.info("+++++++++++ Old Messages Wiped"))
                .doOnNext(serverMessage -> serverMessage.delete(true)).then();
    }

    private Mono<Void> checkPunishmentEnding(Long l) {

        DatabaseLoader.openConnectionIfClosed();
        LazyList<Punishment> punishmentsLazyList = Punishment.find("end_date_passed = ? and permanent = ?", false, false);
        DatabaseLoader.closeConnectionIfOpen();

        return Flux.fromIterable(punishmentsLazyList)
                .filter(this::checkIfOverDue)
                .doOnNext(pun -> logger.info("handling overdue punishment"))
                .doOnNext(this::endPunishment).then();
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

    private Mono<Void> endPunishment(Punishment punishment) {

        DatabaseLoader.openConnectionIfClosed();

        String punishmentType = punishment.getPunishmentType();
        DiscordUser punishedUser = DiscordUser.findById(punishment.getPunishedUserId());
        DiscordServer server = DiscordServer.findById(punishment.getServerId());
        Snowflake userId = Snowflake.of(punishedUser.getUserIdSnowflake());

        return client.getGuildById(Snowflake.of(server.getServerSnowflake()))
                .onErrorReturn(Exception.class, null)
                .doFirst(DatabaseLoader::openConnectionIfClosed)
                .flatMap(guild -> {
                    if (guild == null) {
                        // Ensure bot is still in guild - if not, nothing more is required.
                        punishment.setEnded(true);
                        punishment.setAutomaticEnd(true);
                        punishment.save();
                        punishment.refresh();
                        DatabaseLoader.closeConnectionIfOpen();
                        return Mono.empty();
                    }
                    return guild.getMemberById(userId).flatMap(member -> {
                        switch (punishmentType) {
                            case "ban" -> {
                                return autoUnbanUser(guild, punishment, userId);
                            }
                            case "mute" -> {
                                return autoUnmuteUser(server, punishment, member, guild);
                            }
                            default -> {
                                punishment.setEnded(true);
                                return Mono.empty();
                            }
                        }
                    });
                })
                .doFinally(s -> DatabaseLoader.closeConnectionIfOpen())
                .then();

    }

    private Mono<Void> autoUnbanUser(Guild guild, Punishment punishment, Snowflake userId) {
        return Mono.from(guild.getSelfMember())
                .doFirst(DatabaseLoader::openConnectionIfClosed)
                .flatMap(selfMember -> {
                    DiscordUser selfDiscordUser = DiscordUser.findFirst("user_id_snowflake = ?", selfMember.getId().asLong());
                    punishment.setEnded(true);
                    punishment.setAutomaticEnd(true);
                    punishment.setPunishmentEnder(selfDiscordUser.getUserId());
                    punishment.setPunishmentEnderName(selfMember.getUsername());
                    punishment.setPunishmentEnderDiscrim(selfMember.getDiscriminator());
                    punishment.setEndReason("Automatically unbanned on timer.");
                    punishment.save();
                    punishment.refresh();
                    loggingListener.onUnban(guild, "Automatically unbanned on timer.", punishment);
                    return guild.unban(userId);
                })
                .doFinally(s -> DatabaseLoader.closeConnectionIfOpen());
    }

    private Mono<Void> autoUnmuteUser(DiscordServer server, Punishment punishment, Member member, Guild guild) {

        return Mono.from(guild.getSelfMember())
                .doFirst(DatabaseLoader::openConnectionIfClosed)
                .flatMap(self -> {
                    DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id = ?", server.getServerId());
                    Long mutedRoleSnowflake = serverProperties.getMutedRoleSnowflake();

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
                    if (member != null) {
                        return Mono.just(member)
                                .flatMap(member1 -> member1.removeRole(Snowflake.of(mutedRoleSnowflake)));
                    } else {
                        return Mono.empty();
                    }

                })
                .doFinally(s -> DatabaseLoader.closeConnectionIfOpen());


    }

}
