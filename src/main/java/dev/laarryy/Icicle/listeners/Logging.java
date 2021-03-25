package dev.laarryy.Icicle.listeners;

import dev.laarryy.Icicle.models.Server;
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

    DatabaseLoader loader;
    JDA api;
    private final Logger logger = LogManager.getLogger(this);
    Server server;


    public Logging(JDA api) throws SerializationException {
        this.api = api;
    }

    public void onMessageDelete(MessageDeleteEvent event) {

    }

    public void onGuildJoin(GuildJoinEvent event) {
        logger.debug("Joined New Guild!");
        logger.debug("Guild ID = " + event.getGuild().getIdLong());
        try {
            loader.openConnectionIfClosed();
        } catch (NullPointerException e) {
            logger.debug("Null on #openConnectionIfClosed");
            logger.debug(e.getMessage() + "\n" + "\n");
            logger.debug(e.getStackTrace());

            try {
                loader.openConnection();
            } catch (Exception ex) {
                logger.debug("Null on #openConnection");
                logger.debug(ex.getMessage() + "\n" + "\n");
                logger.debug(ex.getStackTrace());
            }
        }
        Server server = new Server();
        server.set("guild_id", event.getGuild().getIdLong());
        server.set("guild_join_instant", Instant.now());
        server.set("guild_member_count_on_join", event.getGuild().getMemberCount());
        server.saveIt();
    }
}
