package dev.laarryy.eris.models.guilds;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("server_command_uses")
public class CommandUse extends Model {

    public int getServerId() {
        return getInteger("server_id");
    }

    public void setServerId(int serverId) {
        setInteger("server_id", serverId);
    }

    public int getUserId() {
        return getInteger("command_user_id");
    }

    public void setUserId(int userId) {
        setInteger("command_user_id", userId);
    }

    public String getCommandContents() {
        return getString("command_contents");
    }

    public void setCommandContents(String commandContents) {
        setString("command_contents", commandContents);
    }

    public Long getDate() {
        return getLong("date");
    }

    public void setDate(Long dateEpochMilli) {
        setLong("date", dateEpochMilli);
    }

    public boolean getSucceeded() {
        return getBoolean("success");
    }

    public void setSucceeded(boolean succeeded) {
        setBoolean("success", succeeded);
    }
}
