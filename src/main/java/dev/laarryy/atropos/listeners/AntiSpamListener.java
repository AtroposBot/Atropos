package dev.laarryy.atropos.listeners;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.commands.punishments.PunishmentManager;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.managers.PunishmentManagerManager;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.Pair;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AntiSpamListener {

    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();
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
    LoadingCache<Long, Integer> joinHistory = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .build(aLong -> 0);

    private static final Pattern URL = Pattern.compile("https?://[^\\s/$.?#].[^\\s]*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.UNICODE_CASE);
    private static final Pattern SCAM_URL = Pattern.compile("https?://(([^\\s/$.?#])*(?:(d([1li])(?:s+c?o+|c+s+o+))|(.*.c(o)*r([lio])*([debq]))|(.*?:([o0dc])([rjlc])d)|((?:s|5)(?:t|l)(e|3)(a|4)(?:m|rn))|(.*n([1ijl])tr([o0])(.*))|(.*n([i1l])+(?:tr|rt)([o0]).*)|(g([ilj1])([fv])([te])?|:g([fv])([ij1l])t))|(fre+)).*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.UNICODE_CASE | Pattern.DOTALL);
    private final List<String> officialLinkStrings = List.of(
            "dis.gd",
            "discord\\.co",
            "discord\\.com",
            "discord\\.design",
            "discord\\.dev",
            "discord\\.gg",
            "discord\\.gift",
            "discord\\.gifts",
            "discord\\.media",
            "discord\\.new",
            "discord\\.store",
            "discord\\.tools",
            "discordapp\\.com",
            "discordapp\\.net",
            "discordmerch\\.com",
            "discordpartygames\\.com",
            "discord\\-activities\\.com",
            "discordactivities\\.com",
            "discordsays\\.com",
            "discordstatus\\.com",
            "discordapp\\.io",
            "discord4j\\.com",
            "steamcommunity\\.com",
            "steamgames\\.com",
            "steampowered\\.com",
            "discordcdn\\.com",
            "steamdb\\.info",
            "steamdeck\\.com",
            "discohook\\.org"
    );

    private final List<Pattern> officialLinks = Flux.fromIterable(officialLinkStrings)
            .map(officialLinkString -> Pattern.compile("(.*\\.)?" + officialLinkString))
            .collectList()
            .block();

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {
        if (event.getMember().isBot()) {
            return Mono.empty();
        }

        long guildId = event.getGuildId().asLong();
        DiscordServerProperties properties = propertiesCache.get(guildId);
        Member member = event.getMember();

        long userId = member.getId().asLong();
        Long aLong = guildId;

        int joinsToAntiraid = properties.getJoinsToAntiraid();
        int joinInt = joinHistory.get(aLong);

        joinHistory.put(guildId, joinInt + 1);

        int histInt = joinHistory.get(guildId);

        if (joinsToAntiraid > 0 && histInt >= joinsToAntiraid) {
            enableAntiraid(event);
            return Mono.empty();
        }
        return Mono.empty();
    }

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

        if (properties.getAntiScam()) {
            try {
                String match = checkMessageForScam(event.getMessage().getContent());
                if (match != null) {
                    DatabaseLoader.openConnectionIfClosed();
                    muteUserForScam(event, match);
                    DatabaseLoader.closeConnectionIfOpen();
                }
            } catch (MalformedURLException ignored) {
            }
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

    private String checkMessageForScam(String content) throws MalformedURLException {
        Matcher urlMatcher = URL.matcher(content);
        String match = null;
        while (urlMatcher.find()) {
            URL url = new URL(urlMatcher.group());
            String rootHost = url.getHost();
            String domain = url.getProtocol() + "://" + rootHost + "/";
            Matcher matcher = SCAM_URL.matcher(domain);

            Boolean legitLink = Flux.just(officialLinks)
                    .any(link -> {
                        logger.info(link);
                        for (Pattern oneLink : link) {
                            if (oneLink.matcher(rootHost).matches()) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .block();

            if (legitLink != null && legitLink) {
                continue;
            }

            if (matcher.matches()) {
                match = matcher.group();
            }
            return match;
        }

        return null;
    }

    private void enableAntiraid(MemberJoinEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties properties = DiscordServerProperties.findFirst("server_id_snowflake = ?", event.getGuildId().asLong());
        properties.setStopJoins(true);
        properties.save();
        properties.refresh();
        loggingListener.onStopJoinsEnable(event.getGuild().block());
        propertiesCache.invalidate(event.getGuildId().asLong());

    }

    private void muteUserForScam(MessageCreateEvent event, String match) {
        DatabaseLoader.openConnectionIfClosed();

        Guild guild = event.getGuild().block();
        Member punishedMember = event.getMember().get();
        Member self = guild.getSelfMember().block();

        if (permissionChecker.checkIsAdministrator(guild, punishedMember)
                || !punishmentManager.onlyCheckIfPunisherHasHighestRole(self, punishedMember, guild)) {
            return;
        }

        String punishmentMessage = "ANTI-SCAM: Muted automatically for sending suspicious link: `" + match + "`. If you're not a bot, worry not - a moderator will review this action.";

        long userIdSnowflake = event.getMember().get().getId().asLong();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", punishedMember.getId().asLong());
        DiscordUser bot = DiscordUser.findFirst("user_id_snowflake = ?", event.getClient().getSelfId().asLong());
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        punishmentManager.discordMuteUser(guild, userIdSnowflake);

        DatabaseLoader.openConnectionIfClosed();
        Punishment punishment = Punishment.create(
                "user_id_punished", discordUser.getUserId(),
                "name_punished", punishedMember.getUsername(),
                "discrim_punished", punishedMember.getDiscriminator(),
                "user_id_punisher", bot.getUserId(),
                "name_punisher", self.getUsername(),
                "discrim_punisher", self.getDiscriminator(),
                "server_id", discordServer.getServerId(),
                "punishment_type", "mute",
                "punishment_date", Instant.now().toEpochMilli(),
                "punishment_message", punishmentMessage,
                "did_dm", false,
                "end_date_passed", false,
                "automatic", true,
                "permanent", true,
                "punishment_end_reason", "Punishment not ended.");
        punishment.save();
        punishment.refresh();

        event.getMessage().delete("ANTI-SCAM: Message contained suspicious link, user muted. Punishment ID: " + punishment.getPunishmentId()).block();

        Notifier.notifyPunished(guild, punishment, punishmentMessage);
        loggingListener.onPunishment(event, punishment);
        loggingListener.onScamMute(event, punishment);

        DatabaseLoader.closeConnectionIfOpen();
    }

    private void muteUserForSpam(MessageCreateEvent event) {

        Guild guild = event.getGuild().block();
        Member punishedMember = event.getMember().get();
        Member self = guild.getSelfMember().block();

        if (permissionChecker.checkIsAdministrator(guild, punishedMember)
                || !punishmentManager.onlyCheckIfPunisherHasHighestRole(self, punishedMember, guild)) {
            return;
        }

        String punishmentMessage = "ANTI-SPAM: Muted for two hours for severe spam.";

        long userIdSnowflake = event.getMember().get().getId().asLong();

        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", punishedMember.getId().asLong());
        DiscordUser bot = DiscordUser.findFirst("user_id_snowflake = ?", event.getClient().getSelfId().asLong());
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        punishmentManager.discordMuteUser(guild, userIdSnowflake);

        Punishment punishment = Punishment.create("user_id_punished", discordUser.getUserId(),
                "user_id_punished", discordUser.getUserId(),
                "name_punished", punishedMember.getUsername(),
                "discrim_punished", punishedMember.getDiscriminator(),
                "user_id_punisher", bot.getUserId(),
                "name_punisher", self.getUsername(),
                "discrim_punisher", self.getDiscriminator(),
                "server_id", discordServer.getServerId(),
                "punishment_date", Instant.now().toEpochMilli(),
                "punishment_end_date", Instant.now().plus(2, ChronoUnit.HOURS).toEpochMilli(),
                "punishment_message", punishmentMessage,
                "did_dm", false,
                "end_date_passed", false,
                "permanent", false,
                "automatic", true,
                "punishment_end_reason", "Punishment not ended.");
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
        DatabaseLoader.closeConnectionIfOpen();
    }

    private void warnUserForSpam(MessageCreateEvent event) {
        Guild guild = event.getGuild().block();
        Member punishedMember = event.getMember().get();
        Member self = guild.getSelfMember().block();

        if (permissionChecker.checkIsAdministrator(guild, punishedMember)
                || !punishmentManager.onlyCheckIfPunisherHasHighestRole(self, punishedMember, guild)) {
            return;
        }

        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", punishedMember.getId().asLong());
        DiscordUser bot = DiscordUser.findFirst("user_id_snowflake = ?", event.getClient().getSelfId().asLong());
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        String reason = "ANTI-SPAM: Do not send messages so quickly. Your most recent message `"
                + event.getMessage().getContent().replaceAll("`", "") +
                "` in the channel <#" + event.getMessage().getChannel().block().getId().asLong()
                + "> was sent too quickly after the messages preceding it.";

        if (reason.length() > 300) {
            reason = "ANTI-SPAM: Do not send messages so quickly. Your most recent message `[Too large to log]` " +
                    "in the channel <#" + event.getMessage().getChannel().block().getId().asLong()
                    + "> was sent too quickly after the messages preceding it.";
        }

        Punishment punishment = Punishment.create("user_id_punished", discordUser.getUserId(),
                "user_id_punished", discordUser.getUserId(),
                "name_punished", punishedMember.getUsername(),
                "discrim_punished", punishedMember.getDiscriminator(),
                "user_id_punisher", bot.getUserId(),
                "name_punisher", self.getUsername(),
                "discrim_punisher", self.getDiscriminator(),
                "server_id", discordServer.getServerId(),
                "punishment_type", "warn",
                "punishment_date", Instant.now().toEpochMilli(),
                "punishment_message", reason,
                "did_dm", false,
                "end_date_passed", false,
                "permanent", true,
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
        DatabaseLoader.closeConnectionIfOpen();
    }
}
