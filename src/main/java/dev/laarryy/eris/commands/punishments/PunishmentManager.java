package dev.laarryy.eris.commands.punishments;

import dev.laarryy.eris.listeners.logging.LoggingListener;
import dev.laarryy.eris.managers.LoggingListenerManager;
import dev.laarryy.eris.models.guilds.DiscordServer;
import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import dev.laarryy.eris.models.users.DiscordUser;
import dev.laarryy.eris.models.users.Punishment;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.AuditLogger;
import dev.laarryy.eris.utils.DurationParser;
import dev.laarryy.eris.utils.Notifier;
import dev.laarryy.eris.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.ExtendedPermissionOverwrite;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.CategoryEditSpec;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.core.spec.TextChannelEditSpec;
import discord4j.core.spec.VoiceChannelEditSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.RoleData;
import discord4j.rest.util.OrderUtil;
import discord4j.rest.util.PermissionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PunishmentManager {
    private final Logger logger = LogManager.getLogger(this);
    PermissionChecker permissionChecker = new PermissionChecker();
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();

    public Mono<Void> doPunishment(ApplicationCommandRequest request, ChatInputInteractionEvent event) {

        // Make sure this is done in a guild or else stop right here.

        if (event.getInteraction().getGuild().block() == null || event.getInteraction().getMember().isEmpty()) {
            event.reply("This must be done in a guild.").withEphemeral(true).subscribe();
            return Mono.empty();
        }

        // Gather some necessary information for the rest of this

        Guild guild = event.getInteraction().getGuild().block();

        Member member = event.getInteraction().getMember().get();
        User user = event.getInteraction().getUser();
        Long userIdSnowflake = member.getId().asLong();

        DatabaseLoader.openConnectionIfClosed();

        // Make sure user has permission to do this, or stop here

        if (!permissionChecker.checkPermission(guild, user, request.name())) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            return Mono.empty();
        }

        Long guildIdSnowflake = guild.getId().asLong();
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildIdSnowflake);
        int serverId;

        if (discordServer != null) {
            serverId = discordServer.getServerId();
        } else {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        // Handle forceban via API before dealing with literally every other punishment (that can actually provide a user)

        if (event.getOption("id").isPresent() && event.getOption("id").get().getValue().isPresent()) {
            String idInput = event.getOption("id").get().getValue().get().asString();
            Flux.fromArray(idInput.split(" "))
                    .map(Long::valueOf)
                    .onErrorReturn(NumberFormatException.class, 0L)
                    .filter(aLong -> aLong != 0)
                    .filter(aLong -> apiBanId(guild, aLong))
                    .doFirst(() -> event.acknowledge().block())
                    .doOnComplete(() -> {
                        Notifier.notifyPunisherForcebanComplete(event);
                        AuditLogger.addCommandToDB(event, true);
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(aLong -> {

                        // Ensure nobody is trying to forceban their boss or a bot
                        try {
                            guild.getMemberById(Snowflake.of(aLong));
                            if (guild.getMemberById(Snowflake.of(aLong)).block() != null) {
                                if (!checkIfPunisherHasHighestRole(event.getInteraction().getMember().get(), guild.getMemberById(Snowflake.of(aLong)).block(), guild, event)) {
                                    return;
                                }
                                if (guild.getMemberById(Snowflake.of(aLong)).block().isBot()) {
                                    return;
                                }
                            }
                        } catch (Exception ignored) {
                        }

                        DatabaseLoader.openConnectionIfClosed();
                        DiscordUser punished = DiscordUser.findOrCreateIt("user_id_snowflake", aLong);
                        DiscordUser punisher = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);
                        Punishment punishment = createDatabasePunishmentRecord(punisher, punished, serverId, request.name());
                        punishment.save();
                        punishment.refresh();
                        punishment.setEnded(true);
                        punishment.save();
                        punishment.refresh();
                    });
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        // Ensure bot is not a target

        if (event.getOption("user").isPresent() && event.getOption("user").get().getValue().isPresent()) {
            if (event.getOption("user").get().getValue().get().asUser().block().isBot()) {
                Notifier.notifyCommandUserOfError(event, "cannotTargetBots");
                AuditLogger.addCommandToDB(event, false);
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.empty();
            }
        }
        // All other punishments have Users, so if we're missing one here it's a problem, we need to stop.

        if (event.getOption("user").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "noUser");
            AuditLogger.addCommandToDB(event, false);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }
        User punishedUser = event.getOption("user").get().getValue().get().asUser().block();

        // Make sure the punisher is higher up the food chain than the person they're trying to punish in the guild they're both in.

        Member punishedMember;
        try {
            punishedMember = punishedUser.asMember(guild.getId()).block();
            if (!checkIfPunisherHasHighestRole(member, punishedMember, guild, event)) {
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.empty();
            }
        } catch (Exception ignored) {}

        // Get the DB objects for both the punishing user and the punished.

        DiscordUser punisher = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);
        DiscordUser punished = DiscordUser.findFirst("user_id_snowflake = ?", punishedUser.getId().asLong());
        if (punished == null) {
            punished = DiscordUser.create("user_id_snowflake", punishedUser.getId().asLong(), "date", Instant.now().toEpochMilli());
            punished.save();
            punished.refresh();
        }

        if (Punishment.findFirst("user_id_punished = ? and server_id = ? and punishment_type = ? and end_date_passed = ?",
                punished.getUserId(),
                serverId,
                request.name(),
                false) != null) {
            Notifier.notifyCommandUserOfError(event, "alreadyApplied");
            AuditLogger.addCommandToDB(event, false);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }


        Punishment punishment = createDatabasePunishmentRecord(punisher, punished, serverId, request.name());
        punishment.save();
        punishment.refresh();

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

        if (event.getOption("duration").isPresent() && event.getOption("duration").get().getValue().isPresent()) {
            try {
                Duration punishmentDuration = DurationParser.parseDuration(event.getOption("duration").get().getValue().get().asString());
                Instant punishmentEndDate = Instant.now().plus(punishmentDuration);
                punishment.setEndDate(punishmentEndDate.toEpochMilli());
                punishment.save();
            } catch (Exception exception) {
                Notifier.notifyCommandUserOfError(event, "invalidDuration");
                AuditLogger.addCommandToDB(event, false);
                punishment.delete();
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.empty();
            }
        } else {
            punishment.setEnded(true);
            punishment.save();
            punishment.refresh();
        }

        // Find out how many days worth of messages to delete if this is a member ban

        int messageDeleteDays;
        if (event.getOption("days").isPresent() && event.getOption("days").get().getValue().isPresent()) {
            long preliminaryResult = event.getOption("days").get().getValue().get().asLong();
            if (preliminaryResult > 7 || preliminaryResult < 0) {
                messageDeleteDays = 0;
            } else messageDeleteDays = (int) preliminaryResult;
        } else messageDeleteDays = 0;

        if (event.getCommandName().equals("case")) {
            punishment.setDMed(false);
            punishment.save();
            punishment.refresh();
        }

        // DMing the punished user, notifying the punishing user that it's worked out

        if ((event.getOption("dm").isPresent() && event.getOption("dm").get().getValue().get().asBoolean())
                || (event.getOption("dm").isEmpty())) {
            notifyPunishedUser(guild, punishment, punishmentReason);
        }

        // Actually do the punishment, discord-side. Nothing to do for warnings or cases.

        DatabaseLoader.openConnectionIfClosed();
        switch (punishment.getPunishmentType()) {
            case "mute" -> discordMuteUser(guild, punished.getUserIdSnowflake());
            case "ban" -> discordBanUser(guild, punished.getUserIdSnowflake(), messageDeleteDays, punishmentReason);
            case "kick" -> discordKickUser(guild, punished.getUserIdSnowflake(), punishmentReason);
        }

        loggingListener.onPunishment(event, punishment);
        Notifier.notifyPunisher(event, punishment, punishmentReason);
        AuditLogger.addCommandToDB(event, true);
        DatabaseLoader.closeConnectionIfOpen();

        return Mono.empty();
    }

    public void notifyPunishedUser(Guild guild, Punishment punishment, String reason) {
        punishment.setDMed(true);
        punishment.save();
        punishment.refresh();

        Notifier.notifyPunished(guild, punishment, reason);
    }

    private Punishment createDatabasePunishmentRecord(DiscordUser punisher, DiscordUser punished, int serverId, String punishmentType) {
        return Punishment.createIt(
                "user_id_punished", punished.getUserId(),
                "user_id_punisher", punisher.getUserId(),
                "server_id", serverId,
                "punishment_type", punishmentType,
                "punishment_date", Instant.now().toEpochMilli()
        );
    }

    public void discordBanUser(Guild guild, Long userIdSnowflake, int messageDeleteDays, String punishmentReason) {
        guild.ban(Snowflake.of(userIdSnowflake), BanQuerySpec.builder()
                        .deleteMessageDays(messageDeleteDays)
                        .reason(punishmentReason)
                        .build())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    public void discordKickUser(Guild guild, Long userIdSnowflake, String punishmentReason) {
        guild.kick(Snowflake.of(userIdSnowflake), punishmentReason).subscribe();
    }

    private boolean apiBanId(Guild guild, Long id) {
        try {
            guild.ban(Snowflake.of(id), BanQuerySpec.builder()
                    .reason("Mass API banned by staff.")
                    .deleteMessageDays(0)
                    .build()).block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void discordMuteUser(Guild guild, Long userIdSnowflake) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
        Role mutedRole;
        if (discordServerProperties.getMutedRoleSnowflake() == null || discordServerProperties.getMutedRoleSnowflake() == 0) {
            mutedRole = guild.createRole(RoleCreateSpec.builder()
                    .name("Muted")
                    .permissions(PermissionSet.none())
                    .reason("In order to mute users, a muted role must first be created.")
                    .mentionable(false)
                    .hoist(false)
                    .build()).block();
            updateMutedRoleInAllChannels(guild, mutedRole);
        } else {
            mutedRole = guild.getRoleById(Snowflake.of(discordServerProperties.getMutedRoleSnowflake())).block();
            if (mutedRole == null) {
                logger.info("muted role null (try block)");
                mutedRole = guild.createRole(RoleCreateSpec.builder()
                        .name("Muted")
                        .permissions(PermissionSet.none())
                        .reason("In order to mute users, a muted role must first be created.")
                        .mentionable(false)
                        .hoist(false)
                        .build()).block();
                discordServerProperties.setMutedRoleSnowflake(mutedRole.getId().asLong());
                discordServerProperties.save();
                discordServerProperties.refresh();
                updateMutedRoleInAllChannels(guild, mutedRole);
            }

        }

        List<Role> selfRoleList = guild.getSelfMember().block().getRoles().collectList().block();
        Role highestSelfRole = selfRoleList.get(selfRoleList.size() - 1);

        Flux<RoleData> roleDataFlux = Flux.fromIterable(guild.getRoles().map(Role::getData).collectList().block());

        Long roleCount = OrderUtil.orderRoles(roleDataFlux).takeWhile(role -> !role.equals(highestSelfRole)).count().block();
        int roleInt = roleCount != null ? roleCount.intValue() - 2 : 1;

        if (mutedRole != null) {
            discordServerProperties.setMutedRoleSnowflake(mutedRole.getId().asLong());
            discordServerProperties.save();

            mutedRole.changePosition(roleInt)
                    .onErrorResume(e -> {
                        logger.error(e.getMessage());
                        logger.error(e.getMessage(), e);
                        return Mono.empty();
                    })
                    .subscribe();
        }

        discordServerProperties.save();
        discordServerProperties.refresh();
        Member memberToMute = guild
                .getMemberById(Snowflake.of(userIdSnowflake))
                .onErrorResume(e -> {
                    logger.error(e.getMessage());
                    logger.error(e.getMessage(), e);
                    DatabaseLoader.closeConnectionIfOpen();
                    return Mono.empty();
                })
                .block();

        if (memberToMute != null) {
            memberToMute.addRole(mutedRole.getId())
                    .onErrorResume(e -> {
                        logger.error(e.getMessage());
                        logger.error(e.getMessage(), e);
                        DatabaseLoader.closeConnectionIfOpen();
                        return Mono.empty();
                    })
                    .block();
        }
        DatabaseLoader.closeConnectionIfOpen();
    }

    public void updateMutedRoleInAllChannels(Guild guild, Role mutedRole) {

        guild.getChannels().ofType(Category.class)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(category -> {
                    Set<ExtendedPermissionOverwrite> overwrites = category.getPermissionOverwrites();
                    Set<PermissionOverwrite> newOverwrites = new HashSet<>(overwrites);
                    newOverwrites.add(PermissionOverwrite.forRole(mutedRole.getId(),
                            PermissionSet.none(),
                            PermissionSet.of(
                                    discord4j.rest.util.Permission.SEND_MESSAGES,
                                    discord4j.rest.util.Permission.ADD_REACTIONS,
                                    discord4j.rest.util.Permission.CHANGE_NICKNAME
                            )));
                    category.edit(CategoryEditSpec.builder()
                                    .addAllPermissionOverwrites(newOverwrites.stream().toList())
                                    .build())
                            .onErrorResume(e -> {
                                logger.error(e.getMessage());
                                logger.error(e.getMessage(), e);
                                return Mono.empty();
                            })
                            .block();
                    return Mono.empty();
                })
                .subscribe();

        guild.getChannels().ofType(TextChannel.class)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(textChannel -> {
                    Set<ExtendedPermissionOverwrite> overwrites = textChannel.getPermissionOverwrites();
                    Set<PermissionOverwrite> newOverwrites = new HashSet<>(overwrites);
                    newOverwrites.add(PermissionOverwrite.forRole(mutedRole.getId(),
                            PermissionSet.none(),
                            PermissionSet.of(
                                    discord4j.rest.util.Permission.SEND_MESSAGES,
                                    discord4j.rest.util.Permission.ADD_REACTIONS,
                                    discord4j.rest.util.Permission.CHANGE_NICKNAME
                            )));
                    textChannel.edit(TextChannelEditSpec.builder()
                            .addAllPermissionOverwrites(newOverwrites.stream().toList())
                            .build())
                            .onErrorResume(e -> {
                                logger.error(e.getMessage());
                                logger.error(e.getMessage(), e);
                                return Mono.empty();
                            })
                            .block();
                    return Mono.empty();
                })
                .subscribe();

        guild.getChannels().ofType(VoiceChannel.class)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(voiceChannel -> {
                    Set<ExtendedPermissionOverwrite> overwrites = voiceChannel.getPermissionOverwrites();
                    Set<PermissionOverwrite> newOverwrites = new HashSet<>(overwrites);
                    newOverwrites.add(PermissionOverwrite.forRole(mutedRole.getId(),
                            PermissionSet.none(),
                            PermissionSet.of(
                                    discord4j.rest.util.Permission.SPEAK,
                                    discord4j.rest.util.Permission.PRIORITY_SPEAKER,
                                    discord4j.rest.util.Permission.STREAM
                            )));
                    voiceChannel.edit(VoiceChannelEditSpec.builder()
                            .addAllPermissionOverwrites(newOverwrites.stream().toList())
                            .build())
                            .onErrorResume(e -> {
                                logger.error(e.getMessage());
                                logger.error(e.getMessage(), e);
                                return Mono.empty();
                            })
                            .block();
                    return Mono.empty();
                })
                .subscribe();

    }

    public boolean checkIfPunisherHasHighestRole(Member punisher, Member punished, Guild guild, ChatInputInteractionEvent event) {

        if (punisher.equals(punished)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            return false;
        }

        if (permissionChecker.checkIsAdministrator(guild, punisher) && !permissionChecker.checkIsAdministrator(guild, punished)) {
            return true;
        } else if (permissionChecker.checkIsAdministrator(guild, punished) && !permissionChecker.checkIsAdministrator(guild, punisher)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            loggingListener.onAttemptedInsubordination(event, punished);
            return false;
        }

        Set<Snowflake> snowflakeSet = Set.copyOf(punished.getRoles().map(Role::getId).collectList().block());

        if (!guild.getSelfMember().block().hasHigherRoles(snowflakeSet).defaultIfEmpty(false).block()) {
            Notifier.notifyCommandUserOfError(event, "botRoleTooLow");
            AuditLogger.addCommandToDB(event, false);
            return false;
        }

        if (!punisher.hasHigherRoles(snowflakeSet).defaultIfEmpty(false).block()) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            loggingListener.onAttemptedInsubordination(event, punished);
            return false;
        } else {
            return true;
        }
    }

    public boolean checkIfPunisherHasHighestRole(Member punisher, Member punished, Guild guild, ButtonInteractionEvent event) {

        if (punisher.equals(punished)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            return false;
        }

        if (permissionChecker.checkIsAdministrator(guild, punisher) && !permissionChecker.checkIsAdministrator(guild, punished)) {
            return true;
        } else if (permissionChecker.checkIsAdministrator(guild, punished) && !permissionChecker.checkIsAdministrator(guild, punisher)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            loggingListener.onAttemptedInsubordination(event, punished);
            return false;
        }

        Set<Snowflake> snowflakeSet = Set.copyOf(punished.getRoles().map(Role::getId).collectList().block());

        if (!guild.getSelfMember().block().hasHigherRoles(snowflakeSet).defaultIfEmpty(false).block()) {
            Notifier.notifyCommandUserOfError(event, "botRoleTooLow");
            AuditLogger.addCommandToDB(event, false);
            return false;
        }

        if (!punisher.hasHigherRoles(snowflakeSet).defaultIfEmpty(false).block()) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            loggingListener.onAttemptedInsubordination(event, punished);
            return false;
        } else {
            return true;
        }
    }
}
