package dev.laarryy.Eris.listeners;

import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.VoiceChannelEditSpec;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class VoiceChannelCreateListener {

    @EventListener
    public Mono<Void> on(VoiceChannelCreateEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        Guild guild = event.getChannel().getGuild().block();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        if (discordServerProperties != null && discordServerProperties.getMutedRoleSnowflake() != null && discordServerProperties.getMutedRoleSnowflake() != 0) {
            guild.getRoleById(Snowflake.of(discordServerProperties.getMutedRoleSnowflake()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(role -> event.getChannel().edit(VoiceChannelEditSpec.builder()
                            .addPermissionOverwrite(PermissionOverwrite.forRole(role.getId(),
                                    PermissionSet.none(),
                                    PermissionSet.of(
                                            discord4j.rest.util.Permission.SPEAK,
                                            discord4j.rest.util.Permission.PRIORITY_SPEAKER,
                                            discord4j.rest.util.Permission.STREAM
                                    )))
                            .build()))
                    .subscribe();

        }
        return Mono.empty();
    }
}
