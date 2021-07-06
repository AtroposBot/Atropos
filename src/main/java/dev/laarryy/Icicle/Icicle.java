package dev.laarryy.Icicle;

import dev.laarryy.Icicle.commands.SlashCommand;
import dev.laarryy.Icicle.config.ConfigManager;
import dev.laarryy.Icicle.listeners.Logging;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.shard.ShardingStrategy;
import discord4j.discordjson.json.ActivityUpdateRequest;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


public class Icicle {

    private static final Logger logger = LogManager.getLogger(Icicle.class);
    private static final Map<String, SlashCommand> slashCommands = new HashMap<>();

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

        // Add command to map

        slashCommands.put("ping", event -> event.replyEphemeral("I'm alive!"));

        // Register command with Discord

        ApplicationCommandRequest pingCommand = ApplicationCommandRequest.builder()
                .name("ping")
                .description("Are you alive?")
                .defaultPermission(true)
                .build();

        long applicationId = client.getRestClient().getApplicationId().block();

        client.getRestClient().getApplicationService()
                .createGuildApplicationCommand(applicationId, Snowflake.asLong("724025797861572639"), pingCommand)
                .block();

        // Listen for command event and execute from map

        client.getEventDispatcher().on(SlashCommandEvent.class)
                .flatMap(event -> Mono.just(event.getInteraction().getData().data().get().name())
                    .flatMap(content -> Flux.fromIterable(slashCommands.entrySet())
                            .filter(entry -> event.getInteraction().getData().data().get().name().get().equals(entry.getKey()))
                            .flatMap(entry -> entry.getValue().execute(event))
                            .next()))
                .subscribe();

        client.onDisconnect().block();
    }

    // Register Commands & Listeners

}
