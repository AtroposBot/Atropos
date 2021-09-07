package dev.laarryy.Icicle;

import dev.laarryy.Icicle.commands.Command;
import dev.laarryy.Icicle.commands.punishments.AutoPunishmentEnder;
import dev.laarryy.Icicle.commands.punishments.PunishmentManager;
import dev.laarryy.Icicle.config.ConfigManager;
import dev.laarryy.Icicle.listeners.EventListener;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.joins.ServerUser;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.shard.ShardingStrategy;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;


public class Icicle {

    private static final Logger logger = LogManager.getLogger(Icicle.class);
    private static final List<Command> COMMANDS = new ArrayList<>();
    private static PunishmentManager punishmentManager = null;

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
                .setInitialPresence(shardInfo -> ClientPresence.of(Status.DO_NOT_DISTURB,
                        ClientActivity.playing("Shard " + (shardInfo.getIndex() + 1) + " of " + shardInfo.getCount()
                                + " | DM for ModMail | Last Activated "
                                + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(Locale.CANADA).withZone(ZoneId.systemDefault()).format(Instant.now())))
                )
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


        // Register slash and 'normal' commands with Discord
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
                    .subscribe();

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
                    .subscribe(logger::error);
        }

        logger.info("Registered Listeners!");

        // Register all guilds and users in them to database

        client.getGuilds()
                .map(Icicle::addServerToDatabase)
                .doOnError(logger::error)
                .subscribe();

        // Start regularly checking for punishments to end

        Mono.just(client)
                .map(AutoPunishmentEnder::new)
                .doOnError(logger::error)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();

        // Start up the punishment manager

        Mono.just(client)
                .map(PunishmentManager::new)
                .doOnError(logger::error)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(punishmentManager1 -> punishmentManager = punishmentManager1);

        client.onDisconnect().block();
    }

    public static boolean addServerToDatabase(Guild guild) {

        long serverIdSnowflake = guild.getId().asLong();
        DatabaseLoader.openConnectionIfClosed();
        DiscordServer server = DiscordServer.findOrCreateIt("server_id", serverIdSnowflake);
        server.save();
        server.refresh();

        if (server.getDateEntry() == 0) {
            server.setDateEntry(Instant.now().toEpochMilli());
            server.save();
        }

        int serverId = server.getServerId();

        DiscordServerProperties properties = DiscordServerProperties.findOrCreateIt("server_id", serverId, "server_id_snowflake", serverIdSnowflake);

        properties.setServerName(guild.getName());
        properties.save();
        properties.refresh();

        if (properties.getMembersOnFirstJoin() == 0) {
            properties.setMembersOnFirstJoin(guild.getMemberCount());
            properties.save();
        }

        guild.getMembers()
                .map(member -> Icicle.addUserToDatabase(member, guild))
                .doOnError(logger::error)
                .subscribe();

        return true;
    }

    public static boolean addUserToDatabase(Member member, Guild guild) {

        if (member.isBot()) {
            return false;
        }

        DatabaseLoader.openConnectionIfClosed();

        long userIdSnowflake = member.getId().asLong();
        long serverIdSnowflake = guild.getId().asLong();

        DiscordUser user = DiscordUser.findOrCreateIt("user_id_snowflake", userIdSnowflake);
        user.save();
        user.refresh();

        if (user.getDateEntry() == 0) {
            user.setDateEntry(Instant.now().toEpochMilli());
            user.save();
        }

        DiscordServer server = DiscordServer.findOrCreateIt("server_id", serverIdSnowflake);

        int serverId = server.getServerId();
        int userId = user.getUserId();

        ServerUser serverUser = ServerUser.findOrCreateIt("user_id", userId, "server_id", serverId);
        serverUser.save();
        serverUser.refresh();

        if (serverUser.getDate() == 0) {
            serverUser.setDate(Instant.now().toEpochMilli());
            serverUser.save();
        }

        return true;
    }

    public static PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

}
