package dev.laarryy.Icicle.listeners;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.joins.ServerUser;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class MemberJoinListener {

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {
        if (event.getGuild().block() == null) {
            return Mono.empty();
        }

        Member member = event.getMember();
        Guild guild = event.getGuild().block();

        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", member.getId().asLong());
        if (discordUser == null) {
            discordUser = DiscordUser.create("user_id_snowflake", member.getId().asLong());
        }
        discordUser.save();
        discordUser.refresh();
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
        ServerUser serverUser = ServerUser.findFirst("user_id = ? and server_id = ?", discordUser.getUserId(), discordServer.getServerId());
        if (serverUser == null) {
            serverUser = ServerUser.create("user_id", discordUser.getUserId(), "server_id", discordServer.getServerId(), "date", Instant.now().toEpochMilli());
        }
        serverUser.save();
        return Mono.empty();
    }
}
