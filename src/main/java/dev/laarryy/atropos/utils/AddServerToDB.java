package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.Atropos;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.models.joins.ServerUser;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public final class AddServerToDB {
    private static final Logger logger = LogManager.getLogger(Atropos.class);

    public Mono<Void> addServerToDatabase(Guild guild) {

        long serverIdSnowflake = guild.getId().asLong();

        DiscordServer server = DatabaseLoader.use(() -> {
            DiscordServer server1 = DiscordServer.findOrCreateIt("server_id", serverIdSnowflake);
            server1.save();
            server1.refresh();
            if (server1.getDateEntry() == 0) {
                server1.setDateEntry(Instant.now().toEpochMilli());
                server1.save();
            }
            return server1;
        });


        int serverId = server.getServerId();

        DiscordServerProperties properties = DatabaseLoader.use(() -> {
            DiscordServerProperties properties1 = DiscordServerProperties.findOrCreateIt("server_id", serverId, "server_id_snowflake", serverIdSnowflake);
            properties1.setServerName(guild.getName());
            properties1.save();
            properties1.refresh();

            if (properties1.getMembersOnFirstJoin() == 0) {
                properties1.setMembersOnFirstJoin(guild.getMemberCount());
                properties1.save();
            }

            return properties1;
        });


        Mono<Void> registerMembers = guild.getMembers()
                .filter(member -> {
                    try (final var usage = DatabaseLoader.use()) {
                        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", member.getId().asLong());
                        ServerUser unregisteredUser;
                        if (discordUser != null) {
                            unregisteredUser = ServerUser.findFirst("user_id = ? and server_id = ?", discordUser.getUserId(), server.getServerId());
                        } else {
                            unregisteredUser = null;
                        }
                        return unregisteredUser == null;
                    }
                })
                .flatMap(member -> addUserToDatabase(member, guild))
                .then();

        return registerMembers;
    }

    public Mono<Void> addUserToDatabase(Member member, Guild guild) {
        try (final var usage = DatabaseLoader.use()) {
            long userIdSnowflake = member.getId().asLong();
            long serverIdSnowflake = guild.getId().asLong();

            DiscordUser user = DiscordUser.findOrCreateIt("user_id_snowflake", userIdSnowflake);
            user.save();
            user.refresh();

            if (user.getDateEntry() == 0) {
                user.setDateEntry(Instant.now().toEpochMilli());
                user.save();
            }

            DiscordServer server = DiscordServer.findOrCreateIt("server_id", serverIdSnowflake);

            int serverId = server.getServerId();
            int userId = user.getUserId();

            ServerUser serverUser = ServerUser.findOrCreateIt("user_id", userId, "server_id", serverId);
            serverUser.save();

            if (serverUser.getDate() == null || serverUser.getDate() == 0) {
                serverUser.setDate(Instant.now().toEpochMilli());
                serverUser.save();
            }

            serverUser.refresh();
        }

        return Mono.empty();

    }
}
