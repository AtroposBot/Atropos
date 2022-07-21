package dev.laarryy.atropos.commands.punishments;

import dev.laarryy.atropos.exceptions.AlreadyAppliedException;
import dev.laarryy.atropos.exceptions.BotPermissionsException;
import dev.laarryy.atropos.exceptions.BotRoleException;
import dev.laarryy.atropos.exceptions.CannotTargetBotsException;
import dev.laarryy.atropos.exceptions.InvalidDurationException;
import dev.laarryy.atropos.exceptions.NoMemberException;
import dev.laarryy.atropos.exceptions.NoPermissionsException;
import dev.laarryy.atropos.exceptions.NoUserException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.DurationParser;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.RoleData;
import discord4j.rest.util.OrderUtil;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PunishmentManager {
    private final Logger logger = LogManager.getLogger(this);
    PermissionChecker permissionChecker = new PermissionChecker();
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();

    public Mono<Void> doPunishment(ApplicationCommandRequest request, ChatInputInteractionEvent event) {


        return event.getInteraction().getGuild()
                .flatMap(guild -> {

                    logger.info("1");

                    // Make sure this is done in a guild or else stop right here.

                    if (guild == null || event.getInteraction().getMember().isEmpty()) {
                        return event.reply("This must be done in a guild.").withEphemeral(true);
                    }

                    Member member = event.getInteraction().getMember().get();
                    User user = event.getInteraction().getUser();
                    Long punishingUserIdSnowflake = member.getId().asLong();

                    DatabaseLoader.openConnectionIfClosed();

                    logger.info("2");

                    // Make sure user has permission to do this, or stop here
                    return permissionChecker.checkPermission(guild, user, request.name()).flatMap(aBoolean -> {
                        if (!aBoolean) {
                            return Mono.error(new NoPermissionsException("No Permission"));
                        }

                        logger.info("3");

                        Long guildIdSnowflake = guild.getId().asLong();
                        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildIdSnowflake);
                        int serverId;

                        if (discordServer != null) {
                            serverId = discordServer.getServerId();
                        } else {
                            DatabaseLoader.closeConnectionIfOpen();
                            return Mono.error(new NullServerException("Null Server"));
                        }

                        logger.info("4");

                        // Handle forceban via API before dealing with literally every other punishment (that can actually provide a user)

                        if (event.getOption("id").isPresent() && event.getOption("id").get().getValue().isPresent()) {

                            logger.info("4.1");

                            String idInput = event.getOption("id").get().getValue().get().asString();

                            String reason;
                            if (event.getOption("reason").isPresent() && event.getOption("reason").get().getValue().isPresent()) {
                                reason = event.getOption("reason").get().getValue().get().asString();
                            } else {
                                reason = "Mass API banned by staff.";
                            }

                            Punishment latestBatch = Punishment.findFirst("batch_id is not NULL order by batch_id desc");
                            int batchId = latestBatch != null ? latestBatch.getBatchId() + 1 : 1;

                            logger.info("4.2");

                            return Flux.fromArray(idInput.split(" "))
                                    .map(Long::valueOf)
                                    .onErrorReturn(NumberFormatException.class, 0L)
                                    .filter(aLong -> aLong != 0)
                                    .flatMap(aLong -> {

                                        logger.info("4.3");

                                        // Ensure nobody is trying to forceban their boss or a bot or someone that doesn't exist
                                        return guild.getMemberById(Snowflake.of(aLong))
                                                .filter(Objects::nonNull)
                                                .flatMap(punishedMember -> Mono.from(checkIfPunisherHasHighestRole(event.getInteraction().getMember().get(), punishedMember, guild, event))
                                                        .flatMap(aBoolean1 -> {

                                                            logger.info("4.4");

                                                            if (!aBoolean1) {
                                                                return Mono.error(new NoPermissionsException("No Permission"));
                                                            }
                                                            if (punishedMember.isBot()) {
                                                                return Mono.error(new CannotTargetBotsException("Cannot Target Bots"));
                                                            }
                                                            return discordBanUser(guild, aLong, 1, reason);
                                                        }))
                                                .flatMap(unused -> {
                                                    DatabaseLoader.openConnectionIfClosed();
                                                    return event.getClient().getUserById(Snowflake.of(aLong))
                                                            .flatMap(punishedUser -> {
                                                                User punisherUser = event.getInteraction().getUser();

                                                                DiscordUser punished = DiscordUser.findOrCreateIt("user_id_snowflake", aLong);
                                                                DiscordUser punisher = DiscordUser.findFirst("user_id_snowflake = ?", punishingUserIdSnowflake);

                                                                logger.info("4.5");

                                                                Punishment punishment = createDatabasePunishmentRecord(punisher,
                                                                        punisherUser.getUsername(),
                                                                        punisherUser.getDiscriminator(),
                                                                        punished,
                                                                        punishedUser.getUsername(),
                                                                        punisherUser.getDiscriminator(),
                                                                        serverId,
                                                                        request.name());

                                                                punishment.save();
                                                                punishment.refresh();
                                                                punishment.setPermanent(true);
                                                                punishment.setPunishmentMessage(reason);
                                                                punishment.setBatchId(batchId);
                                                                punishment.setEnded(false);

                                                                punishment.save();
                                                                punishment.refresh();
                                                                return Mono.empty();
                                                            });
                                                })
                                                .then();


                                    })
                                    .then(
                                            Notifier.notifyPunisherForcebanComplete(event, "```\n" + idInput.replaceAll(" ", "\n") + "\n ```"))
                                    .then(
                                            AuditLogger.addCommandToDB(event, true));
                        }

                        if (event.getOption("user").isPresent() && event.getOption("user").get().getValue().isPresent()) {

                            logger.info("5");

                            return event.getOption("user").get().getValue().get().asUser().flatMap(punishedUser -> {
                                if (punishedUser.isBot()) {
                                    DatabaseLoader.closeConnectionIfOpen();
                                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new CannotTargetBotsException("Cannot Target Bots")));
                                }

                                logger.info("6");

                                User punishingUser = event.getInteraction().getUser();

                                // Make sure the punisher is higher up the food chain than the person they're trying to punish in the guild they're both in.

                                return punishedUser.asMember(guild.getId()).flatMap(punishedMember -> {

                                    logger.info("7");


                                    if (punishedMember == null && (request.name().equals("kick") || request.name().equals("mute") || request.name().equals("warn"))) {
                                        DatabaseLoader.closeConnectionIfOpen();
                                        return AuditLogger.addCommandToDB(event, false).then(Mono.error(new NoMemberException("No Member")));
                                    }

                                    return checkIfPunisherHasHighestRole(member, punishedMember, guild, event).flatMap(hasHighestRole -> {
                                        // Get the DB objects for both the punishing user and the punished.

                                        logger.info("8");

                                        if (!hasHighestRole) {
                                            return Mono.error(new NoPermissionsException("No Permission"));
                                        }

                                        DiscordUser punisher = DiscordUser.findFirst("user_id_snowflake = ?", punishingUserIdSnowflake);
                                        if (punisher == null) {
                                            punisher = DiscordUser.create("user_id_snowflake", punishingUserIdSnowflake, "date", Instant.now().toEpochMilli());
                                            punisher.save();
                                            punisher.refresh();
                                        }

                                        DiscordUser punished = DiscordUser.findFirst("user_id_snowflake = ?", punishedUser.getId().asLong());
                                        if (punished == null) {
                                            punished = DiscordUser.create("user_id_snowflake", punishedUser.getId().asLong(), "date", Instant.now().toEpochMilli());
                                            punished.save();
                                            punished.refresh();
                                        }

                                        logger.info("9");

                                        if (Punishment.findFirst("user_id_punished = ? and server_id = ? and punishment_type = ? and end_date_passed = ? and permanent = ?",
                                                punished.getUserId(),
                                                serverId,
                                                request.name(),
                                                false,
                                                false) != null) {
                                            DatabaseLoader.closeConnectionIfOpen();
                                            return AuditLogger.addCommandToDB(event, false).then(Mono.error(new AlreadyAppliedException("Punishment Already Applied")));
                                        }

                                        Punishment punishment = createDatabasePunishmentRecord(punisher,
                                                punishingUser.getUsername(),
                                                punishingUser.getDiscriminator(),
                                                punished,
                                                punishedUser.getUsername(),
                                                punishedUser.getDiscriminator(),
                                                serverId,
                                                request.name());

                                        punishment.save();
                                        punishment.refresh();

                                        logger.info("10");

                                        String punishmentReason;
                                        if (event.getOption("reason").isPresent()) {
                                            String punishmentMessage = event.getOption("reason").get().getValue().get().asString();
                                            punishment.refresh();
                                            punishment.setPunishmentMessage(punishmentMessage);
                                            punishment.save();
                                            punishmentReason = punishmentMessage;
                                        } else {
                                            punishmentReason = "No reason provided.";
                                            punishment.refresh();
                                            punishment.setPunishmentMessage(punishmentReason);
                                            punishment.save();
                                        }

                                        // Check if there's a duration on this punishment, and if so save it to database

                                        logger.info("11");

                                        if (event.getOption("duration").isPresent() && event.getOption("duration").get().getValue().isPresent()) {
                                            try {
                                                Duration punishmentDuration = DurationParser.parseDuration(event.getOption("duration").get().getValue().get().asString());
                                                Instant punishmentEndDate = Instant.now().plus(punishmentDuration);
                                                punishment.setEndDate(punishmentEndDate.toEpochMilli());
                                                punishment.setPermanent(false);
                                                punishment.save();
                                            } catch (Exception exception) {
                                                punishment.delete();
                                                DatabaseLoader.closeConnectionIfOpen();
                                                return AuditLogger.addCommandToDB(event, false).then(Mono.error(new InvalidDurationException("Invalid Duration")));
                                            }
                                        } else {
                                            punishment.setEnded(false);
                                            punishment.setPermanent(true);
                                            punishment.save();
                                            punishment.refresh();
                                        }

                                        logger.info("12");

                                        // Find out how many days worth of messages to delete if this is a member ban

                                        int messageDeleteDays;
                                        if (event.getOption("days").isPresent() && event.getOption("days").get().getValue().isPresent()) {
                                            long preliminaryResult = event.getOption("days").get().getValue().get().asLong();
                                            if (preliminaryResult > 7 || preliminaryResult < 0) {
                                                messageDeleteDays = 0;
                                            } else messageDeleteDays = (int) preliminaryResult;
                                        } else messageDeleteDays = 0;

                                        if (event.getCommandName().equals("note")) {
                                            punishment.setDMed(false);
                                            punishment.save();
                                            punishment.refresh();
                                        }

                                        logger.info("13");

                                        punishment.save();
                                        punishment.refresh();

                                        // DMing the punished user, notifying the punishing user that it's worked out

                                        if ((event.getOption("dm").isPresent() && event.getOption("dm").get().getValue().get().asBoolean())
                                                || ((event.getOption("dm").isEmpty()) && !event.getCommandName().equals("note"))) {
                                            return notifyPunishedUser(guild, punishment, punishmentReason)
                                                    .then(carryOutPunishment(guild, punishment, punished, messageDeleteDays, punishmentReason, event));
                                        } else {
                                            return carryOutPunishment(guild, punishment, punished, messageDeleteDays, punishmentReason, event);
                                        }
                                    });
                                });
                            });
                        } else {
                            return Mono.error(new NoUserException("No User"));
                        }
                    });
                });
    }

    public Mono<Void> carryOutPunishment(Guild guild, Punishment punishment, DiscordUser punished, int messageDeleteDays, String punishmentReason, ChatInputInteractionEvent event) {

        // Actually do the punishment.

        logger.info("14");

        return Mono.just(punishment.getPunishmentType()).flatMap(typeString -> {
            switch (punishment.getPunishmentType()) {
                case "mute" -> {
                    return Mono.from(discordMuteUser(guild, punished.getUserIdSnowflake())).flatMap(unused ->
                            loggingListener.onPunishment(event, punishment)
                                    .then(Notifier.notifyPunisher(event, punishment, punishmentReason))
                                    .then(AuditLogger.addCommandToDB(event, true)));
                }
                case "ban" -> {
                    return Mono.from(discordBanUser(guild, punished.getUserIdSnowflake(), messageDeleteDays, punishmentReason)).flatMap(unused ->
                            loggingListener.onPunishment(event, punishment)
                                    .then(Notifier.notifyPunisher(event, punishment, punishmentReason))
                                    .then(AuditLogger.addCommandToDB(event, true)));

                }
                case "kick" -> {
                    return Mono.from(discordKickUser(guild, punished.getUserIdSnowflake(), punishmentReason)).flatMap(unused ->
                            loggingListener.onPunishment(event, punishment)
                                    .then(Notifier.notifyPunisher(event, punishment, punishmentReason))
                                    .then(AuditLogger.addCommandToDB(event, true)));
                }
                case "warn", "note" -> {
                    return loggingListener.onPunishment(event, punishment)
                            .then(Notifier.notifyPunisher(event, punishment, punishmentReason))
                            .then(AuditLogger.addCommandToDB(event, true));
                }
                default -> {
                    DatabaseLoader.closeConnectionIfOpen();
                    return Mono.empty();
                }
            }
        });

    }

    public Mono<Void> notifyPunishedUser(Guild guild, Punishment punishment, String reason) {
        punishment.setDMed(true);
        punishment.save();
        punishment.refresh();

        return Notifier.notifyPunished(guild, punishment, reason);
    }

    private Punishment createDatabasePunishmentRecord(DiscordUser punisher, String punisherName, String punisherDiscrim,
                                                      DiscordUser punished, String punishedName, String punishedDiscrim,
                                                      int serverId, String punishmentType) {
        return Punishment.createIt(
                "user_id_punished", punished.getUserId(),
                "name_punished", punishedName,
                "discrim_punished", punishedDiscrim,
                "user_id_punisher", punisher.getUserId(),
                "name_punisher", punisherName,
                "discrim_punisher", punisherDiscrim,
                "server_id", serverId,
                "punishment_type", punishmentType,
                "punishment_date", Instant.now().toEpochMilli(),
                "automatic", false
        );
    }

    public Mono<Void> discordBanUser(Guild guild, Long userIdSnowflake, int messageDeleteDays, String punishmentReason) {
        return guild.ban(Snowflake.of(userIdSnowflake), BanQuerySpec.builder()
                .deleteMessageDays(messageDeleteDays)
                .reason(punishmentReason)
                .build());
    }

    public Mono<Void> discordKickUser(Guild guild, Long userIdSnowflake, String punishmentReason) {
        return guild.kick(Snowflake.of(userIdSnowflake), punishmentReason);
    }

    public Mono<Void> discordMuteUser(Guild guild, Long userIdSnowflake) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        Mono<Role> mutedRole;
        boolean needToUpdateMutedRole;
        if (discordServerProperties.getMutedRoleSnowflake() == null || discordServerProperties.getMutedRoleSnowflake() == 0) {
            mutedRole = guild.createRole(RoleCreateSpec.builder()
                    .name("Muted")
                    .permissions(PermissionSet.none())
                    .reason("In order to mute users, a muted role must first be created.")
                    .mentionable(false)
                    .hoist(false)
                    .build());
            needToUpdateMutedRole = true;
            discordServerProperties.save();
            discordServerProperties.refresh();
        } else {
            mutedRole = guild.getRoleById(Snowflake.of(discordServerProperties.getMutedRoleSnowflake()));
            needToUpdateMutedRole = false;
            discordServerProperties.save();
            discordServerProperties.refresh();
        }

        return Mono.from(guild.getMemberById(Snowflake.of(userIdSnowflake))).flatMap(punishedMember ->
                Mono.from(guild.getSelfMember()).flatMap(selfMember ->
                        Mono.from(onlyCheckIfPunisherHasHighestRole(selfMember, punishedMember, guild)).flatMap(aBoolean -> {
                            if (!aBoolean) {
                                return Mono.error(new BotPermissionsException("No Bot Permission"));
                            }
                            if (needToUpdateMutedRole) {
                                return Mono.from(updateMutedRoleInAllChannels(guild, mutedRole)).flatMap(unused ->
                                        Mono.from(mutedRole).flatMap(role ->
                                                punishedMember.addRole(role.getId())));
                            } else {
                                return Mono.from(mutedRole).flatMap(role ->
                                        punishedMember.addRole(role.getId()));
                            }
                        })));
    }

    public Mono<Void> updateMutedRoleInAllChannels(Guild guild, Mono<Role> mutedRoleMono) {

        DatabaseLoader.openConnectionIfClosed();

        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        return Mono.from(guild.getSelfMember()).flatMap(selfMember -> {
            return Mono.from(selfMember.getRoles().collectList()).flatMap(selfRoleList -> {
                return Mono.from(guild.getRoles().map(Role::getData).collectList()).flatMap(roleData -> {
                    Flux<RoleData> roleDataFlux = Flux.fromIterable(roleData);
                    Role highestSelfRole = selfRoleList.get(selfRoleList.size() - 1);
                    return Mono.from(OrderUtil.orderRoles(roleDataFlux).takeWhile(role -> !role.equals(highestSelfRole.getData())).count()).flatMap(roleCount -> {
                        int roleInt = roleCount != null ? roleCount.intValue() - 1 : 1;

                        return Mono.from(mutedRoleMono).flatMap(mutedRole -> {
                            Mono<Void> changePositionMono;
                            if (mutedRole != null) {
                                discordServerProperties.setMutedRoleSnowflake(mutedRole.getId().asLong());
                                discordServerProperties.save();

                                changePositionMono = mutedRole.changePosition(roleInt).then();
                            } else {
                                changePositionMono = Mono.empty();
                            }

                            discordServerProperties.save();
                            discordServerProperties.refresh();

                            Mono<Void> updateTextPerms = guild.getChannels().ofType(TextChannel.class)
                                    .flatMap(textChannel -> {
                                        return textChannel.addRoleOverwrite(mutedRole.getId(), PermissionOverwrite.forRole(mutedRole.getId(),
                                                        PermissionSet.none(),
                                                        PermissionSet.of(
                                                                Permission.SEND_MESSAGES,
                                                                Permission.ADD_REACTIONS,
                                                                // Permission.USE_PUBLIC_THREADS,
                                                                // Permission.USE_PRIVATE_THREADS,
                                                                Permission.USE_SLASH_COMMANDS
                                                        )))
                                                .onErrorResume(e -> {
                                                    logger.error("------------------------- Text Channel Edit Prohibited");
                                                    return Mono.empty();
                                                });
                                    })
                                    .then();

                            Mono<Void> updateVoicePerms = guild.getChannels().ofType(VoiceChannel.class)
                                    .flatMap(voiceChannel -> {
                                        return voiceChannel.addRoleOverwrite(mutedRole.getId(), PermissionOverwrite.forRole(mutedRole.getId(),
                                                        PermissionSet.none(),
                                                        PermissionSet.of(
                                                                Permission.SPEAK,
                                                                Permission.STREAM
                                                        )))
                                                .onErrorResume(e -> {
                                                    logger.error("------------------- Voice Channel Edit Prohibited");
                                                    return Mono.empty();
                                                });
                                    }).then();

                            return Mono.when(
                                    changePositionMono,
                                    updateTextPerms,
                                    updateVoicePerms
                            );
                        });
                    });
                });
            });
        });


    }

    public Mono<Boolean> checkIfPunisherHasHighestRole(Member punisher, Member punished, Guild guild, ChatInputInteractionEvent event) {

        logger.info("7.1");

        if (punisher.equals(punished)) {
            return Mono.error(new NoPermissionsException("No Permission"));
        }

        logger.info("7.2");

        Mono<Boolean> adminDiff = permissionChecker.checkIsAdministrator(punisher).flatMap(punisherIsAdmin ->
                        permissionChecker.checkIsAdministrator(punished).flatMap(punishedIsAdmin -> {
            logger.info("7.3");
            if (punisherIsAdmin && !punishedIsAdmin) {
                return Mono.just(true);
            } else if (punishedIsAdmin && punisherIsAdmin) {
                logger.info("7.4");
                return loggingListener.onAttemptedInsubordination(event, punished)
                        .then(AuditLogger.addCommandToDB(event, false))
                        .thenReturn(false);
            }
            logger.info("7.5");
            return Mono.just(false);
        }));

        return punished.getRoles().map(Role::getId).collectList()
                .flatMap(snowflakes -> Mono.from(adminDiff).flatMap(aBoolean -> {
                    logger.info("7.6");
                    if (!aBoolean) {
                        return Mono.error(new NoPermissionsException("No Permission"));
                    }
                    logger.info("7.7");
                    return guild.getSelfMember().flatMap(selfMember -> selfMember.hasHigherRoles(Set.copyOf(snowflakes)).defaultIfEmpty(false).flatMap(botHasHigherRoles -> {
                        if (!botHasHigherRoles) {
                            return Mono.error(new BotRoleException("Bot Role Too Low"));
                        } else {
                            return punisher.hasHigherRoles(Set.copyOf(snowflakes))
                                    .defaultIfEmpty(false)
                                    .flatMap(punisherHasHigherRoles -> {
                                if (!punisherHasHigherRoles) {
                                    return loggingListener.onAttemptedInsubordination(event, punished).then(Mono.error(new NoPermissionsException("No Permission")));
                                } else {
                                    return Mono.just(true);
                                }
                            });
                        }
                    }));
                }));
    }

    public Mono<Boolean> onlyCheckIfPunisherHasHighestRole(Member punisher, Member punished, Guild guild) {

        if (punisher.equals(punished)) {
            return Mono.error(new NoPermissionsException("No Permission"));
        }

        Mono<Boolean> adminDiff = permissionChecker.checkIsAdministrator(punisher).flatMap(punisherIsAdmin ->
                permissionChecker.checkIsAdministrator(punished).flatMap(punishedIsAdmin -> {
            if (punisherIsAdmin && !punishedIsAdmin) {
                return Mono.just(true);
            } else if (punishedIsAdmin && punisherIsAdmin) {
                return Mono.just(false);
            }
            return Mono.just(false);
        }));

        return Mono.from(punished.getRoles().map(Role::getId).collectList())
                .flatMap(snowflakes -> Mono.from(adminDiff).flatMap(aBoolean -> {
                    if (!aBoolean) {
                        return Mono.error(new NoPermissionsException("No Permission"));
                    }
                    return Mono.from(guild.getSelfMember()).flatMap(selfMember -> Mono.from(selfMember.hasHigherRoles(Set.copyOf(snowflakes)).defaultIfEmpty(false)).flatMap(botHasHigherRoles -> {
                        if (!botHasHigherRoles) {
                            return Mono.error(new BotRoleException("Bot Role Too Low"));
                        } else {
                            return Mono.from(punisher.hasHigherRoles(Set.copyOf(snowflakes)).defaultIfEmpty(false)).flatMap(punisherHasHigherRoles -> {
                                if (!punisherHasHigherRoles) {
                                    return Mono.error(new NoPermissionsException("No Permission"));
                                } else {
                                    return Mono.just(true);
                                }
                            });
                        }
                    }));
                }));
    }

    public Mono<Boolean> checkIfPunisherHasHighestRole(Member punisher, Member punished, Guild guild, ButtonInteractionEvent event) {

        if (punisher.equals(punished)) {
            return Mono.error(new NoPermissionsException("No Permission"));
        }

        Mono<Boolean> adminDiff = Mono.empty().flatMap(a -> {
            return Mono.from(permissionChecker.checkIsAdministrator(punisher)).flatMap(punisherIsAdmin -> {
                return Mono.from(permissionChecker.checkIsAdministrator(punished)).flatMap(punishedIsAdmin -> {
                    if (punisherIsAdmin && !punishedIsAdmin) {
                        return Mono.just(true);
                    } else if (punishedIsAdmin && punisherIsAdmin) {
                        return loggingListener.onAttemptedInsubordination(event, punished).thenReturn(false);
                    }
                    return Mono.just(false);
                });
            });
        });

        return Mono.from(punished.getRoles().map(Role::getId).collectList())
                .flatMap(snowflakes -> Mono.from(adminDiff).flatMap(aBoolean -> {
                    if (!aBoolean) {
                        return Mono.error(new NoPermissionsException("No Permission"));
                    }
                    return Mono.from(guild.getSelfMember()).flatMap(selfMember -> Mono.from(selfMember.hasHigherRoles(Set.copyOf(snowflakes)).defaultIfEmpty(false)).flatMap(botHasHigherRoles -> {
                        if (!botHasHigherRoles) {
                            return Mono.error(new BotRoleException("Bot Role Too Low"));
                        } else {
                            return Mono.from(punisher.hasHigherRoles(Set.copyOf(snowflakes)).defaultIfEmpty(false)).flatMap(punisherHasHigherRoles -> {
                                if (!punisherHasHigherRoles) {
                                    return loggingListener.onAttemptedInsubordination(event, punished).then(Mono.error(new NoPermissionsException("No Permission")));
                                } else {
                                    return Mono.just(true);
                                }
                            });
                        }
                    }));
                }));
    }
}
