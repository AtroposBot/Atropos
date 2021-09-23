package dev.laarryy.Icicle.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.listeners.logging.LoggingListener;
import dev.laarryy.Icicle.managers.LoggingListenerManager;
import dev.laarryy.Icicle.managers.PropertiesCacheManager;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class RoleDeleteListener {

    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(RoleDeleteEvent event) {
        if (event.getGuild().block() == null) {
            return Mono.empty();
        }

        DatabaseLoader.openConnectionIfClosed();
        Guild guild = event.getGuild().block();
        Role role = event.getRole().orElse(null);
        if (role == null) {
            return Mono.empty();
        }

        LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();

        DiscordServerProperties serverProperties = cache.get(guild.getId().asLong());

        if (serverProperties == null) {
            return Mono.empty();
        }

        Long mutedRole = serverProperties.getMutedRoleSnowflake();
        if (mutedRole == null || mutedRole == 0) {
            return Mono.empty();
        }

        if (mutedRole.equals(role.getId().asLong())) {
            serverProperties.setMutedRoleSnowflake(null);
            serverProperties.save();
            serverProperties.refresh();
            cache.invalidate(guild.getId().asLong());
            LoggingListener listener = LoggingListenerManager.getManager().getLoggingListener();
            listener.onMutedRoleDelete(guild, role);
        }
        return Mono.empty();
    }
}
