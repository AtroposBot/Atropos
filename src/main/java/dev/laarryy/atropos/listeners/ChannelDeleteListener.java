package dev.laarryy.atropos.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.object.entity.channel.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class ChannelDeleteListener {

    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(TextChannelDeleteEvent event) {
        LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();

        return event.getChannel().getGuild().flatMap(guild -> {
            if (guild == null) {
                return Mono.error(new NullServerException("No Server"));
            }

            DiscordServerProperties serverProperties = cache.get(guild.getId().asLong());
            if (serverProperties == null) {
                return Mono.empty();
            }

            TextChannel channel = event.getChannel();

            if (serverProperties.getMemberLogChannelSnowflake().equals(channel.getId().asLong())) {
                serverProperties.setMemberLogChannelSnowflake(null);
            }

            if (serverProperties.getMessageLogChannelSnowflake().equals(channel.getId().asLong())) {
                serverProperties.setMessageLogChannelSnowflake(null);
            }

            if (serverProperties.getGuildLogChannelSnowflake().equals(channel.getId().asLong())) {
                serverProperties.setGuildLogChannelSnowflake(null);
            }

            if (serverProperties.getPunishmentLogChannelSnowflake().equals(channel.getId().asLong())) {
                serverProperties.setPunishmentLogChannelSnowflake(null);
            }

            serverProperties.save();
            serverProperties.refresh();
            cache.invalidate(guild.getId().asLong());
            return Mono.empty();
        });
    }
}
