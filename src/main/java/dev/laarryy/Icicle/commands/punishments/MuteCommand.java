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

public class MuteCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("mute")
            .description("Mute a user.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("User to mute.")
                    .type(ApplicationCommandOptionType.USER.getValue())
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("duration")
                    .description("How long should this mute be in effect? Defaults to forever. Format: 1mo2w3d13h45m")
                    .type(ApplicationCommandOptionType.STRING.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("reason")
                    .description("Why?")
                    .type(ApplicationCommandOptionType.STRING.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("dm")
                    .description("Attempt to DM user a notification? Defaults to true.")
                    .type(ApplicationCommandOptionType.BOOLEAN.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {
        Icicle icicle = new Icicle();
        PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();
        Mono.just(event)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(event1 -> punishmentManager.doPunishment(request, event1));
        return Mono.empty();
    }
}
