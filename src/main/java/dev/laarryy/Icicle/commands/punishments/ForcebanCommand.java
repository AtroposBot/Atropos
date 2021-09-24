package dev.laarryy.Icicle.commands.punishments;

import dev.laarryy.Icicle.Icicle;
import dev.laarryy.Icicle.commands.Command;
import dev.laarryy.Icicle.managers.PunishmentManagerManager;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ForcebanCommand implements Command {

    private final Logger logger = LogManager.getLogger(this);
    private final PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("forceban")
            .description("Forceban one or more user IDs")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("id")
                    .description("ID of one user to forceban OR space-delineated list of multiple users to forceban.")
                    .type(ApplicationCommandOptionType.STRING.getValue())
                    .required(true)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        Mono.just(event)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(event1 -> punishmentManager.doPunishment(request, event1));

    return Mono.empty();
    }
}
