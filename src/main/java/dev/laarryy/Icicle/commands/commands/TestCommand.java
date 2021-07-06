package dev.laarryy.Icicle.commands.commands;

import dev.laarryy.Icicle.commands.SlashCommand;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.Random;


public class TestCommand implements SlashCommand {
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

    @Override
    public Mono<Void> execute(SlashCommandEvent event) {
        if (event.getOption("random").isPresent()) {
            Random random = new Random();
            event.replyEphemeral("Random number request understood. Your number is " + random.nextInt(9)).subscribe();
            return Mono.empty();
        } else {
            event.replyEphemeral("Hi. Your test was successful! Congratulation.").subscribe();
        }
        return Mono.empty();
    }
}
