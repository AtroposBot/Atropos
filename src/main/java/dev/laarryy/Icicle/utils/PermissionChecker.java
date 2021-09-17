package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.permissions.ServerRolePermission;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

public final class PermissionChecker {
    private final Logger logger = LogManager.getLogger(this);

    public boolean checkPermission(Guild guild, User user, int permissionId) {
        Snowflake guildIdSnowflake = guild.getId();
        Member member = user.asMember(guildIdSnowflake).block();

        DatabaseLoader.openConnectionIfClosed();

        logger.info("Permission check in progress - permission ID = " + permissionId);
        int guildId = DiscordServer.findFirst("server_id = ?", guildIdSnowflake.asLong()).getInteger("id");

        Boolean hasRoleWithPermission =
                guild.getRoles()
                        .filter(role ->
                                (ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", guildId, permissionId, role.getId().asLong()) != null)
                                        || (ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", guildId, 69, role.getId().asLong()) != null))
                        .any(role ->
                                member.getRoles()
                                        .any(memberRole -> memberRole.equals(role))
                                        .block())
                        .block();

        return hasRoleWithPermission != null && hasRoleWithPermission;
    }

    public boolean checkIsAdministrator(Guild guild, Member member) {

        logger.info("Checking if Admin");
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
            logger.info("Has role with admin");
            return true;
        } else {
            logger.info("No role with admin");
        }

        if (isUserWithAdmin != null && isUserWithAdmin) {
            logger.info("Is user with admin");
            return true;
        } else {
            logger.info("Is not user with admin");
        }

        if (ownsGuild != null && ownsGuild) {
            logger.info("Owns guild");
            return true;
        } else {
            logger.info("Does not own guild");
            return false;
        }
    }
}
