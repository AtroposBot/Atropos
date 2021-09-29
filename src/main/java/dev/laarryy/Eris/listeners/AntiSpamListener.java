package dev.laarryy.Eris.listeners;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Eris.commands.punishments.PunishmentManager;
import dev.laarryy.Eris.config.EmojiManager;
import dev.laarryy.Eris.listeners.logging.LoggingListener;
import dev.laarryy.Eris.managers.LoggingListenerManager;
import dev.laarryy.Eris.managers.PropertiesCacheManager;
import dev.laarryy.Eris.managers.PunishmentManagerManager;
import dev.laarryy.Eris.models.guilds.DiscordServer;
import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.models.users.DiscordUser;
import dev.laarryy.Eris.models.users.Punishment;
import dev.laarryy.Eris.storage.DatabaseLoader;
import dev.laarryy.Eris.utils.Pair;
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

public class AntiSpamListener {

    private final Logger logger = LogManager.getLogger(this);
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    LoadingCache<Long, DiscordServerProperties> propertiesCache = PropertiesCacheManager.getManager().getPropertiesCache();
    PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();
    LoadingCache<Pair<Long, Long>, Integer> messageHistory = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(6))
            .build(aLong -> 0);
    LoadingCache<Pair<Long, Long>, Integer> pingHistory = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(6))
            .build(aLong -> 0);
    LoadingCache<Pair<Long, Long>, Integer> warnHistory = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .build(aLong -> 0);


    @EventListener
    public Mono<Void> on(MessageCreateEvent event) {

        if (event.getGuildId().isEmpty() || event.getMember().isEmpty() || event.getMember().get().isBot()) {
            return Mono.empty();
        }

        long guildId = event.getGuildId().get().asLong();

        DiscordServerProperties properties = propertiesCache.get(guildId);

        Member member = event.getMember().get();
        long userId = member.getId().asLong();
        Pair<Long, Long> pair = new Pair<>(userId, guildId);

        int messagesToWarn = properties.getMessagesToWarn();
        int pingsToWarn = properties.getPingsToWarn();
        int warnsToMute = properties.getWarnsToMute();

        int initialInt = messageHistory.get(pair);
        int pingInt = pingHistory.get(pair);
        int warnInt = warnHistory.get(pair);

        messageHistory.put(pair, initialInt + 1);

        int histInt = messageHistory.get(pair);

        if (!event.getMessage().getUserMentions().isEmpty()) {
            int userMentions = event.getMessage().getUserMentions().size();
            pingHistory.put(pair, pingInt + userMentions);
        }

        if (warnsToMute > 0 && warnInt >= warnsToMute) {
            muteUserForSpam(event);
            warnHistory.invalidate(pair);
            return Mono.empty();
        }

        if (pingsToWarn > 0 && pingInt == pingsToWarn) {
            warnUserForSpam(event);
            warnHistory.put(pair, warnInt + 1);
            return Mono.empty();
        }

        if (messagesToWarn > 0 && histInt == messagesToWarn) {
            warnUserForSpam(event);
            warnHistory.put(pair, warnInt + 1);
            return Mono.empty();
        }

        return Mono.empty();
    }

    private void muteUserForSpam(MessageCreateEvent event) {
        Guild guild = event.getGuild().block();
        Member member = event.getMember().get();
        String punishmentMessage = "ANTI-SPAM: Muted for two hours for severe spam.";
        long userIdSnowflake = event.getMember().get().getId().asLong();
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", member.getId().asLong());
        DiscordUser bot = DiscordUser.findFirst("user_id_snowflake = ?", event.getClient().getSelfId().asLong());
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        punishmentManager.discordMuteUser(guild, userIdSnowflake);

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
