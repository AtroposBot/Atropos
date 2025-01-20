package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.commands.punishments.ManualPunishmentEnder;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static dev.laarryy.atropos.jooq.Tables.SERVERS;
import static dev.laarryy.atropos.jooq.Tables.SERVER_COMMAND_USES;
import static dev.laarryy.atropos.jooq.Tables.USERS;
import static dev.laarryy.atropos.storage.DatabaseLoader.sqlContext;
import static org.jooq.impl.DSL.select;

public final class AuditLogger {
    private static final Logger logger = LogManager.getLogger(ManualPunishmentEnder.class);

    private AuditLogger() {
    }

    /**
     * @param event   {@link ChatInputInteractionEvent} to add command use to audit database
     * @param success Whether the command use was successful or not
     * @return a {@link Mono}<{@link Void}> on completion or if unable to complete
     */

    public static Mono<Void> addCommandToDB(ChatInputInteractionEvent event, boolean success) {
        final Snowflake userSnowflake = event.getInteraction().getUser().getId();
        return Mono.justOrEmpty(event.getInteraction().getGuildId()).flatMap(serverSnowflake ->
                Flux.fromIterable(event.getOptions())
                        .flatMap(AuditLogger::generateOptionString)
                        .reduce(event.getCommandName(), String::concat)
                        .flatMap(commandContent -> addCommandToDB(serverSnowflake, userSnowflake, commandContent, success))
        );
    }

    /**
     * @param event   {@link ButtonInteractionEvent} to add button interaction to audit database
     * @param entry   The button interaction's content to be added
     * @param success Whether the button interaction was successful or not
     * @return a {@link Mono}<{@link Void}> on completion or if unable to complete
     */

    public static Mono<Void> addCommandToDB(ButtonInteractionEvent event, String entry, boolean success) {
        final Snowflake userSnowflake = event.getInteraction().getUser().getId();
        return Mono.justOrEmpty(event.getInteraction().getGuildId()).flatMap(serverSnowflake -> addCommandToDB(serverSnowflake, userSnowflake, entry, success));
    }

    private static Mono<Void> addCommandToDB(final Snowflake serverSnowflake, final Snowflake userSnowflake, final String commandContent, final boolean success) {
        return Mono.fromDirect(sqlContext.insertInto(SERVER_COMMAND_USES)
                        .set(SERVER_COMMAND_USES.SERVER_ID, select(SERVERS.ID).from(SERVERS).where(SERVERS.SERVER_ID_SNOWFLAKE.eq(serverSnowflake)))
                        .set(SERVER_COMMAND_USES.COMMAND_USER_ID, select(USERS.ID).from(USERS).where(USERS.USER_ID_SNOWFLAKE.eq(userSnowflake)))
                        .set(SERVER_COMMAND_USES.COMMAND_CONTENTS, commandContent)
                        .set(SERVER_COMMAND_USES.DATE, Instant.now())
                        .set(SERVER_COMMAND_USES.SUCCESS, success))
                .then();
    }

    public static Mono<String> generateOptionString(ApplicationCommandInteractionOption option) {
        final Mono<String> nameAndValueMono = stringifyOptionValue(option).map(value -> ' ' + option.getName() + ':' + value).defaultIfEmpty(' ' + option.getName());

        return Flux.fromIterable(option.getOptions())
                .flatMap(AuditLogger::generateOptionString)
                .reduce("", String::concat) // make sure there's at least an empty string, otherwise the zip combinator below won't ever be called
                .zipWith(nameAndValueMono, (nameAndValue, concatenatedChildren) -> nameAndValue + concatenatedChildren);

/*
        return Flux.fromIterable(option.getOptions())
                .flatMap(opt -> {
                    if (opt.getOptions().isEmpty()) {
                        if (opt.getValue().isPresent()) {
                            return AuditLogger.stringifyOptionValue(opt).map(result -> ":" + result);
                        } else {
                            return Mono.just("");
                        }
                    } else {
                        return Flux.fromIterable(opt.getOptions())
                                .flatMap(AuditLogger::generateOptionString)
                                .reduce(opt.getName(), String::concat);
                    }

                })
                .reduce(" " + option.getName(), String::concat);
*/

        /*return Mono.just(sb).flatMap(unused -> {
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
        });*/

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
        return switch (option.getType()) {
            case STRING -> Mono.justOrEmpty(option.getValue()).map(value -> value.asString());
            case INTEGER -> Mono.justOrEmpty(option.getValue()).map(value -> String.valueOf(value.asLong()));
            case BOOLEAN -> Mono.justOrEmpty(option.getValue()).map(value -> String.valueOf(value.asBoolean()));
            case MENTIONABLE -> Mono.justOrEmpty(option.getValue()).map(value -> value.asSnowflake().asString());
            default -> Mono.just(option.getName());
        };
    }
}
