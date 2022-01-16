package dev.laarryy.atropos.commands.settings;

import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AddServerToDB;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class ModMailSettings {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final AddServerToDB addServerToDB = new AddServerToDB();

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        Guild guild = event.getInteraction().getGuild().block();
        TextChannel messageChannel = event.getInteraction().getChannel().ofType(TextChannel.class).block();

        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        if (event.getOption("modmail").get().getOption("set").isPresent()) {
            discordServerProperties.setModMailChannelSnowflake(messageChannel.getId().asLong());
            discordServerProperties.save();
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Success")
                    .color(Color.SEA_GREEN)
                    .description("Set ModMail destination channel!")
                    .timestamp(Instant.now())
                    .build();
            event.reply().withEmbeds(embed).subscribe();
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("modmail").get().getOption("unset").isPresent()) {
            discordServerProperties.setModMailChannelSnowflake(null);
            discordServerProperties.save();
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Success")
                    .color(Color.SEA_GREEN)
                    .description("Unset ModMail destination channel!")
                    .timestamp(Instant.now())
                    .build();
            event.reply().withEmbeds(embed).subscribe();
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        Notifier.notifyCommandUserOfError(event, "malformedInput");
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
