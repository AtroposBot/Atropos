package dev.laarryy.eris.utils;

import dev.laarryy.eris.Eris;
import dev.laarryy.eris.models.guilds.DiscordServer;
import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import dev.laarryy.eris.models.joins.ServerUser;
import dev.laarryy.eris.models.users.DiscordUser;
import dev.laarryy.eris.storage.DatabaseLoader;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

public final class AddServerToDB {
    private static final Logger logger = LogManager.getLogger(Eris.class);

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
                    return DiscordUser.findFirst("user_id_snowflake = ?", member.getId().asLong()) == null;
                })
                .collectList().block();

        if (unregisteredMembers != null && !unregisteredMembers.isEmpty()) {
            Flux.fromIterable(unregisteredMembers)
                    .map(member -> addUserToDatabase(member, guild))
                    .subscribe();
        }

        this.addUserToDatabase(guild.getSelfMember().block(), guild);

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
        serverUser.refresh();

        if (serverUser.getDate() == 0 || serverUser.getDate() == null) {
            serverUser.setDate(Instant.now().toEpochMilli());
            serverUser.save();
        }

        return true;
    }
}