package dev.laarryy.eris.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.eris.commands.punishments.PunishmentManager;
import dev.laarryy.eris.listeners.logging.LoggingListener;
import dev.laarryy.eris.managers.BlacklistCacheManager;
import dev.laarryy.eris.managers.LoggingListenerManager;
import dev.laarryy.eris.managers.PunishmentManagerManager;
import dev.laarryy.eris.models.guilds.Blacklist;
import dev.laarryy.eris.models.guilds.DiscordServer;
import dev.laarryy.eris.models.users.DiscordUser;
import dev.laarryy.eris.models.users.Punishment;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.Notifier;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
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
    private final PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();


    @EventListener
    public Mono<Void> on(MessageCreateEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) {
            return Mono.empty();
        }

        if (event.getMember().isEmpty() || event.getMessage().getAuthor().isEmpty() || event.getMessage().getAuthor().get().isBot()) {
            return Mono.empty();
        }

        if (event.getMember().isPresent() && event.getMember().get().equals(event.getGuild().block().getSelfMember().block())) {
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
            default -> "note";
        };
        Punishment p = createPunishment(event, b, type);

        if (!punishmentManager.onlyCheckIfPunisherHasHighestRole(guild.getSelfMember().block(), event.getMember().get(), guild)) {
            loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p);
            return Mono.empty();
        }

        if (action.equals("delete")){
            event.getMessage().delete().block();
        }

        if (action.equals("warn")) {
            event.getMessage().delete().block();
            Notifier.notifyPunished(guild, p, "Warned for triggering blacklist: `" + b.getServerBlacklist().getTrigger() + "`");
        }

        if (action.equals("mute")) {
            event.getMessage().delete().block();
            Notifier.notifyPunished(guild, p, "Muted for triggering blacklist: `" + b.getServerBlacklist().getTrigger() + "`");
            punishmentManager.discordMuteUser(guild, userIdSnowflake);
            loggingListener.onBlacklistMute(event, p);
        }

        if (action.equals("ban")) {
            event.getMessage().delete().block();
            Notifier.notifyPunished(guild, p, "Banned for triggering blacklist: `" + b.getServerBlacklist().getTrigger() + "`");
            punishmentManager.discordBanUser(guild, userIdSnowflake, 0, "Triggered the blacklist: `" + b.getServerBlacklist().getTrigger() + "`");
        }

        loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p);

        return Mono.empty();
    }

    private Punishment createPunishment(MessageCreateEvent event, Blacklist blacklist, String type) {
        DatabaseLoader.openConnectionIfClosed();
        Member punishedMember = event.getMember().get();
        User self = event.getGuild().block().getSelfMember().block();

        long punishedSnowflake = event.getMember().get().getId().asLong();
        long punisherSnowflake = event.getGuild().block().getSelfMember().block().getId().asLong();
        DiscordUser punishedUser = DiscordUser.findOrCreateIt("user_id_snowflake", punishedSnowflake);
        DiscordUser punisherUser = DiscordUser.findOrCreateIt("user_id_snowflake", punisherSnowflake);
        DiscordServer server = DiscordServer.findFirst("server_id = ?", event.getGuild().block().getId().asLong());
        long date = Instant.now().toEpochMilli();

        String reason = "Triggered the blacklist, which matched to the guild-level filter `" + blacklist.getServerBlacklist().getTrigger() + "`. This action will be reviewed by moderators.";

        Punishment punishment = Punishment.create(
                "user_id_punished", punishedUser.getUserId(),
                "name_punished", punishedMember.getUsername(),
                "discrim_punished", Integer.parseInt(punishedMember.getDiscriminator()),
                "user_id_punisher", punisherUser.getUserId(),
                "name_punisher", self.getUsername(),
                "discrim_punisher", Integer.parseInt(self.getDiscriminator()),
                "server_id", server.getServerId(),
                "punishment_type", type,
                "punishment_date", date,
                "punishment_message", reason,
                "automatic", true,
                "end_date_passed", false,
                "permanent", true
        );

        punishment.save();
        punishment.refresh();
        loggingListener.onPunishment(event, punishment);
        DatabaseLoader.closeConnectionIfOpen();
        return punishment;
    }
}
