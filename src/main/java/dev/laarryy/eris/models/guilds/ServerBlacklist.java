package dev.laarryy.eris.models.guilds;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("server_blacklist")
public class ServerBlacklist extends Model {

    public int getBlacklistId() {
        return getInteger("id");
    }

    public void setBlacklistId(int id) {
        setInteger("id", id);
    }

    public int getServerId() {
        return getInteger("server_id");
    }

    public void setServerId(int serverId) {
        setInteger("server_id", serverId);
    }

    public String getTrigger() {
        return getString("regex_trigger");
    }

    public void setTrigger(String trigger) {
        setString("regex_trigger", trigger);
    }

    public String getType() {
        return getString("type");
    }

    public void setType(String type) {
        setString("type", type);
    }

    public String getAction() {
        return getString("action");
    }

    public void setAction(String action) {
        setString("action", action);
    }

}
