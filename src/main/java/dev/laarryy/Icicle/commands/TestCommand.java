package dev.laarryy.Icicle.commands;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
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

        if (event.getOption("random").isPresent() && Boolean.parseBoolean(String.valueOf(event.getOption("random")))) {
            event.replyEphemeral("Random number request understood. Your number is " + new Random().nextInt(25))
                    .retry(3)
                    .subscribe();
            return Mono.empty();
        } else {
            event.replyEphemeral("Hi. Your test was successful! Congratulation.").subscribe();
        }
        return Mono.empty();
    }

    public Mono<Void> execute(MessageCreateEvent event) {

        logger.info("Test command (normal) executed");

        CommandInfo commandInfo = new CommandInfo(event, request);
        LinkedList<String> args = commandInfo.getArgs();

        doTestCommand(commandInfo, args);


        return Mono.empty();
    }

    private void doTestCommand(CommandInfo commandInfo, LinkedList<String> args) {

        commandInfo.getEvent().getMessage().getChannel().subscribe(messageChannel ->
                messageChannel.createMessage("Test Command Successful!")
                        .subscribe());

        if (!args.get(0).isEmpty() && Boolean.parseBoolean(args.get(0))) {

            commandInfo.getEvent().getMessage().getChannel().subscribe(messageChannel ->
                    messageChannel.createMessage("You have chosen true!")
                            .subscribe());
        }

        if (!args.get(0).isEmpty() && !Boolean.parseBoolean(args.get(0))) {

            commandInfo.getEvent().getMessage().getChannel().subscribe(messageChannel ->
                    messageChannel.createMessage("You have chosen false!")
                            .subscribe());
        }

    }

}
