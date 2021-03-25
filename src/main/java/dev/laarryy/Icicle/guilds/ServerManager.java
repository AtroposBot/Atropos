package dev.laarryy.Icicle.guilds;

import net.dv8tion.jda.api.entities.Guild;

public abstract class ServerManager implements Guild {

    Guild guild;

    public ServerManager(Long guildId) {
        guild = getJDA().getGuildById(guildId);
    }

    public Guild getServerById(Long guildId) {
        return this.guild;
    }
}
