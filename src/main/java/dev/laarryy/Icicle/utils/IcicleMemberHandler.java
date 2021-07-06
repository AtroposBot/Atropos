package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.models.users.DiscordUser;
import discord4j.core.object.entity.Member;
import org.checkerframework.checker.nullness.qual.NonNull;

public class IcicleMemberHandler {

    public DiscordUser getDiscordUserFromSnowflake(@NonNull Member member) {
        return DiscordUser.findFirst("user_id_snowflake = ?", member.getId().asLong());
    }
}
