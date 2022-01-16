package dev.laarryy.atropos.utils;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;

public class CommandChecks {

    static final PermissionChecker permissionChecker = new PermissionChecker();


    public static boolean commandChecks(ChatInputInteractionEvent event, String requestName) {

        if (event.getInteraction().getGuild().blockOptional().isEmpty() || event.getInteraction().getGuild().block() == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return false;
        }

        Guild guild = event.getInteraction().getGuild().block();

        if (!permissionChecker.checkPermission(guild, event.getInteraction().getUser(), requestName)) {
            if (permissionChecker.checkIsAdministrator(guild, event.getInteraction().getMember().get())) {
                return true;
            }
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            return false;
        }

        return true;
    }

    public static boolean commandChecks(ButtonInteractionEvent event, String requestName) {

        if (event.getInteraction().getGuild().blockOptional().isEmpty() || event.getInteraction().getGuild().block() == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return false;
        }

        Guild guild = event.getInteraction().getGuild().block();

        if (!permissionChecker.checkPermission(guild, event.getInteraction().getUser(), requestName)) {
            if (permissionChecker.checkIsAdministrator(guild, event.getInteraction().getMember().get())) {
                return true;
            }
            Notifier.notifyCommandUserOfError(event, "noPermission");
            return false;
        }

        return true;
    }
}
