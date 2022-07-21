package dev.laarryy.atropos.models.guilds;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ImmutableMessageData;
import discord4j.discordjson.json.MessageData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;
import org.javalite.json.JSONMap;

import java.sql.Clob;

@Table("server_messages")
public class ServerMessage extends Model {

    private static final ObjectMapper JACKSON_OBJECT_MAPPER = JacksonResources.create().getObjectMapper();

    Logger logger = LogManager.getLogger(this);

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

    public MessageData getMessageData() {
        return messageFromJson(getString("message_data"));
    }

    public void setMessageData(MessageData data) {
        setString("message_data", messageToJson(data));
    }

    private static MessageData messageFromJson(final String json) {
        try {
            return JACKSON_OBJECT_MAPPER.readValue(json, MessageData.class);
        } catch (final JsonProcessingException exception) {
            exception.printStackTrace(); // take care of this or smth
            return null;
        }
    }

    private static String messageToJson(final MessageData message) {
        try {
            return JACKSON_OBJECT_MAPPER.writeValueAsString(message);
        } catch (final JsonProcessingException exception) {
            exception.printStackTrace(); // take care of this or smth
            return null;
        }
    }

    public boolean getDeleted() {
        return getBoolean("deleted");
    }

    public void setDeleted(boolean deleted) {
        setBoolean("deleted", deleted);
    }

}
