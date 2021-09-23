package dev.laarryy.Icicle.listeners;

import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.channel.TextChannelCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.TextChannelEditSpec;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class TextChannelCreateListener {

    @EventListener
    public Mono<Void> on(TextChannelCreateEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        Guild guild = event.getChannel().getGuild().block();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        if (discordServerProperties != null && discordServerProperties.getMutedRoleSnowflake() != null && discordServerProperties.getMutedRoleSnowflake() != 0) {
            guild.getRoleById(Snowflake.of(discordServerProperties.getMutedRoleSnowflake()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(role -> event.getChannel().edit((TextChannelEditSpec.builder()
                            .addPermissionOverwrite(PermissionOverwrite.forRole(role.getId(),
                                    PermissionSet.none(),
                                    PermissionSet.of(
                                            discord4j.rest.util.Permission.SEND_MESSAGES,
                                            discord4j.rest.util.Permission.ADD_REACTIONS,
                                            discord4j.rest.util.Permission.CHANGE_NICKNAME
                                    )))
                            .build())))
                    .subscribe();

        }
        return Mono.empty();
    }
}
