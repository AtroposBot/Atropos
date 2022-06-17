package dev.laarryy.atropos.commands.settings;

import dev.laarryy.atropos.exceptions.MalformedInputException;
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

        return event.getInteraction().getGuild().flatMap(guild -> {
            return event.getInteraction().getChannel().ofType(TextChannel.class).flatMap(messageChannel -> {
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

                    DatabaseLoader.closeConnectionIfOpen();
                    return Notifier.sendResultsEmbed(event, embed);
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

                    DatabaseLoader.closeConnectionIfOpen();
                    return Notifier.sendResultsEmbed(event, embed);
                }

                DatabaseLoader.closeConnectionIfOpen();
                return Mono.error(new MalformedInputException("Malformed Input"));
            });
        });
    }
}
