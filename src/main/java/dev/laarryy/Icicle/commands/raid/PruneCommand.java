package dev.laarryy.Icicle.commands.raid;

import dev.laarryy.Icicle.commands.Command;
import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.AuditLogger;
import dev.laarryy.Icicle.utils.Notifier;
import dev.laarryy.Icicle.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
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
                    .type(ApplicationCommandOptionType.INTEGER.getValue())
                    .required(true)
                    .build())

            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    //TODO: Make this work and refactor permission checking everywhere.

    public Mono<Void> execute(SlashCommandEvent event) {
        if (event.getInteraction().getChannel().block() == null || event.getInteraction().getGuild().block() == null) {
            return Mono.empty();
        }

        DatabaseLoader.openConnectionIfClosed();
        Permission permission = Permission.findOrCreateIt("permission", request.name());
        permission.save();
        permission.refresh();
        int permissionId = permission.getInteger("id");
        Guild guild = event.getInteraction().getGuild().block();

        if (!permissionChecker.checkPermission(guild, event.getInteraction().getUser(), permissionId)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            return Mono.empty();
        }

        if (event.getOption("number").isEmpty() || event.getOption("number").get().getValue().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            return Mono.empty();
        }

        long number = event.getOption("number").get().getValue().get().asLong();
        TextChannel channel = event.getInteraction().getChannel().ofType(TextChannel.class).block();
        channel.getMessagesBefore(Snowflake.of(Instant.now()))
                .take(number)
                .map(Message::getId)
                .transform(channel::bulkDelete)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        return Mono.empty();
    }

}
