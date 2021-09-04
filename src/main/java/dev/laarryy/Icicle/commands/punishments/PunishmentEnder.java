package dev.laarryy.Icicle.commands.punishments;

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

public class PunishmentEnder {

    private final Logger logger = LogManager.getLogger(this);
    GatewayDiscordClient client;

    public PunishmentEnder(GatewayDiscordClient client) {
        if (client != null) {
            this.client = client;
        } else {
            logger.error("Client is null.");
        }

        DatabaseLoader.openConnectionIfClosed();
        Flux.interval(Duration.ofMinutes(1))
                .doOnNext(this::checkPunishmentEnding)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        logger.info("Constructor called, Flux should be started.");
    }

    private void checkPunishmentEnding(Long l) {
        logger.info("Checking punishment ending - minute " + l.toString());

        DatabaseLoader.openConnectionIfClosed();
        LazyList<Punishment> punishmentsLazyList = Punishment.find("end_date_passed = ?", false);

        Flux.fromIterable(punishmentsLazyList)
                .filter(this::checkIfOverDue)
                .subscribeOn(Schedulers.boundedElastic())
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

    public void endPunishment(Punishment punishment) {

        logger.info("Ending punishment of type: " + punishment.getPunishmentType());

        String punishmentType = punishment.getPunishmentType();
        DiscordUser punishedUser = DiscordUser.findById(punishment.getPunishedUserId());
        DiscordServer server = DiscordServer.findById(punishment.getServerId());
        Snowflake userId = Snowflake.of(punishedUser.getUserIdSnowflake());

        // Ensure bot is still in guild - if not, nothing more is required.
        Guild guild;
        if (client.getGuildById(Snowflake.of(server.getServerSnowflake())).block() != null) {
            guild = client.getGuildById(Snowflake.of(server.getServerSnowflake())).block();
        } else {
            guild = null;
        }
        if (guild == null) return;

        Member member = guild.getMemberById(userId).block();

        switch (punishmentType) {
            case "ban" -> discordUnbanUser(guild, punishment, userId);
            case "mute" -> discordUnmuteUser(server, punishment, member);
            default -> punishment.setEnded(true);
        }
    }

    private void discordUnbanUser(Guild guild, Punishment punishment, Snowflake userId) {
        DatabaseLoader.openConnectionIfClosed();
        guild.unban(userId).subscribe();
        punishment.setEnded(true);
        punishment.save();
    }

    private void discordUnmuteUser(DiscordServer server, Punishment punishment, Member member) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id = ?", server.getServerId());
        Long mutedRoleSnowflake = serverProperties.getMutedRoleSnowflake();
        if (member != null) {
            member.removeRole(Snowflake.of(mutedRoleSnowflake)).subscribe();
        }
        punishment.setEnded(true);
        punishment.save();
    }

}
