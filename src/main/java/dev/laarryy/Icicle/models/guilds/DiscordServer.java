package dev.laarryy.Icicle.models.guilds;

import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.annotations.Table;

import java.time.Instant;

@Table("servers")
public class DiscordServer extends Model {

    public int getServerId() {
        return getInteger("id");
    }

    public Instant getDateEntry() {
        return Instant.ofEpochMilli(getLong("date"));
    }

}