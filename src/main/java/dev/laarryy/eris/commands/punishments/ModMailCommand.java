package dev.laarryy.eris.commands.punishments;

import dev.laarryy.eris.commands.Command;
import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.AddServerToDB;
import dev.laarryy.eris.utils.Notifier;
import dev.laarryy.eris.utils.PermissionChecker;
import dev.laarryy.eris.utils.SlashCommandChecks;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class ModMailCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final AddServerToDB addServerToDB = new AddServerToDB();


    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("modmail")
            .description("If configured, sends staff a message privately.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("message")
                    .description("What to send to staff")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {
        if (!SlashCommandChecks.slashCommandChecks(event, request.name())) {
            return Mono.empty();
        }

        Guild guild = event.getInteraction().getGuild().block();
        Member member = event.getInteraction().getMember().get();

        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties properties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        if (event.getOption("message").isEmpty() || event.getOption("message").get().getValue().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            return Mono.empty();
        }

        String input = event.getOption("message").get().getValue().get().asString();

        if (properties.getModMailChannelSnowflake() != null) {
            TextChannel channel = guild.getChannelById(Snowflake.of(properties.getModMailChannelSnowflake())).ofType(TextChannel.class).block();
            String content;
            if (input.length() > 3985) {
                content = "```\n" + input.substring(0, 3975) + "\n```";
            } else {
                content = "```\n" + input + "\n```";
            }

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("ModMail from: " + member.getUsername() + "#" + member.getDiscriminator())
                    .description(content)
                    .color(Color.ENDEAVOUR)
                    .thumbnail(event.getInteraction().getMember().get().getAvatarUrl())
                    .footer("Sent on the least laden swallows available", event.getClient().getSelf().block().getAvatarUrl())
                    .timestamp(Instant.now())
                    .build();

            channel.createMessage(embed).block();

            EmbedCreateSpec embed2 = EmbedCreateSpec.builder()
                    .title("Success")
                    .description("Sent to ModMail successfully.")
                    .color(Color.SEA_GREEN)
                    .timestamp(Instant.now())
                    .build();

            event.reply().withEmbeds(embed2).withEphemeral(true).block();
        } else {
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Unable To Send ModMail")
                    .description("This guild does not yet have a ModMail channel set up - please contact its staff directly.")
                    .color(Color.JAZZBERRY_JAM)
                    .footer("And maybe mention this to em, eh?", event.getClient().getSelf().block().getAvatarUrl())
                    .build();
            event.reply().withEmbeds(embed).withEphemeral(true).block();
        }


        return Mono.empty();
    }
}
