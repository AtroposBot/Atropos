package dev.laarryy.Icicle.commands.logging;

import dev.laarryy.Icicle.commands.Command;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class LogSettingsCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("logsettings")
            .description("Modify logging settings.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("setlogchannel")
                    .description("Generate a random number?")
                    .type(ApplicationCommandOptionType.BOOLEAN.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        return Mono.empty();
    }
}
