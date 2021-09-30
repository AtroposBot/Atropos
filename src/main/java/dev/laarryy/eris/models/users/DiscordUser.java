package dev.laarryy.eris.models.users;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

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

    public @NonNull Long getDateEntry() {
        return getLong("date");
    }

    public void setDateEntry(@NonNull Long epochMilli) {
        set("date", epochMilli);
    }

}
