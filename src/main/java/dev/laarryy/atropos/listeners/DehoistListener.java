package dev.laarryy.atropos.listeners;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.commands.punishments.PunishmentManager;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.managers.PunishmentManagerManager;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.utils.PermissionChecker;
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

    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {

        return event.getGuild().flatMap(guild -> {
            if (guild == null) {
                return Mono.empty();
            }

            return guild.getSelfMember().flatMap(selfMember -> {
                Member member = event.getMember();
                if (member.isBot()) {
                    return Mono.empty();
                }

                DiscordServerProperties properties = cache.get(event.getGuildId().asLong());

                if (properties.getDehoist()) {
                    return punishmentManager.onlyCheckIfPunisherHasHighestRole(selfMember, member, guild).flatMap(aBoolean -> {
                        if (!aBoolean) {
                            return Mono.empty();
                        } else {
                            return permissionChecker.checkBotPermission(selfMember).flatMap(theBoolean -> {
                                if (!theBoolean) {
                                    return loggingListener.onNoDehoistPermission(guild, member);
                                } else {
                                    return dehoist(member);
                                }
                            });
                        }
                    });
                }
                return Mono.empty();
            }).then();
        });
    }

    @EventListener
    public Mono<Void> on(MemberUpdateEvent event) {

        return event.getGuild().flatMap(guild -> {
            if (guild == null) {
                return Mono.empty();
            }

            return Mono.zip(event.getMember(), guild.getSelfMember(), (member, selfMember) -> {
                        if (member.isBot()) {
                            return Mono.empty();
                        }

                        DiscordServerProperties properties = cache.get(event.getGuildId().asLong());

                        if (properties.getDehoist()) {
                            return punishmentManager.onlyCheckIfPunisherHasHighestRole(selfMember, member, guild).flatMap(aBoolean -> {
                                if (!aBoolean) {
                                    return Mono.empty();
                                } else {
                                    return permissionChecker.checkBotPermission(selfMember).flatMap(theBoolean -> {
                                        if (!theBoolean) {
                                            return loggingListener.onNoDehoistPermission(guild, member);
                                        } else {
                                            return dehoist(member);
                                        }
                                    });
                                }
                            });
                        }
                        return Mono.empty();
                    }).flatMap($ -> $)
                    .then();
        });
    }

    @EventListener
    public Mono<Void> on(PresenceUpdateEvent event) {

        return event.getGuild().flatMap(guild -> {
            if (guild == null) {
                return Mono.empty();
            }

            return Mono.zip(event.getMember(), guild.getSelfMember(), (member, selfMember) -> {
                        if (member.isBot()) {
                            return Mono.empty();
                        }

                        DiscordServerProperties properties = cache.get(event.getGuildId().asLong());

                        if (properties.getDehoist()) {
                            return punishmentManager.onlyCheckIfPunisherHasHighestRole(selfMember, member, guild).flatMap(aBoolean -> {
                                if (!aBoolean) {
                                    return Mono.empty();
                                } else {
                                    return permissionChecker.checkBotPermission(selfMember).flatMap(theBoolean -> {
                                        if (!theBoolean) {
                                            return loggingListener.onNoDehoistPermission(guild, member);
                                        } else {
                                            return dehoist(member);
                                        }
                                    });
                                }
                            });
                        }
                        return Mono.empty();
                    }).flatMap($ -> $)
                    .then();
        });
    }

    private Mono<Void> dehoist(Member member) {
        String newName = checkAndDehoist(member.getDisplayName());
        if (newName.equals(member.getDisplayName())) {
            return Mono.empty();
        } else {
            return member.edit(GuildMemberEditSpec.builder().nicknameOrNull(newName).build()).then();
        }
    }

    private String checkAndDehoist(String name) {
        String newName = name;
        while (hoistChars.contains(newName.substring(0, 1))) {
            newName = name.substring(1);
        }
        return newName;
    }
}
