package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.users.DiscordUser;

public class SnowflakeIdFinder {

    public int getUserIdFromSnowflake(Long userSnowflake) {
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userSnowflake);
        return discordUser.getUserId();
    }

    public int getServerIdFromSnowflake(Long serverSnowflake) {
        DiscordServer discordUser = DiscordServer.findFirst("server_id_snowflake = ?", serverSnowflake);
        return discordUser.getServerId();
    }
}
