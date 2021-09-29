package dev.laarryy.Eris.commands.settings;

import dev.laarryy.Eris.commands.Command;
import dev.laarryy.Eris.models.guilds.DiscordServer;
import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.storage.DatabaseLoader;
import dev.laarryy.Eris.utils.Notifier;
import dev.laarryy.Eris.utils.SlashCommandChecks;
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

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("spamsettings")
            .description("Modify and view anti-spam settings")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("set")
                    .description("Set anti-spam thresholds. Must input one or more of the following sub-options")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("messages")
                            .description("How many messages sent within 6 seconds will cause the user to be warned?")
                            .type(ApplicationCommandOption.Type.INTEGER.getValue())
                            .required(false)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("pings")
                            .description("How many pings sent within 6 seconds will cause the user to be warned?")
                            .type(ApplicationCommandOption.Type.INTEGER.getValue())
                            .required(false)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("warns")
                            .description("How many anti-spam warnings within 10 minutes before user is muted for 2 hours?")
                            .type(ApplicationCommandOption.Type.INTEGER.getValue())
                            .required(false)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("info")
                    .description("Display current anti-spam settings")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        if (event.getOption("antispam").get().getOption("info").isPresent()) {
            antiSpamInfo(event);
            return Mono.empty();
        }

        if (event.getOption("antispam").get().getOption("set").isPresent()) {
            setAntiSpam(event);
            return Mono.empty();
        }

        Notifier.notifyCommandUserOfError(event, "malformedInput");
        return Mono.empty();
    }

    private void setAntiSpam(ChatInputInteractionEvent event) {
        if (event.getOption("set").get().getOption("messages").isEmpty()
                && event.getOption("set").get().getOption("pings").isEmpty()
                && event.getOption("set").get().getOption("warns").isEmpty()
        ) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            return;
        }

        Guild guild = event.getInteraction().getGuild().block();

        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        if (event.getOption("set").get().getOption("messages").isPresent()
                && event.getOption("set").get().getOption("messages").get().getValue().isPresent()) {
            long messagesToWarn = event.getOption("set").get().getOption("messages").get().getValue().get().asLong();
            if (messagesToWarn < 0) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                return;
            }
            discordServerProperties.setMessagesToWarn((int) messagesToWarn);
        }

        if (event.getOption("set").get().getOption("pings").isPresent()
                && event.getOption("set").get().getOption("pings").get().getValue().isPresent()) {
            long pingsToWarn = event.getOption("set").get().getOption("pings").get().getValue().get().asLong();
            if (pingsToWarn < 0) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                return;
            }
            discordServerProperties.setPingsToWarn((int) pingsToWarn);
        }

        if (event.getOption("set").get().getOption("warns").isPresent()
                && event.getOption("set").get().getOption("warns").get().getValue().isPresent()) {
            long warnsToMute = event.getOption("set").get().getOption("warns").get().getValue().get().asLong();
            if (warnsToMute < 0) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                return;
            }
            discordServerProperties.setWarnsToMute((int) warnsToMute);
        }

        discordServerProperties.save();
        discordServerProperties.refresh();

        int messagesToWarn = discordServerProperties.getMessagesToWarn();
        int pingsToWarn = discordServerProperties.getPingsToWarn();
        int warnsToMute = discordServerProperties.getWarnsToMute();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Success")
                .color(Color.SEA_GREEN)
                .description("Set anti-spam values successfully. Current values are:\n" +
                        "Messages to Warn: `" + messagesToWarn + "`\n" +
                        "Pings to Warn: `" + pingsToWarn + "`\n" +
                        "Warns to Mute: `" + warnsToMute + "`")
                .footer("For more information, run /spamsettings info", "")
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

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Anti-Spam Settings")
                .color(Color.ENDEAVOUR)
                .description("The following are the current settings for your anti-spam. To effectively disable any of these, set their values to 0.")
                .addField("Messages to Warn: " + messagesToWarn, "If a player sends `" + messagesToWarn + "` messages within 6 seconds, they will be warned for spam.", false)
                .addField("Pings to Warn: " + pingsToWarn, "If a player sends `" + pingsToWarn + "` pings (mentions) within 6 seconds, they will be warned for spam.", false)
                .addField("Warns to Mute: " + warnsToMute, "If a player is warned for spamming `" + warnsToMute + "` times within 10 minutes, they will be muted for 2 hours.", false)
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).block();
    }
}
