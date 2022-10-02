package dev.laarryy.atropos.commands.search;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NoResultsException;
import dev.laarryy.atropos.exceptions.NoUserException;
import dev.laarryy.atropos.exceptions.NotFoundException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.models.guilds.CommandUse;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.CommandChecks;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import dev.laarryy.atropos.utils.TimestampMaker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class AuditCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("audit")
            .description("View commands used in this guild")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("Search a user's commands.")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("snowflake")
                            .description("User ID to view commands of")
                            .type(ApplicationCommandOption.Type.STRING.getValue())
                            .required(false)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("mention")
                            .description("User mention to view commands of")
                            .type(ApplicationCommandOption.Type.USER.getValue())
                            .required(false)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("recent")
                    .description("View recent commands")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("id")
                    .description("View details of an audit by its ID")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("number")
                            .description("The number of the audit you'd like to view")
                            .type(ApplicationCommandOption.Type.INTEGER.getValue())
                            .required(true)
                            .build())
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {
        return CommandChecks.commandChecks(event, request.name()).then(Mono.defer(() -> {
            if (event.getOption("user").isPresent()) {
                return searchAuditByUser(event);
            }

            if (event.getOption("recent").isPresent()) {
                return recentAudits(event);
            }

            if (event.getOption("id").isPresent()) {
                return searchAuditById(event);
            }

            return Mono.error(new NotFoundException("404 Not Found"));
        }));
    }

    private Mono<Void> searchAuditById(ChatInputInteractionEvent event) {
        return Mono.defer(() -> {
            try (final var usage = DatabaseLoader.use()) {
                if (event.getOption("id").get().getOption("number").isEmpty() || event.getOption("id").get().getOption("number").get().getValue().isEmpty()) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new MalformedInputException("Malformed Input")));
                }

                DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());
                if (discordServer == null) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NullServerException("Null Server")));
                }

                int auditInt = (int) event.getOption("id").get().getOption("number").get().getValue().get().asLong();
                int serverId = discordServer.getServerId();
                CommandUse commandUse = CommandUse.findFirst("id = ? and server_id = ?", auditInt, serverId);
                if (commandUse == null) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NotFoundException("404 Not Found")));
                }

                DiscordUser discordUser = DiscordUser.findFirst("id = ?", commandUse.getUserId());
                if (discordUser == null) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NoUserException("No User")));
                }

                String succeeded;
                if (commandUse.getSucceeded()) {
                    succeeded = "Command succeeded.";
                } else {
                    succeeded = "Command failed.";
                }

                Long userSnowflake = discordUser.getUserIdSnowflake();
                String date = TimestampMaker.getTimestampFromEpochSecond(
                        Instant.ofEpochMilli(commandUse.getDate()).getEpochSecond(),
                        TimestampMaker.TimestampType.LONG_DATETIME);

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .color(Color.ENDEAVOUR)
                        .title("Audit: " + auditInt)
                        .description("Command contents: `" + commandUse.getCommandContents() + "`")
                        .addField("User", "`%d`:<@%d>".formatted(userSnowflake, userSnowflake), false)
                        .addField("Date", date, false)
                        .addField("Result", succeeded, false)
                        .build();

                return Notifier.sendResultsEmbed(event, embed).then(AuditLogger.addCommandToDB(event, true));
            }
        });
    }

    private Mono<Void> recentAudits(ChatInputInteractionEvent event) {
        return event.getInteraction().getGuild().flatMap(guild -> {
            LazyList<CommandUse> commandUseLazyList = DatabaseLoader.use(() -> {
                DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());
                Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
                long tenDaysAgoStamp = tenDaysAgo.toEpochMilli();

                return CommandUse.where("server_id = ? and date > ?", discordServer.getServerId(), tenDaysAgoStamp).limit(25).orderBy("id desc");
            });

            logger.info("Got the list");

            if (commandUseLazyList.isEmpty()) {
                logger.info("List is empty");
                EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                        .color(Color.ENDEAVOUR)
                        .title("Recent Commands")
                        .description("Nothing found in the past 10 days.")
                        .footer("Run some commands and try again!", "")
                        .timestamp(Instant.now())
                        .build();

                logger.info("Sending results");
                return Notifier.sendResultsEmbed(event, resultEmbed).then(AuditLogger.addCommandToDB(event, true));
            }

            logger.info("List not empty");

            return createFormattedAuditTable(commandUseLazyList, guild).flatMap(results -> {
                logger.info("List is populated");
                EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                        .color(Color.ENDEAVOUR)
                        .title("Recent Commands")
                        .description(results)
                        .footer("For detailed information, run /audit id <id>", "")
                        .timestamp(Instant.now())
                        .build();

                logger.info("Sending results");
                return Notifier.sendResultsEmbed(event, resultEmbed).then(AuditLogger.addCommandToDB(event, true));
            });
        });
    }

    private Mono<Void> searchAuditByUser(ChatInputInteractionEvent event) {
        return Mono.defer(() -> {
            long userIdSnowflake;
            if (event.getOption("user").get().getOption("snowflake").isPresent()) {
                String snowflakeString = event.getOption("user").get().getOption("snowflake").get().getValue().get().asString();
                Pattern snowflakePattern = Pattern.compile("\\d{10,20}");

                if (!snowflakePattern.matcher(snowflakeString).matches()) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new MalformedInputException("Malformed Input")));
                }

                userIdSnowflake = Long.parseLong(snowflakeString);
            } else if (event.getOption("user").get().getOption("mention").isPresent()) {
                return event.getOption("user").get().getOption("mention").get().getValue().get().asUser().flatMap(user ->
                        handleUserSearch(event, user.getId().asLong()));
            } else {
                userIdSnowflake = 0L;
            }

            return handleUserSearch(event, userIdSnowflake);
        });
    }

    private Mono<Void> handleUserSearch(ChatInputInteractionEvent event, Long userIdSnowflake) {
        return event.getInteraction().getGuild().flatMap(guild -> {
            LazyList<CommandUse> commandUsesLazyList;
            try (final var usage = DatabaseLoader.use()) {
                long guildId = event.getInteraction().getGuildId().get().asLong();
                DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);
                DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildId);

                if (discordUser == null) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NoUserException("No User")));
                }

                if (discordServer == null) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NullServerException("Null Server")));
                }

                int userId = discordUser.getUserId();
                int serverId = discordServer.getServerId();

                Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
                long tenDaysAgoStamp = tenDaysAgo.toEpochMilli();

                commandUsesLazyList = CommandUse.find("command_user_id = ? and server_id = ? and date > ?", userId, serverId, tenDaysAgoStamp).limit(30).orderBy("id desc");
                if (commandUsesLazyList.isEmpty()) {
                    return AuditLogger.addCommandToDB(event, true).then(Mono.error(new NoResultsException("No Results")));
                }
            }

            return createFormattedAuditTable(commandUsesLazyList, guild).flatMap(results -> {
                EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                        .color(Color.ENDEAVOUR)
                        .title("Results")
                        .description(results)
                        .footer("For detailed information, run /audit id <id>", "")
                        .timestamp(Instant.now())
                        .build();

                return Notifier.sendResultsEmbed(event, resultEmbed).then(AuditLogger.addCommandToDB(event, true));
            });
        });
    }

    private Mono<String> createFormattedAuditTable(LazyList<CommandUse> commandUseLazyList, Guild guild) {
        return Flux.just(
                        "```",
                        String.format("| %-6s | %-12s | %-15s | %-11s |", "ID", "Date", "User", "Preview"),
                        "---------------------------------------------------------"
                )
                .concatWith(
                        Flux.fromIterable(commandUseLazyList)
                                .filter(Objects::nonNull)
                                .flatMap(c -> {
                                    DiscordUser discordUser = DatabaseLoader.use(() -> DiscordUser.findFirst("id = ?", c.getUserId()));
                                    String userId = discordUser.getUserIdSnowflake().toString();
                                    return guild.getClient().getUserById(Snowflake.of(userId)).map(member -> {
                                        String username = member.getTag();
                                        if (username.length() > 15) {
                                            username = username.substring(0, 12) + "...";
                                        }
                                        String auditId = c.getInteger("id").toString();
                                        Instant date = Instant.ofEpochMilli(c.getDate());
                                        String dateString = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                                                .withLocale(Locale.CANADA)
                                                .withZone(ZoneId.systemDefault())
                                                .format(date);
                                        String preview;
                                        if (c.getCommandContents().length() > 7) {
                                            preview = c.getCommandContents().substring(0, 7) + "...";
                                        } else {
                                            preview = c.getCommandContents();
                                        }

                                        return String.format("| %-6s | %-12s | %-15s | %-11s |", auditId, dateString, username, preview);
                                    });
                                })
                )
                .concatWithValues("```")
                .reduce("%s%n%s"::formatted);
    }
}
