/*
 * This file is generated by jOOQ.
 */
package dev.laarryy.atropos.jooq.tables.records;


import dev.laarryy.atropos.jooq.tables.Users;
import discord4j.common.util.Snowflake;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import java.time.Instant;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class UsersRecord extends UpdatableRecordImpl<UsersRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>atropos.users.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>atropos.users.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>atropos.users.user_id_snowflake</code>.
     */
    public void setUserIdSnowflake(Snowflake value) {
        set(1, value);
    }

    /**
     * Getter for <code>atropos.users.user_id_snowflake</code>.
     */
    public Snowflake getUserIdSnowflake() {
        return (Snowflake) get(1);
    }

    /**
     * Setter for <code>atropos.users.date</code>.
     */
    public void setDate(Instant value) {
        set(2, value);
    }

    /**
     * Getter for <code>atropos.users.date</code>.
     */
    public Instant getDate() {
        return (Instant) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached UsersRecord
     */
    public UsersRecord() {
        super(Users.USERS);
    }

    /**
     * Create a detached, initialised UsersRecord
     */
    public UsersRecord(Integer id, Snowflake userIdSnowflake, Instant date) {
        super(Users.USERS);

        setId(id);
        setUserIdSnowflake(userIdSnowflake);
        setDate(date);
        resetChangedOnNotNull();
    }
}
