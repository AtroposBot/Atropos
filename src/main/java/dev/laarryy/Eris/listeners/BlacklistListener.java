package dev.laarryy.Eris.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Eris.commands.punishments.PunishmentManager;
import dev.laarryy.Eris.listeners.logging.LoggingListener;
import dev.laarryy.Eris.managers.BlacklistCacheManager;
import dev.laarryy.Eris.managers.LoggingListenerManager;
import dev.laarryy.Eris.managers.PunishmentManagerManager;
import dev.laarryy.Eris.models.guilds.Blacklist;
import dev.laarryy.Eris.models.guilds.DiscordServer;
import dev.laarryy.Eris.models.users.DiscordUser;
import dev.laarryy.Eris.models.users.Punishment;
import dev.laarryy.Eris.storage.DatabaseLoader;
import dev.laarryy.Eris.utils.Notifier;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

public class BlacklistListener {
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    LoadingCache<Long, List<Blacklist>> cache = BlacklistCacheManager.getManager().getBlacklistCache();
    private final Logger logger = LogManager.getLogger(this);
    private final PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();


    @EventListener
    public Mono<Void> on(MessageCreateEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) {
            return Mono.empty();
        }

        if (event.getMember().isPresent() && event.getMember().get().equals(event.getGuild().block().getSelfMember().block())) {
            return Mono.empty();
        }

        if (event.getMessage().getAuthor().isPresent() && event.getMessage().getAuthor().get().isBot()) {
            return Mono.empty();
        }

        long guildId = guild.getId().asLong();

        List<Blacklist> blacklistList = cache.get(guildId);

        if (blacklistList == null || blacklistList.isEmpty()) {
            return Mono.empty();
        }

        List<Attachment> attachments = event.getMessage().getAttachments();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(event.getMessage().getContent()).append("\n");

        String files;

        if (!attachments.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Attachment attachment : attachments) {
                sb.append(attachment.getFilename()).append("\n");
                stringBuilder.append(attachment.getFilename()).append("\n");
            }
            files = sb.toString();
        } else {
            files = null;
        }

        String content = stringBuilder.toString();
        long userIdSnowflake = event.getMember().get().getId().asLong();

        for (Blacklist b : blacklistList) {
            Pattern pattern = b.getPattern();
            String action =  b.getServerBlacklist().getAction();
            String type = b.getServerBlacklist().getType();

            if (type.equals("file")) {
                if (files != null && pattern.matcher(files).matches()) {
                    return handleTrigger(event, guild, userIdSnowflake, b, action);
                }
            }
            if (type.equals("string")) {
                if (pattern.matcher(content).matches()) {
                    return handleTrigger(event, guild, userIdSnowflake, b, action);
                }
            }
        }

        return Mono.empty();
    }

    private Mono<Void> handleTrigger(MessageCreateEvent event, Guild guild, long userIdSnowflake, Blacklist b, String action) {
        String type = switch (b.getServerBlacklist().getAction()) {
            case "ban" -> "ban";
            case "mute" -> "mute";
            case "warn" -> "warn";
            default -> "case";
        };
        Punishment p = createPunishment(event, b, type);

        if (action.equals("delete")){
            event.getMessage().delete().block();
        }

        if (action.equals("warn")) {
            event.getMessage().delete().block();
            Notifier.notifyPunished(guild, p, "Muted for triggering blacklist: `" + b.getServerBlacklist().getTrigger() + "`");
        }

        if (action.equals("mute")) {
            event.getMessage().delete();
            Notifier.notifyPunished(guild, p, "Muted for triggering blacklist: `" + b.getServerBlacklist().getTrigger() + "`");
            punishmentManager.discordMuteUser(guild, userIdSnowflake);
        }

        if (action.equals("ban")) {
            event.getMessage().delete();
            Notifier.notifyPunished(guild, p, "Banned for triggering blacklist: `" + b.getServerBlacklist().getTrigger() + "`");
            punishmentManager.discordBanUser(guild, userIdSnowflake, 0, "Triggered the blacklist: `" + b.getServerBlacklist().getTrigger() + "`");
        }
        loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p);
        return Mono.empty();
    }

    private Punishment createPunishment(MessageCreateEvent event, Blacklist blacklist, String type) {
        DatabaseLoader.openConnectionIfClosed();
        long punishedSnowflake = event.getMember().get().getId().asLong();
        long punisherSnowflake = event.getGuild().block().getSelfMember().block().getId().asLong();
        DiscordUser punishedUser = DiscordUser.findOrCreateIt("user_id_snowflake", punishedSnowflake);
        DiscordUser punisherUser = DiscordUser.findOrCreateIt("user_id_snowflake", punisherSnowflake);
        DiscordServer server = DiscordServer.findFirst("server_id = ?", event.getGuild().block().getId().asLong());
        long date = Instant.now().toEpochMilli();

        String reason = "Triggered the blacklist, which matched to the guild-level filter `" + blacklist.getServerBlacklist().getTrigger() + "`";

        Punishment punishment = Punishment.create(
                "user_id_punished", punishedUser.getUserId(),
                "user_id_punisher", punisherUser.getUserId(),
                "server_id", server.getServerId(),
                "punishment_type", type,
                "punishment_date", date,
                "punishment_message", reason,
                "did_dm", false,
                "end_date_passed", true
        );

        punishment.save();
        punishment.refresh();
        loggingListener.onPunishment(event, punishment);
        return punishment;
    }
}
