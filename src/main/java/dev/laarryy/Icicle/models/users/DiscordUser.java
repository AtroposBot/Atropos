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

    public void setUserIdSnowflake(@NonNull Long snowflake) {
        set("user_id_snowflake", snowflake);
    }

    public @NonNull Instant getDateEntry() {
        return Instant.ofEpochMilli(getLong("date"));
    }

    public void setDateEntry(@NonNull Instant instant) {
        set("date", instant.toEpochMilli());
    }

    public void setDateEntry(@NonNull Long epochMilli) {
        set("date", epochMilli);
    }

    public @Nullable String getUserCommandPrefix() {
        return getString("user_command_prefix");
    }

    public void setUserCommandPrefix(@Nullable String userCommandPrefix) {
        set("user_command_prefix", userCommandPrefix);
    }
}
