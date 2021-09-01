package dev.laarryy.Icicle.commands;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class ServerInfoCommand implements Command{

    private final Logger logger = LogManager.getLogger(this);

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("serverinfo")
            .description("Provides info about this guild.")
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        if (event.getInteraction().getGuildId().isEmpty()) {
            return Mono.empty();
        }

        DatabaseLoader.openConnectionIfClosed();
        Long guildId = event.getInteraction().getGuildId().get().asLong();

        DiscordServer server = DiscordServer.findOrCreateIt("server_id", guildId);
        int serverId = server.getServerId();
        DiscordServerProperties properties = DiscordServerProperties.findOrCreateIt("server_id", serverId, "server_id_snowflake", guildId);

        EmbedCreateSpec embedCreateSpec = EmbedCreateSpec.builder()
                .title(properties.getServerName())
                .color(Color.BLUE)
                .addField("Members", getGuildMembers(event).toString(), false)
                .addField("Roles", getGuildRoleCount(event).toString(), false)
                .build();

        event.reply().withEmbeds(embedCreateSpec).withEphemeral(true).subscribe();

        return Mono.empty();
    }

    private Integer getGuildMembers(SlashCommandEvent event) {
        return event.getInteraction().getGuild().map(Guild::getMemberCount).block();
    }

    private Long getGuildRoleCount(SlashCommandEvent event) {
        return event.getInteraction().getGuild().flatMap(guild -> guild.getRoles().count()).block();
    }
}
