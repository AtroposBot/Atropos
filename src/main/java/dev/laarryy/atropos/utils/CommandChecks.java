package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.exceptions.NoPermissionsException;
import dev.laarryy.atropos.exceptions.NullServerException;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import reactor.core.publisher.Mono;

public class CommandChecks {

    static final PermissionChecker permissionChecker = new PermissionChecker();


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

    public static Mono<Boolean> commandChecks(ButtonInteractionEvent event, String requestName) throws NullServerException, NoPermissionsException {

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
