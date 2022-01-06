package dev.laarryy.eris.commands.settings;

import dev.laarryy.eris.commands.Command;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.Notifier;
import dev.laarryy.eris.utils.SlashCommandChecks;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public class SettingsCommand implements Command {

    private final AntiSpamSettings antiSpamSettings = new AntiSpamSettings();
    private final BlacklistSettings blacklistSettings = new BlacklistSettings();
    private final LogSettings logSettings = new LogSettings();
    private final ModMailSettings modMailSettings = new ModMailSettings();

    List<ApplicationCommandOptionChoiceData> blacklistTypes = List.of(
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("File (jar, exe, etc.)")
                    .value("file")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("String (regex)")
                    .value("string")
                    .build());

    List<ApplicationCommandOptionChoiceData> blacklistActions = List.of(
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Delete Message, ban user, log to punishments channel")
                    .value("ban")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Delete Message, mute for 2 hours, log to punishments channel")
                    .value("mute")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Delete Message, warn user, log to punishments channel")
                    .value("warn")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Delete Message, create a case, log to punishments channel")
                    .value("delete")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Create a case, log to punishments channel")
                    .value("notify")
                    .build());

    List<ApplicationCommandOptionChoiceData> logChannelTypes = List.of(
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
            .name("settings")
            .description("Modify and view various settings")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("antispam")
                    .description("Manage anti-spam settings")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
                    .required(false)
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
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("antiraid")
                                    .description("How many joins within 30 seconds will cause anti-raid to be enabled?")
                                    .type(ApplicationCommandOption.Type.INTEGER.getValue())
                                    .required(false)
                                    .build())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("antiscam")
                                    .description("Should this bot watch for and auto-mute users who send known scam links?")
                                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                                    .required(false)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("info")
                            .description("Display current anti-spam settings")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("blacklist")
                    .description("Manage blacklist settings")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("add")
                            .description("Add a blacklist entry")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("type")
                                    .description("Type of entry")
                                    .type(ApplicationCommandOption.Type.STRING.getValue())
                                    .choices(blacklistTypes)
                                    .required(true)
                                    .build())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("entry")
                                    .description("Entry to add to the blacklist")
                                    .type(ApplicationCommandOption.Type.STRING.getValue())
                                    .required(true)
                                    .build())
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("action")
                                    .description("What to do when blacklist is triggered")
                                    .type(ApplicationCommandOption.Type.STRING.getValue())
                                    .choices(blacklistActions)
                                    .required(true)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("remove")
                            .description("Remove a blacklist entry")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("id")
                                    .description("ID of blacklist entry to remove")
                                    .type(ApplicationCommandOption.Type.INTEGER.getValue())
                                    .required(true)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("list")
                            .description("List blacklist entries for this guild")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("info")
                            .description("Display info for a blacklist entry")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("id")
                                    .description("ID of blacklist entry to show info for")
                                    .type(ApplicationCommandOption.Type.INTEGER.getValue())
                                    .required(true)
                                    .build())
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("log")
                    .description("Manage logging settings")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("set")
                            .description("Sets this channel as a logging channel.")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("type")
                                    .description("Logging type to send to this channel. Run /logsettings info to learn more.")
                                    .type(ApplicationCommandOption.Type.STRING.getValue())
                                    .choices(logChannelTypes)
                                    .required(true)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("unset")
                            .description("Unsets this channel as a logging channel.")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("type")
                                    .description("Logging type to stop sending to this channel. Run /logsettings info to learn more.")
                                    .type(ApplicationCommandOption.Type.STRING.getValue())
                                    .choices(logChannelTypes)
                                    .required(true)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("info")
                            .description("Info on logging types and what they log.")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("modmail")
                    .description("Manage ModMail settings")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("set")
                            .description("Sets this channel as ModMail destination channel")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("unset")
                            .description("Unsets ModMail destination channel")
                            .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                            .required(false)
                            .build())
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        if (event.getOption("antispam").isPresent()) {
            if (!SlashCommandChecks.slashCommandChecks(event, "antispamsettings")) {
                return Mono.empty();
            }
            antiSpamSettings.execute(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("blacklist").isPresent()) {
            if (!SlashCommandChecks.slashCommandChecks(event, "blacklistsettings")) {
                return Mono.empty();
            }
            blacklistSettings.execute(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("log").isPresent()) {
            if (!SlashCommandChecks.slashCommandChecks(event, "logsettings")) {
                return Mono.empty();
            }
            logSettings.execute(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("modmail").isPresent()) {
            if (!SlashCommandChecks.slashCommandChecks(event, "modmailsettings")) {
                return Mono.empty();
            }
            modMailSettings.execute(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        Notifier.notifyCommandUserOfError(event, "malformedInput");
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
