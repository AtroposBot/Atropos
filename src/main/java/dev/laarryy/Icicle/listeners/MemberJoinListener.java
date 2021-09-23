package dev.laarryy.Icicle.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.managers.PropertiesCacheManager;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.utils.AddServerToDB;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class MemberJoinListener {
    LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();
    private final Logger logger = LogManager.getLogger(this);


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

        AddServerToDB.addUserToDatabase(event.getMember(), event.getGuild().block());
        return Mono.empty();
    }
}
