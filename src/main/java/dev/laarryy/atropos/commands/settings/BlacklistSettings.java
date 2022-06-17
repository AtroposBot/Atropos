package dev.laarryy.atropos.commands.settings;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.exceptions.AlreadyBlacklistedException;
import dev.laarryy.atropos.exceptions.InputTooLongException;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NotFoundException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.exceptions.TooManyEntriesException;
import dev.laarryy.atropos.managers.BlacklistCacheManager;
import dev.laarryy.atropos.models.guilds.Blacklist;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.ServerBlacklist;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.Notifier;
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

        if (event.getOption("blacklist").get().getOption("add").isPresent()) {
            return addBlacklistEntry(event);
        }

        if (event.getOption("blacklist").get().getOption("remove").isPresent()) {
            return removeBlacklistEntry(event);
        }

        if (event.getOption("blacklist").get().getOption("list").isPresent()) {
            return listBlacklistEntries(event);
        }

        if (event.getOption("blacklist").get().getOption("info").isPresent()) {
            return getBlacklistEntryInfo(event);
        }

        DatabaseLoader.closeConnectionIfOpen();
        return Mono.error(new MalformedInputException("Malformed Input"));
    }

    private Mono<Void> getBlacklistEntryInfo(ChatInputInteractionEvent event) {

        return event.getInteraction().getGuild().flatMap(guild -> {
            long guildId = guild.getId().asLong();

            if (event.getOption("blacklist").get().getOption("info").get().getOption("id").isEmpty() || event.getOption("blacklist").get().getOption("info").get().getOption("id").get().getValue().isEmpty()) {
                AuditLogger.addCommandToDB(event, false);
                return Mono.error(new MalformedInputException("Malformed Input"));
            }

            DatabaseLoader.openConnectionIfClosed();
            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildId);

            if (discordServer == null) {
                return Mono.error(new NullServerException("Null Server"));
            }

            int serverId = discordServer.getServerId();
            long inputId = event.getOption("blacklist").get().getOption("info").get().getOption("id").get().getValue().get().asLong();

            ServerBlacklist blacklist = ServerBlacklist.findFirst("server_id = ? and id = ?", serverId, inputId);

            if (blacklist == null) {
                AuditLogger.addCommandToDB(event, false);
                return Mono.error(new NotFoundException("404 Not Found"));
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

            return Notifier.sendResultsEmbed(event, embed);
        });
    }

    private Mono<Void> listBlacklistEntries(ChatInputInteractionEvent event) {

        return event.getInteraction().getGuild().flatMap(guild -> {
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

            return Notifier.sendResultsEmbed(event, embed);
        });
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

    private Mono<Void> removeBlacklistEntry(ChatInputInteractionEvent event) {
        if (event.getOption("blacklist").get().getOption("remove").get().getOption("id").isEmpty()) {
            AuditLogger.addCommandToDB(event, false);
            return Mono.error(new MalformedInputException("Malformed Input"));
        }

        return event.getInteraction().getGuild().flatMap(guild -> {
            DatabaseLoader.openConnectionIfClosed();

            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

            if (discordServer == null) {
                return Mono.error(new NullServerException("Null Server"));
            }

            if (event.getOption("blacklist").get().getOption("remove").get().getOption("id").get().getValue().isEmpty()) {
                return Mono.error(new MalformedInputException("Malformed Input"));
            }

            long blacklistId = event.getOption("blacklist").get().getOption("remove").get().getOption("id").get().getValue().get().asLong();

            ServerBlacklist serverBlacklist = ServerBlacklist.findFirst("id = ?", blacklistId);

            if (serverBlacklist == null) {
                return Mono.error(new NotFoundException("404 Not Found"));
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

            return Notifier.sendResultsEmbed(event, embed);
        });
    }

    private Mono<Void> addBlacklistEntry(ChatInputInteractionEvent event) {
        if (event.getOption("blacklist").get().getOption("add").get().getOption("type").get().getValue().isEmpty()
                || event.getOption("blacklist").get().getOption("add").get().getOption("entry").get().getValue().isEmpty()
                || event.getOption("blacklist").get().getOption("add").get().getOption("action").get().getValue().isEmpty()) {
            AuditLogger.addCommandToDB(event, false);
            return Mono.error(new MalformedInputException("Malformed Input"));
        }

        return event.getInteraction().getGuild().flatMap(guild -> {
            DatabaseLoader.openConnectionIfClosed();

            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

            if (discordServer == null) {
                return Mono.error(new NullServerException("Null Server"));
            }

            if (event.getOption("blacklist").get().getOption("add").get().getOption("entry").get().getValue().get().asString().length() >= 200) {
                AuditLogger.addCommandToDB(event, false);
                return Mono.error(new InputTooLongException("Input Too Long"));
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
                AuditLogger.addCommandToDB(event, false);
                return Mono.error(new AlreadyBlacklistedException("Already Blacklisted"));
            }

            LoadingCache<Long, List<Blacklist>> blacklistCache = BlacklistCacheManager.getManager().getBlacklistCache();

            List<Blacklist> blacklistList = blacklistCache.get(guild.getId().asLong());

            if (blacklistList.size() >= 31) {
                AuditLogger.addCommandToDB(event, false);
                return Mono.error(new TooManyEntriesException("Too Many Entries"));
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

            return Notifier.sendResultsEmbed(event, embed);
        });
    }
}
