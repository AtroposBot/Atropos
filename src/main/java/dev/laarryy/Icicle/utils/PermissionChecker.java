package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.permissions.ServerRolePermission;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PermissionChecker {
    private final Logger logger = LogManager.getLogger(this);

    public boolean checkPermission(Guild guild, User user, int permissionId) {
        Snowflake guildIdSnowflake = guild.getId();
        Member member = user.asMember(guildIdSnowflake).block();

        DatabaseLoader.openConnectionIfClosed();

        logger.info("Permission check in progress - permission ID = " + permissionId);
        int guildId = DiscordServer.findFirst("server_id = ?", guildIdSnowflake.asLong()).getInteger("id");

        Boolean hasRoleWithPermission =
                guild.getRoles()
                        .map(Role::getId)
                        .any(snowflake -> ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", guildId, permissionId, snowflake.asLong()) != null)
                        .block();

        return hasRoleWithPermission != null && hasRoleWithPermission;
    }
}
