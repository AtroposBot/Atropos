package dev.laarryy.Eris.commands.settings;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Eris.commands.Command;
import dev.laarryy.Eris.managers.BlacklistCacheManager;
import dev.laarryy.Eris.models.guilds.Blacklist;
import dev.laarryy.Eris.models.guilds.DiscordServer;
import dev.laarryy.Eris.models.guilds.ServerBlacklist;
import dev.laarryy.Eris.storage.DatabaseLoader;
import dev.laarryy.Eris.utils.AuditLogger;
import dev.laarryy.Eris.utils.Notifier;
import dev.laarryy.Eris.utils.PermissionChecker;
import dev.laarryy.Eris.utils.SlashCommandChecks;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public class BlacklistCommand implements Command {

    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    List<ApplicationCommandOptionChoiceData> optionChoiceDataList = List.of(
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("File (jar, exe, etc.)")
                    .value("file")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("String (regex)")
                    .value("string")
                    .build());

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("blacklist")
            .description("Manage the blacklist - blacklisted strings or files will be auto-deleted")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("add")
                    .description("Add a blacklist entry")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("type")
                            .description("Type of entry")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .choices(optionChoiceDataList)
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("entry")
                            .description("Entry to add to the blacklist")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("remove")
                    .description("Remove a blacklist entry")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("id")
                            .description("ID of blacklist entry to remove")
                            .type(ApplicationCommandOptionType.INTEGER.getValue())
                            .required(true)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("list")
                    .description("List blacklist entries for this guild")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("info")
                    .description("Display info for a blacklist entry")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("id")
                            .description("ID of blacklist entry to show info for")
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

        if (!SlashCommandChecks.slashCommandChecks(event, request)) {
            return Mono.empty();
        }

        Guild guild = event.getInteraction().getGuild().block();
        User user = event.getInteraction().getUser();

        if (event.getOption("add").isPresent()) {
            addBlacklistEntry(event);
            return Mono.empty();
        }

        if (event.getOption("remove").isPresent()) {
            removeBlacklistEntry(event);
            return Mono.empty();
        }

        if (event.getOption("list").isPresent()) {
            listBlacklistEntries(event);
            return Mono.empty();
        }

        if (event.getOption("info").isPresent()) {
            getBlacklistEntryInfo(event);
            return Mono.empty();
        }

        return Mono.empty();
    }

    private void getBlacklistEntryInfo(SlashCommandEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        long guildId = guild.getId().asLong();

        if (event.getOption("info").get().getOption("id").isEmpty() || event.getOption("info").get().getOption("id").get().getValue().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        DatabaseLoader.openConnectionIfClosed();
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildId);

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return;
        }

        int serverId = discordServer.getServerId();
        long inputId = event.getOption("info").get().getOption("id").get().getValue().get().asLong();

        ServerBlacklist blacklist = ServerBlacklist.findFirst("server_id = ? and id = ?", serverId, inputId);

        if (blacklist == null) {
            Notifier.notifyCommandUserOfError(event, "404");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        long blacklistId = blacklist.getBlacklistId();
        String type = blacklist.getType();
        String trigger = blacklist.getTrigger();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Blacklist Entry: ID #" + blacklistId)
                .addField("Type", type.toUpperCase(), false)
                .addField("Entry", trigger, false)
                .timestamp(Instant.now())
                .footer("To remove this entry, run /blacklist remove " + blacklistId, "")
                .build();

        event.reply().withEmbeds(embed).subscribe();
    }

    private void listBlacklistEntries(SlashCommandEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        long guildId = guild.getId().asLong();

        LoadingCache<Long, List<Blacklist>> cache = BlacklistCacheManager.getManager().getBlacklistCache();

        List<Blacklist> blacklistList = cache.get(guildId);

        String blacklistInfo;
        if (blacklistList == null || blacklistList.isEmpty()) {
            blacklistInfo = "none";
        } else {
            blacklistInfo = getBlacklistListInfo(blacklistList);
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Blacklist Entries")
                .color(Color.ENDEAVOUR)
                .timestamp(Instant.now())
                .build();

        if (!blacklistInfo.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .description(blacklistInfo)
                    .build();
        } else {
            embed = EmbedCreateSpec.builder().from(embed)
                    .description("> No blacklist entries found in this guild")
                    .build();
        }

        event.reply().withEmbeds(embed).subscribe();
    }

    private String getBlacklistListInfo(List<Blacklist> serverBlacklistList) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("```");
        stringBuilder.append(String.format("| %-4s | %-7s | %-25s |\n", "ID", "Type", "Entry"));
        stringBuilder.append("----------------------------------------------\n");
        for (Blacklist blacklist : serverBlacklistList) {
            String trigger;
            if (blacklist.getServerBlacklist().getTrigger().length() >= 25) {
                trigger = blacklist.getServerBlacklist().getTrigger().substring(0, 22) + "...";
            } else {
                trigger = blacklist.getServerBlacklist().getTrigger();
            }
            if (serverBlacklistList.indexOf(blacklist) == serverBlacklistList.size() - 1) {
                stringBuilder.append(String.format("| %-4s | %-7s | %-25s |", blacklist.getServerBlacklist().getBlacklistId(), blacklist.getServerBlacklist().getType(), trigger));
            } else {
                stringBuilder.append(String.format("| %-4s | %-7s | %-25s |\n", blacklist.getServerBlacklist().getBlacklistId(), blacklist.getServerBlacklist().getType(), trigger));
            }
        }
        stringBuilder.append("```");
        return stringBuilder.toString();
    }

    private void removeBlacklistEntry(SlashCommandEvent event) {
        if (event.getOption("remove").get().getOption("id").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            return;
        }

        Guild guild = event.getInteraction().getGuild().block();
        DatabaseLoader.openConnectionIfClosed();

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return;
        }

        if (event.getOption("remove").get().getOption("id").get().getValue().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
        }

        long blacklistId = event.getOption("remove").get().getOption("id").get().getValue().get().asLong();

        ServerBlacklist serverBlacklist = ServerBlacklist.findFirst("id = ?", blacklistId);

        if (serverBlacklist == null) {
            Notifier.notifyCommandUserOfError(event, "404");
            return;
        }

        LoadingCache<Long, List<Blacklist>> cache = BlacklistCacheManager.getManager().getBlacklistCache();
        serverBlacklist.delete();
        cache.invalidate(guild.getId().asLong());
        AuditLogger.addCommandToDB(event, true);

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Success")
                .color(Color.SEA_GREEN)
                .description("Deleted blacklist entry with ID `" + blacklistId + "`")
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).withEphemeral(true).subscribe();
    }

    private void addBlacklistEntry(SlashCommandEvent event) {
        if (event.getOption("add").get().getOption("type").get().getValue().isEmpty() || event.getOption("add").get().getOption("entry").get().getValue().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        Guild guild = event.getInteraction().getGuild().block();
        DatabaseLoader.openConnectionIfClosed();

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return;
        }

        if (event.getOption("add").get().getOption("entry").get().getValue().get().asString().length() >= 200) {
            Notifier.notifyCommandUserOfError(event, "inputTooLong");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        long serverId = discordServer.getServerId();
        String type = event.getOption("add").get().getOption("type").get().getValue().get().asString().replaceAll("\\.", "");
        String regexTrigger = event.getOption("add").get().getOption("entry").get().getValue().get().asString();

        ServerBlacklist serverBlacklist = ServerBlacklist.findFirst("server_id = ? and regex_trigger = ? and type = ?",
                serverId,
                regexTrigger,
                type);

        if (serverBlacklist != null) {
            Notifier.notifyCommandUserOfError(event, "alreadyBlacklisted");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        LoadingCache<Long, List<Blacklist>> blacklistCache = BlacklistCacheManager.getManager().getBlacklistCache();

        List<Blacklist> blacklistList = blacklistCache.get(guild.getId().asLong());

        if (blacklistList.size() >= 31) {
            Notifier.notifyCommandUserOfError(event, "tooManyEntries");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        ServerBlacklist blacklist = ServerBlacklist.create("server_id", serverId, "type", type, "regex_trigger", regexTrigger);
        blacklist.save();
        blacklist.refresh();
        blacklistCache.invalidate(guild.getId().asLong());
        AuditLogger.addCommandToDB(event, true);

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Success")
                .color(Color.SEA_GREEN)
                .description("Created blacklist entry with ID `" + blacklist.getBlacklistId() + "`")
                .addField("Blacklisted Entry", "`" + blacklist.getTrigger() + "`", false)
                .addField("Type", blacklist.getType().toUpperCase(), false)
                .footer("To remove this entry, run /blacklist remove " + blacklist.getBlacklistId(), "")
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).subscribe();
    }
}
