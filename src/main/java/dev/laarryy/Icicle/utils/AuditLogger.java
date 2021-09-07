package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.commands.punishments.ManualPunishmentEnder;
import dev.laarryy.Icicle.models.guilds.CommandUse;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Instant;

public final class AuditLogger {
    private static final Logger logger = LogManager.getLogger(ManualPunishmentEnder.class);

    private AuditLogger() {}

    public static void addCommandToDB(SlashCommandEvent event, boolean success) {
        DatabaseLoader.openConnectionIfClosed();
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
        stringBuffer.append(event.getCommandName() + " ");

        Flux.fromIterable(event.getOptions())
                .subscribe(option -> stringBuffer.append(generateOptionString(option)));

        String commandContent = stringBuffer.toString();

        CommandUse commandUse = CommandUse.findOrCreateIt("server_id", serverId, "command_user_id", commandUserId, "command_contents", commandContent, "date", Instant.now().toEpochMilli(), "success", success);
        commandUse.save();

    }

    private static String generateOptionString(ApplicationCommandInteractionOption option) {
        StringBuffer sb = new StringBuffer();
        if (option.getType().name().equals("SUB_COMMAND_GROUP") || option.getType().name().equals("SUB_COMMAND")) {

            option.getOptions().forEach(op -> {
                sb.append(op.getName() + " " + generateOptionString(op));
            });
        } else {
            String valuedOption = option.getName() + ":" + stringifyOptionValue(option) + " ";
            sb.append(valuedOption);
        }
        return sb.toString();
    }

    private static String stringifyOptionValue(ApplicationCommandInteractionOption option) {

        if (option.getValue().isEmpty()) {
            return "";
        }

        switch (option.getType().name()) {
            case "USER": return option.getValue().get().asUser().block().getId().asString();
            case "STRING": return option.getValue().get().asString();
            case "INTEGER": return String.valueOf(option.getValue().get().asLong());
            case "BOOLEAN": return String.valueOf(option.getValue().get().asBoolean());
            case "CHANNEL": return option.getValue().get().asChannel().block().getId().asString();
            case "ROLE": return option.getValue().get().asRole().block().getId().asString();
            case "MENTIONABLE": return option.getValue().get().toString();
            default: return option.getName();
        }
    }
}
