package dev.laarryy.atropos.commands.punishments;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.exceptions.CannotSendModMailException;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AddServerToDB;
import dev.laarryy.atropos.utils.CommandChecks;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
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
        Mono<Guild> guildMono = event.getInteraction().getGuild();

        return guildMono
                .flatMap(guild -> {

                    if (guild == null) {
                        return Mono.error(new NullServerException("No Server"));
                    }

                    Member member = event.getInteraction().getMember().get();

                    DatabaseLoader.openConnectionIfClosed();
                    DiscordServerProperties properties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

                    if (event.getOption("message").isEmpty() || event.getOption("message").get().getValue().isEmpty()) {
                        return Mono.error(new MalformedInputException("Malformed Input"));
                    }


                    String input = event.getOption("message").get().getValue().get().asString();

                    if (properties.getModMailChannelSnowflake() != null) {
                        Mono<TextChannel> channelMono = guild.getChannelById(Snowflake.of(properties.getModMailChannelSnowflake())).ofType(TextChannel.class);

                        return channelMono.flatMap(channel -> {
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
                                    .footer("Sent on the least laden swallows available", "")
                                    .timestamp(Instant.now())
                                    .build();

                            return channel.createMessage(embed).flatMap(message -> {
                                EmbedCreateSpec embed2 = EmbedCreateSpec.builder()
                                        .title("Success")
                                        .description("Sent to ModMail successfully.")
                                        .color(Color.SEA_GREEN)
                                        .timestamp(Instant.now())
                                        .build();

                                return Notifier.sendResultsEmbed(event, embed2);
                            });
                        });
                    } else {
                        return Mono.error(new CannotSendModMailException("Cannot Send ModMail"));
                    }
                });
    }
}
