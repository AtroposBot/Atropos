package dev.laarryy.Icicle.models.guilds.permissions;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("server_role_permissions")
public class ServerRolePermission extends Model {

    public int getServerId() {
        return getInteger("server_id");
    }

    public void setServerId(int serverId) {
        setInteger("server_id", serverId);
    }

    public int getPermissionId() {
        return getInteger("permission_id");
    }

    public void setPermissionId(int permissionId) {
        setInteger("permission_id", permissionId);
    }

    public Long getRoleIdSnowflake() {
        return getLong("role_id_snowflake");
    }

    public void setRoleIdSnowflake(Long roleIdSnowflake) {
        setLong("role_id_snowflake", roleIdSnowflake);
    }

    public void setFullPermission(int serverId, int permissionId, Long roleIdSnowflake) {
        findOrCreateIt("server_id", serverId, "permission_id", permissionId, "role_id_snowflake", roleIdSnowflake);
        save();
    }
}
