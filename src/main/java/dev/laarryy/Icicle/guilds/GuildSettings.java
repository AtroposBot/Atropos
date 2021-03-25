package dev.laarryy.Icicle.guilds;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GuildSettings {

    private Long guildId;
    private Long logChannelId;
    private String guildCommandPrefix;

    public GuildSettings() {
        this.guildId = -1L;
        this.logChannelId = -1L;
        this.guildCommandPrefix = "!";
    }

    public @NonNull Long getGuildId() {
        return guildId;
    }

    public void setGuildId(@NonNull Long guildId) {
        this.guildId = guildId;
    }

    public @Nullable Long getLogChannelId() {
        return logChannelId;
    }

    public void setLogChannelId(@Nullable Long logChannelId) {
        this.logChannelId = logChannelId;
    }

    public @NonNull String getGuildCommandPrefix() {
        return guildCommandPrefix;
    }

    public void setGuildCommandPrefix(@NonNull String guildCommandPrefix) {
        this.guildCommandPrefix = guildCommandPrefix;
    }

}
