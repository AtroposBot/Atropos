package dev.laarryy.Icicle.models.guilds;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("server_properties")
public class DiscordServerProperties extends Model {

    public @NonNull Integer getServerPropertiesId() {
        return getInteger("id");
    }

    public @NonNull Integer getServerId() {
        return getInteger("server_id");
    }

    public @NonNull Long getServerIdSnowflake() {
        return getLong("server_id_snowflake");
    }

    public void setServerIdSnowflake(long serverIdSnowflake) {
        set("server_id_snowflake", serverIdSnowflake);
    }

    public @Nullable String getServerName() {
        return getString("server_name");
    }

    public void setServerName(@NonNull String serverName) {
        set("server_name", serverName);
    }

    public int getMembersOnFirstJoin() {
        return getInteger("member_count_on_icicle_join");
    }

    public void setMembersOnFirstJoin(int membersonFirstJoin) {
        setInteger("member_count_on_icicle_join", membersonFirstJoin);
    }

    public @Nullable Long getServerLogChannelSnowflake() {
        return getLong("server_log_channel_snowflake");
    }

    public void setServerLogChannelSnowflake(@Nullable Long serverLogChannelSnowflake) {
        set("server_log_channel_snowflake", serverLogChannelSnowflake);
    }

    public Long getMutedRoleSnowflake() {
        return getLong("muted_role_id_snowflake");
    }

    public void setMutedRoleSnowflake(Long mutedRoleSnowflake) {
        setLong("muted_role_id_snowflake", mutedRoleSnowflake);
    }
}
