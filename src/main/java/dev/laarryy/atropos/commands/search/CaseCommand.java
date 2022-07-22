package dev.laarryy.atropos.commands.search;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NoPermissionsException;
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
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

        // Have to do CommandChecks here because permissions are too granular - there is one for each type of case command instead of one for the entire case command

        if (event.getOption("delete").isPresent()) {
            return CommandChecks.commandChecks(event, "casedelete").flatMap(aBoolean -> {
                if (!aBoolean) {
                    return Mono.error(new NoPermissionsException("No Permission"));
                }
                return Mono.just(event).flatMap(this::deletePunishment);
            });
        }

        if (event.getOption("update").isPresent()) {
            return CommandChecks.commandChecks(event, "caseupdate").flatMap(aBoolean -> {
                if (!aBoolean) {
                    return Mono.error(new NoPermissionsException("No Permission"));
                }
                return Mono.just(event).flatMap(this::updatePunishment);
            });
        }
        if (event.getOption("search").isPresent()) {
            return CommandChecks.commandChecks(event, "casesearch").flatMap(aBoolean -> {
                if (!aBoolean) {
                    return Mono.error(new NoPermissionsException("No Permission"));
                }
                if (event.getOption("search").isPresent() && event.getOption("search").get().getOption("user").isPresent()) {
                    return Mono.just(event).flatMap(this::searchPunishments);
                }

                if (event.getOption("search").isPresent() && event.getOption("search").get().getOption("id").isPresent()) {
                    return Mono.just(event).flatMap(this::searchForCase);
                }

                if (event.getOption("search").get().getOption("recent").isPresent()) {
                    return Mono.just(event).flatMap(this::recentCases);
                }
                return Mono.error(new MalformedInputException("Malformed Input"));
            });
        }

        return Mono.error(new MalformedInputException("Malformed Input"));
    }

    private Mono<Void> deletePunishment(ChatInputInteractionEvent event) {
        return event.getInteraction().getGuild().flatMap(guild -> {
            if (event.getOption("delete").get().getOption("id").isEmpty()) {
                return Mono.error(new MalformedInputException("Malformed Input"));
            }

            Long id = event.getOption("delete").get().getOption("id").get().getValue().get().asLong();

            DatabaseLoader.openConnectionIfClosed();

            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

            if (discordServer == null) {
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.error(new NullServerException("Null Server"));
            }

            Punishment punishment = Punishment.findFirst("id = ? and server_id = ?", id, discordServer.getServerId());

            if (punishment == null) {
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.error(new NotFoundException("404 Not Found"));
            }

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getMessageDelete() + " Case #" + punishment.getPunishmentId() + " Deleted")
                    .timestamp(Instant.now())
                    .build();

            punishment.delete();
            DatabaseLoader.closeConnectionIfOpen();
            return Notifier.sendResultsEmbed(event, embed);
        });
    }

    private Mono<Void> recentCases(ChatInputInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();

        return event.getInteraction().getGuild().flatMap(guild -> {
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

                    DatabaseLoader.closeConnectionIfOpen();
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

            DatabaseLoader.closeConnectionIfOpen();
            return Notifier.sendResultsEmbed(event, resultEmbed).then(AuditLogger.addCommandToDB(event, true));
        });
    }

    private Mono<Void> searchForCase(ChatInputInteractionEvent event) {

        if (event.getOption("search").get().getOption("id").isEmpty() || event.getOption("search").get().getOption("id").get().getOption("caseid").isEmpty()) {
            return AuditLogger.addCommandToDB(event, false).then(Mono.error(new MalformedInputException("Malformed Input")));
        }

        DatabaseLoader.openConnectionIfClosed();

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

        DatabaseLoader.closeConnectionIfOpen();
        return Notifier.sendResultsEmbed(event, embed).then(AuditLogger.addCommandToDB(event, true));

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
        DatabaseLoader.openConnectionIfClosed();

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

                DatabaseLoader.closeConnectionIfOpen();
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

        DatabaseLoader.closeConnectionIfOpen();
        return Notifier.sendResultsEmbed(event, resultEmbed).then(AuditLogger.addCommandToDB(event, true));
    }

    private Mono<Void> updatePunishment(ChatInputInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();

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
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.error(new MalformedInputException("Malformed Input"));
        }
    }

    private Mono<String> createFormattedPunishmentsTable(LazyList<Punishment> punishmentLazyList, Guild guild) {
        List<String> rows = new ArrayList<>();
        rows.add("```");
        rows.add(String.format("| %-6s | %-8s | %-30s |\n", "ID", "Type", "Mod"));
        rows.add("------------------------------------------------------\n");

        Mono<Void> populateTable = Flux.fromIterable(punishmentLazyList)
                .filter(Objects::nonNull)
                .flatMap(p -> {
                    DiscordUser punishingUser = DiscordUser.findFirst("id = ?", p.getPunishingUserId());
                    int id = p.getPunishmentId();

                    return guild.getMemberById(Snowflake.of(punishingUser.getUserIdSnowflake())).flatMap(member -> {
                        String punisher = getUsernameDefaultID(punishingUser, member, 30);

                        String type = p.getPunishmentType();
                        rows.add(String.format("| %-6s | %-8s | %-30s |\n", id, type, punisher));
                        return Mono.empty();
                    });
                }).then();

        return populateTable.flatMap(unused -> {
            rows.add("```");

            StringBuilder stringBuffer = new StringBuilder();
            for (String row : rows) {
                stringBuffer.append(row);
            }

            return Mono.just(stringBuffer.toString());
        });
    }

    private Mono<String> createFormattedRecentPunishmentsTable(LazyList<Punishment> punishmentLazyList, Guild guild) {
        List<String> rowList = new ArrayList<>();

        Mono<List<String>> populateTable = Flux.fromIterable(punishmentLazyList)
                .filter(Objects::nonNull)
                .flatMap(p -> {
                    DiscordUser punishedUser = DiscordUser.findFirst("id = ?", p.getPunishedUserId());
                    int id = p.getPunishmentId();
                    return guild.getMemberById(Snowflake.of(punishedUser.getUserIdSnowflake()))
                            .map(member -> {

                                String punished = getUsernameDefaultID(punishedUser, member, 30);

                                String type = p.getPunishmentType();
                                return String.format("| %-6s | %-8s | %-30s |\n", id, type, punished);
                            });
                }).collectList();


        return Mono.just(rowList).flatMap(rows -> {
            rows.add("```");
            rows.add(String.format("| %-6s | %-8s | %-30s |\n", "ID", "Type", "Punished User"));
            rows.add("------------------------------------------------------\n");
            return populateTable.flatMap(stringList -> {
                rows.addAll(stringList);
                rows.add("```");
                StringBuilder stringBuffer = new StringBuilder();
                for (String row : rows) {
                    stringBuffer.append(row);
                }

                return Mono.just(stringBuffer.toString());
            });
        });
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

    private String getUsernameDefaultID(DiscordUser discordUser, User user, int allowedLength) {
        Long userId = discordUser.getUserIdSnowflake();
        String usernameOrId;
        try {
            usernameOrId = user.getTag();
        } catch (Exception ignored) {
            usernameOrId = userId.toString();
        }

        if (usernameOrId.length() > allowedLength) {
            usernameOrId = usernameOrId.substring(0, allowedLength - 3) + "...";
        }

        return usernameOrId;
    }
}
