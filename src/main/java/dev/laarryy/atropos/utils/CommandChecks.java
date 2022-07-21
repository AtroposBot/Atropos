package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.exceptions.NoPermissionsException;
import dev.laarryy.atropos.exceptions.NullServerException;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import reactor.core.publisher.Mono;

public class CommandChecks {

    static final PermissionChecker permissionChecker = new PermissionChecker();

    /**
     *
     * @param event {@link ChatInputInteractionEvent} to check command validity and permissions for
     * @param requestName Command name to check validity and permission of
     * @return a true {@link Mono}<{@link Boolean}> if command is permitted in event's guild, or an error signal indicating no permission.
     */

    public static Mono<Boolean> commandChecks(ChatInputInteractionEvent event, String requestName) {

        return Mono.from(event.getInteraction().getGuild())
                .flatMap(guild -> {
                    if (guild == null) {
                        return Mono.error(new NullServerException("No Guild"));
                    }
                    return Mono.from((permissionChecker.checkPermission(guild, event.getInteraction().getUser(), requestName)))
                            .flatMap(aBoolean -> {
                                if (!aBoolean) {
                                    AuditLogger.addCommandToDB(event, false);
                                    return Mono.error(new NoPermissionsException("No Permission"));
                                } else {
                                    return Mono.just(true);
                                }
                            });
                });
    }

    /**
     *
     * @param event {@link ButtonInteractionEvent} to check button-use command validity and permissions for
     * @param requestName Command name to check validity and permission of
     * @return a true {@link Mono}<{@link Boolean}> if button-use command is permitted in event's guild, or an error signal indicating no permission.
     */

    public static Mono<Boolean> commandChecks(ButtonInteractionEvent event, String requestName) {

        return Mono.from(event.getInteraction().getGuild())
                .flatMap(guild -> {
                    if (guild == null) {
                        return Mono.error(new NullServerException("No Guild"));
                    }
                    return Mono.from((permissionChecker.checkPermission(guild, event.getInteraction().getUser(), requestName)))
                            .flatMap(aBoolean -> {
                                if (!aBoolean) {
                                    return Mono.error(new NoPermissionsException("No Permission"));
                                } else {
                                    return Mono.just(true);
                                }
                            });
                });
    }
}
