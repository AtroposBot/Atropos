package dev.laarryy.Icicle.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.listeners.logging.LoggingListener;
import dev.laarryy.Icicle.managers.BlacklistCacheManager;
import dev.laarryy.Icicle.managers.LoggingListenerManager;
import dev.laarryy.Icicle.models.guilds.Blacklist;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

public class BlacklistListener {
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    LoadingCache<Long, List<Blacklist>> cache = BlacklistCacheManager.getManager().getBlacklistCache();
    private final Logger logger = LogManager.getLogger(this);


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
                stringBuilder.append(attachment.getUrl()).append("\n");
                stringBuilder.append(attachment.getFilename()).append("\n");
            }
            files = sb.toString();
        } else {
            files = null;
        }


        String content = stringBuilder.toString();

        for (Blacklist b : blacklistList) {
            Pattern pattern = b.getPattern();
            if (pattern.matcher(content).matches()) {
                Punishment p = createCase(event, b);
                loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p);
                event.getMessage().delete().block();
                return Mono.empty();
            }
            if (files != null && pattern.matcher(files).matches()) {
                Punishment p = createCase(event, b);
                event.getMessage().delete().block();
                loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p);
                return Mono.empty();
            }
        }

        return Mono.empty();
    }

    private Punishment createCase(MessageCreateEvent event, Blacklist blacklist) {
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
                "punishment_type", "case",
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
