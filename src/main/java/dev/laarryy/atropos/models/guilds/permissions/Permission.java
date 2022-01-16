package dev.laarryy.atropos.models.guilds.permissions;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

@Table("permissions")
public class Permission extends Model {
    public int getPermissionId() {
        return getInteger("id");
    }

    public void setPermissionId(int permissionId) {
        setInteger("id", permissionId);
    }

    public String getName() {
        return getString("permission");
    }

    public void setName(String name) {
        setString("permission", name);
    }
}
