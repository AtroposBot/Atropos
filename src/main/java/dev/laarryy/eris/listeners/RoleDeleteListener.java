package dev.laarryy.eris.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.eris.listeners.logging.LoggingListener;
import dev.laarryy.eris.managers.LoggingListenerManager;
import dev.laarryy.eris.managers.PropertiesCacheManager;
import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import dev.laarryy.eris.storage.DatabaseLoader;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class RoleDeleteListener {

    private final Logger logger = LogManager.getLogger(this);
    LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();

    @EventListener
    public Mono<Void> on(RoleDeleteEvent event) {
        if (event.getGuild().block() == null) {
            return Mono.empty();
        }

        DatabaseLoader.openConnectionIfClosed();
        Guild guild = event.getGuild().block();
        Long roleId = event.getRoleId().asLong();

        DiscordServerProperties serverProperties = cache.get(guild.getId().asLong());

        if (serverProperties == null) {
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        Long mutedRole = serverProperties.getMutedRoleSnowflake();
        if (mutedRole == null || mutedRole == 0) {
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (mutedRole.equals(roleId)) {
            serverProperties.setMutedRoleSnowflake(null);
            serverProperties.save();
            serverProperties.refresh();
            cache.invalidate(guild.getId().asLong());
            LoggingListener listener = LoggingListenerManager.getManager().getLoggingListener();
            listener.onMutedRoleDelete(guild, roleId);
        }
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
