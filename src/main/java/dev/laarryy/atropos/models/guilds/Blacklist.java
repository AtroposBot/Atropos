package dev.laarryy.atropos.models.guilds;

import java.util.regex.Pattern;

public class Blacklist {
    Pattern pattern;
    ServerBlacklist serverBlacklist;

    public Blacklist(ServerBlacklist serverBlacklist) {
        this.serverBlacklist = serverBlacklist;
        this.pattern = Pattern.compile((("(?:.*)?" + serverBlacklist.getTrigger() + "(?:.*)?")),
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    }

    public Pattern getPattern() {
        return this.pattern;
    }

    public ServerBlacklist getServerBlacklist() {
        return this.serverBlacklist;
    }
}


