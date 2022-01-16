package dev.laarryy.atropos.models.joins;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("server_user")
public class ServerUser extends Model {

    public int getUserId() {
        return getInteger("user_id");
    }

    public void setUserId(int userId) {
        setInteger("user_id", userId);
    }

    public int getServerId() {
        return getInteger("server_id");
    }

    public void setServerId(int serverId) {
        setInteger("server_id", serverId);
    }

    public Long getDate() {
        return getLong("date");
    }

    public void setDate(Long dateEpochMillis) {
        setLong("date", dateEpochMillis);
    }


}
