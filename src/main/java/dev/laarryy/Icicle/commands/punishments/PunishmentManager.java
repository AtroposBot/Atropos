package dev.laarryy.Icicle.commands.punishments;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.DurationParser;
import dev.laarryy.Icicle.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.TopLevelGuildChannel;
import discord4j.core.object.entity.channel.TopLevelGuildMessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.BanQuerySpec;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.core.spec.TextChannelEditMono;
import discord4j.core.spec.TextChannelEditSpec;
import discord4j.core.spec.VoiceChannelEditSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutablePermissionsEditRequest;
import discord4j.discordjson.json.PermissionsEditRequest;
import discord4j.discordjson.json.RoleData;
import discord4j.rest.entity.RestChannel;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import discord4j.rest.util.OrderUtil;
import discord4j.rest.util.PermissionSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Set;

public class PunishmentManager {
    private final Logger logger = LogManager.getLogger(this);
    GatewayDiscordClient client;
    PermissionChecker permissionChecker = new PermissionChecker();

    public PunishmentManager(GatewayDiscordClient client) {
        if (client != null) {
            this.client = client;
        } else {
            logger.error("Client is null.");
        }
    }

    public Mono<Void> doPunishment(ApplicationCommandRequest request, SlashCommandEvent event) {

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

        Permission permission = Permission.findOrCreateIt("permission", request.name());
        permission.save();
        permission.refresh();
        int permissionId = permission.getInteger("id");

        // Make sure user has permission to do this, or stop here

        if (!permissionChecker.checkPermission(guild, user, permissionId)) {
            event.reply("No permission.").withEphemeral(true).subscribe();
            return Mono.empty();
        }

        Long guildIdSnowflake = guild.getId().asLong();
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildIdSnowflake);
        int serverId;

