package dev.laarryy.atropos.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
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

        return event.getGuild().flatMap(guild -> {
            if (guild == null) {
                return Mono.empty();
            }

            if (event.getMember().isBot()) {
                return Mono.empty();
            }

            DiscordServerProperties properties = cache.get(event.getGuildId().asLong());

            if (properties.getDehoist()) {
                return dehoist(event.getMember());
            }

            return Mono.empty();
        });
    }

    @EventListener
    public Mono<Void> on(MemberUpdateEvent event) {

        return event.getGuild().flatMap(guild -> {
            if (guild == null) {
                return Mono.empty();
            }

            return event.getMember().flatMap(member -> {
                if (member.isBot()) {
                    return Mono.empty();
                }

                DiscordServerProperties properties = cache.get(event.getGuildId().asLong());

                if (properties.getDehoist()) {
                    return dehoist(member);
                }
                return Mono.empty();
            });
        });
    }

    @EventListener
    public Mono<Void> on(PresenceUpdateEvent event) {

        return event.getGuild().flatMap(guild -> {
            if (guild == null) {
                return Mono.empty();
            }

            return event.getMember().flatMap(member -> {
                if (member.isBot()) {
                    return Mono.empty();
                }

                DiscordServerProperties properties = cache.get(event.getGuildId().asLong());

                if (properties.getDehoist()) {
                    return dehoist(member);
                }
                return Mono.empty();
            });
        });
    }

    private Mono<Void> dehoist(Member member) {
        String newName = checkAndDehoist(member.getDisplayName());
        if (newName.equals(member.getDisplayName())) {
            return Mono.empty();
        } else {
            return member.edit(GuildMemberEditSpec.builder().nickname(newName).build()).then();
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
