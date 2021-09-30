package dev.laarryy.eris.models.guilds;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("server_roles")
public class DiscordServerRoles extends Model {

    public @NonNull Integer getServerRoleId() {
        return getInteger("id");
    }

    public @NonNull Long getServerRoleIdSnowflake() {
        return getLong("role_id_snowflake");
    }

    public void setServerRoleIdSnowflake(@NonNull Long serverRoleIdSnowflake) {
        set("role_id_snowflake", serverRoleIdSnowflake);
    }

    public @NonNull Integer getServerId() {
        return getInteger("server_id");
    }

    public void setServerId(@NonNull Integer serverId) {
        set("server_id", serverId);
    }
}
