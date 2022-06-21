package dev.laarryy.atropos.managers;

import dev.laarryy.atropos.Atropos;
import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.ConfigManager;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.ErrorHandler;
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

    public Mono<Void> registerCommands(GatewayDiscordClient client) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // Register slash commands with Discord
        Reflections reflections = new Reflections("dev.laarryy.atropos.commands", new SubTypesScanner());
        Flux<Class<? extends Command>> commandsToRegister = Flux.fromIterable(reflections.getSubTypesOf(Command.class));

        return client.getRestClient().getApplicationId().flatMap(applicationId -> {
            ApplicationService applicationService = client.getRestClient().getApplicationService();

            Flux<Map<String, ApplicationCommandData>> discordCommands = applicationService
                    .getGlobalApplicationCommands(applicationId)
                    .collectMap(ApplicationCommandData::name)
                    .flux();

            Flux<Void> registerCommands = Flux.from(commandsToRegister)
                    .mapNotNull(aClass -> {
                        Command command;
                        try {
                            command = aClass.getDeclaredConstructor().newInstance();
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                 NoSuchMethodException e) {
                            e.printStackTrace();
                            command = null;
                        }
                        COMMANDS.add(command);
                        return command;
                    })
                    .onErrorContinue((throwable, o) -> {
                        logger.error(throwable);
                    })
                    .filterWhen(command -> discordCommands.any(cMap -> !cMap.containsKey(command.getRequest().name())))
                    .flatMap(command -> registerCommand(client, command, applicationId, ConfigManager.getControlGuildId()));


            // Uncomment this to force re-send all commands: will force update their options in case you add any
            /*List<ApplicationCommandRequest> applicationCommandRequestList = new ArrayList<>();

            for (Command command : COMMANDS) {
                if (!command.getRequest().name().equals("presence")) {
                    applicationCommandRequestList.add(command.getRequest());
                }
            }

            Mono<Void> overwriteCommands = applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, applicationCommandRequestList).then();*/

            // Listen for command event and execute from map

            Mono<Void> commandInteraction = client.getEventDispatcher().on(ChatInputInteractionEvent.class)
                    .filterWhen(permissionChecker::checkBotPermission) // make sure bot has perms
                    .flatMap(event -> Mono.just(event.getInteraction().getData().data().get().name().get())
                            .flatMap(content -> Flux.fromIterable(COMMANDS)
                                    .filter(entry -> event.getInteraction().getData().data().get().name().get().equals(entry.getRequest().name()))
                                    .flatMap(entry -> {
                                                logger.info("Command Received");
                                                return event.deferReply().withEphemeral(true).flatMap(unused -> {
                                                                    DatabaseLoader.openConnectionIfClosed();
                                                                    logger.info("-- Executing Command --");
                                                                    return entry.execute(event);
                                                                })
                                                        .doFirst(DatabaseLoader::openConnectionIfClosed)
                                                        .doFinally(s -> logger.info("Command Done"));
                                            }
                                    )
                                    .onErrorResume(e -> ErrorHandler.handleError(e, event))
                                    .next()))
                    .then();

            logger.info("Registered Slash Commands!");

            return Mono.when(
                    registerCommands,
                    commandInteraction
            );
        });


    }

    private Mono<Void> registerCommand(GatewayDiscordClient client, Command command, long applicationId, String controlGuildId) {
        return Mono.just(command)
                .flatMap(command1 -> {
                    if (command.getRequest().name().equals("presence")) {
                        return client.getRestClient().getApplicationService()
                                .createGuildApplicationCommand(applicationId, Long.parseLong(controlGuildId), command.getRequest());
                    } else {
                        return client.getRestClient().getApplicationService()
                                .createGlobalApplicationCommand(applicationId, command.getRequest());
                    }
                })
                .then();
    }
}
