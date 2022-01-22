package dev.laarryy.atropos.commands.punishments;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.managers.PunishmentManagerManager;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class ForcebanCommand implements Command {

    private final Logger logger = LogManager.getLogger(this);
    private final PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("forceban")
            .description("Forceban one or more user IDs")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("id")
                    .description("ID of one user to forceban OR space-delineated list of multiple users to forceban")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("reason")
                    .description("Optional reason for this forceban")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        Mono.just(event)
                
                .subscribe(event1 -> punishmentManager.doPunishment(request, event1));

    return Mono.empty();
    }
}