        if (discordServer != null) {
            serverId = discordServer.getServerId();
        } else {
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
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(aLong -> {

                        // Ensure nobody is trying to forceban their boss

                        if (guild.getMemberById(Snowflake.of(aLong)).onErrorReturn(Exception.class, null).block() != null) {
                            if (!checkIfPunisherHasHighestRole(event.getInteraction().getMember().get(), guild.getMemberById(Snowflake.of(aLong)).block(), guild)) {
                                return;
                            }
                        }
                        DatabaseLoader.openConnectionIfClosed();
                        DiscordUser punished;
                        if (DiscordUser.findFirst("user_id_snowflake = ?", aLong) == null) {
                            punished = DiscordUser.createIt("user_id_snowflake", aLong, "date", Instant.now().toEpochMilli());
                        } else {
                            punished = DiscordUser.findFirst("user_id_snowflake = ? ", aLong);
                        }
                        DiscordUser punisher = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);
                        Punishment punishment = createDatabasePunishmentRecord(punisher, punished, serverId, request.name());
                        punishment.save();
                        punishment.refresh();
                        punishment.setEnded(true);
                        punishment.save();
                        logger.info("Saved forceban for ID: " + aLong);
                    });
            event.reply("Forceban in progress.").withEphemeral(true).subscribe();
            return Mono.empty();
        }

        // All other punishments have Users, so if we're missing one here it's a problem, we need to stop.

        if (event.getOption("user").isEmpty()) {
            return Mono.empty();
        }

        // Get the DB objects for both the punishing user and the punished.

        User punishedUser = event.getOption("user").get().getValue().get().asUser().block();
        DiscordUser punisher = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);
        DiscordUser punished = DiscordUser.findFirst("user_id_snowflake = ?", punishedUser.getId().asLong());

        // Make sure the punisher is higher up the food chain than the person they're trying to punish in the guild they're both in.

        if (!checkIfPunisherHasHighestRole(member, punishedUser.asMember(guild.getId()).block(), guild)) {
            event.reply("The target of this action has higher roles than you, or is an administrator while you are not.").withEphemeral(true).subscribe();
            return Mono.empty();
        }

        logger.info("Passed relative permission check.");

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
        } else punishmentReason = "No reason provided.";

        // Check if there's a duration on this punishment, and if so save it to database

        boolean durationSuccessful;
        if (event.getOption("duration").isPresent() && event.getOption("duration").get().getValue().isPresent()) {
            try {
                Duration punishmentDuration = DurationParser.parseDuration(event.getOption("duration").get().getValue().get().asString());
                Instant punishmentEndDate = Instant.now().plus(punishmentDuration);
                punishment.setEndDate(punishmentEndDate.toEpochMilli());
                punishment.save();
                durationSuccessful = true;

            } catch (Exception exception) {
                durationSuccessful = false;
            }
        } else {
            durationSuccessful = false;
            punishment.setEnded(true);
            punishment.save();
        }

        // Format punishment end date to string for use in DMing user and notifying punisher.

        String punishmentEnd;
        if (punishment.getEndDate() != null) {
            Instant endDate = Instant.ofEpochMilli(punishment.getEndDate());
            punishmentEnd = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(ZoneId.systemDefault()).format(endDate);
        } else {
            punishmentEnd = "Indefinite.";
        }

        // Find out how many days worth of messages to delete if this is a member ban

        int messageDeleteDays;
        if (event.getOption("days").isPresent() && event.getOption("days").get().getValue().isPresent()) {
            long preliminaryResult = event.getOption("days").get().getValue().get().asLong();
            if (preliminaryResult > 7 || preliminaryResult < 0) {
                messageDeleteDays = 0;
            } else messageDeleteDays = (int) preliminaryResult;
        } else messageDeleteDays = 0;

        // DMing the punished user, notifying the punishing user that it's worked out

        if ((event.getOption("dm").isPresent() && event.getOption("dm").get().getValue().get().asBoolean()) || event.getOption("dm").isEmpty()) {
            punishment.setDMed(true);
            punishment.save();
            punishment.refresh();

            String actionType = punishment.getPunishmentType().toUpperCase();

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(guild.getName())
                    .author("Notice from Guild:", "", guild.getIconUrl(Image.Format.PNG).orElse(guild.getSelfMember().block().getAvatarUrl()))
                    .description(punishedUser.getMention() + ", this message is to notify you of moderation action taken by the staff of "
                            + guild.getName()
                            + ". This incident will be recorded.")
                    .addField("Action Taken", actionType, true)
                    .addField("Reason", punishmentReason, false)
                    .addField("End Date", punishmentEnd, false)
                    .color(Color.RUST)
                    .timestamp(Instant.now())
                    .build();

            PrivateChannel privateChannel = punishedUser.getPrivateChannel().block();

            try {
                privateChannel.createMessage(embed).block();
                if (durationSuccessful) {
                    event.reply("Successfully punished " + punishedUser.getUsername() + ". Punishment ID is " + punishment.getInteger("id") + " and will end on " + punishmentEnd + ".").withEphemeral(true).subscribe();
                } else {
                    event.reply("Successfully punished " + punishedUser.getUsername() + ". Punishment ID is " + punishment.getInteger("id") + " and has no expiration.").withEphemeral(true).subscribe();
                }
            } catch (Exception e) {
                event.reply("Successfully recorded punishment, but unable to DM user. " +
                        "Punishment ID is " + punishment.getInteger("id") + ".").withEphemeral(true).subscribe();
            }
        }

        // Let punishing user know that it worked in case they manually disabled DMing.

        if (event.getOption("dm").isPresent() && !event.getOption("dm").get().getValue().get().asBoolean()) {
            if (durationSuccessful) {
                event.reply("Successfully punished " + punishedUser.getUsername() + ". Punishment ID is " + punishment.getInteger("id") + " and will end on " + punishmentEnd + ".").withEphemeral(true).subscribe();
            } else {
                event.reply("Successfully punished " + punishedUser.getUsername() + ". Punishment ID is " + punishment.getInteger("id") + " and has no expiration.").withEphemeral(true).subscribe();
            }
        }
        // Actually do the punishment, discord-side. Nothing to do for warnings.

        DatabaseLoader.openConnectionIfClosed();
        switch (punishment.getPunishmentType()) {
            case "mute" -> discordMuteUser(guild, punished.getUserIdSnowflake(), DiscordServerProperties.findFirst("server_id = ?", discordServer.getServerId()));
            case "ban" -> discordBanUser(guild, punished.getUserIdSnowflake(), messageDeleteDays, punishmentReason);
        }

        return Mono.empty();
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

    private void discordBanUser(Guild guild, Long userIdSnowflake, int messageDeleteDays, String punishmentReason) {
        guild.ban(Snowflake.of(userIdSnowflake), BanQuerySpec.builder()
                        .deleteMessageDays(messageDeleteDays)
                        .reason(punishmentReason)
                        .build())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
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

    private void discordMuteUser(Guild guild, Long userIdSnowflake, DiscordServerProperties discordServerProperties) {
        DatabaseLoader.openConnectionIfClosed();
        if (discordServerProperties.getMutedRoleSnowflake() == null || discordServerProperties.getMutedRoleSnowflake() == 0) {
            logger.info("Creating missing Muted role.");
            Role mutedRole = guild.createRole(RoleCreateSpec.builder()
                    .name("Muted")
                    .permissions(PermissionSet.none())
                    .reason("In order to mute users, a muted role must first be created.")
                    .mentionable(false)
                    .hoist(false)
                    .build()).block();

            List<Role> selfRoleList = guild.getSelfMember().block().getRoles().collectList().block();
            logger.info("Role list size - 1 is equal to: " + (selfRoleList.size() - 1));
            Role highestSelfRole = selfRoleList.get(selfRoleList.size() - 1);
            logger.info("Highest role is " + highestSelfRole.getName());

            Flux<RoleData> roleDataFlux = Flux.fromIterable(guild.getRoles().map(Role::getData).collectList().block());

            Long roleCount = OrderUtil.orderRoles(roleDataFlux).takeWhile(role -> !role.equals(highestSelfRole)).count().block();
            logger.info("Rolecount = " + roleCount);
            int roleInt = roleCount != null ? roleCount.intValue() - 1 : 1;

            if (mutedRole != null) {
                logger.info("Attempting to hoist it real high.");
                mutedRole.changePosition(roleInt).subscribe();
                discordServerProperties.setMutedRoleSnowflake(mutedRole.getId().asLong());
                discordServerProperties.save();
                logger.info("Done hoisting and saving to db.");
            }
        }
        discordServerProperties.refresh();

        Role mutedRole = guild.getRoleById(Snowflake.of(discordServerProperties.getMutedRoleSnowflake())).block();
        updateMutedRoleInAllChannels(guild, mutedRole);

        Member memberToMute = guild.getMemberById(Snowflake.of(userIdSnowflake)).block();

        if (memberToMute != null) {
            logger.info("Adding role to user");
            logger.info(discordServerProperties.getMutedRoleSnowflake());
            memberToMute.addRole(Snowflake.of(discordServerProperties.getMutedRoleSnowflake())).block();
            logger.info("Role added.");
        } else {
            logger.info("MUTE ROLE NULL! WHYYYYYYY");
        }

    }

    public void updateMutedRoleInAllChannels(Guild guild, Role mutedRole) {

        guild.getChannels().ofType(TextChannel.class)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(textChannel -> textChannel.edit(TextChannelEditSpec.builder()
                                .addPermissionOverwrite(PermissionOverwrite.forRole(mutedRole.getId(),
                                        PermissionSet.none(),
                                        PermissionSet.of(
                                                discord4j.rest.util.Permission.SEND_MESSAGES,
                                                discord4j.rest.util.Permission.ADD_REACTIONS,
                                                discord4j.rest.util.Permission.CHANGE_NICKNAME
                                        )))
                                .build()))
                .subscribe();

        guild.getChannels().ofType(VoiceChannel.class)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(voiceChannel -> voiceChannel.edit(VoiceChannelEditSpec.builder()
                        .addPermissionOverwrite(PermissionOverwrite.forRole(mutedRole.getId(),
                                PermissionSet.none(),
                                PermissionSet.of(
                                        discord4j.rest.util.Permission.SPEAK,
                                        discord4j.rest.util.Permission.PRIORITY_SPEAKER,
                                        discord4j.rest.util.Permission.STREAM
                                )))
                        .build()))
                .subscribe();

    }

    private boolean checkIfPunisherHasHighestRole(Member punisher, Member punished, Guild guild) {
        if (permissionChecker.checkIsAdministrator(guild, punisher) && !permissionChecker.checkIsAdministrator(guild, punished)) {
            logger.info("Admin punisher and non-admin punished. You may pass.");
            return true;
        } else if (permissionChecker.checkIsAdministrator(guild, punished) && !permissionChecker.checkIsAdministrator(guild, punisher)) {
            logger.info("Admin punished and non-admin punisher. No.");
            // TODO: Log this in server log channel
            return false;
        }

        Set<Snowflake> snowflakeSet = Set.copyOf(punished.getRoles().map(Role::getId).collectList().block());
        return punisher.hasHigherRoles(snowflakeSet).defaultIfEmpty(false).block();

    }
}
