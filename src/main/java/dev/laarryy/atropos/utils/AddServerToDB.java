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

import java.time.Instant;
import java.util.List;

public final class AddServerToDB {
    private static final Logger logger = LogManager.getLogger(Atropos.class);

    public AddServerToDB() {}

    public boolean addServerToDatabase(Guild guild) {

        long serverIdSnowflake = guild.getId().asLong();
        DatabaseLoader.openConnectionIfClosed();
        DiscordServer server = DiscordServer.findOrCreateIt("server_id", serverIdSnowflake);
        server.save();
        server.refresh();

        if (server.getDateEntry() == 0) {
            server.setDateEntry(Instant.now().toEpochMilli());
            server.save();
        }

        int serverId = server.getServerId();

        DiscordServerProperties properties = DiscordServerProperties.findOrCreateIt("server_id", serverId, "server_id_snowflake", serverIdSnowflake);

        properties.setServerName(guild.getName());
        properties.save();
        properties.refresh();

        if (properties.getMembersOnFirstJoin() == 0) {
            properties.setMembersOnFirstJoin(guild.getMemberCount());
            properties.save();
        }

        List<Member> unregisteredMembers = guild.getMembers()
                .filter(member -> {
                    DatabaseLoader.openConnectionIfClosed();
                    DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", member.getId().asLong());
                    ServerUser unregisteredUser;
                    if (discordUser != null) {
                        unregisteredUser = ServerUser.findFirst("user_id = ? and server_id = ?", discordUser.getUserId(), server.getServerId());
                    } else {
                        unregisteredUser = null;
                    }

                    return unregisteredUser == null;
                })
                .collectList().block();

        if (unregisteredMembers != null && !unregisteredMembers.isEmpty()) {
            Flux.fromIterable(unregisteredMembers)
                    .map(member -> addUserToDatabase(member, guild))
                    .subscribe();
        }

        this.addUserToDatabase(guild.getSelfMember().block(), guild);

        DatabaseLoader.openConnectionIfClosed();
        properties.refresh();
        server.refresh();

        DatabaseLoader.closeConnectionIfOpen();

        return true;
    }

    public boolean addUserToDatabase(Member member, Guild guild) {

        DatabaseLoader.openConnectionIfClosed();

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

        DatabaseLoader.closeConnectionIfOpen();

        return true;
    }
}
