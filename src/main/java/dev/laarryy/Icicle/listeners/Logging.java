package dev.laarryy.Icicle.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class Logging {

    private final Logger logger = LogManager.getLogger(this);
    private GatewayDiscordClient client = null;

    private Logging(GatewayDiscordClient client) {
        this.client = client;
    }


    //    public void onGuildJoin(GuildJoinEvent event) {
//        DatabaseLoader.openConnectionIfClosed();
//
//        DiscordServer server = DiscordServer.findOrCreateIt("server_id", event.getGuild().getIdLong());
//        server.set("name", event.getGuild().getName());
//        server.set("date", Instant.now().toEpochMilli());
//
//        server.saveIt();
//    }
}
