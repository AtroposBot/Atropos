package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.channel.TextChannelCreateEvent;
import discord4j.core.object.ExtendedPermissionOverwrite;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelEditSpec;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Set;

public class TextChannelCreateListener {

    private final Logger logger = LogManager.getLogger(this);

    @EventListener
    public Mono<Void> on(TextChannelCreateEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        Guild guild = event.getChannel().getGuild().block();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        if (discordServerProperties != null && discordServerProperties.getMutedRoleSnowflake() != null && discordServerProperties.getMutedRoleSnowflake() != 0) {
            try {
                guild.getRoleById(Snowflake.of(discordServerProperties.getMutedRoleSnowflake()))
                                .flatMap(role -> {
                                    //TODO: Fix to be cleaner as D4J #1009 is resolved
                                    TextChannel textChannel = event.getChannel();
                                    Set<ExtendedPermissionOverwrite> overwrites = textChannel.getPermissionOverwrites();
                                    Set<PermissionOverwrite> newOverwrites = new HashSet<>(overwrites);
                                    newOverwrites.add(PermissionOverwrite.forRole(role.getId(),
                                            PermissionSet.none(),
                                            PermissionSet.of(
                                                    Permission.SEND_MESSAGES,
                                                    Permission.ADD_REACTIONS,
                                                    Permission.CHANGE_NICKNAME,
                                                   // Permission.USE_PUBLIC_THREADS,
                                                   // Permission.USE_PRIVATE_THREADS,
                                                    Permission.USE_SLASH_COMMANDS
                                            )));
                                    return textChannel.edit(TextChannelEditSpec.builder()
                                                    .addAllPermissionOverwrites(newOverwrites.stream().toList())
                                                    .build())
                                            .onErrorResume(e -> {
                                                logger.info("Error in TextChannelCreate Listener: " + e.getMessage());
                                                logger.info(e.getStackTrace());
                                                return Mono.empty();
                                            });
                                }
                        )
                        .subscribe();

            } catch (Exception ignored) {}
        }
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
