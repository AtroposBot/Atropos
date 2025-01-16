/*
 * This file is generated by jOOQ.
 */
package dev.laarryy.atropos.jooq.tables;


import dev.laarryy.atropos.jooq.Atropos;
import dev.laarryy.atropos.jooq.Keys;
import dev.laarryy.atropos.jooq.tables.records.ServerMessagesRecord;
import dev.laarryy.atropos.utils.converters.SnowflakeToLongConverter;
import jooq.tables.Servers.ServersPath;
import jooq.tables.Users.UsersPath;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServerMessages extends TableImpl<ServerMessagesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>atropos.server_messages</code>
     */
    public static final ServerMessages SERVER_MESSAGES = new ServerMessages();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ServerMessagesRecord> getRecordType() {
        return ServerMessagesRecord.class;
    }

    /**
     * The column <code>atropos.server_messages.id</code>.
     */
    public final TableField<ServerMessagesRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>atropos.server_messages.message_id_snowflake</code>.
     */
    public final TableField<ServerMessagesRecord, Long> MESSAGE_ID_SNOWFLAKE = createField(DSL.name("message_id_snowflake"), SQLDataType.BIGINT.nullable(false), this, "", new SnowflakeToLongConverter());

    /**
     * The column <code>atropos.server_messages.server_id</code>.
     */
    public final TableField<ServerMessagesRecord, Integer> SERVER_ID = createField(DSL.name("server_id"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field(DSL.raw("0"), SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>atropos.server_messages.server_id_snowflake</code>.
     */
    public final TableField<ServerMessagesRecord, Long> SERVER_ID_SNOWFLAKE = createField(DSL.name("server_id_snowflake"), SQLDataType.BIGINT.nullable(false).defaultValue(DSL.field(DSL.raw("0"), SQLDataType.BIGINT)), this, "", new SnowflakeToLongConverter());

    /**
     * The column <code>atropos.server_messages.user_id</code>.
     */
    public final TableField<ServerMessagesRecord, Integer> USER_ID = createField(DSL.name("user_id"), SQLDataType.INTEGER.nullable(false).defaultValue(DSL.field(DSL.raw("0"), SQLDataType.INTEGER)), this, "");

    /**
     * The column <code>atropos.server_messages.user_id_snowflake</code>.
     */
    public final TableField<ServerMessagesRecord, Long> USER_ID_SNOWFLAKE = createField(DSL.name("user_id_snowflake"), SQLDataType.BIGINT.nullable(false).defaultValue(DSL.field(DSL.raw("0"), SQLDataType.BIGINT)), this, "", new SnowflakeToLongConverter());

    /**
     * The column <code>atropos.server_messages.date</code>.
     */
    public final TableField<ServerMessagesRecord, Instant> DATE = createField(DSL.name("date"), SQLDataType.INSTANT.nullable(false).defaultValue(DSL.field(DSL.raw("0"), SQLDataType.INSTANT)), this, "");

    /**
     * The column <code>atropos.server_messages.content</code>.
     */
    public final TableField<ServerMessagesRecord, String> CONTENT = createField(DSL.name("content"), SQLDataType.VARCHAR(4000).defaultValue(DSL.field(DSL.raw("NULL"), SQLDataType.VARCHAR)), this, "");

    /**
     * The column <code>atropos.server_messages.deleted</code>.
     */
    public final TableField<ServerMessagesRecord, Boolean> DELETED = createField(DSL.name("deleted"), SQLDataType.BOOLEAN.nullable(false).defaultValue(DSL.field(DSL.raw("0"), SQLDataType.BOOLEAN)), this, "");

    /**
     * The column <code>atropos.server_messages.message_data</code>.
     */
    public final TableField<ServerMessagesRecord, String> MESSAGE_DATA = createField(DSL.name("message_data"), SQLDataType.VARCHAR(4000).defaultValue(DSL.field(DSL.raw("NULL"), SQLDataType.VARCHAR)), this, "");

    private ServerMessages(Name alias, Table<ServerMessagesRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private ServerMessages(Name alias, Table<ServerMessagesRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>atropos.server_messages</code> table reference
     */
    public ServerMessages(String alias) {
        this(DSL.name(alias), SERVER_MESSAGES);
    }

    /**
     * Create an aliased <code>atropos.server_messages</code> table reference
     */
    public ServerMessages(Name alias) {
        this(alias, SERVER_MESSAGES);
    }

    /**
     * Create a <code>atropos.server_messages</code> table reference
     */
    public ServerMessages() {
        this(DSL.name("server_messages"), null);
    }

    public <O extends Record> ServerMessages(Table<O> path, ForeignKey<O, ServerMessagesRecord> childPath, InverseForeignKey<O, ServerMessagesRecord> parentPath) {
        super(path, childPath, parentPath, SERVER_MESSAGES);
    }

    /**
     * A subtype implementing {@link Path} for simplified path-based joins.
     */
    public static class ServerMessagesPath extends ServerMessages implements Path<ServerMessagesRecord> {

        private static final long serialVersionUID = 1L;
        public <O extends Record> ServerMessagesPath(Table<O> path, ForeignKey<O, ServerMessagesRecord> childPath, InverseForeignKey<O, ServerMessagesRecord> parentPath) {
            super(path, childPath, parentPath);
        }
        private ServerMessagesPath(Name alias, Table<ServerMessagesRecord> aliased) {
            super(alias, aliased);
        }

        @Override
        public ServerMessagesPath as(String alias) {
            return new ServerMessagesPath(DSL.name(alias), this);
        }

        @Override
        public ServerMessagesPath as(Name alias) {
            return new ServerMessagesPath(alias, this);
        }

        @Override
        public ServerMessagesPath as(Table<?> alias) {
            return new ServerMessagesPath(alias.getQualifiedName(), this);
        }
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Atropos.ATROPOS;
    }

    @Override
    public Identity<ServerMessagesRecord, Integer> getIdentity() {
        return (Identity<ServerMessagesRecord, Integer>) super.getIdentity();
    }

    @Override
    public UniqueKey<ServerMessagesRecord> getPrimaryKey() {
        return Keys.KEY_SERVER_MESSAGES_PRIMARY;
    }

    @Override
    public List<UniqueKey<ServerMessagesRecord>> getUniqueKeys() {
        return Arrays.asList(Keys.KEY_SERVER_MESSAGES_SERVER_MESSAGES_ID_UINDEX);
    }

    @Override
    public List<ForeignKey<ServerMessagesRecord, ?>> getReferences() {
        return Arrays.asList(Keys.SERVER_MESSAGES_SERVERS_ID_FK, Keys.SERVER_MESSAGES_USERS_ID_FK);
    }

    private transient ServersPath _servers;

    /**
     * Get the implicit join path to the <code>atropos.servers</code> table.
     */
    public ServersPath servers() {
        if (_servers == null)
            _servers = new ServersPath(this, Keys.SERVER_MESSAGES_SERVERS_ID_FK, null);

        return _servers;
    }

    private transient UsersPath _users;

    /**
     * Get the implicit join path to the <code>atropos.users</code> table.
     */
    public UsersPath users() {
        if (_users == null)
            _users = new UsersPath(this, Keys.SERVER_MESSAGES_USERS_ID_FK, null);

        return _users;
    }

    @Override
    public ServerMessages as(String alias) {
        return new ServerMessages(DSL.name(alias), this);
    }

    @Override
    public ServerMessages as(Name alias) {
        return new ServerMessages(alias, this);
    }

    @Override
    public ServerMessages as(Table<?> alias) {
        return new ServerMessages(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public ServerMessages rename(String name) {
        return new ServerMessages(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public ServerMessages rename(Name name) {
        return new ServerMessages(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public ServerMessages rename(Table<?> name) {
        return new ServerMessages(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ServerMessages where(Condition condition) {
        return new ServerMessages(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ServerMessages where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ServerMessages where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ServerMessages where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ServerMessages where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ServerMessages where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ServerMessages where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public ServerMessages where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ServerMessages whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public ServerMessages whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
