package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.models.users.DiscordUser;
import net.dv8tion.jda.api.entities.*;
import org.checkerframework.checker.nullness.qual.NonNull;

public class IcicleMemberHandler {

    public DiscordUser getDiscordUser(@NonNull Member member) {
        return DiscordUser.findFirst("user_id_snowflake = ?", member.getUser().getIdLong());
    }
}
