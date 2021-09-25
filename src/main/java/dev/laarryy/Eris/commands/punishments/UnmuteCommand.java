package dev.laarryy.Eris.commands.punishments;

import dev.laarryy.Eris.commands.Command;
import dev.laarryy.Eris.utils.PermissionChecker;
import dev.laarryy.Eris.utils.SlashCommandChecks;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
                    .type(ApplicationCommandOptionType.USER.getValue())
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("reason")
                    .description("Why?")
                    .type(ApplicationCommandOptionType.STRING.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {
        if (!SlashCommandChecks.slashCommandChecks(event, request.name())) {
            return Mono.empty();
        }

        Mono.just(event)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(event1 -> manualPunishmentEnder.endPunishment(event));
        return Mono.empty();
    }
}
