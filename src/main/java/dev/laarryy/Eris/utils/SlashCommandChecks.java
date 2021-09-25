package dev.laarryy.Eris.utils;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandRequest;

public class SlashCommandChecks {

    public static boolean slashCommandChecks(SlashCommandEvent event, String requestName) {
        PermissionChecker permissionChecker = new PermissionChecker();

        if (event.getInteraction().getGuild().block() == null) {
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
}
