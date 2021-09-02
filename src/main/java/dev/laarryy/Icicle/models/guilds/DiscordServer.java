package dev.laarryy.Icicle.models.guilds;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("servers")
public class DiscordServer extends Model {

    public int getServerId() {
        return getInteger("id");
    }

    public Long getDateEntry() {
        return getLong("date");
    }

    public void setDateEntry(Long epochMilli) {
        setLong("date", epochMilli);
    }

    public Long getServerSnowflake() {
        return getLong("server_id");
    }

    public void setServerSnowflake(Long snowflake) {
        setLong("server_id", snowflake);
    }

}