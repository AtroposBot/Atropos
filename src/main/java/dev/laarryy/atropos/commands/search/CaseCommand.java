package dev.laarryy.atropos.commands.search;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
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
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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

        if (event.getOption("delete").isPresent()) {
            if (!CommandChecks.commandChecks(event, "casedelete")) {
                return Mono.empty();
            }
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::deletePunishment);
            return Mono.empty();
        }

        if (event.getOption("update").isPresent()) {
            if (!CommandChecks.commandChecks(event, "caseupdate")) {
                return Mono.empty();
            }
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::updatePunishment);
            return Mono.empty();
        }

        if (!CommandChecks.commandChecks(event, "casesearch")) {
            return Mono.empty();
        }

        if (event.getOption("search").isPresent() && event.getOption("search").get().getOption("user").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::searchPunishments);
            return Mono.empty();
        }

        if (event.getOption("search").isPresent() && event.getOption("search").get().getOption("id").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::searchForCase);
            return Mono.empty();
        }

        if (event.getOption("search").get().getOption("recent").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::recentCases);
            return Mono.empty();
        }

        return Mono.empty();
    }

    private void deletePunishment(ChatInputInteractionEvent event) {
        Guild guild = event.getInteraction().getGuild().block();

        if (guild == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return;
        }

        if (event.getOption("delete").get().getOption("id").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            return;
        }

        Long id = event.getOption("delete").get().getOption("id").get().getValue().get().asLong();

        DatabaseLoader.openConnectionIfClosed();

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            DatabaseLoader.closeConnectionIfOpen();
            return;
        }

        Punishment punishment = Punishment.findFirst("id = ? and server_id = ?", id, discordServer.getServerId());

        if (punishment == null) {
            Notifier.notifyCommandUserOfError(event, "404");
            DatabaseLoader.closeConnectionIfOpen();
            return;
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getMessageDelete() + " Case #" + punishment.getPunishmentId() + " Deleted")
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).block();

        punishment.delete();

        DatabaseLoader.closeConnectionIfOpen();
    }

    private void recentCases(ChatInputInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();

        event.deferReply().block();

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
            results = createFormattedRecentPunishmentsTable(punishmentLazyList, event);
        }

        EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Recent Cases")
                .description(results)
                .footer("For detailed information, run /case search id <id>", "")
                .timestamp(Instant.now())
                .build();

        event.getInteractionResponse().editInitialResponse(
                WebhookMessageEditRequest.builder()
                        .addEmbed(resultEmbed.asRequest())
                        .build()).block();

        AuditLogger.addCommandToDB(event, true);
        DatabaseLoader.closeConnectionIfOpen();
    }

    private void searchForCase(ChatInputInteractionEvent event) {

        if (event.getOption("search").get().getOption("id").isEmpty() || event.getOption("search").get().getOption("id").get().getOption("caseid").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        DatabaseLoader.openConnectionIfClosed();

        int caseInt = (int) event.getOption("search").get().getOption("id").get().getOption("caseid").get().getValue().get().asLong();
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        int serverId = discordServer.getServerId();
        Punishment punishment = Punishment.findFirst("id = ? and server_id = ?", caseInt, serverId);

        if (punishment == null) {
            Notifier.notifyCommandUserOfError(event, "404");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        event.deferReply().block();

        DiscordUser discordUser = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        DiscordUser punisher = DiscordUser.findFirst("id = ?", punishment.getPunishingUserId());

        DiscordUser punishmentEnderUser;
        if (punishment.getPunishmentEnder() != null) {
            punishmentEnderUser = DiscordUser.findFirst("id = ?", punishment.getPunishmentEnder());
        } else {
            punishmentEnderUser = null;
        }
        if (discordUser == null || punisher == null) {
            Notifier.notifyCommandUserOfError(event, "noUser");
            AuditLogger.addCommandToDB(event, false);
            return;
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
            moderator = "`" + punisherSnowflake + "`:" +"<@" + punisherSnowflake + ">";
        } else {
            moderator = "`" + punisherSnowflake + "`:" +"<@" + punisherSnowflake + ">:`"
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

        event.getInteractionResponse().editInitialResponse(
                WebhookMessageEditRequest.builder()
                        .addEmbed(embed.asRequest())
                        .build()).subscribe();

        AuditLogger.addCommandToDB(event, true);
        DatabaseLoader.closeConnectionIfOpen();
    }

    private void searchPunishments(ChatInputInteractionEvent event) {

        event.deferReply().block();

        long userIdSnowflake;
        if (event.getOption("search").get().getOption("user").isPresent()
                && event.getOption("search").get().getOption("user").get().getOption("snowflake").isPresent()
                && event.getOption("search").get().getOption("user").get().getOption("snowflake").get().getValue().isPresent()) {

            String snowflakeString = event.getOption("search").get().getOption("user").get().getOption("snowflake").get().getValue().get().asString();

            if (!snowflakePattern.matcher(snowflakeString).matches()) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                AuditLogger.addCommandToDB(event, false);
                return;
            }

            userIdSnowflake = Long.parseLong(snowflakeString);
        } else if (event.getOption("search").get().getOption("user").isPresent()
                && event.getOption("search").get().getOption("user").get().getOption("mention").isPresent()
                && event.getOption("search").get().getOption("user").get().getOption("mention").get().getValue().isPresent()) {

            userIdSnowflake = event.getOption("search").get().getOption("user").get().getOption("mention").get().getValue().get().asUser().block().getId().asLong();

        } else {
            userIdSnowflake = 0L;
        }

        DatabaseLoader.openConnectionIfClosed();


        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);

        if (discordUser == null) {
            Notifier.notifyCommandUserOfError(event, "noUser");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        int userId = discordUser.getUserId();
        long guildId = event.getInteraction().getGuildId().get().asLong();
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildId);

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        int serverId = discordServer.getServerId();

        LazyList<Punishment> punishmentLazyList = Punishment.find("user_id_punished = ? and server_id = ?", userId, serverId).limit(70).orderBy("id desc");

        String results;
        if (punishmentLazyList == null || punishmentLazyList.isEmpty()) {
            results = "No results found for user.";
        } else {
            results = createFormattedPunishmentsTable(punishmentLazyList, event);
        }

        EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Results for User: " + userIdSnowflake)
                .description(results)
                .footer("For detailed information, run /case search id <id>", "")
                .timestamp(Instant.now())
                .build();

        AuditLogger.addCommandToDB(event, true);
        event.getInteractionResponse().editInitialResponse(
                WebhookMessageEditRequest.builder()
                        .addEmbed(resultEmbed.asRequest())
                        .build()).subscribe();
        DatabaseLoader.closeConnectionIfOpen();
    }

    private void updatePunishment(ChatInputInteractionEvent event) {
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
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                AuditLogger.addCommandToDB(event, false);
                return;
            }

            if (punishment == null) {
                Notifier.notifyCommandUserOfError(event, "404");
                AuditLogger.addCommandToDB(event, false);
                return;
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
            AuditLogger.addCommandToDB(event, true);
            event.reply().withEmbeds(spec).subscribe();
        }
        DatabaseLoader.closeConnectionIfOpen();
    }

    private String createFormattedPunishmentsTable(LazyList<Punishment> punishmentLazyList, ChatInputInteractionEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        List<String> rows = new ArrayList<>();
        rows.add("```");
        rows.add(String.format("| %-6s | %-8s | %-30s |\n", "ID", "Type", "Mod"));
        rows.add("------------------------------------------------------\n");

        for (Punishment p : punishmentLazyList) {
            if (p == null) {
                continue;
            }
            int id = p.getPunishmentId();

            DiscordUser punishingUser = DiscordUser.findFirst("id = ?", p.getPunishingUserId());
            String punisher = getUsernameDefaultID(punishingUser, guild, 30);

            String type = p.getPunishmentType();
            rows.add(String.format("| %-6s | %-8s | %-30s |\n", id, type, punisher));
        }

        rows.add("```");

        StringBuilder stringBuffer = new StringBuilder();
        for (String row: rows) {
            stringBuffer.append(row);
        }

        return stringBuffer.toString();
    }

    private String createFormattedRecentPunishmentsTable(LazyList<Punishment> punishmentLazyList, ChatInputInteractionEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        List<String> rows = new ArrayList<>();
        rows.add("```");
        rows.add(String.format("| %-6s | %-8s | %-30s |\n", "ID", "Type", "Punished User"));
        rows.add("------------------------------------------------------\n");

        for (Punishment p : punishmentLazyList) {
            if (p == null) {
                continue;
            }

            DiscordUser punishedUser = DiscordUser.findFirst("id = ?", p.getPunishedUserId());
            int id = p.getPunishmentId();

            String punished = getUsernameDefaultID(punishedUser, guild, 30);

            String type = p.getPunishmentType();
            rows.add(String.format("| %-6s | %-8s | %-30s |\n", id, type, punished));
        }

        rows.add("```");

        StringBuilder stringBuffer = new StringBuilder();
        for (String row: rows) {
            stringBuffer.append(row);
        }

        return stringBuffer.toString();
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

    private String getUsernameDefaultID(DiscordUser user, Guild guild, int allowedLength) {
        Long userId = user.getUserIdSnowflake();
        String usernameOrId;
        try {
            usernameOrId = guild.getMemberById(Snowflake.of(userId)).block().getUsername() + "#" + guild.getMemberById(Snowflake.of(userId)).block().getDiscriminator();
        } catch (Exception ignored) {
            usernameOrId = userId.toString();
        }

        if (usernameOrId.length() > allowedLength) {
            usernameOrId = usernameOrId.substring(0, allowedLength - 3) + "...";
        }

        return usernameOrId;
    }
}
