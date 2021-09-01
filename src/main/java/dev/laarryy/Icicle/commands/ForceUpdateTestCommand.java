package dev.laarryy.Icicle.commands;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Role;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class ForceUpdateTestCommand implements Command {

    private final Logger logger = LogManager.getLogger(this);

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("forceupdate")
            .description("Mimics GuildJoinEvent")
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        logger.info("Joining Guild: " + event.getInteraction().getGuild().block().getName());

        Long guildId = event.getInteraction().getGuild().block().getId().asLong();

        DatabaseLoader.openConnectionIfClosed();
        DiscordServer server = DiscordServer.findOrCreateIt("server_id", guildId);

        server.setDateEntry(Instant.now().toEpochMilli());

        int serverId = server.getServerId();

        DiscordServerProperties properties = DiscordServerProperties.findOrCreateIt("server_id", serverId, "server_id_snowflake", event.getInteraction().getGuild().block().getId().asLong());

        if (properties.getLong("icicle_join_server_date") == 0) {
            properties.setServerJoinDate(Instant.now().toEpochMilli());
        }

        properties.setServerName(event.getInteraction().getGuild().block().getName());
        properties.setServerIdSnowflake(event.getInteraction().getGuild().block().getId().asLong());
        properties.saveIt();

        event.reply("Done. Database thinks server is named " + properties.getServerName()).withEphemeral(true).subscribe();
        return Mono.empty();
    }
}
