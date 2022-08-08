package dev.laarryy.atropos.commands.punishments;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.utils.CommandChecks;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class UnmuteCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final ManualPunishmentEnder manualPunishmentEnder = new ManualPunishmentEnder();
    private final PermissionChecker permissionChecker = new PermissionChecker();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("unmute")
            .description("Unmute a user.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("User to unmute.")
                    .type(ApplicationCommandOption.Type.USER.getValue())
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("reason")
                    .description("Why?")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        return CommandChecks.commandChecks(event, request.name()).then(manualPunishmentEnder.endPunishment(event));
    }
}
