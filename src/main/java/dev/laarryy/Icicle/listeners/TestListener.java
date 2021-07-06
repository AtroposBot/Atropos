package dev.laarryy.Icicle.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class TestListener {

    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(final MessageCreateEvent event) {
        if (event.getMessage().getContent().equals("!ping"))
            event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createMessage("Pong!")).subscribe();
        return Mono.empty();
    }
//        DatabaseLoader.openConnectionIfClosed();
//
//        DiscordServer server = DiscordServer.findOrCreateIt("server_id", event.getGuild().getIdLong());
//        server.set("name", event.getGuild().getName());
//        server.set("date", Instant.now().toEpochMilli());
//
//        server.saveIt();

}
