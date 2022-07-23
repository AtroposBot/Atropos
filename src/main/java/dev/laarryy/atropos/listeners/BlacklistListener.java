package dev.laarryy.atropos.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.commands.punishments.PunishmentManager;
import dev.laarryy.atropos.exceptions.NotFoundException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.BlacklistCacheManager;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import dev.laarryy.atropos.managers.PunishmentManagerManager;
import dev.laarryy.atropos.models.guilds.Blacklist;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.Notifier;
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

        return event.getGuild().flatMap(guild -> {
            if (guild == null) {
                return Mono.error(new NullServerException("Null Server in Blacklist Listener"));
            }

            if (event.getMember().isEmpty() || event.getMessage().getAuthor().isEmpty() || event.getMessage().getAuthor().get().isBot()) {
                return Mono.empty();
            }

            return guild.getSelfMember().flatMap(selfMember -> {
                if (event.getMember().isPresent() && event.getMember().get().equals(selfMember)) {
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
            });
        });
    }

    private Mono<Void> handleTrigger(MessageCreateEvent event, Guild guild, long userIdSnowflake, Blacklist b, String action) {
        String type = switch (b.getServerBlacklist().getAction()) {
            case "ban" -> "ban";
            case "mute" -> "mute";
            case "warn" -> "warn";
            default -> "note";
        };

        return createPunishment(event, b, type).flatMap(p ->
                guild.getSelfMember().flatMap(selfMember ->
                        punishmentManager.onlyCheckIfPunisherHasHighestRole(selfMember, event.getMember().get(), guild).flatMap(aBoolean -> {
            if (!aBoolean) {
                return loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p);
            }

            if (action.equals("delete")){
                return event.getMessage().delete()
                        .then(loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p));
            }

            if (action.equals("warn")) {
                return event.getMessage().delete().then(Notifier.notifyPunished(guild, p, "Warned for triggering blacklist: `" + b.getServerBlacklist().getTrigger() + "`"))
                        .then(loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p));
            }

            if (action.equals("mute")) {
                return event.getMessage().delete().then(
                                Notifier.notifyPunished(guild, p, "Muted for triggering blacklist: `" + b.getServerBlacklist().getTrigger() + "`"))
                        .then(punishmentManager.discordMuteUser(guild, userIdSnowflake))
                        .then(loggingListener.onBlacklistMute(event, p));
            }

            if (action.equals("ban")) {
                return event.getMessage().delete().then(Notifier.notifyPunished(guild, p, "Banned for triggering blacklist: `" + b.getServerBlacklist().getTrigger() + "`"))
                        .then(punishmentManager.discordBanUser(guild, userIdSnowflake, 0, "Triggered the blacklist: `" + b.getServerBlacklist().getTrigger() + "`"))
                        .then(loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p));
            }

            return loggingListener.onBlacklistTrigger(event, b.getServerBlacklist(), p);
    })));
    }

    private Mono<Punishment> createPunishment(MessageCreateEvent event, Blacklist blacklist, String type) {
        DatabaseLoader.openConnectionIfClosed();
        Member punishedMember = event.getMember().get();

        return event.getGuild().flatMap(guild -> guild.getSelfMember().flatMap(self -> {
            long punishedSnowflake = event.getMember().get().getId().asLong();
            long punisherSnowflake = self.getId().asLong();
            DiscordUser punishedUser = DiscordUser.findOrCreateIt("user_id_snowflake", punishedSnowflake);
            DiscordUser punisherUser = DiscordUser.findOrCreateIt("user_id_snowflake", punisherSnowflake);
            DiscordServer server = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
            long date = Instant.now().toEpochMilli();

            String reason = "Triggered the blacklist, which matched to the guild-level filter `" + blacklist.getServerBlacklist().getTrigger() + "`. This action will be reviewed by moderators.";

            Punishment punishment = Punishment.create(
                    "user_id_punished", punishedUser.getUserId(),
                    "name_punished", punishedMember.getUsername(),
                    "discrim_punished", punishedMember.getDiscriminator(),
                    "user_id_punisher", punisherUser.getUserId(),
                    "name_punisher", self.getUsername(),
                    "discrim_punisher", self.getDiscriminator(),
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
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.just(punishment);
        }));
    }
}
