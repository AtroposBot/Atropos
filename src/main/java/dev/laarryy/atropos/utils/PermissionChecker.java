package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.permissions.ServerRolePermission;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

public final class PermissionChecker {
    private final Logger logger = LogManager.getLogger(this);

    public boolean checkPermission(Guild guild, User user, String requestName) {
        Snowflake guildIdSnowflake = guild.getId();
        Member member = user.asMember(guildIdSnowflake).block();

        DatabaseLoader.openConnectionIfClosed();
        dev.laarryy.atropos.models.guilds.permissions.Permission permission = dev.laarryy.atropos.models.guilds.permissions.Permission.findOrCreateIt("permission", requestName);
        permission.save();
        permission.refresh();
        int permissionId = permission.getInteger("id");

        logger.info("Permission check in progress - permission ID = " + permissionId);

        if (checkIsAdministrator(guild, member)) {
            return true;
        }

        int guildId = DiscordServer.findFirst("server_id = ?", guildIdSnowflake.asLong()).getInteger("id");

        Boolean hasRoleWithPermission =
                guild.getRoles()
                        .filter(role ->
                                (ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", guildId, permissionId, role.getId().asLong()) != null)
                                        || (ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", guildId, 69, role.getId().asLong()) != null)
                                        || role.getPermissions().contains(Permission.ADMINISTRATOR))
                        .any(role ->
                                member.getRoles()
                                        .mergeWith(guild.getEveryoneRole())
                                        .any(memberRole -> memberRole.equals(role))
                                        .block())
                        .block();

        return hasRoleWithPermission != null && hasRoleWithPermission;
    }

    public boolean checkIsAdministrator(Guild guild, Member member) {

        Boolean hasRoleWithAdmin =
                member.getRoles()
                        .map(Role::getPermissions)
                        .any(permissions -> permissions.contains(Permission.ADMINISTRATOR))
                        .block();

        Boolean isUserWithAdmin =
                member.getBasePermissions()
                        .flux()
                        .any(permissions -> permissions.contains(Permission.ADMINISTRATOR))
                        .block();

        Boolean ownsGuild = Flux.just(member).any(member1 -> member1.equals(guild.getOwner().block())).block();

        if (hasRoleWithAdmin != null && hasRoleWithAdmin) {
            return true;
        }

        if (isUserWithAdmin != null && isUserWithAdmin) {
            return true;
        }

        if (ownsGuild != null && ownsGuild) {
            return true;
        } else {
            return false;
        }
    }

    public boolean checkBotPermission(ChatInputInteractionEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return false;
        }
        Member self = guild.getSelfMember().block();

        if (self.getBasePermissions().block().contains(Permission.ADMINISTRATOR)) {
            return true;
        }

        PermissionSet requiredPermissions = PermissionSet.of(
                Permission.VIEW_CHANNEL,
                Permission.MANAGE_CHANNELS,
                Permission.MANAGE_ROLES,
                Permission.VIEW_AUDIT_LOG,
                Permission.MANAGE_NICKNAMES,
                //Permission.USE_PRIVATE_THREADS,
                //Permission.USE_PUBLIC_THREADS,
                Permission.KICK_MEMBERS,
                Permission.BAN_MEMBERS,
                Permission.SEND_MESSAGES,
                Permission.USE_EXTERNAL_EMOJIS,
                Permission.MANAGE_MESSAGES,
                Permission.READ_MESSAGE_HISTORY,
                Permission.MUTE_MEMBERS
        );

        PermissionSet selfPermissions = self.getBasePermissions().block();
        if (selfPermissions == null) {
            Notifier.notifyCommandUserOfError(event, "noBotPermission");
            AuditLogger.addCommandToDB(event, false);
            return false;
        }
        for (Permission permission : requiredPermissions) {
            if (!selfPermissions.contains(permission)) {
                Notifier.notifyCommandUserOfError(event, "noBotPermission");
                AuditLogger.addCommandToDB(event, false);
                return false;
            }
        }
        return true;
    }

}
