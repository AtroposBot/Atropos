package dev.laarryy.Icicle.commands.search;

import dev.laarryy.Icicle.commands.Command;
import dev.laarryy.Icicle.models.guilds.CommandUse;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.AuditLogger;
import dev.laarryy.Icicle.utils.Notifier;
import dev.laarryy.Icicle.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class AuditCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("audit")
            .description("View commands used in this guild.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("Search a user's commands.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("snowflake")
                            .description("User ID to view commands of.")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(false)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("mention")
                            .description("User mention to view commands of.")
                            .type(ApplicationCommandOptionType.USER.getValue())
                            .required(false)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("recent")
                    .description("View recent commands.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("id")
                    .description("View details of an audit by its ID.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("number")
                            .description("The number of the audit you'd like to view.")
                            .type(ApplicationCommandOptionType.INTEGER.getValue())
                            .required(true)
                            .build())
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        if (event.getOption("user").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::searchAuditByUser);
            return Mono.empty();
        }

        if (event.getOption("recent").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::recentAudits);
            return Mono.empty();
        }

        if (event.getOption("id").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::searchAuditById);
        }

        return Mono.empty();
    }

    private void searchAuditById(SlashCommandEvent event) {
        if (ensureGuildAndPermissionCheck(event)) return;

        if (event.getOption("id").get().getOption("number").isEmpty() || event.getOption("id").get().getOption("number").get().getValue().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());
        int auditInt = (int) event.getOption("id").get().getOption("number").get().getValue().get().asLong();

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        int serverId = discordServer.getServerId();
        CommandUse commandUse = CommandUse.findFirst("id = ? and server_id = ?", auditInt, serverId);

        if (commandUse == null) {
            Notifier.notifyCommandUserOfError(event, "404");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        DiscordUser discordUser = DiscordUser.findFirst("id = ?", commandUse.getUserId());

        if (discordUser == null) {
            Notifier.notifyCommandUserOfError(event, "noUser");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        String succeeded;
        if (commandUse.getSucceeded()) {
            succeeded = "Command succeeded.";
        } else {
            succeeded = "Command failed.";
        }

        Long userSnowflake = discordUser.getUserIdSnowflake();
        String date = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.FULL)
                .withLocale(Locale.CANADA)
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(commandUse.getDate()));

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Audit: " + auditInt)
                .description("Command contents: `" + commandUse.getCommandContents() + "`")
                .addField("User", "`" + userSnowflake + "`:<@" + userSnowflake + ">", false)
                .addField("Date", date, false)
                .addField("Result", succeeded, false)
                .build();

        event.reply().withEmbeds(embed).subscribe();


    }

    private void recentAudits(SlashCommandEvent event) {

        if (ensureGuildAndPermissionCheck(event)) return;

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());

        Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        long tenDaysAgoStamp = tenDaysAgo.toEpochMilli();

        LazyList<CommandUse> commandUseLazyList = CommandUse.where("server_id = ? and date > ?", discordServer.getServerId(), tenDaysAgoStamp).limit(25).orderBy("id desc");

        String results = createFormattedAuditTable(commandUseLazyList, event);

        EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Recent Cases")
                .description(results)
                .footer("For detailed information, run /audit id <id>", "")
                .timestamp(Instant.now())
                .build();
        event.reply().withEmbeds(resultEmbed).subscribe();
        AuditLogger.addCommandToDB(event, true);
    }

    private void searchAuditByUser(SlashCommandEvent event) {

        if (ensureGuildAndPermissionCheck(event)) return;

        long userIdSnowflake;
        if (event.getOption("user").get().getOption("snowflake").isPresent()) {
            String snowflakeString = event.getOption("user").get().getOption("snowflake").get().getValue().get().asString();
            Pattern snowflakePattern = Pattern.compile("\\d{10,20}");

            if (!snowflakePattern.matcher(snowflakeString).matches()) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                AuditLogger.addCommandToDB(event, false);
                return;
            }

            userIdSnowflake = Long.parseLong(snowflakeString);
        } else if (event.getOption("user").get().getOption("mention").isPresent()) {

            userIdSnowflake = event.getOption("user").get().getOption("mention").get().getValue().get().asUser().block().getId().asLong();

        } else {
            userIdSnowflake = 0L;
        }

        long guildId = event.getInteraction().getGuildId().get().asLong();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildId);

        if (discordUser == null) {
            Notifier.notifyCommandUserOfError(event, "noUser");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        int userId = discordUser.getUserId();
        int serverId = discordServer.getServerId();

        Instant tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS);
        long tenDaysAgoStamp = tenDaysAgo.toEpochMilli();

        LazyList<CommandUse> commandUsesLazyList = CommandUse.find("command_user_id = ? and server_id = ? and date > ?", userId, serverId, tenDaysAgoStamp).limit(30).orderBy("id desc");

        if (commandUsesLazyList.isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "noResults");
            AuditLogger.addCommandToDB(event, true);
            return;
        }

        String results = createFormattedAuditTable(commandUsesLazyList, event);

        AuditLogger.addCommandToDB(event, true);

        EmbedCreateSpec resultEmbed = EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Results")
                .description(results)
                .footer("For detailed information, run /audit id <id>", "")
                .timestamp(Instant.now())
                .build();
        event.reply().withEmbeds(resultEmbed).subscribe();
        AuditLogger.addCommandToDB(event, true);
    }

    private String createFormattedAuditTable(LazyList<CommandUse> commandUseLazyList, SlashCommandEvent event) {
        List<String> rows = new ArrayList<>();
        rows.add("```");
        rows.add(String.format("| %-6s | %-12s | %-18s | %-8s |\n", "ID", "Date", "User", "Preview"));
        rows.add("---------------------------------------------------------\n");

        for (CommandUse c : commandUseLazyList) {
            if (c == null) {
                continue;
            }
            DiscordUser discordUser = DiscordUser.findFirst("id = ?", c.getUserId());
            String userId = discordUser.getUserIdSnowflake().toString();
            Guild guild = event.getInteraction().getGuild().block();
            String username;
            if (guild.getMemberById(Snowflake.of(userId)).block() != null) {
                username = guild.getMemberById(Snowflake.of(userId)).block().getUsername() + "#" + guild.getMemberById(Snowflake.of(userId)).block().getDiscriminator();
            } else username = userId;

            if (username.length() > 18) {
                username = username.substring(0,15) + "...";
            }
            String auditId = c.getInteger("id").toString();
            Instant date = Instant.ofEpochMilli(c.getDate());
            String dateString = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.CANADA).withZone(ZoneId.systemDefault()).format(date);
            String preview;
            if (c.getCommandContents().length() > 4) {
                preview = "/" + c.getCommandContents().substring(0,4) + "...";
            } else preview = c.getCommandContents();

            rows.add(String.format("| %-6s | %-12s | %-18s | %-8s |\n", auditId, dateString, username, preview));
        }

        rows.add("```");

        StringBuffer stringBuffer = new StringBuffer();
        for (String row: rows) {
            stringBuffer.append(row);
        }

        return stringBuffer.toString();
    }

    private boolean ensureGuildAndPermissionCheck(SlashCommandEvent event) {
        DatabaseLoader.openConnectionIfClosed();

        if (event.getInteraction().getGuild().block() == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return true;
        }

        int permissionID = Permission.findOrCreateIt("permission", "audit").getInteger("id");
        if (!permissionChecker.checkPermission(event.getInteraction().getGuild().block(), event.getInteraction().getUser(), permissionID)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            return true;
        }
        return false;
    }
}
