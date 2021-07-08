package dev.laarryy.Icicle.commands;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
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
        if (event.getOption("random").isPresent() && Boolean.parseBoolean(String.valueOf(event.getOption("random")))) {
            event.replyEphemeral("Random number request understood. Your number is " + getRandom().nextInt(25))
                    .retry(3)
                    .subscribe();
            return Mono.empty();
        } else {
            event.replyEphemeral("Hi. Your test was successful! Congratulation.").subscribe();
        }
        return Mono.empty();
    }

    public Mono<Void> execute(MessageCreateEvent event) {
        CommandData commandData = new CommandDataConverter(request).getCommandData();
        String[] args = event.getMessage().getContent().split(" ");

        logger.info(commandData.name());
        logger.info(commandData.description());

        if (args.length >= 2 && Boolean.parseBoolean(args[1]) == commandData.options().get().) {
            Mono.just(event.getMessage().getChannel()
                    .flatMap(messageChannel -> messageChannel.createMessage("Test command execution (normal) succeeded. " +
                            "Your random number is " + getRandom().nextInt(25))))
                    .retry(3)
                    .subscribe();
                return Mono.empty();
        }

        Mono.just(event.getMessage().getChannel()
                .flatMap(messageChannel -> messageChannel.createMessage("Test command execution (normal) succeeded.")))
                .retry(3)
                .subscribe();

        commandData.options().get().isEmpty();
        return Mono.empty();
    }

    private Random getRandom() {
        return new Random();
    }

}
