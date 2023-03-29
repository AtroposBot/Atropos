/*
 * This file is generated by jOOQ.
 */
package dev.laarryy.atropos.jooq.tables;


import dev.laarryy.atropos.jooq.Indexes;
import dev.laarryy.atropos.jooq.Keys;
import dev.laarryy.atropos.jooq.tables.records.ServerBlacklistRecord;

import java.util.Arrays;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Function5;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Records;
import org.jooq.Row5;
import org.jooq.Schema;
import org.jooq.SelectField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServerBlacklist extends TableImpl<ServerBlacklistRecord> {

    private static final long serialVersionUID = 1L;

    public static final ServerBlacklist SERVER_BLACKLIST = new ServerBlacklist();

    @Override
    @NonNull
    public Class<ServerBlacklistRecord> getRecordType() {
        return ServerBlacklistRecord.class;
    }

    public final TableField<ServerBlacklistRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    public final TableField<ServerBlacklistRecord, Integer> SERVER_ID = createField(DSL.name("server_id"), SQLDataType.INTEGER.nullable(false), this, "");

    public final TableField<ServerBlacklistRecord, String> REGEX_TRIGGER = createField(DSL.name("regex_trigger"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    public final TableField<ServerBlacklistRecord, String> TYPE = createField(DSL.name("type"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    public final TableField<ServerBlacklistRecord, String> ACTION = createField(DSL.name("action"), SQLDataType.VARCHAR(255).nullable(false), this, "");

    private ServerBlacklist(Name alias, Table<ServerBlacklistRecord> aliased) {
        this(alias, aliased, null);
    }

    private ServerBlacklist(Name alias, Table<ServerBlacklistRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    public ServerBlacklist(String alias) {
        this(DSL.name(alias), SERVER_BLACKLIST);
    }

    public ServerBlacklist(Name alias) {
        this(alias, SERVER_BLACKLIST);
    }

    public ServerBlacklist() {
        this(DSL.name("server_blacklist"), null);
    }

    public <O extends Record> ServerBlacklist(Table<O> child, ForeignKey<O, ServerBlacklistRecord> key) {
        super(child, key, SERVER_BLACKLIST);
    }

    @Override
    @Nullable
    public Schema getSchema() {
        return null;
    }

    @Override
    @NonNull
    public List<Index> getIndexes() {
        return Arrays.asList(Indexes.SERVER_BLACKLIST_SERVER_ID);
    }

    @Override
    @NonNull
    public Identity<ServerBlacklistRecord, Integer> getIdentity() {
        return (Identity<ServerBlacklistRecord, Integer>) super.getIdentity();
    }

    @Override
    @NonNull
    public UniqueKey<ServerBlacklistRecord> getPrimaryKey() {
        return Keys.KEY_SERVER_BLACKLIST_PRIMARY;
    }

    @Override
    @NonNull
    public List<ForeignKey<ServerBlacklistRecord, ?>> getReferences() {
        return Arrays.asList(Keys.SERVER_BLACKLIST_IBFK_1);
    }

    private transient Servers _servers;

    public Servers servers() {
        if (_servers == null)
            _servers = new Servers(this, Keys.SERVER_BLACKLIST_IBFK_1);

        return _servers;
    }

    @Override
    @NonNull
    public ServerBlacklist as(String alias) {
        return new ServerBlacklist(DSL.name(alias), this);
    }

    @Override
    @NonNull
    public ServerBlacklist as(Name alias) {
        return new ServerBlacklist(alias, this);
    }

    @Override
    @NonNull
    public ServerBlacklist as(Table<?> alias) {
        return new ServerBlacklist(alias.getQualifiedName(), this);
    }

    @Override
    @NonNull
    public ServerBlacklist rename(String name) {
        return new ServerBlacklist(DSL.name(name), null);
    }

    @Override
    @NonNull
    public ServerBlacklist rename(Name name) {
        return new ServerBlacklist(name, null);
    }

    @Override
    @NonNull
    public ServerBlacklist rename(Table<?> name) {
        return new ServerBlacklist(name.getQualifiedName(), null);
    }

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------

    @Override
    @NonNull
    public Row5<Integer, Integer, String, String, String> fieldsRow() {
        return (Row5) super.fieldsRow();
    }

    public <U> SelectField<U> mapping(Function5<? super Integer, ? super Integer, ? super String, ? super String, ? super String, ? extends U> from) {
        return convertFrom(Records.mapping(from));
    }

    public <U> SelectField<U> mapping(Class<U> toType, Function5<? super Integer, ? super Integer, ? super String, ? super String, ? super String, ? extends U> from) {
        return convertFrom(toType, Records.mapping(from));
    }
}
