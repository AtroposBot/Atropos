package dev.laarryy.Icicle.managers;

import dev.laarryy.Icicle.Icicle;
import dev.laarryy.Icicle.commands.Command;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommandManager {
    private final List<Command> COMMANDS = new ArrayList<>();
    private final Logger logger = LogManager.getLogger(Icicle.class);

    public void registerCommands(GatewayDiscordClient client) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // Register slash commands with Discord
        Reflections reflections = new Reflections("dev.laarryy.Icicle.commands", new SubTypesScanner());
        Set<Class<? extends Command>> commandsToRegister = reflections.getSubTypesOf(Command.class);

        for (Class<? extends Command> registerableCommand : commandsToRegister) {
            final Command command = registerableCommand.getDeclaredConstructor().newInstance();

            // Add to commands map
            COMMANDS.add(command);

            // Register the command with discord
            long applicationId = client.getRestClient().getApplicationId().block();

            logger.info("Beginning command registration with discord: " + command.getRequest().name());

            client.getRestClient().getApplicationService()
                    .createGuildApplicationCommand(applicationId, Snowflake.asLong("724025797861572639"), command.getRequest())
                    .block();

            logger.info("Command registration with discord sent.");
        }

        // Listen for command event and execute from map

        client.getEventDispatcher().on(SlashCommandEvent.class)
                .flatMap(event -> Mono.just(event.getInteraction().getData().data().get().name().get())
                        .flatMap(content -> Flux.fromIterable(COMMANDS)
                                .filter(entry -> event.getInteraction().getData().data().get().name().get().equals(entry.getRequest().name()))
                                .flatMap(entry -> entry.execute(event))
                                .next()))
                .subscribe();

        logger.info("Registered Slash Commands!");
    }
}
