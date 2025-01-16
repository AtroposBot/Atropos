/*
 * This file is generated by jOOQ.
 */
package dev.laarryy.atropos.jooq;


import dev.laarryy.atropos.jooq.tables.Punishments;
import dev.laarryy.atropos.jooq.tables.ServerBlacklist;
import dev.laarryy.atropos.jooq.tables.ServerProperties;
import dev.laarryy.atropos.jooq.tables.ServerRolePermissions;
import org.jooq.Index;
import org.jooq.OrderField;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;


/**
 * A class modelling indexes of tables in atropos.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Indexes {

    // -------------------------------------------------------------------------
    // INDEX definitions
    // -------------------------------------------------------------------------

    public static final Index SERVER_ROLE_PERMISSIONS_PERMISSION_ID = Internal.createIndex(DSL.name("permission_id"), ServerRolePermissions.SERVER_ROLE_PERMISSIONS, new OrderField[] { ServerRolePermissions.SERVER_ROLE_PERMISSIONS.PERMISSION_ID }, false);
    public static final Index PUNISHMENTS_SERVER_ID = Internal.createIndex(DSL.name("server_id"), Punishments.PUNISHMENTS, new OrderField[] { Punishments.PUNISHMENTS.SERVER_ID }, false);
    public static final Index SERVER_BLACKLIST_SERVER_ID = Internal.createIndex(DSL.name("server_id"), ServerBlacklist.SERVER_BLACKLIST, new OrderField[] { ServerBlacklist.SERVER_BLACKLIST.SERVER_ID }, false);
    public static final Index SERVER_ROLE_PERMISSIONS_SERVER_ID = Internal.createIndex(DSL.name("server_id"), ServerRolePermissions.SERVER_ROLE_PERMISSIONS, new OrderField[] { ServerRolePermissions.SERVER_ROLE_PERMISSIONS.SERVER_ID }, false);
    public static final Index SERVER_PROPERTIES_SERVER_PROPERTIES_SERVERS_SERVER_ID_FK = Internal.createIndex(DSL.name("server_properties_servers_server_id_fk"), ServerProperties.SERVER_PROPERTIES, new OrderField[] { ServerProperties.SERVER_PROPERTIES.SERVER_ID_SNOWFLAKE }, false);
    public static final Index PUNISHMENTS_USER_ID = Internal.createIndex(DSL.name("user_id"), Punishments.PUNISHMENTS, new OrderField[] { Punishments.PUNISHMENTS.USER_ID_PUNISHED }, false);
}
