package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.utils.AddServerToDB;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.entity.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class GuildJoinListener {
    private final Logger logger = LogManager.getLogger(this);
    private final AddServerToDB addServerToDB = new AddServerToDB();

    @EventListener
    public Mono<Void> on(GuildCreateEvent event) {

        logger.info("Joining Guild: " + event.getGuild().getName());

        Guild guild = event.getGuild();

        addServerToDB.addServerToDatabase(guild);

        return Mono.empty();
    }
}
