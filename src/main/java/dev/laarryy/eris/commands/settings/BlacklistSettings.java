package dev.laarryy.eris.commands.settings;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.eris.managers.BlacklistCacheManager;
import dev.laarryy.eris.models.guilds.Blacklist;
import dev.laarryy.eris.models.guilds.DiscordServer;
import dev.laarryy.eris.models.guilds.ServerBlacklist;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.AuditLogger;
import dev.laarryy.eris.utils.Notifier;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public class BlacklistSettings {

    private final Logger logger = LogManager.getLogger(this);

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        Guild guild = event.getInteraction().getGuild().block();
        User user = event.getInteraction().getUser();

        if (event.getOption("blacklist").get().getOption("add").isPresent()) {
            addBlacklistEntry(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("blacklist").get().getOption("remove").isPresent()) {
            removeBlacklistEntry(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("blacklist").get().getOption("list").isPresent()) {
            listBlacklistEntries(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("blacklist").get().getOption("info").isPresent()) {
            getBlacklistEntryInfo(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }

    private void getBlacklistEntryInfo(ChatInputInteractionEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        long guildId = guild.getId().asLong();

        if (event.getOption("blacklist").get().getOption("info").get().getOption("id").isEmpty() || event.getOption("blacklist").get().getOption("info").get().getOption("id").get().getValue().isEmpty()) {
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
        long inputId = event.getOption("blacklist").get().getOption("info").get().getOption("id").get().getValue().get().asLong();

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

    private void listBlacklistEntries(ChatInputInteractionEvent event) {
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

    private void removeBlacklistEntry(ChatInputInteractionEvent event) {
        if (event.getOption("blacklist").get().getOption("remove").get().getOption("id").isEmpty()) {
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

        if (event.getOption("blacklist").get().getOption("remove").get().getOption("id").get().getValue().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
        }

        long blacklistId = event.getOption("blacklist").get().getOption("remove").get().getOption("id").get().getValue().get().asLong();

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

        event.reply().withEmbeds(embed).subscribe();
    }

    private void addBlacklistEntry(ChatInputInteractionEvent event) {
        if (event.getOption("blacklist").get().getOption("add").get().getOption("type").get().getValue().isEmpty()
                || event.getOption("blacklist").get().getOption("add").get().getOption("entry").get().getValue().isEmpty()
                || event.getOption("blacklist").get().getOption("add").get().getOption("action").get().getValue().isEmpty()) {
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

        if (event.getOption("blacklist").get().getOption("add").get().getOption("entry").get().getValue().get().asString().length() >= 200) {
            Notifier.notifyCommandUserOfError(event, "inputTooLong");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        long serverId = discordServer.getServerId();
        String type = event.getOption("blacklist").get().getOption("add").get().getOption("type").get().getValue().get().asString().replaceAll("\\.", "");
        String regexTrigger = event.getOption("blacklist").get().getOption("add").get().getOption("entry").get().getValue().get().asString();

        ServerBlacklist serverBlacklist = ServerBlacklist.findFirst("server_id = ? and regex_trigger = ? and type = ?",
                serverId,
                regexTrigger,
                type);

        String action = event.getOption("blacklist").get().getOption("add").get().getOption("action").get().getValue().get().asString();

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

        ServerBlacklist blacklist = ServerBlacklist.create("server_id", serverId, "type", type, "regex_trigger", regexTrigger, "action", action);
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
                .addField("Action", blacklist.getAction().toUpperCase(), false)
                .footer("To remove this entry, run /settings blacklist remove " + blacklist.getBlacklistId(), "")
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).subscribe();
    }
}
