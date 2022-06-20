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
import reactor.core.publisher.Mono;

import java.time.Instant;

public final class AuditLogger {
    private static final Logger logger = LogManager.getLogger(ManualPunishmentEnder.class);

    private AuditLogger() {}

    public static Mono<Void> addCommandToDB(ChatInputInteractionEvent event, boolean success) {
        DatabaseLoader.openConnectionIfClosed();

        if (event.getInteraction().getGuildId().isEmpty()) {
            return Mono.empty();
        }

        DiscordServer server = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());

        if (server == null) {
            return Mono.empty();
        }

        int serverId = server.getServerId();

        DiscordUser user = DiscordUser.findFirst("user_id_snowflake = ?", event.getInteraction().getUser().getId().asLong());

        if (user == null) {
            return Mono.empty();
        }
        int commandUserId = user.getUserId();

        StringBuffer stringBuffer = new StringBuffer();
        StringBuilder sb = new StringBuilder();

        stringBuffer.append(event.getCommandName());

        return Flux.fromIterable(event.getOptions())
                .flatMap(options -> generateOptionString(options, sb))
                .map(stringBuffer::append)
                .doOnComplete(() -> {
                    String commandContent = stringBuffer.toString();
                    CommandUse commandUse = CommandUse.findOrCreateIt("server_id", serverId, "command_user_id", commandUserId, "command_contents", commandContent, "date", Instant.now().toEpochMilli(), "success", success);
                    commandUse.save();
                })
                .then();
    }

    public static Mono<Void> addCommandToDB(ButtonInteractionEvent event, String entry, boolean success) {
        DatabaseLoader.openConnectionIfClosed();

        if (event.getInteraction().getGuildId().isEmpty()) {
            return Mono.empty();
        }

        DiscordServer server = DiscordServer.findFirst("server_id = ?", event.getInteraction().getGuildId().get().asLong());

        if (server == null) {
            return Mono.empty();
        }

        int serverId = server.getServerId();

        DiscordUser user = DiscordUser.findFirst("user_id_snowflake = ?", event.getInteraction().getUser().getId().asLong());

        if (user == null) {
            return Mono.empty();
        }
        int commandUserId = user.getUserId();

        CommandUse commandUse = CommandUse.findOrCreateIt("server_id", serverId, "command_user_id", commandUserId, "command_contents", entry, "date", Instant.now().toEpochMilli(), "success", success);
        commandUse.save();

        return Mono.empty();
    }

    public static Mono<String> generateOptionString(ApplicationCommandInteractionOption option) {
        return generateOptionString(option, new StringBuilder());
    }

    public static Mono<String> generateOptionString(ApplicationCommandInteractionOption option, StringBuilder sb) {

        return Mono.just(sb).flatMap(unused -> {
            if (option.getValue().isEmpty()) {
                return Mono.just(sb.append(" ").append(option.getName())).flatMap($ -> {
                    if (!option.getOptions().isEmpty()) {
                        for (ApplicationCommandInteractionOption opt : option.getOptions()) {
                            return generateOptionString(opt, sb);
                        }
                    }
                    return Mono.just(sb.toString());
                });
            } else {
                return stringifyOptionValue(option).flatMap(result ->
                        Mono.just(sb.append(" ").append(option.getName()).append(":").append(result)).flatMap($ -> {
                    if (!option.getOptions().isEmpty()) {
                        for (ApplicationCommandInteractionOption opt : option.getOptions()) {
                            return generateOptionString(opt, sb);
                        }
                    }
                    return Mono.just(sb.toString());
                }));
            }
        });

        /*if (option.getValue().isEmpty()) {
            sb.append(" ").append(option.getName());
        } else {
            sb.append(" ").append(option.getName()).append(":").append(stringifyOptionValue(option));
        }

        if (!option.getOptions().isEmpty()) {
            for (ApplicationCommandInteractionOption opt : option.getOptions()) {
                generateOptionString(opt, sb);
            }
        }
        return Mono.just(sb.toString());*/
    }

    private static Mono<String> stringifyOptionValue(ApplicationCommandInteractionOption option) {

        if (option.getValue().isEmpty()) {
            return Mono.just("");
        }

        return switch (option.getType().name()) {
            case "USER" -> option.getValue().get().asUser().flatMap(user -> Mono.just(user.getId().asString()));
            case "STRING" -> Mono.just(option.getValue().get().asString());
            case "INTEGER" -> Mono.just(String.valueOf(option.getValue().get().asLong()));
            case "BOOLEAN" -> Mono.just(String.valueOf(option.getValue().get().asBoolean()));
            case "CHANNEL" -> option.getValue().get().asChannel().flatMap(channel -> Mono.just(channel.getId().asString()));
            case "ROLE" -> option.getValue().get().asRole().flatMap(role -> Mono.just(role.getId().asString()));
            case "MENTIONABLE" -> Mono.just(option.getValue().get().toString());
            default -> Mono.just(option.getName());
        };
    }
}
