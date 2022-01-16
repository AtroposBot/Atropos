package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.utils.AddServerToDB;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
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

        // Add Early Adopter Role - quick n dirty because I'll remove at release

        if (event.getGuild().getId().asString().equals("931389256180580433")) {
            for (Member guildMember : guild.getMembers().collectList().block()) {
                for (Guild botGuild : event.getClient().getGuilds().collectList().block()) {
                    if (botGuild.getOwner().block().equals(guildMember)) {
                        guildMember.addRole(Snowflake.of("931389913503506502")).block();
                    }
                }
            }
        }

        return Mono.empty();
    }
}
