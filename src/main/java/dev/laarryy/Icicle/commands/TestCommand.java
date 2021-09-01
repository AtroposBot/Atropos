package dev.laarryy.Icicle.commands;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.Random;


public class TestCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("test")
            .description("This is a test. Move along dearie.")
            .addOption(ApplicationCommandOptionData.builder()
                .name("random")
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

        logger.info("Test command (slash) executed");

        if (event.getOption("random").isPresent()
                && event.getOption("random").get().getValue().isPresent()
                && event.getOption("random").get().getValue().get().asBoolean()) {

            event.reply("Random number request understood. Your number is " + new Random().nextInt(25))
                    .retry(3)
                    .subscribe();

            return Mono.empty();
        } else {
            event.reply("Hi. Your test was successful! Congratulation.").subscribe();
        }
        return Mono.empty();
    }

}
