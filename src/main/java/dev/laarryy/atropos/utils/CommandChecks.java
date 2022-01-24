package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.exceptions.NoPermissionsException;
import dev.laarryy.atropos.exceptions.NullServerException;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;

public class CommandChecks {

    static final PermissionChecker permissionChecker = new PermissionChecker();


    public static boolean commandChecks(ChatInputInteractionEvent event, String requestName) throws NullServerException, NoPermissionsException {

        if (event.getInteraction().getGuild().blockOptional().isEmpty() || event.getInteraction().getGuild().block() == null) {
            throw new NullServerException("No Guild");
        }

        Guild guild = event.getInteraction().getGuild().block();

        if (!permissionChecker.checkPermission(guild, event.getInteraction().getUser(), requestName)) {
            if (permissionChecker.checkIsAdministrator(guild, event.getInteraction().getMember().get())) {
                return true;
            }
            AuditLogger.addCommandToDB(event, false);
            throw new NoPermissionsException("No Permission");
        }

        return true;
    }

    public static boolean commandChecks(ButtonInteractionEvent event, String requestName) throws NullServerException, NoPermissionsException {

        if (event.getInteraction().getGuild().blockOptional().isEmpty() || event.getInteraction().getGuild().block() == null) {
            throw new NullServerException("No Guild");
        }

        Guild guild = event.getInteraction().getGuild().block();

        if (!permissionChecker.checkPermission(guild, event.getInteraction().getUser(), requestName)) {
            if (permissionChecker.checkIsAdministrator(guild, event.getInteraction().getMember().get())) {
                return true;
            }
            throw new NoPermissionsException("No Permission");
        }

        return true;
    }
}
