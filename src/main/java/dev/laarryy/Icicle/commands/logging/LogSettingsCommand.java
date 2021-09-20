package dev.laarryy.Icicle.commands.logging;

import dev.laarryy.Icicle.managers.CacheManager;
import dev.laarryy.Icicle.Icicle;
import dev.laarryy.Icicle.commands.Command;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.AuditLogger;
import dev.laarryy.Icicle.utils.Notifier;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public class LogSettingsCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);

    List<ApplicationCommandOptionChoiceData> optionChoiceDataList = List.of(
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Guild")
                    .value("guild")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Message")
                    .value("message")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Member")
                    .value("member")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Punishment")
                    .value("punishment")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("All")
                    .value("all")
                    .build());

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("logsettings")
            .description("Modify logging settings.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("set")
                    .description("Sets this channel as a logging channel.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("type")
                            .description("Logging type to send to this channel. Run /logsettings info to learn more.")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .choices(optionChoiceDataList)
                            .required(true)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("unset")
                    .description("Unsets this channel as a logging channel.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("type")
                            .description("Logging type to stop sending to this channel. Run /logsettings info to learn more.")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .choices(optionChoiceDataList)
                            .required(true)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("info")
                    .description("Info on logging types and what they log.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        if (event.getOption("info").isPresent()) {
            logSettingsInfo(event);
            return Mono.empty();
        }

        if (event.getOption("set").isPresent()) {
            setLogChannel(event);
            return Mono.empty();
        }

        if (event.getOption("unset").isPresent()) {
            unsetLogChannel(event);
            return Mono.empty();
        }

        AuditLogger.addCommandToDB(event, false);
        Notifier.notifyCommandUserOfError(event, "malformedInput");
        return Mono.empty();
    }

    private void unsetLogChannel(SlashCommandEvent event) {
        if (event.getOption("unset").get().getOption("type").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
        }

        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        DatabaseLoader.openConnectionIfClosed();

        CacheManager.getManager().getCache().invalidate(guild.getId().asLong());

        String logType = event.getOption("unset").get().getOption("type").get().getValue().get().asString();

        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        MessageChannel channel = event.getInteraction().getChannel().block();

        if (!(channel instanceof TextChannel textChannel)) {
            Notifier.notifyCommandUserOfError(event, "invalidChannel");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        if (serverProperties == null) {
            Icicle.addServerToDatabase(guild);
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        switch (logType) {
            case "message" -> serverProperties.setMessageLogChannelSnowflake(null);
            case "member" -> serverProperties.setMemberLogChannelSnowflake(null);
            case "guild" -> serverProperties.setGuildLogChannelSnowflake(null);
            case "punishment" -> serverProperties.setPunishmentLogChannelSnowflake(null);
            case "all" -> {
                serverProperties.setMessageLogChannelSnowflake(null);
                serverProperties.setMemberLogChannelSnowflake(null);
                serverProperties.setGuildLogChannelSnowflake(null);
                serverProperties.setPunishmentLogChannelSnowflake(null);
            }
            default -> {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                AuditLogger.addCommandToDB(event, false);
                return;
            }
        }
        serverProperties.save();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Unset Log Channel Successfully")
                .color(Color.ENDEAVOUR)
                .description("Unset this channel as a logging channel of type: `" + logType + "`. Run `/logsettings info` " +
                        "to learn more and `/logsettings set " + logType + "` in this channel to undo.")
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).withEphemeral(true).subscribe();
        AuditLogger.addCommandToDB(event, true);
    }

    private void setLogChannel(SlashCommandEvent event) {
        if (event.getOption("set").get().getOption("type").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        DatabaseLoader.openConnectionIfClosed();

        String logType = event.getOption("set").get().getOption("type").get().getValue().get().asString();

        CacheManager.getManager().getCache().invalidate(guild.getId().asLong());

        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        if (serverProperties == null) {
            Icicle.addServerToDatabase(guild);
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        MessageChannel channel = event.getInteraction().getChannel().block();

        if (!(channel instanceof TextChannel textChannel)) {
            Notifier.notifyCommandUserOfError(event, "invalidChannel");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        Long targetChannelId = textChannel.getId().asLong();

        switch (logType) {
            case "message" -> serverProperties.setMessageLogChannelSnowflake(targetChannelId);
            case "member" -> serverProperties.setMemberLogChannelSnowflake(targetChannelId);
            case "guild" -> serverProperties.setGuildLogChannelSnowflake(targetChannelId);
            case "punishment" -> serverProperties.setPunishmentLogChannelSnowflake(targetChannelId);
            case "all" -> {
                serverProperties.setMessageLogChannelSnowflake(targetChannelId);
                serverProperties.setMemberLogChannelSnowflake(targetChannelId);
                serverProperties.setGuildLogChannelSnowflake(targetChannelId);
                serverProperties.setPunishmentLogChannelSnowflake(targetChannelId);
            }
            default -> {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                AuditLogger.addCommandToDB(event, false);
                return;
            }
        }
        serverProperties.save();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Set Log Channel Successfully")
                .color(Color.ENDEAVOUR)
                .description("Set this channel to be a logging channel of type: `" + logType + "`. Run `/logsettings info` " +
                        "to learn more and `/logsettings unset " + logType + "` in this channel to undo.")
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).withEphemeral(true).subscribe();
        AuditLogger.addCommandToDB(event, true);

    }

    private void logSettingsInfo(SlashCommandEvent event) {

        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Log Settings")
                .color(Color.ENDEAVOUR)
                .description("There are four log types that you can configure: `Member`, `Message`, `Guild`, and `Punishment`. " +
                        "Each type can be logged to whichever channel you choose; there is no need to keep them separate nor together. " +
                        "Below are the four types explained along with what they keep track of.")
                .addField("Member Log",
                        "This type logs updates on members of the server. Presence and status updates as well as nickname changes.",
                        false)
                .addField("Message Log",
                        "This type logs message updates: deletes and edits.",
                        false)
                .addField("Guild Log",
                        "This type logs guild-sided updates. Role changes, joins, leaves, bans, and invite creations. " +
                                "*Note: this logs only what discord's API provides for bans. For detailed information, use punishment logs.*",
                        false)
                .addField("Punishment Log",
                        "This type logs punishments through this bot. Bans, warns, mutes, kicks, and cases. " +
                                "It will also log unmutes and unbans through the bot.",
                        false)
                .addField("Command Use", "Run `/logsettings set <type>` in the channel you'd like to set as a logging channel. " +
                        "Run `/logsettings unset <type>` in the channel you'd like to unset as a logging channel.",
                        false)
                .build();

        event.reply().withEmbeds(embed).withEphemeral(true).subscribe();

    }
}
