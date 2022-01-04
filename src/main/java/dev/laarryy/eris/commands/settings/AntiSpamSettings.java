package dev.laarryy.eris.commands.settings;

import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.Notifier;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class AntiSpamSettings {

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        if (event.getOption("antispam").get().getOption("info").isPresent()) {
            antiSpamInfo(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("antispam").get().getOption("set").isPresent()) {
            setAntiSpam(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        Notifier.notifyCommandUserOfError(event, "malformedInput");
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }

    private void setAntiSpam(ChatInputInteractionEvent event) {
        if (event.getOption("antispam").get().getOption("set").get().getOption("messages").isEmpty()
                && event.getOption("antispam").get().getOption("set").get().getOption("pings").isEmpty()
                && event.getOption("antispam").get().getOption("set").get().getOption("warns").isEmpty()
                && event.getOption("antispam").get().getOption("set").get().getOption("antiraid").isEmpty()
        ) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            return;
        }

        Guild guild = event.getInteraction().getGuild().block();

        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        if (event.getOption("antispam").get().getOption("set").get().getOption("messages").isPresent()
                && event.getOption("antispam").get().getOption("set").get().getOption("messages").get().getValue().isPresent()) {
            long messagesToWarn = event.getOption("antispam").get().getOption("set").get().getOption("messages").get().getValue().get().asLong();
            if (messagesToWarn < 0) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                return;
            }
            discordServerProperties.setMessagesToWarn((int) messagesToWarn);
        }

        if (event.getOption("antispam").get().getOption("set").get().getOption("pings").isPresent()
                && event.getOption("antispam").get().getOption("set").get().getOption("pings").get().getValue().isPresent()) {
            long pingsToWarn = event.getOption("antispam").get().getOption("set").get().getOption("pings").get().getValue().get().asLong();
            if (pingsToWarn < 0) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                return;
            }
            discordServerProperties.setPingsToWarn((int) pingsToWarn);
        }

        if (event.getOption("antispam").get().getOption("set").get().getOption("warns").isPresent()
                && event.getOption("antispam").get().getOption("set").get().getOption("warns").get().getValue().isPresent()) {
            long warnsToMute = event.getOption("antispam").get().getOption("set").get().getOption("warns").get().getValue().get().asLong();
            if (warnsToMute < 0) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                return;
            }
            discordServerProperties.setWarnsToMute((int) warnsToMute);
        }

        if (event.getOption("antispam").get().getOption("set").get().getOption("antiraid").isPresent()
                && event.getOption("antispam").get().getOption("set").get().getOption("antiraid").get().getValue().isPresent()) {
            long joinsToAntiraid = event.getOption("antispam").get().getOption("set").get().getOption("antiraid").get().getValue().get().asLong();
            if (joinsToAntiraid < 0) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                return;
            }
            discordServerProperties.setJoinsToAntiraid((int) joinsToAntiraid);
        }

        discordServerProperties.save();
        discordServerProperties.refresh();

        int messagesToWarn = discordServerProperties.getMessagesToWarn();
        int pingsToWarn = discordServerProperties.getPingsToWarn();
        int warnsToMute = discordServerProperties.getWarnsToMute();
        int joinsToAntiraid = discordServerProperties.getJoinsToAntiraid();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Success")
                .color(Color.SEA_GREEN)
                .description("Set anti-spam values successfully. Current values are:\n" +
                        "Messages to Warn: `" + messagesToWarn + "`\n" +
                        "Pings to Warn: `" + pingsToWarn + "`\n" +
                        "Warns to Mute: `" + warnsToMute + "`" +
                        "Joins to Antiraid: `" + joinsToAntiraid + "`")
                .footer("For more information, run /settings antispam info", "")
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).block();
    }

    private void antiSpamInfo(ChatInputInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        Guild guild = event.getInteraction().getGuild().block();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        int messagesToWarn = discordServerProperties.getMessagesToWarn();
        int pingsToWarn = discordServerProperties.getPingsToWarn();
        int warnsToMute = discordServerProperties.getWarnsToMute();
        int joinsToAntiRaid = discordServerProperties.getJoinsToAntiraid();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Anti-Spam Settings")
                .color(Color.ENDEAVOUR)
                .description("The following are the current settings for your anti-spam. To effectively disable any of these, set their values to 0.")
                .addField("Messages to Warn: " + messagesToWarn, "If a player sends `" + messagesToWarn + "` messages within 6 seconds, they will be warned for spam.", false)
                .addField("Pings to Warn: " + pingsToWarn, "If a player sends `" + pingsToWarn + "` pings (mentions) within 6 seconds, they will be warned for spam.", false)
                .addField("Warns to Mute: " + warnsToMute, "If a player is warned for spamming `" + warnsToMute + "` times within 10 minutes, they will be muted for 2 hours.", false)
                .addField("Joins to Antiraid: " + joinsToAntiRaid, "If `" + joinsToAntiRaid + "` users join within 30 seconds, the `/stopjoins` anti-raid system will be automatically enabled.", false)
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).block();
    }
}
