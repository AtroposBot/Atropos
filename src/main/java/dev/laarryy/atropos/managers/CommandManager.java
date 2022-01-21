package dev.laarryy.atropos.managers;

import dev.laarryy.atropos.Atropos;
import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.ConfigManager;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandManager {
    private final List<Command> COMMANDS = new ArrayList<>();
    private final Logger logger = LogManager.getLogger(Atropos.class);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    public void registerCommands(GatewayDiscordClient client) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // Register slash commands with Discord
        Reflections reflections = new Reflections("dev.laarryy.atropos.commands", new SubTypesScanner());
        Set<Class<? extends Command>> commandsToRegister = reflections.getSubTypesOf(Command.class);

        long applicationId = client.getRestClient().getApplicationId().block();
        ApplicationService applicationService = client.getRestClient().getApplicationService();
        Map<String, ApplicationCommandData> discordCommands = applicationService
                .getGlobalApplicationCommands(applicationId)
                .collectMap(ApplicationCommandData::name)
                .block();

        for (Class<? extends Command> registerableCommand : commandsToRegister) {
            final Command command = registerableCommand.getDeclaredConstructor().newInstance();

            // Add to command list
            COMMANDS.add(command);

            // Register the command with discord
            if (!discordCommands.containsKey(command.getRequest().name())) {
                logger.info("Beginning command registration with discord: " + command.getRequest().name());
                if (registerableCommand.getName().equals("PresenceCommand")) {
                    client.getRestClient().getApplicationService()
                            .createGuildApplicationCommand(applicationId, Long.parseLong(ConfigManager.getControlGuildId()), command.getRequest())
                            .subscribe();
                } else {
                    client.getRestClient().getApplicationService()
                            .createGlobalApplicationCommand(applicationId, command.getRequest())
                            .subscribe();
                    logger.info("Command registration with discord sent.");
                }
            } else {
                logger.info("Command already registered");
            }
        }

        // Uncomment this to force re-send all commands: will force update their options in case you add any
        List<ApplicationCommandRequest> applicationCommandRequestList = new ArrayList<>();

        for (Command command : COMMANDS) {
            if (!command.getRequest().name().equals("presence")) {
                applicationCommandRequestList.add(command.getRequest());
            }
        }

        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, applicationCommandRequestList);

        // Listen for command event and execute from map

        client.getEventDispatcher().on(ChatInputInteractionEvent.class)
                .subscribeOn(Schedulers.boundedElastic())
                .filter(permissionChecker::checkBotPermission) // make sure bot has perms
                .flatMap(event -> Mono.just(event.getInteraction().getData().data().get().name().get())
                        .flatMap(content -> Flux.fromIterable(COMMANDS)
                                .filter(entry -> event.getInteraction().getData().data().get().name().get().equals(entry.getRequest().name()))
                                .flatMap(entry -> {
                                    logger.info("Command Received");
                                    return entry.execute(event)
                                            .onErrorResume(e -> {
                                                logger.error(e.getMessage());
                                                logger.error("Error in Command: ", e);
                                                return Mono.empty();
                                            })
                                            .log()
                                            .doFinally(signalType -> logger.info("Command Done"));
                                        }
                                ).onErrorResume(e -> {
                                     logger.error(e.getMessage());
                                     logger.error("Error in Command: ", e);
                                     return Mono.empty();
                                 })
                                .next()))
                .onErrorResume(e -> {
                    logger.error(e.getMessage());
                    logger.error("Error in Command: ", e);
                    return Mono.empty();
                })
                .subscribe(logger::error);

        logger.info("Registered Slash Commands!");
    }
}
