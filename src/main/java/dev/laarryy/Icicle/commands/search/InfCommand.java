package dev.laarryy.Icicle.commands.search;

import dev.laarryy.Icicle.commands.Command;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.AuditLogger;
import dev.laarryy.Icicle.utils.Notifier;
import dev.laarryy.Icicle.utils.PermissionChecker;
import dev.laarryy.Icicle.utils.TimestampMaker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
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

public class InfCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final Pattern snowflakePattern = Pattern.compile("\\d{10,20}");

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("inf")
            .description("Search and manage infractions.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("search")
                    .description("Search infractions.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("user")
                            .description("Search by user ID.")
                            .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("snowflake")
                                    .description("User ID to search.")
                                    .type(ApplicationCommandOptionType.STRING.getValue())
                                    .required(false)
                                    .build())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("mention")
                                    .description("User mention to search.")
                                    .type(ApplicationCommandOptionType.USER.getValue())
                                    .required(false)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("case")
                            .description("Search by case ID.")
                            .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("caseid")
                                    .description("Case ID to search.")
                                    .type(ApplicationCommandOptionType.INTEGER.getValue())
                                    .required(true)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("recent")
                            .description("Show recent cases.")
                            .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                            .required(false)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("update")
                    .description("Update reason for infraction.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("id")
                            .description("ID of the infraction you want to modify.")
                            .type(ApplicationCommandOptionType.INTEGER.getValue())
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("reason")
                            .description("New reason for infraction.")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("which")
                            .description("Which reason to update - punishment or end punishment reason?")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .choices(List.of(
                                    ApplicationCommandOptionChoiceData.builder().name("punishment").value("punishment").build(),
                                    ApplicationCommandOptionChoiceData.builder().name("endpunishment").value("endpunishment").build()))
                            .build())
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        if (event.getOption("update").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::updatePunishment);
            return Mono.empty();
        }

        if (event.getOption("search").isPresent() && event.getOption("search").get().getOption("user").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::searchPunishments);
            return Mono.empty();
        }

        if (event.getOption("search").isPresent() && event.getOption("search").get().getOption("case").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::searchForCase);
            return Mono.empty();
        }

        if (event.getOption("search").get().getOption("recent").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::recentCases);
            return Mono.empty();
        }

        return Mono.empty();
    }

    private void recentCases(SlashCommandEvent event) {

        if (ensureGuildAndPermissionCheck(event)) return;

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());

        Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        long tenDaysAgoStamp = tenDaysAgo.toEpochMilli();

        LazyList<Punishment> punishmentLazyList = Punishment.where("server_id = ? and punishment_date > ?", discordServer.getServerId(), tenDaysAgoStamp).limit(25).orderBy("id desc");

        String results = createFormattedPunishmentsTable(punishmentLazyList, event);

        EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Recent Cases")
                .description(results)
                .footer("For detailed information, run /inf search case <id>", "")
                .timestamp(Instant.now())
                .build();
        event.reply().withEmbeds(resultEmbed).subscribe();
        AuditLogger.addCommandToDB(event, true);
    }



    private void searchForCase(SlashCommandEvent event) {

        if (ensureGuildAndPermissionCheck(event)) return;

        if (event.getOption("search").get().getOption("case").isEmpty() || event.getOption("search").get().getOption("case").get().getOption("caseid").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        int caseInt = (int) event.getOption("search").get().getOption("case").get().getOption("caseid").get().getValue().get().asLong();
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

        DiscordUser discordUser = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        DiscordUser punisher = DiscordUser.findFirst("id = ?", punishment.getPunishingUserId());

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

        if (punishment.getEndDate() != null) {
            endDate = TimestampMaker.getTimestampFromEpochSecond(
                            Instant.ofEpochMilli(punishment.getEndDate()).getEpochSecond(),
                            TimestampMaker.TimestampType.RELATIVE);
        } else {
            endDate = "Not set.";
        }

        String date = TimestampMaker.getTimestampFromEpochSecond(
                Instant.ofEpochMilli(punishment.getDateEntry()).getEpochSecond(),
                TimestampMaker.TimestampType.RELATIVE);

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Case " + punishment.getPunishmentId())
                .addField("User", "`" + userSnowflake + "`: " + "<@" + userSnowflake + ">", false)
                .addField("Moderator", "`" + punisherSnowflake + "`:" +"<@" + punisherSnowflake + ">", false)
                .addField("Moderation Action", punishment.getPunishmentType().toUpperCase(), false)
                .addField("Date", date, false)
                .addField("Reason", reason, false)
                .addField("End Date", endDate, false)
                .addField("End Reason", endReason, false)
                .addField("Attempted to DM User?", didDMMessage, false)
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).subscribe();
        AuditLogger.addCommandToDB(event, true);
    }

    private void searchPunishments(SlashCommandEvent event) {

        if (ensureGuildAndPermissionCheck(event)) return;

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

            logger.info("Snowflake String is: ||" + snowflakeString + "||");
            userIdSnowflake = Long.parseLong(snowflakeString);
        } else if (event.getOption("search").get().getOption("user").isPresent()
                && event.getOption("search").get().getOption("user").get().getOption("mention").isPresent()
                && event.getOption("search").get().getOption("user").get().getOption("mention").get().getValue().isPresent()) {

            userIdSnowflake = event.getOption("search").get().getOption("user").get().getOption("mention").get().getValue().get().asUser().block().getId().asLong();

        } else {
            userIdSnowflake = 0L;
        }


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

        Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        long tenDaysAgoStamp = tenDaysAgo.toEpochMilli();

        LazyList<Punishment> punishmentLazyList = Punishment.find("user_id_punished = ? and server_id = ? and punishment_date > ?", userId, serverId, tenDaysAgoStamp).limit(50).orderBy("id desc");

        if (punishmentLazyList.isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "noResults");
            AuditLogger.addCommandToDB(event, true);
            return;
        }

        String results = createFormattedPunishmentsTable(punishmentLazyList, event);

        EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Results for user " + userIdSnowflake)
                .description(results)
                .footer("For detailed information, run /inf search case <id>", "")
                .timestamp(Instant.now())
                .build();

        AuditLogger.addCommandToDB(event, true);
        event.reply().withEmbeds(resultEmbed).subscribe();
    }

    private void updatePunishment(SlashCommandEvent event) {

        if (event.getInteraction().getGuild().block() == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        DatabaseLoader.openConnectionIfClosed();

        int permissionID = Permission.findOrCreateIt("permission", "infupdate").getInteger("id");
        if (!permissionChecker.checkPermission(event.getInteraction().getGuild().block(), event.getInteraction().getUser(), permissionID)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

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
    }

    private boolean ensureGuildAndPermissionCheck(SlashCommandEvent event) {
        DatabaseLoader.openConnectionIfClosed();

        if (event.getInteraction().getGuild().block() == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return true;
        }

        int permissionID = Permission.findOrCreateIt("permission", "infsearch").getInteger("id");
        if (!permissionChecker.checkPermission(event.getInteraction().getGuild().block(), event.getInteraction().getUser(), permissionID)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            return true;
        }
        return false;
    }

    private String createFormattedPunishmentsTable(LazyList<Punishment> punishmentLazyList, SlashCommandEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        List<String> rows = new ArrayList<>();
        rows.add("```");
        rows.add(String.format("| %-6s | %-8s | %-16s | %-15s |\n", "ID", "Type", "Mod", "Target"));
        rows.add("----------------------------------------------------------\n");

        for (Punishment p : punishmentLazyList) {
            if (p == null) {
                continue;
            }
            int id = p.getPunishmentId();

            DiscordUser punishedUser = DiscordUser.findFirst("id = ?", p.getPunishedUserId());

            String punished = getUsernameDefaultID(punishedUser, guild);

            DiscordUser punishingUser = DiscordUser.findFirst("id = ?", p.getPunishingUserId());
            String punisher = getUsernameDefaultID(punishingUser, guild);

            String type = p.getPunishmentType();
            rows.add(String.format("| %-6s | %-8s | %-16s | %-15s |\n", id, type, punisher, punished));
        }

        rows.add("```");

        StringBuilder stringBuffer = new StringBuilder();
        for (String row: rows) {
            stringBuffer.append(row);
        }

        return stringBuffer.toString();
    }

    private String getUsernameDefaultID(DiscordUser user, Guild guild) {
        Long userId = user.getUserIdSnowflake();
        String usernameOrId;
        try {
            if (guild.getMemberById(Snowflake.of(userId)).block() != null) {
                usernameOrId = guild.getMemberById(Snowflake.of(userId)).block().getUsername() + "#" + guild.getMemberById(Snowflake.of(userId)).block().getDiscriminator();
            } else usernameOrId = String.valueOf(userId);
        } catch (Exception ignored) {
            usernameOrId = userId.toString();
        }

        if (usernameOrId.length() > 15) {
            usernameOrId = usernameOrId.substring(0,12) + "...";
        }

        return usernameOrId;
    }
}
