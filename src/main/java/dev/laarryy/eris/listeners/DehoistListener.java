package dev.laarryy.eris.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.eris.listeners.logging.LoggingListener;
import dev.laarryy.eris.managers.LoggingListenerManager;
import dev.laarryy.eris.managers.PropertiesCacheManager;
import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.GuildMemberEditSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;

public class DehoistListener {
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();
    private final Logger logger = LogManager.getLogger(this);
    private final List<String> hoistChars = List.of("!", "\"", "#", "$", "%", "&", "'", "(", ")", "*", "+", ",", "-", ".", "/");

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {
        if (event.getGuild().block() == null) {
            return Mono.empty();
        }

        if (event.getMember().isBot()) {
            return Mono.empty();
        }

        DiscordServerProperties properties = cache.get(event.getGuildId().asLong());

        if (properties.getDehoist()) {
            dehoist(event.getMember());
        }

        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(MemberUpdateEvent event) {
        if (event.getGuild().block() == null) {
            return Mono.empty();
        }

        if (event.getMember().block().isBot()) {
            return Mono.empty();
        }

        DiscordServerProperties properties = cache.get(event.getGuildId().asLong());

        if (properties.getDehoist()) {
            dehoist(event.getMember().block());
        }
        return Mono.empty();
    }

    @EventListener
    public Mono<Void> on(PresenceUpdateEvent event) {
        if (event.getGuild().block() == null) {
            return Mono.empty();
        }

        if (event.getMember().block().isBot()) {
            return Mono.empty();
        }

        DiscordServerProperties properties = cache.get(event.getGuildId().asLong());

        if (properties.getDehoist()) {
            dehoist(event.getMember().block());
        }
        return Mono.empty();
    }

    private void dehoist(Member member) {
        String newName = checkAndDehoist(member.getDisplayName());
        if (newName.equals(member.getDisplayName())) {
            return;
        } else {
            member.edit(GuildMemberEditSpec.builder().nickname(newName).build()).subscribe();
        }
    }

    private String checkAndDehoist(String name) {
        String newName = name;
        while (hoistChars.contains(newName.substring(0,1))) {
            newName = name.substring(1);
        }

        return newName;
    }
}
