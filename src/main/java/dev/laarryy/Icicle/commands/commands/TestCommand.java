package dev.laarryy.Icicle.commands.commands;

import dev.laarryy.Icicle.commands.SlashCommand;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;


public class TestCommand implements SlashCommand {
    private final Logger logger = LogManager.getLogger(this);

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("test")
            .description("This is a test. Move along dearie.")
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    @Override
    public Mono<Void> execute(SlashCommandEvent event) {
        event.replyEphemeral("Hi. Your test was successful! Congratulation.").subscribe();
        return Mono.empty();
    }
}
