package dev.laarryy.Icicle.listeners;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.commands.punishments.PunishmentManager;
import dev.laarryy.Icicle.config.EmojiManager;
import dev.laarryy.Icicle.listeners.logging.LoggingListener;
import dev.laarryy.Icicle.managers.LoggingListenerManager;
import dev.laarryy.Icicle.managers.PunishmentManagerManager;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AntiSpamListener {

    private final Logger logger = LogManager.getLogger(this);
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();
    LoadingCache<Long, Integer> history = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(5))
            .build(aLong -> 0);


    @EventListener
    public Mono<Void> on(MessageCreateEvent event) {

        if (event.getGuildId().isEmpty() || event.getMember().isEmpty() || event.getMember().get().isBot()) {
            logger.info("Guild or Member Empty");
            return Mono.empty();
        }

        if (!event.getMessage().getUserMentions().isEmpty() && event.getMessage().getUserMentions().size() > 5) {
            muteUserForSpam(event, "pings");
            return Mono.empty();
        }

        Member member = event.getMember().get();
        long userId = member.getId().asLong();

        history.get(userId);

        Integer histInt = history.get(userId) + 1;
        history.put(userId, histInt);

        if (histInt >= 7) {
            muteUserForSpam(event, "spam");
        } else if (histInt == 4) {
            warnUserForSpam(event);
        }

        return Mono.empty();
    }

    private void muteUserForSpam(MessageCreateEvent event, String reason) {
        Guild guild = event.getGuild().block();
        Member member = event.getMember().get();
        String punishmentMessage = switch (reason) {
          case "spam" -> "ANTI-SPAM: Muted for two hours for severe spam.";
          case "pings" -> "ANTI-SPAM: Muted for pinging more than five people.";
            default -> "ANTI-SPAM: Muted for severe spam.";
        };
        long userIdSnowflake = event.getMember().get().getId().asLong();
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", member.getId().asLong());
        DiscordUser bot = DiscordUser.findFirst("user_id_snowflake = ?", event.getClient().getSelfId().asLong());
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        punishmentManager.discordMuteUser(guild, userIdSnowflake, discordServerProperties);

        Punishment punishment = Punishment.create("user_id_punished", discordUser.getUserId(),
                "user_id_punisher", bot.getUserId(),
                "server_id", discordServer.getServerId(),
                "punishment_type", "mute",
                "punishment_date", Instant.now().toEpochMilli(),
                "punishment_end_date", Instant.now().plus(2, ChronoUnit.HOURS).toEpochMilli(),
                "punishment_message", punishmentMessage,
                "did_dm", false,
                "end_date_passed", false,
                "punishment_end_reason", "No reason provided.");
        punishment.save();
        punishment.refresh();

        event.getMessage().delete("ANTI-SPAM: Message was sent far too fast, user muted. Punishment ID: " + punishment.getPunishmentId()).block();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getMessageDelete() + " Muted for Spam")
                .description(punishmentMessage)
                .color(Color.JAZZBERRY_JAM)
                .footer("This incident has been recorded with case number " + punishment.getPunishmentId(), "")
                .build();

        event.getMessage().getChannel().block().createMessage(embed).block();

        loggingListener.onPunishment(event, punishment);
    }

    private void warnUserForSpam(MessageCreateEvent event) {
        Guild guild = event.getGuild().block();
        Member member = event.getMember().get();

        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", member.getId().asLong());
        DiscordUser bot = DiscordUser.findFirst("user_id_snowflake = ?", event.getClient().getSelfId().asLong());
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        Punishment punishment = Punishment.create("user_id_punished", discordUser.getUserId(),
                "user_id_punisher", bot.getUserId(),
                "server_id", discordServer.getServerId(),
                "punishment_type", "warn",
                "punishment_date", Instant.now().toEpochMilli(),
                "punishment_message", "ANTI-SPAM: Do not send messages so quickly.",
                "did_dm", false,
                "end_date_passed", true,
                "punishment_end_reason", "No reason provided.");
        punishment.save();
        punishment.refresh();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getMessageDelete() + " Warning: Do Not Spam")
                .description(event.getMember().get().getMention() + ", you have been warned for spam, please stop. Further spam will be punished more harshly.")
                .color(Color.JAZZBERRY_JAM)
                .footer("This incident has been recorded with case number " + punishment.getPunishmentId(), "")
                .build();

        event.getMessage().getChannel().block().createMessage(embed).block();

        loggingListener.onPunishment(event, punishment);
    }
}
