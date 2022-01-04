package dev.laarryy.eris.commands.punishments;

import dev.laarryy.eris.commands.Command;
import dev.laarryy.eris.utils.PermissionChecker;
import dev.laarryy.eris.utils.SlashCommandChecks;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class UnbanCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final ManualPunishmentEnder manualPunishmentEnder = new ManualPunishmentEnder();


    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("unban")
            .description("Unban a user.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("id")
                    .description("ID of one user to unban OR space-delineated list of multiple users to unban.")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
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
        if (!SlashCommandChecks.slashCommandChecks(event, request.name())) {
            return Mono.empty();
        }

        Mono.just(event)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(manualPunishmentEnder::endPunishment);
        return Mono.empty();
    }
}