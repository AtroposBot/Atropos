/*
 * This file is generated by jOOQ.
 */
package dev.laarryy.atropos.jooq.tables.records;


import dev.laarryy.atropos.jooq.tables.ServerRolePermissions;

import org.checkerframework.checker.nullness.qual.NonNull;
import discord4j.common.util.Snowflake;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.Row4;
import org.jooq.impl.UpdatableRecordImpl;


@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServerRolePermissionsRecord extends UpdatableRecordImpl<ServerRolePermissionsRecord> implements Record4<Integer, Integer, Integer, Snowflake> {

    private static final long serialVersionUID = 1L;

    public ServerRolePermissionsRecord setId(@NonNull Integer value) {
        set(0, value);
        return this;
    }

    @NonNull
    public Integer getId() {
        return (Integer) get(0);
    }

    public ServerRolePermissionsRecord setServerId(@NonNull Integer value) {
        set(1, value);
        return this;
    }

    @NonNull
    public Integer getServerId() {
        return (Integer) get(1);
    }

    public ServerRolePermissionsRecord setPermissionId(@NonNull Integer value) {
        set(2, value);
        return this;
    }

    @NonNull
    public Integer getPermissionId() {
        return (Integer) get(2);
    }

    public ServerRolePermissionsRecord setRoleIdSnowflake(@NonNull Snowflake value) {
        set(3, value);
        return this;
    }

    @NonNull
    public Snowflake getRoleIdSnowflake() {
        return (Snowflake) get(3);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    @NonNull
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record4 type implementation
    // -------------------------------------------------------------------------

    @Override
    @NonNull
    public Row4<Integer, Integer, Integer, Snowflake> fieldsRow() {
        return (Row4) super.fieldsRow();
    }

    @Override
    @NonNull
    public Row4<Integer, Integer, Integer, Snowflake> valuesRow() {
        return (Row4) super.valuesRow();
    }

    @Override
    @NonNull
    public Field<Integer> field1() {
        return ServerRolePermissions.SERVER_ROLE_PERMISSIONS.ID;
    }

    @Override
    @NonNull
    public Field<Integer> field2() {
        return ServerRolePermissions.SERVER_ROLE_PERMISSIONS.SERVER_ID;
    }

    @Override
    @NonNull
    public Field<Integer> field3() {
        return ServerRolePermissions.SERVER_ROLE_PERMISSIONS.PERMISSION_ID;
    }

    @Override
    @NonNull
    public Field<Snowflake> field4() {
        return ServerRolePermissions.SERVER_ROLE_PERMISSIONS.ROLE_ID_SNOWFLAKE;
    }

    @Override
    @NonNull
    public Integer component1() {
        return getId();
    }

    @Override
    @NonNull
    public Integer component2() {
        return getServerId();
    }

    @Override
    @NonNull
    public Integer component3() {
        return getPermissionId();
    }

    @Override
    @NonNull
    public Snowflake component4() {
        return getRoleIdSnowflake();
    }

    @Override
    @NonNull
    public Integer value1() {
        return getId();
    }

    @Override
    @NonNull
    public Integer value2() {
        return getServerId();
    }

    @Override
    @NonNull
    public Integer value3() {
        return getPermissionId();
    }

    @Override
    @NonNull
    public Snowflake value4() {
        return getRoleIdSnowflake();
    }

    @Override
    @NonNull
    public ServerRolePermissionsRecord value1(@NonNull Integer value) {
        setId(value);
        return this;
    }

    @Override
    @NonNull
    public ServerRolePermissionsRecord value2(@NonNull Integer value) {
        setServerId(value);
        return this;
    }

    @Override
    @NonNull
    public ServerRolePermissionsRecord value3(@NonNull Integer value) {
        setPermissionId(value);
        return this;
    }

    @Override
    @NonNull
    public ServerRolePermissionsRecord value4(@NonNull Snowflake value) {
        setRoleIdSnowflake(value);
        return this;
    }

    @Override
    @NonNull
    public ServerRolePermissionsRecord values(@NonNull Integer value1, @NonNull Integer value2, @NonNull Integer value3, @NonNull Snowflake value4) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ServerRolePermissionsRecord() {
        super(ServerRolePermissions.SERVER_ROLE_PERMISSIONS);
    }

    public ServerRolePermissionsRecord(@NonNull Integer id, @NonNull Integer serverId, @NonNull Integer permissionId, @NonNull Snowflake roleIdSnowflake) {
        super(ServerRolePermissions.SERVER_ROLE_PERMISSIONS);

        setId(id);
        setServerId(serverId);
        setPermissionId(permissionId);
        setRoleIdSnowflake(roleIdSnowflake);
    }
}
