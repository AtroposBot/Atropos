package dev.laarryy.atropos.models.guilds;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("server_messages")
public class ServerMessage extends Model {

    public int getMessageId() {
        return getInteger("id");
    }

    public long getMessageSnowflake() {
        return getLong("message_id_snowflake");
    }

    public void setMessageSnowflake(long messageSnowflake) {
        setLong("message_id_snowflake", messageSnowflake);
    }

    public int getServerId() {
        return getInteger("server_id");
    }

    public void setServerId(int serverId) {
        setInteger("server_id", serverId);
    }

    public long getServerSnowflake() {
        return getLong("server_id_snowflake");
    }

    public void setServerSnowflake(long serverSnowflake) {
        setLong("server_id_snowflake", serverSnowflake);
    }

    public int getUserId() {
        return getInteger("user_id");
    }

    public void setUserId(int userId) {
        setInteger("user_id", userId);
    }

    public long getUserSnowflake() {
        return getLong("user_id_snowflake");
    }

    public void setUserSnowflake(long userSnowflake) {
        setLong("user_id_snowflake", userSnowflake);
    }

    public long getDateEpochMilli() {
        return getLong("date");
    }

    public void setDateEpochMilli(long epochMilli) {
        setLong("date", epochMilli);
    }

    public String getContent() {
        return getString("content");
    }

    public void setContent(String content) {
        setString("content", content);
    }

    public boolean getDeleted() {
        return getBoolean("deleted");
    }

    public void setDeleted(boolean deleted) {
        setBoolean("deleted", deleted);
    }

}
