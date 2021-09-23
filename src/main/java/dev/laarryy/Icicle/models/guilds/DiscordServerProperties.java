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

    public @Nullable Long getGuildLogChannelSnowflake() {
        return getLong("guild_log_channel_snowflake");
    }

    public void setGuildLogChannelSnowflake(@Nullable Long guildLogChannelSnowflake) {
        set("guild_log_channel_snowflake", guildLogChannelSnowflake);
    }

    public @Nullable Long getMessageLogChannelSnowflake() {
        return getLong("message_log_channel_snowflake");
    }

    public void setMessageLogChannelSnowflake(@Nullable Long messageLogChannelSnowflake) {
        set("message_log_channel_snowflake", messageLogChannelSnowflake);
    }

    public @Nullable Long getMemberLogChannelSnowflake() {
        return getLong("member_log_channel_snowflake");
    }

    public void setMemberLogChannelSnowflake(@Nullable Long memberLogChannelSnowflake) {
        set("member_log_channel_snowflake", memberLogChannelSnowflake);
    }

    public @Nullable Long getPunishmentLogChannelSnowflake() {
        return getLong("punishment_log_channel_snowflake");
    }

    public void setPunishmentLogChannelSnowflake(@Nullable Long punishmentLogChannelSnowflake) {
        set("punishment_log_channel_snowflake", punishmentLogChannelSnowflake);
    }

    public boolean getMessageLogEnabled() {
        return getBoolean("message_log_enabled");
    }

    public void setMessageLogEnabled(boolean messageLogEnabled) {
        setBoolean("message_log_enabled", messageLogEnabled);
    }

    public boolean getMemberLogEnabled() {
        return getBoolean("member_log_enabled");
    }

    public void setMemberLogEnabled(boolean memberLogEnabled) {
        setBoolean("member_log_enabled", memberLogEnabled);
    }

    public boolean getGuildLogEnabled() {
        return getBoolean("guild_log_enabled");
    }

    public void setGuildLogEnabled(boolean guildLogEnabled) {
        setBoolean("guild_log_enabled", guildLogEnabled);
    }

    public boolean getPunishmentLogEnabled() {
        return getBoolean("punishment_log_enabled");
    }

    public void setPunishmentLogEnabled(boolean punishmentLogEnabled) {
        setBoolean("punishment_log_enabled", punishmentLogEnabled);
    }

    public Long getMutedRoleSnowflake() {
        return getLong("muted_role_id_snowflake");
    }

    public void setMutedRoleSnowflake(Long mutedRoleSnowflake) {
        setLong("muted_role_id_snowflake", mutedRoleSnowflake);
    }

    public boolean getStopJoins() {
        return getBoolean("stop_joins");
    }

    public void setStopJoins(boolean stopJoins) {
        setBoolean("stop_joins", stopJoins);
    }
}
