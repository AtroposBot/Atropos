package dev.laarryy.atropos.commands.raid;

import dev.laarryy.atropos.commands.Command;
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
import reactor.core.publisher.Mono;

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

        event.deferReply().block();

        TextChannel channel = event.getInteraction().getChannel().ofType(TextChannel.class).block();
        channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .take(number)
                .map(Message::getId)
                .transform(channel::bulkDelete)
                
                .subscribe();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Success")
                .color(Color.SEA_GREEN)
                .description("Pruned `" + number + "` messages.")
                .timestamp(Instant.now())
                .build();

        Notifier.replyDeferredInteraction(event, embed);

        AuditLogger.addCommandToDB(event, true);

        return Mono.empty();
    }

}
