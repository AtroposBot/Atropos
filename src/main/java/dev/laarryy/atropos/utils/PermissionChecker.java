package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.exceptions.BotPermissionsException;
import dev.laarryy.atropos.exceptions.NullServerException;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import static dev.laarryy.atropos.jooq.Tables.PERMISSIONS;
import static dev.laarryy.atropos.jooq.Tables.SERVERS;
import static dev.laarryy.atropos.jooq.Tables.SERVER_ROLE_PERMISSIONS;
import static dev.laarryy.atropos.storage.DatabaseLoader.sqlContext;
import static java.util.function.Predicate.isEqual;
import static org.jooq.impl.DSL.select;

public final class PermissionChecker {
    private final Logger logger = LogManager.getLogger(this);

    /**
     * @param guild       Guild to check permission in
     * @param user        User to check permissions of
     * @param requestName Command name to check permission of
     * @return a true {@link Mono}<{@link Boolean}> if user has permission in guild, or an error signal indicating no permission
     */

    public Mono<Boolean> checkPermission(Guild guild, User user, String requestName) {
        Snowflake serverSnowflake = guild.getId();

        return user.asMember(serverSnowflake).flatMap(member -> checkIsAdministrator(member).flatMap(isAdmin -> {
            if (isAdmin) {
                return Mono.just(true);
            }

            return Mono.fromDirect(sqlContext.insertInto(PERMISSIONS)
                            .set(PERMISSIONS.PERMISSION, requestName)
                            .onDuplicateKeyIgnore())
                    .thenMany(guild.getRoles())
                    .filterWhen(role -> {
                        if (role.getPermissions().contains(Permission.ADMINISTRATOR)) {
                            return Mono.just(true);
                        }

                        return Mono.fromDirect(sqlContext.selectOne()
                                        .from(SERVER_ROLE_PERMISSIONS)
                                        .where(SERVER_ROLE_PERMISSIONS.ROLE_ID_SNOWFLAKE.eq(role.getId()))
                                        .and(SERVER_ROLE_PERMISSIONS.SERVER_ID.in(select(SERVERS.ID).from(SERVERS).where(SERVERS.SERVER_ID_SNOWFLAKE.eq(serverSnowflake))))
                                        .and(SERVER_ROLE_PERMISSIONS.PERMISSION_ID.in(select(PERMISSIONS.ID).from(PERMISSIONS).where(PERMISSIONS.PERMISSION.eq(requestName)))
                                                .or(SERVER_ROLE_PERMISSIONS.PERMISSION_ID.eq(69))))
                                .hasElement();
                    })
                    .flatMap(role ->
                            member.getRoles()
                                    .mergeWith(guild.getEveryoneRole())
                                    .any(isEqual(role))
                    )
                    .next();
        }));
    }

    /**
     * @param member A guild {@link Member} to check if administrator
     * @return A true {@link Mono}<{@link Boolean}> if the {@link Member} is an administrator or false if not
     */

    public Mono<Boolean> checkIsAdministrator(Member member) {

        return member.getRoles()
                .map(Role::getPermissions)
                .mergeWith(member.getBasePermissions())
                .any(permissions -> permissions.contains(Permission.ADMINISTRATOR));
    }

    public Mono<Boolean> checkBotPermission(ChatInputInteractionEvent event) {

        return event.getInteraction().getGuild()
                .switchIfEmpty(AuditLogger.addCommandToDB(event, false).then(Mono.error(new NullServerException("No Guild"))))
                .flatMap(guild -> guild.getSelfMember().flatMap(self ->
                        self.getBasePermissions().flatMap(basePerms -> {
                            if (basePerms.contains(Permission.ADMINISTRATOR)) {
                                return Mono.just(true);
                            }

                            PermissionSet requiredPermissions = PermissionSet.of(
                                    Permission.VIEW_CHANNEL,
                                    Permission.MANAGE_CHANNELS,
                                    Permission.MANAGE_ROLES,
                                    Permission.VIEW_AUDIT_LOG,
                                    Permission.MANAGE_NICKNAMES,
                                    //Permission.USE_PRIVATE_THREADS,
                                    //Permission.USE_PUBLIC_THREADS,
                                    Permission.KICK_MEMBERS,
                                    Permission.BAN_MEMBERS,
                                    Permission.SEND_MESSAGES,
                                    Permission.USE_EXTERNAL_EMOJIS,
                                    Permission.MANAGE_MESSAGES,
                                    Permission.READ_MESSAGE_HISTORY,
                                    Permission.MUTE_MEMBERS
                            );

                            return self.getBasePermissions().flatMap(selfPermissions -> {
                                if (selfPermissions.containsAll(requiredPermissions)) {
                                    return Mono.just(true);
                                }
                                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new BotPermissionsException("No Bot Permission")));
                            });
                        })));
    }

    public Mono<Boolean> checkBotPermission(Member selfMember) {


        return selfMember.getBasePermissions().flatMap(basePerms -> {
            if (basePerms.contains(Permission.ADMINISTRATOR)) {
                return Mono.just(true);
            }

            PermissionSet requiredPermissions = PermissionSet.of(
                    Permission.VIEW_CHANNEL,
                    Permission.MANAGE_CHANNELS,
                    Permission.MANAGE_ROLES,
                    Permission.VIEW_AUDIT_LOG,
                    Permission.MANAGE_NICKNAMES,
                    //Permission.USE_PRIVATE_THREADS,
                    //Permission.USE_PUBLIC_THREADS,
                    Permission.KICK_MEMBERS,
                    Permission.BAN_MEMBERS,
                    Permission.SEND_MESSAGES,
                    Permission.USE_EXTERNAL_EMOJIS,
                    Permission.MANAGE_MESSAGES,
                    Permission.READ_MESSAGE_HISTORY,
                    Permission.MUTE_MEMBERS
            );

            return selfMember.getBasePermissions()
                    .map(selfPermissions -> selfPermissions.containsAll(requiredPermissions))
                    .defaultIfEmpty(false);
        });
    }
}
