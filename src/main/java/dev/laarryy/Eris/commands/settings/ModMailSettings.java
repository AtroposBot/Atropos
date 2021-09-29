package dev.laarryy.Eris.commands.settings;

import dev.laarryy.Eris.commands.Command;
import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.storage.DatabaseLoader;
import dev.laarryy.Eris.utils.AddServerToDB;
import dev.laarryy.Eris.utils.Notifier;
import dev.laarryy.Eris.utils.PermissionChecker;
import dev.laarryy.Eris.utils.SlashCommandChecks;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class ModMailSettings {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final AddServerToDB addServerToDB = new AddServerToDB();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("modmailsettings")
            .description("Control the ModMail destination channel")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("set")
                    .description("Sets this channel as ModMail destination channel")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("unset")
                    .description("Unsets ModMail destination channel")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

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
            return Mono.empty();
        }

        Notifier.notifyCommandUserOfError(event, "malformedInput");
        return Mono.empty();
    }
}
