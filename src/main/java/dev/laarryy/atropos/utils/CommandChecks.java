package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.exceptions.NoPermissionsException;
import dev.laarryy.atropos.exceptions.NullServerException;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class CommandChecks {

    static final PermissionChecker permissionChecker = new PermissionChecker();
    private static final Logger logger = LogManager.getLogger(CommandChecks.class);

    /**
     * @param event       {@link ChatInputInteractionEvent} to check command validity and permissions for
     * @param requestName Command name to check validity and permission of
     * @return a {@link Mono}<{@link Void}> if command is permitted in event's guild, or an error signal indicating no permission.
     */

    public static Mono<Void> commandChecks(ChatInputInteractionEvent event, String requestName) {

        return event.getInteraction().getGuild()
                .switchIfEmpty(Mono.error(new NullServerException("No Guild")))
                .flatMap(guild ->
                        permissionChecker.checkPermission(guild, event.getInteraction().getUser(), requestName).flatMap(hasPerm -> {
                            if (!hasPerm) {
                                logger.info("NO PERMS!");
                                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NoPermissionsException("No Permission")));
                            } else {
                                return Mono.empty();
                            }
                        }));
    }

    /**
     * @param event       {@link ButtonInteractionEvent} to check button-use command validity and permissions for
     * @param requestName Command name to check validity and permission of
     * @return a true {@link Mono}<{@link Boolean}> if button-use command is permitted in event's guild, or an error signal indicating no permission.
     */

    public static Mono<Void> commandChecks(ButtonInteractionEvent event, String requestName, String auditString) {

        return event.getInteraction().getGuild()
                .switchIfEmpty(Mono.error(new NullServerException("No Guild")))
                .flatMap(guild ->
                        permissionChecker.checkPermission(guild, event.getInteraction().getUser(), requestName).flatMap(hasPerm -> {
                            if (!hasPerm) {
                                return AuditLogger.addCommandToDB(event, auditString, false).then(Mono.error(new NoPermissionsException("No Permission")));
                            } else {
                                return Mono.empty();
                            }
                        }));
    }
}
