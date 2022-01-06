package dev.laarryy.eris.commands.raid;

import dev.laarryy.eris.commands.Command;
import dev.laarryy.eris.utils.AuditLogger;
import dev.laarryy.eris.utils.Notifier;
import dev.laarryy.eris.utils.PermissionChecker;
import dev.laarryy.eris.utils.CommandChecks;
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
        if (!CommandChecks.commandChecks(event, request.name())) {
            return Mono.empty();
        }

        if (event.getInteraction().getChannel().block() == null) {
            return Mono.empty();
        }
        Guild guild = event.getInteraction().getGuild().block();

        if (event.getOption("number").isEmpty() || event.getOption("number").get().getValue().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            return Mono.empty();
        }

        long number = event.getOption("number").get().getValue().get().asLong();

        if (number < 2 || number > 100) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
            return Mono.empty();
        }

        TextChannel channel = event.getInteraction().getChannel().ofType(TextChannel.class).block();
        channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .take(number)
                .map(Message::getId)
                .transform(channel::bulkDelete)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Success")
                .color(Color.SEA_GREEN)
                .description("Pruned `" + number + "` messages.")
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).subscribe();

        AuditLogger.addCommandToDB(event, true);

        return Mono.empty();
    }

}
