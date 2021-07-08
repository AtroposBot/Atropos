package dev.laarryy.Icicle.models.guilds;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import java.time.Instant;

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

    public @NonNull Instant getServerJoinDate() {
        return Instant.ofEpochMilli(getLong("icicle_join_server_date"));
    }

    public void setServerJoinDate(@NonNull Instant serverJoinDate) {
        set("icicle_join_server_date", serverJoinDate);
    }

    public @NonNull String getServerCommandPrefix() {
        return getString("server_command_prefix");
    }

    public void setServerCommandPrefix(@NonNull String serverCommandPrefix) {
        set("server_command_prefix", serverCommandPrefix);
    }

    public @Nullable Long getServerLogChannelSnowflake() {
        return getLong("server_log_channel_snowflake");
    }

    public void setServerLogChannelSnowflake(@Nullable Long serverLogChannelSnowflake) {
        set("server_log_channel_snowflake", serverLogChannelSnowflake);
    }
}
