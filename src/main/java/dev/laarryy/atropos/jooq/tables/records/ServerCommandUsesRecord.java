/*
 * This file is generated by jOOQ.
 */
package dev.laarryy.atropos.jooq.tables.records;


import dev.laarryy.atropos.jooq.tables.ServerCommandUses;
import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;

import java.time.Instant;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ServerCommandUsesRecord extends UpdatableRecordImpl<ServerCommandUsesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>atropos.server_command_uses.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>atropos.server_command_uses.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>atropos.server_command_uses.server_id</code>.
     */
    public void setServerId(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>atropos.server_command_uses.server_id</code>.
     */
    public Integer getServerId() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>atropos.server_command_uses.command_user_id</code>.
     */
    public void setCommandUserId(Integer value) {
        set(2, value);
    }

    /**
     * Getter for <code>atropos.server_command_uses.command_user_id</code>.
     */
    public Integer getCommandUserId() {
        return (Integer) get(2);
    }

    /**
     * Setter for <code>atropos.server_command_uses.command_contents</code>.
     */
    public void setCommandContents(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>atropos.server_command_uses.command_contents</code>.
     */
    public String getCommandContents() {
        return (String) get(3);
    }

    /**
     * Setter for <code>atropos.server_command_uses.date</code>.
     */
    public void setDate(Instant value) {
        set(4, value);
    }

    /**
     * Getter for <code>atropos.server_command_uses.date</code>.
     */
    public Instant getDate() {
        return (Instant) get(4);
    }

    /**
     * Setter for <code>atropos.server_command_uses.success</code>.
     */
    public void setSuccess(Boolean value) {
        set(5, value);
    }

    /**
     * Getter for <code>atropos.server_command_uses.success</code>.
     */
    public Boolean getSuccess() {
        return (Boolean) get(5);
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
     * Create a detached ServerCommandUsesRecord
     */
    public ServerCommandUsesRecord() {
        super(ServerCommandUses.SERVER_COMMAND_USES);
    }

    /**
     * Create a detached, initialised ServerCommandUsesRecord
     */
    public ServerCommandUsesRecord(Integer id, Integer serverId, Integer commandUserId, String commandContents, Instant date, Boolean success) {
        super(ServerCommandUses.SERVER_COMMAND_USES);

        setId(id);
        setServerId(serverId);
        setCommandUserId(commandUserId);
        setCommandContents(commandContents);
        setDate(date);
        setSuccess(success);
        resetChangedOnNotNull();
    }
}
