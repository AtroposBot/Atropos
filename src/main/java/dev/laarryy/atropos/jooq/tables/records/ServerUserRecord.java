/*
 * This file is generated by jOOQ.
 */
package dev.laarryy.atropos.jooq.tables.records;


import dev.laarryy.atropos.jooq.tables.ServerUser;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import java.time.Instant;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServerUserRecord extends UpdatableRecordImpl<ServerUserRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>atropos.server_user.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>atropos.server_user.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>atropos.server_user.user_id</code>.
     */
    public void setUserId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>atropos.server_user.user_id</code>.
     */
    public Integer getUserId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>atropos.server_user.server_id</code>.
     */
    public void setServerId(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>atropos.server_user.server_id</code>.
     */
    public Integer getServerId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>atropos.server_user.date</code>.
     */
    public void setDate(Instant value) {
        set(3, value);
    }

    /**
     * Getter for <code>atropos.server_user.date</code>.
     */
    public Instant getDate() {
        return (Instant) get(3);
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
     * Create a detached ServerUserRecord
     */
    public ServerUserRecord() {
        super(ServerUser.SERVER_USER);
    }

    /**
     * Create a detached, initialised ServerUserRecord
     */
    public ServerUserRecord(Integer id, Integer userId, Integer serverId, Instant date) {
        super(ServerUser.SERVER_USER);

        setId(id);
        setUserId(userId);
        setServerId(serverId);
        setDate(date);
        resetChangedOnNotNull();
    }
}
