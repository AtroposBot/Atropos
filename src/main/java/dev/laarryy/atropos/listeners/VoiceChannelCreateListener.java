package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent;
import discord4j.core.object.ExtendedPermissionOverwrite;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.VoiceChannelEditSpec;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Set;

public class VoiceChannelCreateListener {

    @EventListener
    public Mono<Void> on(VoiceChannelCreateEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        Guild guild = event.getChannel().getGuild().block();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        if (discordServerProperties != null && discordServerProperties.getMutedRoleSnowflake() != null && discordServerProperties.getMutedRoleSnowflake() != 0) {
            guild.getRoleById(Snowflake.of(discordServerProperties.getMutedRoleSnowflake()))
                        .flatMap(role -> {
                        VoiceChannel voiceChannel = event.getChannel();
                        Set<ExtendedPermissionOverwrite> overwrites = voiceChannel.getPermissionOverwrites();
                        Set<PermissionOverwrite> newOverwrites = new HashSet<>(overwrites);
                        newOverwrites.add(PermissionOverwrite.forRole(role.getId(),
                                PermissionSet.none(),
                                PermissionSet.of(
                                        discord4j.rest.util.Permission.SPEAK,
                                        discord4j.rest.util.Permission.PRIORITY_SPEAKER,
                                        discord4j.rest.util.Permission.STREAM
                                )));
                        try {
                            voiceChannel.edit(VoiceChannelEditSpec.builder()
                                            .addAllPermissionOverwrites(newOverwrites.stream().toList())
                                            .build())
                                                        .subscribe();
                        } catch (Exception ignored) {}

                        return Mono.empty();
                    })
                    .subscribe();

        }
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }
}
