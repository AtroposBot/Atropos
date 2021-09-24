package dev.laarryy.Icicle.utils;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public class SlashCommandChecks {

    public static boolean slashCommandChecks(SlashCommandEvent event, ApplicationCommandRequest request) {
        PermissionChecker permissionChecker = new PermissionChecker();

        if (event.getInteraction().getGuild().block() == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return false;
        }

        Guild guild = event.getInteraction().getGuild().block();

        if (!permissionChecker.checkPermission(guild, event.getInteraction().getUser(), request)) {
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
