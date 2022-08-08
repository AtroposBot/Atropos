package dev.laarryy.atropos.commands.settings;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.CommandChecks;
import dev.laarryy.atropos.utils.Notifier;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class AntiSpamSettings {

    LoadingCache<Long, DiscordServerProperties> propertiesCache = PropertiesCacheManager.getManager().getPropertiesCache();

    public Mono<Void> execute(ChatInputInteractionEvent event) {
        return Mono.defer(() -> {
            if (event.getOption("antispam").get().getOption("info").isPresent()) {
                return antiSpamInfo(event);
            }

            if (event.getOption("antispam").get().getOption("set").isPresent()) {
                return setAntiSpam(event);
            }

            return Mono.error(new MalformedInputException("Malformed Input"));
        });
    }

    private Mono<Void> setAntiSpam(ChatInputInteractionEvent event) {
        return Mono.defer(() -> {
            if (event.getOption("antispam").get().getOption("set").get().getOption("messages").isEmpty()
                    && event.getOption("antispam").get().getOption("set").get().getOption("pings").isEmpty()
                    && event.getOption("antispam").get().getOption("set").get().getOption("warns").isEmpty()
                    && event.getOption("antispam").get().getOption("set").get().getOption("antiraid").isEmpty()
                    && event.getOption("antispam").get().getOption("set").get().getOption("antiscam").isEmpty()
                    && event.getOption("antispam").get().getOption("set").get().getOption("dehoist").isEmpty()

            ) {
                return Mono.error(new MalformedInputException("Malformed Input"));
            }

            return event.getInteraction().getGuild().flatMap(guild -> DatabaseLoader.use(() -> {
                DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
                if (event.getOption("antispam").get().getOption("set").get().getOption("messages").isPresent()
                        && event.getOption("antispam").get().getOption("set").get().getOption("messages").get().getValue().isPresent()) {
                    long messagesToWarn = event.getOption("antispam").get().getOption("set").get().getOption("messages").get().getValue().get().asLong();
                    if (messagesToWarn < 0) {
                        return Mono.error(new MalformedInputException("Malformed Input"));
                    }
                    discordServerProperties.setMessagesToWarn((int) messagesToWarn);
                }

                if (event.getOption("antispam").get().getOption("set").get().getOption("pings").isPresent()
                        && event.getOption("antispam").get().getOption("set").get().getOption("pings").get().getValue().isPresent()) {
                    long pingsToWarn = event.getOption("antispam").get().getOption("set").get().getOption("pings").get().getValue().get().asLong();
                    if (pingsToWarn < 0) {
                        return Mono.error(new MalformedInputException("Malformed Input"));
                    }
                    discordServerProperties.setPingsToWarn((int) pingsToWarn);
                }

                if (event.getOption("antispam").get().getOption("set").get().getOption("warns").isPresent()
                        && event.getOption("antispam").get().getOption("set").get().getOption("warns").get().getValue().isPresent()) {
                    long warnsToMute = event.getOption("antispam").get().getOption("set").get().getOption("warns").get().getValue().get().asLong();
                    if (warnsToMute < 0) {
                        return Mono.error(new MalformedInputException("Malformed Input"));
                    }
                    discordServerProperties.setWarnsToMute((int) warnsToMute);
                }

                if (event.getOption("antispam").get().getOption("set").get().getOption("antiraid").isPresent()
                        && event.getOption("antispam").get().getOption("set").get().getOption("antiraid").get().getValue().isPresent()) {
                    long joinsToAntiraid = event.getOption("antispam").get().getOption("set").get().getOption("antiraid").get().getValue().get().asLong();
                    if (joinsToAntiraid < 0) {
                        return Mono.error(new MalformedInputException("Malformed Input"));
                    }
                    discordServerProperties.setJoinsToAntiraid((int) joinsToAntiraid);
                }

                if (event.getOption("antispam").get().getOption("set").get().getOption("antiscam").isPresent()
                        && event.getOption("antispam").get().getOption("set").get().getOption("antiscam").get().getValue().isPresent()) {
                    boolean antiScam = event.getOption("antispam").get().getOption("set").get().getOption("antiscam").get().getValue().get().asBoolean();
                    discordServerProperties.setAntiScam(antiScam);
                }

                if (event.getOption("antispam").get().getOption("set").get().getOption("dehoist").isPresent()
                        && event.getOption("antispam").get().getOption("set").get().getOption("dehoist").get().getValue().isPresent()) {
                    boolean dehoist = event.getOption("antispam").get().getOption("set").get().getOption("dehoist").get().getValue().get().asBoolean();
                    discordServerProperties.setDehoist(dehoist);
                }

                discordServerProperties.save();
                discordServerProperties.refresh();

                propertiesCache.invalidate(guild.getId().asLong());

                int messagesToWarn = discordServerProperties.getMessagesToWarn();
                int pingsToWarn = discordServerProperties.getPingsToWarn();
                int warnsToMute = discordServerProperties.getWarnsToMute();
                int joinsToAntiraid = discordServerProperties.getJoinsToAntiraid();
                boolean antiScam = discordServerProperties.getAntiScam();
                boolean dehoist = discordServerProperties.getDehoist();

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .title("Success")
                        .color(Color.SEA_GREEN)
                        .description("Set anti-spam values successfully. Current values are:\n" +
                                "Messages to Warn: `" + messagesToWarn + "`\n" +
                                "Pings to Warn: `" + pingsToWarn + "`\n" +
                                "Warns to Mute: `" + warnsToMute + "`\n" +
                                "Joins to Antiraid: `" + joinsToAntiraid + "`\n" +
                                "Anti-Scam Enabled: `" + String.valueOf(antiScam).toLowerCase() + "`\n" +
                                "De-Hoist Enabled: `" + String.valueOf(dehoist).toLowerCase() + "`")
                        .footer("For more information, run /settings antispam info", "")
                        .timestamp(Instant.now())
                        .build();

                return Notifier.sendResultsEmbed(event, embed);
            }));
        });
    }

    private Mono<Void> antiSpamInfo(ChatInputInteractionEvent event) {

        return event.getInteraction().getGuild().flatMap(guild -> {
            int messagesToWarn;
            int pingsToWarn;
            int warnsToMute;
            int joinsToAntiRaid;
            String antiScam;
            String dehoist;
            try (final var usage = DatabaseLoader.use()) {
                DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
                messagesToWarn = discordServerProperties.getMessagesToWarn();
                pingsToWarn = discordServerProperties.getPingsToWarn();
                warnsToMute = discordServerProperties.getWarnsToMute();
                joinsToAntiRaid = discordServerProperties.getJoinsToAntiraid();
                antiScam = String.valueOf(discordServerProperties.getAntiScam()).toLowerCase();
                dehoist = String.valueOf(discordServerProperties.getDehoist()).toLowerCase();
            }

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Anti-Spam Settings")
                    .color(Color.ENDEAVOUR)
                    .description("The following are the current settings for your anti-spam. To effectively disable any of these, set their values to 0.")
                    .addField("Messages to Warn: " + messagesToWarn, "If a player sends `" + messagesToWarn + "` messages within 6 seconds, they will be warned for spam.", false)
                    .addField("Pings to Warn: " + pingsToWarn, "If a player sends `" + pingsToWarn + "` pings (mentions) within 6 seconds, they will be warned for spam.", false)
                    .addField("Warns to Mute: " + warnsToMute, "If a player is warned for spamming `" + warnsToMute + "` times within 10 minutes, they will be muted for 2 hours.", false)
                    .addField("Joins to Antiraid: " + joinsToAntiRaid, "If `" + joinsToAntiRaid + "` users join within 30 seconds, the `/stopjoins` anti-raid system will be automatically enabled.", false)
                    .addField("Anti-Scam Enabled: " + antiScam, "If enabled, users that send known or likely malicious or scam links will be muted pending manual review.", false)
                    .addField("De-Hoist Enabled: " + dehoist, "If enabled, users that join, update their nicknames, or change usernames will have any hoisting characters removed.", false)
                    .timestamp(Instant.now())
                    .build();

            return Notifier.sendResultsEmbed(event, embed);
        });
    }
}
