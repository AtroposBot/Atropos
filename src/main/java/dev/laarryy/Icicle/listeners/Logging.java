package dev.laarryy.Icicle.listeners;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.configurate.serialize.SerializationException;

import java.time.Instant;

public class Logging extends ListenerAdapter {

    JDA api;
    private final Logger logger = LogManager.getLogger(this);

    public Logging(JDA api) throws SerializationException {
        this.api = api;
    }

    public void onMessageDelete(MessageDeleteEvent event) {

    }

    public void onGuildJoin(GuildJoinEvent event) {
        DatabaseLoader.openConnectionIfClosed();

        DiscordServer server = DiscordServer.findOrCreateIt("server_id", event.getGuild().getIdLong());
        server.set("name", event.getGuild().getName());
        server.set("date", Instant.now().toEpochMilli());

        server.saveIt();
    }
}
