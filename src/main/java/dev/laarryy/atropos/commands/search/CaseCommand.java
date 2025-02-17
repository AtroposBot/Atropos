package dev.laarryy.atropos.commands.search;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NoUserException;
import dev.laarryy.atropos.exceptions.NotFoundException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
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
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class CaseCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final Pattern snowflakePattern = Pattern.compile("\\d{10,20}");

    List<ApplicationCommandOptionChoiceData> optionChoiceDataList = List.of(
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Note")
                    .value("note")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Warn")
                    .value("warn")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Mute")
                    .value("mute")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Kick")
                    .value("Kick")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Ban")
                    .value("ban")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Forceban")
                    .value("forceban")
                    .build());

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("case")
            .description("Search and manage infractions.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("search")
                    .description("Search infractions")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("user")
                            .description("Search by user ID")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("snowflake")
                                    .description("User ID to search")
                                    .type(ApplicationCommandOption.Type.STRING.getValue())
                                    .required(false)
                                    .build())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("mention")
                                    .description("User mention to search")
                                    .type(ApplicationCommandOption.Type.USER.getValue())
                                    .required(false)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("id")
                            .description("Search by case ID")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("caseid")
                                    .description("Case ID to search")
                                    .type(ApplicationCommandOption.Type.INTEGER.getValue())
                                    .required(true)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("recent")
                            .description("Show recent cases")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("type")
                                    .description("Type of punishment to filter for")
                                    .type(ApplicationCommandOption.Type.STRING.getValue())
                                    .choices(optionChoiceDataList)
                                    .required(false)
                                    .build())
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("update")
                    .description("Update reason for infraction")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("id")
                            .description("ID of the case you want to modify")
                            .type(ApplicationCommandOption.Type.INTEGER.getValue())
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("reason")
                            .description("New reason for case")
                            .type(ApplicationCommandOption.Type.STRING.getValue())
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("which")
                            .description("Which reason to update - punishment or end punishment reason?")
                            .type(ApplicationCommandOption.Type.STRING.getValue())
                            .required(true)
                            .choices(List.of(
                                    ApplicationCommandOptionChoiceData.builder().name("punishment").value("punishment").build(),
                                    ApplicationCommandOptionChoiceData.builder().name("endpunishment").value("endpunishment").build()))
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("delete")
                    .description("Delete case")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("id")
                            .description("Delete case")
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
        return Mono.defer(() -> {
            // Have to do CommandChecks here because permissions are too granular - there is one for each type of case command instead of one for the entire case command

            if (event.getOption("delete").isPresent()) {
                return CommandChecks.commandChecks(event, "casedelete").then(deletePunishment(event));
            }

            if (event.getOption("update").isPresent()) {
                return CommandChecks.commandChecks(event, "caseupdate").then(updatePunishment(event));
            }
            if (event.getOption("search").isPresent()) {
                return CommandChecks.commandChecks(event, "casesearch").then(Mono.defer(() -> {
                    if (event.getOption("search").isPresent() && event.getOption("search").get().getOption("user").isPresent()) {
                        return searchPunishments(event);
                    }

                    if (event.getOption("search").isPresent() && event.getOption("search").get().getOption("id").isPresent()) {
                        return searchForCase(event);
                    }

                    if (event.getOption("search").get().getOption("recent").isPresent()) {
                        return recentCases(event);
                    }
                    return Mono.error(new MalformedInputException("Malformed Input"));
                }));
            }

            return Mono.error(new MalformedInputException("Malformed Input"));
        });
    }

    private Mono<Void> deletePunishment(ChatInputInteractionEvent event) {
        return event.getInteraction().getGuild().flatMap(guild -> {
            if (event.getOption("delete").get().getOption("id").isEmpty()) {
                return Mono.error(new MalformedInputException("Malformed Input"));
            }

            try (final var usage = DatabaseLoader.use()) {
                Long id = event.getOption("delete").get().getOption("id").get().getValue().get().asLong();
                DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
                if (discordServer == null) {
                    return Mono.error(new NullServerException("Null Server"));
                }

                Punishment punishment = Punishment.findFirst("id = ? and server_id = ?", id, discordServer.getServerId());
                if (punishment == null) {
                    return Mono.error(new NotFoundException("404 Not Found"));
                }

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .title(EmojiManager.getMessageDelete() + " Case #" + punishment.getPunishmentId() + " Deleted")
                        .timestamp(Instant.now())
                        .build();

                punishment.delete();
                return Notifier.sendResultsEmbed(event, embed);
            }
        });
    }

    private Mono<Void> recentCases(ChatInputInteractionEvent event) {
        return event.getInteraction().getGuild().flatMap(guild -> DatabaseLoader.use(() -> {
            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());
            Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
            long tenDaysAgoStamp = tenDaysAgo.toEpochMilli();

            LazyList<Punishment> punishmentLazyList;
            if (event.getOption("search").get().getOption("recent").get().getOption("type").isEmpty()) {
                punishmentLazyList = Punishment.where("server_id = ? and punishment_date > ?", discordServer.getServerId(), tenDaysAgoStamp).limit(25).orderBy("id desc");
            } else {
                String type = event.getOption("search").get().getOption("recent").get().getOption("type").get().getValue().get().asString();
                punishmentLazyList = Punishment.where("server_id = ? and punishment_type = ? and punishment_date > ?", discordServer.getServerId(), type, tenDaysAgoStamp).limit(25).orderBy("id desc");
            }

            String results;
            if (punishmentLazyList == null || punishmentLazyList.isEmpty()) {
                results = "Nothing found in the past 10 days";
            } else {
                return createFormattedRecentPunishmentsTable(punishmentLazyList, guild).flatMap(populatedResults -> {
                    EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                            .color(Color.ENDEAVOUR)
                            .title("Recent Cases")
                            .description(populatedResults)
                            .footer("For detailed information, run /case search id <id>", "")
                            .timestamp(Instant.now())
                            .build();

                    return Notifier.sendResultsEmbed(event, resultEmbed).then(AuditLogger.addCommandToDB(event, true));
                });
            }

            EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                    .color(Color.ENDEAVOUR)
                    .title("Recent Cases")
                    .description(results)
                    .footer("For detailed information, run /case search id <id>", "")
                    .timestamp(Instant.now())
                    .build();

            return Notifier.sendResultsEmbed(event, resultEmbed).then(AuditLogger.addCommandToDB(event, true));
        }));
    }

    private Mono<Void> searchForCase(ChatInputInteractionEvent event) {
        return Mono.defer(() -> DatabaseLoader.use(() -> {
            if (event.getOption("search").get().getOption("id").isEmpty() || event.getOption("search").get().getOption("id").get().getOption("caseid").isEmpty()) {
                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new MalformedInputException("Malformed Input")));
            }

            int caseInt = (int) event.getOption("search").get().getOption("id").get().getOption("caseid").get().getValue().get().asLong();
            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());

            if (discordServer == null) {
                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NullServerException("Null Server")));
            }

            int serverId = discordServer.getServerId();
            Punishment punishment = Punishment.findFirst("id = ? and server_id = ?", caseInt, serverId);

            if (punishment == null) {
                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NotFoundException("404 Not Found")));
            }

            DiscordUser discordUser = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
            DiscordUser punisher = DiscordUser.findFirst("id = ?", punishment.getPunishingUserId());

            DiscordUser punishmentEnderUser;
            if (punishment.getPunishmentEnder() != null) {
                punishmentEnderUser = DiscordUser.findFirst("id = ?", punishment.getPunishmentEnder());
            } else {
                punishmentEnderUser = null;
            }
            if (discordUser == null || punisher == null) {
                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NoUserException("No User")));
            }

            Long userSnowflake = discordUser.getUserIdSnowflake();
            Long punisherSnowflake = punisher.getUserIdSnowflake();

            String endDate;
            String endReason;
            String reason;
            String didDMMessage;
            String automatic;
            String automaticallyEnded;
            String permanent;
            String punishmentEnder;

            if (punishment.getEndDate() != null) {
                endDate = TimestampMaker.getTimestampFromEpochSecond(
                        Instant.ofEpochMilli(punishment.getEndDate()).getEpochSecond(),
                        TimestampMaker.TimestampType.RELATIVE);
            } else {
                endDate = "Not ended.";
            }

            if (punishment.getAutomatic()) {
                automatic = "Yes";
            } else {
                automatic = "No";
            }

            if (punishment.getAutomaticEnd()) {
                automaticallyEnded = "Yes";
            } else {
                automaticallyEnded = "No";
            }

            if (punishment.getPermanent()) {
                permanent = "Yes";
            } else {
                permanent = "No";
            }

            if (punishmentEnderUser == null) {
                punishmentEnder = "No punishment ender found.";
            } else {
                long enderId = punishmentEnderUser.getUserIdSnowflake();
                punishmentEnder = "`" + enderId + "`:<@" + enderId + ">:`";
                if (punishment.getPunishmentEnderName() != null && punishment.getPunishmentEnderDiscrim() != null) {
                    punishmentEnder = "`" + enderId + "`:<@" + enderId + ">:`"
                            + punishment.getPunishmentEnderName() + "#" + punishment.getPunishmentEnderDiscrim() + "`";
                }

            }

            if (punishment.getEndReason() != null) {
                endReason = punishment.getEndReason();
            } else {
                endReason = "No reason provided.";
            }

            if (punishment.getPunishmentMessage() != null) {
                reason = punishment.getPunishmentMessage();
            } else {
                reason = "No reason provided.";
            }

            if (punishment.getIfDMed()) {
                didDMMessage = "Yes";
            } else {
                didDMMessage = "No";
            }

            String batchId;

            if (punishment.getBatchId() != null) {
                batchId = punishment.getBatchId().toString();
            } else {
                batchId = null;
            }

            String date = TimestampMaker.getTimestampFromEpochSecond(
                    Instant.ofEpochMilli(punishment.getDateEntry()).getEpochSecond(),
                    TimestampMaker.TimestampType.RELATIVE);

            String user;
            String moderator;
            if (punishment.getPunishedUserName() == null || punishment.getPunishedUserDiscrim() == null) {
                user = "`" + userSnowflake + "`: " + "<@" + userSnowflake + ">";
            } else {
                user = "`" + userSnowflake + "`: " + "<@" + userSnowflake + ">:`"
                        + punishment.getPunishedUserName() + "#" + punishment.getPunishedUserDiscrim() + "`";
            }

            if (punishment.getPunishingUserName() == null || punishment.getPunishingUserDiscrim() == null) {
                moderator = "`" + punisherSnowflake + "`:" + "<@" + punisherSnowflake + ">";
            } else {
                moderator = "`" + punisherSnowflake + "`:" + "<@" + punisherSnowflake + ">:`"
                        + punishment.getPunishingUserName() + "#" + punishment.getPunishingUserDiscrim() + "`";
            }

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .color(Color.ENDEAVOUR)
                    .title("Case " + punishment.getPunishmentId())
                    .addField("User", user, false)
                    .addField("Moderator", moderator, false)
                    .addField("Moderation Action", punishment.getPunishmentType().toUpperCase(), true)
                    .addField("Date", date, true)
                    .addField("Reason", reason, false)
                    .addField("End Date", endDate, true)
                    .addField("Automatically Issued?", automatic, true)
                    .addField("Permanent When Issued?", permanent, true)
                    .addField("Attempted to DM User?", didDMMessage, true)
                    .timestamp(Instant.now())
                    .build();

            if (batchId != null) {
                embed = EmbedCreateSpec.builder().from(embed)
                        .footer("Batch ID: " + batchId, "")
                        .build();
            }

            if (!endDate.equals("Not ended.")) {
                embed = EmbedCreateSpec.builder().from(embed)
                        .addField("Ended By", punishmentEnder, false)
                        .addField("End Reason", endReason, false)
                        .addField("Automatically Ended?", automaticallyEnded, true)
                        .build();
            }

            return Notifier.sendResultsEmbed(event, embed).then(AuditLogger.addCommandToDB(event, true));
        }));
    }

    private Mono<Void> searchPunishments(ChatInputInteractionEvent event) {
        return event.getInteraction().getGuild().flatMap(guild -> {
            long userIdSnowflake;
            if (event.getOption("search").get().getOption("user").isPresent()
                    && event.getOption("search").get().getOption("user").get().getOption("snowflake").isPresent()
                    && event.getOption("search").get().getOption("user").get().getOption("snowflake").get().getValue().isPresent()) {

                String snowflakeString = event.getOption("search").get().getOption("user").get().getOption("snowflake").get().getValue().get().asString();

                if (!snowflakePattern.matcher(snowflakeString).matches()) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new MalformedInputException("Malformed Input")));
                }

                userIdSnowflake = Long.parseLong(snowflakeString);
            } else if (event.getOption("search").get().getOption("user").isPresent()
                    && event.getOption("search").get().getOption("user").get().getOption("mention").isPresent()
                    && event.getOption("search").get().getOption("user").get().getOption("mention").get().getValue().isPresent()) {

                return event.getOption("search").get().getOption("user").get().getOption("mention").get().getValue().get().asUser().flatMap(user -> {
                    Long userMentionSnowflake = user.getId().asLong();
                    return handleUserResults(userMentionSnowflake, event, guild);
                });

            } else {
                userIdSnowflake = 0L;
            }

            return handleUserResults(userIdSnowflake, event, guild);
        });
    }


    private Mono<Void> handleUserResults(Long userIdSnowflake, ChatInputInteractionEvent event, Guild guild) {
        return Mono.defer(() -> DatabaseLoader.use(() -> {
            DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);

            if (discordUser == null) {
                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NoUserException("No User")));
            }

            int userId = discordUser.getUserId();
            long guildId = event.getInteraction().getGuildId().get().asLong();
            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildId);

            if (discordServer == null) {
                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NullServerException("Null Server")));
            }

            int serverId = discordServer.getServerId();

            LazyList<Punishment> punishmentLazyList = Punishment.find("user_id_punished = ? and server_id = ?", userId, serverId).limit(70).orderBy("id desc");

            String results;
            if (punishmentLazyList == null || punishmentLazyList.isEmpty()) {
                results = "No results found for user.";
            } else {
                return createFormattedPunishmentsTable(punishmentLazyList, guild).flatMap(populatedResults -> {
                    EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                            .color(Color.ENDEAVOUR)
                            .title("Results for User: " + userIdSnowflake)
                            .description(populatedResults)
                            .footer("For detailed information, run /case search id <id>", "")
                            .timestamp(Instant.now())
                            .build();

                    return Notifier.sendResultsEmbed(event, resultEmbed).then(AuditLogger.addCommandToDB(event, true));
                });
            }

            EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                    .color(Color.ENDEAVOUR)
                    .title("Results for User: " + userIdSnowflake)
                    .description(results)
                    .footer("For detailed information, run /case search id <id>", "")
                    .timestamp(Instant.now())
                    .build();

            return Notifier.sendResultsEmbed(event, resultEmbed).then(AuditLogger.addCommandToDB(event, true));
        }));
    }

    private Mono<Void> updatePunishment(ChatInputInteractionEvent event) {
        return Mono.defer(() -> DatabaseLoader.use(() -> {
            if (event.getOption("update").get().getOption("id").isPresent()
                    && event.getOption("update").get().getOption("id").get().getValue().isPresent()) {
                long caseIdLong = event.getOption("update").get().getOption("id").get().getValue().get().asLong();
                int caseId = (int) caseIdLong;

                Punishment punishment = Punishment.findFirst("id = ?", caseId);

                String newReason;
                if (event.getOption("update").get().getOption("reason").isPresent()
                        && event.getOption("update").get().getOption("reason").get().getValue().isPresent()) {
                    newReason = event.getOption("update").get().getOption("reason").get().getValue().get().asString();
                } else {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new MalformedInputException("Malformed Input")));
                }

                if (punishment == null) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NotFoundException("404 Not Found")));
                }

                EmbedCreateSpec spec = EmbedCreateSpec.builder()
                        .title("Punishment Updated")
                        .color(Color.ENDEAVOUR)
                        .description("Punishment successfully updated with new reason: `" + newReason + "`")
                        .footer("Case ID: " + punishment.getPunishmentId(), "")
                        .timestamp(Instant.now())
                        .build();

                if (event.getOption("update").get().getOption("which").get().getValue().get().asString().equals("punishment")) {
                    punishment.setPunishmentMessage(newReason);
                }

                if (event.getOption("update").get().getOption("which").get().getValue().get().asString().equals("endpunishment")) {
                    punishment.setEndReason(newReason);
                }
                punishment.save();
                return Notifier.sendResultsEmbed(event, spec).then(AuditLogger.addCommandToDB(event, true));
            } else {
                return Mono.error(new MalformedInputException("Malformed Input"));
            }
        }));
    }

    private Mono<String> createFormattedPunishmentsTable(LazyList<Punishment> punishmentLazyList, Guild guild) {
        return Flux.just(
                        "```",
                        String.format("| %-6s | %-8s | %-30s |", "ID", "Type", "Mod"),
                        "------------------------------------------------------"
                )
                .concatWith(
                        Flux.fromIterable(punishmentLazyList)
                                .filter(Objects::nonNull)
                                .flatMap(p -> {
                                    int id;
                                    String type;
                                    Long punisherSnowflake;
                                    try (final var usage = DatabaseLoader.use()) {
                                        DiscordUser punishingUser = DiscordUser.findFirst("id = ?", p.getPunishingUserId());
                                        id = p.getPunishmentId();
                                        type = p.getPunishmentType();
                                        punisherSnowflake = punishingUser.getUserIdSnowflake();
                                    }

                                    return guild.getMemberById(Snowflake.of(punisherSnowflake)).map(member -> {
                                        String punisher = getUsernameDefaultID(punisherSnowflake, member, 30);
                                        return String.format("| %-6s | %-8s | %-30s |", id, type, punisher);
                                    });
                                })
                )
                .concatWithValues("```")
                .reduce("%s%n%s"::formatted);
    }

    private Mono<String> createFormattedRecentPunishmentsTable(LazyList<Punishment> punishmentLazyList, Guild guild) {
        return Flux.just(
                        "```",
                        String.format("| %-6s | %-8s | %-30s |", "ID", "Type", "Punished User"),
                        "------------------------------------------------------"
                )
                .concatWith(
                        Flux.fromIterable(punishmentLazyList)
                                .filter(Objects::nonNull)
                                .flatMap(p -> {
                                    int id;
                                    String type;
                                    final Long punishedUserSnowflake;
                                    try (final var usage = DatabaseLoader.use()) {
                                        DiscordUser punishedUser = DiscordUser.findFirst("id = ?", p.getPunishedUserId());
                                        id = p.getPunishmentId();
                                        type = p.getPunishmentType();
                                        punishedUserSnowflake = punishedUser.getUserIdSnowflake();
                                    }

                                    return guild.getMemberById(Snowflake.of(punishedUserSnowflake)).map(member -> {
                                        String punished = getUsernameDefaultID(punishedUserSnowflake, member, 30);
                                        return String.format("| %-6s | %-8s | %-30s |", id, type, punished);
                                    });
                                })
                )
                .concatWithValues("```")
                .reduce("%s%n%s"::formatted);
    }

    private String getStringOfLegalLength(String reason, int length) {
        String result;
        if (reason.length() > length) {
            result = reason.substring(0, length - 3) + "...";
        } else {
            result = reason;
        }
        return result;
    }

    private String getUsernameDefaultID(long snowflake, User user, int allowedLength) {
        String usernameOrId;
        try {
            usernameOrId = user.getTag();
        } catch (Exception ignored) {
            usernameOrId = String.valueOf(snowflake);
        }

        if (usernameOrId.length() > allowedLength) {
            usernameOrId = usernameOrId.substring(0, allowedLength - 3) + "...";
        }

        return usernameOrId;
    }
}
