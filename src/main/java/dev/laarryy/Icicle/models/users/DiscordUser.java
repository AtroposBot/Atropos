package dev.laarryy.Icicle.models.users;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import java.time.Instant;

@Table("users")
public class DiscordUser extends Model {

    public @NonNull int getUserId() {
        return getInteger("id");
    }

    public @NonNull Long getUserIdSnowflake() {
        return getLong("user_id_snowflake");
    }

    public @NonNull Instant getDateEntry() {
        return Instant.ofEpochMilli(getLong("date"));
    }

    public @Nullable String getUserCommandPrefix() {
        return getString("user_command_prefix");
    }

    public void setUserCommandPrefix(@Nullable String userCommandPrefix) {
        set("user_command_prefix", userCommandPrefix);
    }
}
