package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.commands.punishments.ManualPunishmentEnder;
import dev.laarryy.atropos.models.guilds.CommandUse;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Instant;

public final class AuditLogger {
    private static final Logger logger = LogManager.getLogger(ManualPunishmentEnder.class);

    private AuditLogger() {}

    public static void addCommandToDB(ChatInputInteractionEvent event, boolean success) {
        DatabaseLoader.openConnectionIfClosed();

        if (event.getInteraction().getGuildId().isEmpty()) {
            return;
        }

        DiscordServer server = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());

        if (server == null) {
            return;
        }

        int serverId = server.getServerId();

        DiscordUser user = DiscordUser.findFirst("user_id_snowflake = ?", event.getInteraction().getUser().getId().asLong());

        if (user == null) {
            return;
        }
        int commandUserId = user.getUserId();

        StringBuffer stringBuffer = new StringBuffer();
        StringBuilder sb = new StringBuilder();

        stringBuffer.append(event.getCommandName());

        Flux.fromIterable(event.getOptions())
                .map(option -> stringBuffer.append(generateOptionString(option, sb)))
                .doOnComplete(() -> {
                    String commandContent = stringBuffer.toString();
                    CommandUse commandUse = CommandUse.findOrCreateIt("server_id", serverId, "command_user_id", commandUserId, "command_contents", commandContent, "date", Instant.now().toEpochMilli(), "success", success);
                    commandUse.save();
                })
                .subscribe();
    }

    public static void addCommandToDB(ButtonInteractionEvent event, String entry, boolean success) {
        DatabaseLoader.openConnectionIfClosed();

        if (event.getInteraction().getGuildId().isEmpty()) {
            return;
        }

        DiscordServer server = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());

        if (server == null) {
            return;
        }

        int serverId = server.getServerId();

        DiscordUser user = DiscordUser.findFirst("user_id_snowflake = ?", event.getInteraction().getUser().getId().asLong());

        if (user == null) {
            return;
        }
        int commandUserId = user.getUserId();

        CommandUse commandUse = CommandUse.findOrCreateIt("server_id", serverId, "command_user_id", commandUserId, "command_contents", entry, "date", Instant.now().toEpochMilli(), "success", success);
        commandUse.save();
    }

    public static String generateOptionString(ApplicationCommandInteractionOption option) {
        return generateOptionString(option, new StringBuilder());
    }

    public static String generateOptionString(ApplicationCommandInteractionOption option, StringBuilder sb) {

        if (option.getValue().isEmpty()) {
            sb.append(" ").append(option.getName());
        } else {
            sb.append(" ").append(option.getName()).append(":").append(stringifyOptionValue(option));
        }

        if (!option.getOptions().isEmpty()) {
            for (ApplicationCommandInteractionOption opt : option.getOptions()) {
                generateOptionString(opt, sb);
            }
        }
        return sb.toString();
    }

    private static String stringifyOptionValue(ApplicationCommandInteractionOption option) {

        if (option.getValue().isEmpty()) {
            return "";
        }

        return switch (option.getType().name()) {
            case "USER" -> option.getValue().get().asUser().block().getId().asString();
            case "STRING" -> option.getValue().get().asString();
            case "INTEGER" -> String.valueOf(option.getValue().get().asLong());
            case "BOOLEAN" -> String.valueOf(option.getValue().get().asBoolean());
            case "CHANNEL" -> option.getValue().get().asChannel().block().getId().asString();
            case "ROLE" -> option.getValue().get().asRole().block().getId().asString();
            case "MENTIONABLE" -> option.getValue().get().toString();
            default -> option.getName();
        };
    }
}
