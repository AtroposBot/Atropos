package dev.laarryy.Eris.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Eris.managers.PropertiesCacheManager;
import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.utils.AddServerToDB;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import reactor.core.publisher.Mono;

public class MemberJoinListener {
    LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();
    private final Logger logger = LogManager.getLogger(this);
    private final AddServerToDB addServerToDB = new AddServerToDB();


    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {
        if (event.getGuild().block() == null) {
            return Mono.empty();
        }
        Guild guild = event.getGuild().block();

        DiscordServerProperties discordServerProperties = cache.get(guild.getId().asLong());

        if (discordServerProperties.getStopJoins()) {
            event.getMember().kick("Automatically kicked as part of anti-raid measures.").subscribe();
            return Mono.empty();
        }

        addServerToDB.addUserToDatabase(event.getMember(), event.getGuild().block());
        return Mono.empty();
    }
}
