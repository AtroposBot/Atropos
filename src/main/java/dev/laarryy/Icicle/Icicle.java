package dev.laarryy.Icicle;

import dev.laarryy.Icicle.commands.SlashCommand;
import dev.laarryy.Icicle.config.ConfigManager;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.listeners.EventListener;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.shard.ShardingStrategy;
import discord4j.discordjson.json.ActivityUpdateRequest;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;


public class Icicle {

    private static final Logger logger = LogManager.getLogger(Icicle.class);
    private static final List<SlashCommand> slashCommands = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        // Print token and other args to console
        for (String arg : args) {
            logger.debug(arg);
        }

        if (args.length != 1) {
            logger.error("Invalid Arguments. Allowed Number of Arguments is 1");
        }

        logger.info("Connecting to Discord!");

        GatewayDiscordClient client = DiscordClientBuilder.create(args[0])
                .setDefaultAllowedMentions(AllowedMentions.suppressEveryone())
                .build()
                .gateway()
                .setEnabledIntents(IntentSet.all())
                .setSharding(ShardingStrategy.recommended())
                .setInitialPresence(shardInfo -> Presence.doNotDisturb(ActivityUpdateRequest
                        .builder()
                        .from(Activity.playing("Shard: " + (shardInfo.getIndex() + 1)  + " | DM for ModMail")).build()))
                .login()
                .block(Duration.ofSeconds(30));

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    final User self = event.getSelf();
                    logger.info("Logged in as: " + self.getUsername() + "#" + self.getDiscriminator());
                });

        logger.debug("Connected! Loading Config");

        // Load Config

        ConfigManager manager = new ConfigManager();
        manager.loadDatabaseConfig();

        logger.info("Loaded Config");

        // Connect to DB

        logger.debug("Connecting to Database...");

        DatabaseLoader.openConnection();

        logger.info("Connected to Database!");


        // Register slash commands with Discord
        Reflections reflections = new Reflections("dev.laarryy.Icicle.commands", new SubTypesScanner());
        Set<Class<? extends SlashCommand>> commandsToRegister = reflections.getSubTypesOf(SlashCommand.class);

        for (Class<? extends SlashCommand> registerableCommand : commandsToRegister) {
          final SlashCommand command = registerableCommand.getDeclaredConstructor().newInstance();

            // Add to commands map
            slashCommands.add(command);

            // Register the command with discord
            long applicationId = client.getRestClient().getApplicationId().block();

            client.getRestClient().getApplicationService()
                    .createGuildApplicationCommand(applicationId, Snowflake.asLong("724025797861572639"), command.getRequest())
                    .block();
        }

        // Listen for command event and execute from map

        client.getEventDispatcher().on(SlashCommandEvent.class)
                .flatMap(event -> Mono.just(event.getInteraction().getData().data().get().name().get())
                        .flatMap(content -> Flux.fromIterable(slashCommands)
                                .filter(entry ->  event.getInteraction().getData().data().get().name().get().equals(entry.getRequest().name()))
                                .flatMap(entry -> entry.execute(event))
                                .next()))
                .subscribe();

        logger.info("Registered Slash Commands!");

        // Register event listeners
        Reflections reflections2 = new Reflections("dev.laarryy.Icicle.listeners",
                new MethodAnnotationsScanner());
        Set<Method> listenersToRegister = reflections2.getMethodsAnnotatedWith(EventListener.class);

        for (Method registerableListener : listenersToRegister) {
            Object listener = registerableListener.getDeclaringClass().getDeclaredConstructor().newInstance();
            Parameter[] params = registerableListener.getParameters();

            if (params.length == 0) {
                logger.error("You have a listener with no parameters!");
                // do something?
                continue;
            }

            Class<? extends Event> type = (Class<? extends Event>) params[0].getType();

            client.getEventDispatcher().on(type)
                    .flatMap(event -> {
                        try {
                            return (Mono<Void>) registerableListener.invoke(listener, event);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        return Mono.empty();
                    })
                    .retry(3)
                    .subscribe(logger::error);
        }

        logger.info("Registered Listeners!");

        client.onDisconnect().block();
    }




}
