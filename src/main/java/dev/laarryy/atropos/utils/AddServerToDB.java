package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.Atropos;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static dev.laarryy.atropos.jooq.Tables.SERVERS;
import static dev.laarryy.atropos.jooq.Tables.SERVER_PROPERTIES;
import static dev.laarryy.atropos.jooq.Tables.SERVER_USER;
import static dev.laarryy.atropos.jooq.Tables.USERS;
import static dev.laarryy.atropos.storage.DatabaseLoader.sqlContext;
import static org.jooq.impl.DSL.select;

public final class AddServerToDB {
    private static final Logger logger = LogManager.getLogger(Atropos.class);

    public static Mono<Void> addServerToDatabase(Guild guild) {
        return Mono.fromDirect(sqlContext.insertInto(SERVERS)
                        .set(SERVERS.SERVER_ID, guild.getId())
                        .set(SERVERS.DATE, Instant.now())
                        .onDuplicateKeyIgnore()
                        .returning())
                .flatMap(server ->
                        Mono.fromDirect(sqlContext.update(SERVER_PROPERTIES)
                                        .set(SERVER_PROPERTIES.SERVER_NAME, guild.getName())
                                        .where(SERVER_PROPERTIES.SERVER_ID.eq(server.getId()))
                                        .and(SERVER_PROPERTIES.SERVER_ID_SNOWFLAKE.eq(guild.getId())))
                                .filter(i -> i > 0)
                                .switchIfEmpty(Mono.fromDirect(
                                        sqlContext.insertInto(SERVER_PROPERTIES)
                                                .set(SERVER_PROPERTIES.SERVER_ID, server.getId())
                                                .set(SERVER_PROPERTIES.SERVER_ID_SNOWFLAKE, guild.getId())
                                                .set(SERVER_PROPERTIES.SERVER_NAME, guild.getName())
                                                .set(SERVER_PROPERTIES.MEMBER_COUNT_ON_BOT_JOIN, (long) guild.getMemberCount())
                                ))
                                .thenReturn(server)
                )
                .flatMap(server ->
                        guild.getMembers()
                                .filterWhen(member ->
                                        Mono.fromDirect(sqlContext.selectOne()
                                                        .from(SERVER_USER)
                                                        .where(SERVER_USER.SERVER_ID.eq(server.getId()))
                                                        .and(SERVER_USER.USER_ID.in(select(USERS.ID).from(USERS))))
                                                .hasElement()
                                                .transform(BooleanUtils::not)
                                )
                                .flatMap(member -> addUserToDatabase(member, guild))
                                .then()
                );
    }

    public static Mono<Void> addUserToDatabase(Member member, Guild guild) {
        return Mono.fromDirect(sqlContext.insertInto(USERS)
                        .set(USERS.USER_ID_SNOWFLAKE, member.getId())
                        .set(USERS.DATE, Instant.now())
                        .onDuplicateKeyIgnore()
                        .returning())
                .flatMap(user ->
                        // attempt a simple select for those server_id and user_id
                        Mono.fromDirect(sqlContext.selectOne()
                                        .from(SERVER_USER)
                                        .where(SERVER_USER.USER_ID.eq(user.getId()))
                                        .and(SERVER_USER.SERVER_ID.in(select(SERVERS.ID).from(SERVERS))))
                                .map(result -> result.value1()) // bleh
                                // if it yields no results, insert a new entry,
                                // can't do insertInto(..)..onDuplicateKeyIgnore() because
                                // server_id/user_id are not marked as unique keys (should they?)
                                .switchIfEmpty(Mono.fromDirect(
                                        sqlContext.insertInto(SERVER_USER)
                                                .set(SERVER_USER.USER_ID, user.getId())
                                                .set(SERVER_USER.SERVER_ID, select(SERVERS.ID).from(SERVERS).where(SERVERS.SERVER_ID.eq(guild.getId())))
                                                .set(SERVER_USER.DATE, Instant.now())
                                ))
                )
                .then();
    }
}
