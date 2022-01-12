package dev.laarryy.eris.listeners;

import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import dev.laarryy.eris.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.channel.TextChannelCreateEvent;
import discord4j.core.object.ExtendedPermissionOverwrite;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.TextChannelEditSpec;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Set;

public class TextChannelCreateListener {

    @EventListener
    public Mono<Void> on(TextChannelCreateEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        Guild guild = event.getChannel().getGuild().block();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        if (discordServerProperties != null && discordServerProperties.getMutedRoleSnowflake() != null && discordServerProperties.getMutedRoleSnowflake() != 0) {
            guild.getRoleById(Snowflake.of(discordServerProperties.getMutedRoleSnowflake()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(role -> {
                        //TODO: Fix to be cleaner as D4J #1009 is resolved
                        TextChannel textChannel = event.getChannel();
                            Set<ExtendedPermissionOverwrite> overwrites = textChannel.getPermissionOverwrites();
                            Set<PermissionOverwrite> newOverwrites = new HashSet<>(overwrites);
                            newOverwrites.add(PermissionOverwrite.forRole(role.getId(),
                                    PermissionSet.none(),
                                    PermissionSet.of(
                                            discord4j.rest.util.Permission.SEND_MESSAGES,
                                            discord4j.rest.util.Permission.ADD_REACTIONS,
                                            discord4j.rest.util.Permission.CHANGE_NICKNAME
                                    )));
                            try {
                                textChannel.edit(TextChannelEditSpec.builder()
                                        .addAllPermissionOverwrites(newOverwrites.stream().toList())
                                        .build())
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe();
                            } catch (Exception ignored) {}

                            return Mono.empty();
                            }
                    )
                    .subscribe();

        }
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
