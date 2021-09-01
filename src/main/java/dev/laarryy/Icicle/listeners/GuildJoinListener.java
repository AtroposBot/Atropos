package dev.laarryy.Icicle.listeners;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.guilds.DiscordServerRoles;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.entity.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class GuildJoinListener {
    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(GuildCreateEvent event) {

        logger.info("Joining Guild: " + event.getGuild().getName());

        Long guildId = event.getGuild().getId().asLong();

        DatabaseLoader.openConnectionIfClosed();
        DiscordServer server = DiscordServer.findOrCreateIt("server_id", guildId);

        server.setDateEntry(Instant.now().toEpochMilli());

        int serverId = server.getServerId();

        DiscordServerProperties properties = DiscordServerProperties.findOrCreateIt("server_id", serverId, "server_id_snowflake", event.getGuild().getId().asLong());

        if (properties.getLong("icicle_join_server_date") == 0) {
            properties.setServerJoinDate(Instant.now().toEpochMilli());
        }

        properties.setServerName(event.getGuild().getName());
        properties.setServerIdSnowflake(event.getGuild().getId().asLong());
        properties.saveIt();

        return Mono.empty();
    }
}
