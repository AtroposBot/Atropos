package dev.laarryy.Eris.listeners;

import dev.laarryy.Eris.utils.AddServerToDB;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.entity.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import reactor.core.publisher.Mono;

public class GuildJoinListener {
    private final Logger logger = LogManager.getLogger(this);
    private AddServerToDB addServerToDB = new AddServerToDB();

    @EventListener
    public Mono<Void> on(GuildCreateEvent event) {

        logger.info("Joining Guild: " + event.getGuild().getName());

        Guild guild = event.getGuild();

        addServerToDB.addServerToDatabase(guild);

        return Mono.empty();
    }
}
