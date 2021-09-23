package dev.laarryy.Icicle.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.managers.LoggingListenerManager;
import dev.laarryy.Icicle.managers.PropertiesCacheManager;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.LogExecutor;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ChannelDeleteListener {

    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(TextChannelDeleteEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();
        Guild guild = event.getChannel().getGuild().block();
        if (guild == null) {
            return Mono.empty();
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
    }
}
