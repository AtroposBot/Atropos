package dev.laarryy.atropos.models.guilds;

import dev.laarryy.atropos.jooq.tables.records.ServerBlacklistRecord;

import java.util.regex.Pattern;

public class Blacklist {
    Pattern pattern;
    ServerBlacklistRecord serverBlacklist;

    public Blacklist(ServerBlacklistRecord serverBlacklist) {
        this.serverBlacklist = serverBlacklist;
        this.pattern = Pattern.compile((("(?:.*)?" + serverBlacklist.getRegexTrigger() + "(?:.*)?")),
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    }

    public Pattern getPattern() {
        return this.pattern;
    }

    public ServerBlacklistRecord getServerBlacklist() {
        return this.serverBlacklist;
    }
}
