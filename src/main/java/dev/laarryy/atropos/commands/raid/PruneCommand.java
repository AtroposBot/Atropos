package dev.laarryy.atropos.commands.raid;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NoPermissionsException;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.CommandChecks;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

public class PruneCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("prune")
            .description("Mass-delete a specified number of messages (2-100)")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("number")
                    .description("Number of messages to prune")
                    .type(ApplicationCommandOption.Type.INTEGER.getValue())
                    .required(true)
                    .build())

            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        return Mono.from(CommandChecks.commandChecks(event, request.name()))
                .flatMap(aBoolean -> {
                    if (!aBoolean) {
                        return Mono.error(new NoPermissionsException("No Permission"));
                    }
                    return Mono.from(event.getInteraction().getChannel().ofType(TextChannel.class)).flatMap(channel ->
                            Mono.from(event.getInteraction().getGuild()).flatMap(guild -> {

                                if (event.getOption("number").isEmpty() || event.getOption("number").get().getValue().isEmpty()) {
                                    return Mono.error(new MalformedInputException("Malformed Input"));
                                }

                                long number = event.getOption("number").get().getValue().get().asLong();

                                if (number < 2 || number > 100) {
                                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new MalformedInputException("Malformed Input")));
                                }

                                Flux<Snowflake> snowflakeFlux = channel.getMessagesBefore(Snowflake.of(Instant.now()))
                                        .take(number)
                                        .map(Message::getId);

                                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                        .title("Success")
                                        .color(Color.SEA_GREEN)
                                        .description("Pruned `" + number + "` messages.")
                                        .timestamp(Instant.now())
                                        .build();

                                return Mono.from(channel.bulkDelete(snowflakeFlux))
                                        .then(Notifier.sendResultsEmbed(event, embed))
                                        .then(AuditLogger.addCommandToDB(event, true));

                            }));
                });
    }

}
