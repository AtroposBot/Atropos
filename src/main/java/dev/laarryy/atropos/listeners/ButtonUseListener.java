package dev.laarryy.atropos.listeners;

import dev.laarryy.atropos.commands.punishments.ManualPunishmentEnder;
import dev.laarryy.atropos.commands.punishments.PunishmentManager;
import dev.laarryy.atropos.exceptions.NoMutedRoleException;
import dev.laarryy.atropos.exceptions.NoPermissionsException;
import dev.laarryy.atropos.exceptions.NoUserException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.exceptions.UserNotMutedException;
import dev.laarryy.atropos.listeners.logging.LoggingListener;
import dev.laarryy.atropos.managers.LoggingListenerManager;
import dev.laarryy.atropos.managers.PunishmentManagerManager;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.models.guilds.ServerMessage;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.CommandChecks;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.EmbedFieldData;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ButtonUseListener {

    PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    ManualPunishmentEnder manualPunishmentEnder = new ManualPunishmentEnder();

    PermissionChecker permissionChecker = new PermissionChecker();

    private final Logger logger = LogManager.getLogger(this);
    private static final Pattern BAN = Pattern.compile("(.*)-atropos-ban-(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);
    private static final Pattern UNMUTE = Pattern.compile("(.*)-atropos-unmute-(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);
    private static final Pattern KICK = Pattern.compile("(.*)-atropos-kick-(.*)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);

    private static final Pattern DE_EPHEMERALIZE = Pattern.compile("deephemeralize", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL);


    @EventListener
    public Mono<Void> on(ButtonInteractionEvent event) {

        return event.getInteraction().getGuild().flatMap(guild -> {
            if (event.getInteraction().getMember().isEmpty()) {
                return Mono.error(new NoUserException("Member Not Found"));
            }

            Member member = event.getInteraction().getMember().get();
            if (guild == null) {
                return Mono.error(new NullServerException("No Server"));
            }

            Member mod = event.getInteraction().getMember().get();
            String id = event.getCustomId();
            Matcher ban = BAN.matcher(id);
            Matcher kick = KICK.matcher(id);
            Matcher unmute = UNMUTE.matcher(id);
            Matcher deEphemeralize = DE_EPHEMERALIZE.matcher(id);

            if (deEphemeralize.matches()) {
                return sendMessageNonEphemerally(event);
            }

            if (ban.matches()) {
                DatabaseLoader.openConnectionIfClosed();
                String punishmentId = ban.group(1);
                String userId = ban.group(2);
                return banUser(punishmentId, userId, guild, mod, event);
            }

            if (kick.matches()) {
                DatabaseLoader.openConnectionIfClosed();
                String punishmentId = kick.group(1);
                String userId = kick.group(2);
                return kickUser(punishmentId, userId, guild, mod, event);
            }

            if (unmute.matches()) {
                DatabaseLoader.openConnectionIfClosed();
                String punishmentId = unmute.group(1);
                String userId = unmute.group(2);
                return unmuteUser(punishmentId, userId, guild, mod, event);
            }

            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        });
    }

    private MessageCreateSpec messageSpecFromData(MessageData data) {

        EmbedData embedData = data.embeds().get(0);
        EmbedCreateSpec.Builder spec = EmbedCreateSpec.builder();

        embedData.title().toOptional().ifPresent(spec::title);
        embedData.author().toOptional().ifPresent(a -> spec.author(a.name().toOptional().orElse(""), a.url().toOptional().orElse(""), a.iconUrl().toOptional().orElse("")));
        embedData.color().toOptional().ifPresent(c -> spec.color(Color.of(c)));
        embedData.description().toOptional().ifPresent(spec::description);
        embedData.fields().toOptional().ifPresent(fields -> {
            for (EmbedFieldData fieldData : fields) {
                EmbedCreateFields.Field field = EmbedCreateFields.Field.of(fieldData.name(), fieldData.value(), fieldData.inline().toOptional().orElse(false));
                spec.addField(field);
            }
        });
        embedData.image().toOptional().ifPresent(i -> spec.image(i.url().toOptional().orElse(i.proxyUrl().toOptional().orElse(""))));
        embedData.thumbnail().toOptional().ifPresent(t -> spec.thumbnail(t.url().toOptional().orElse(t.proxyUrl().toOptional().orElse(""))));
        embedData.footer().toOptional().ifPresent(f -> spec.footer(f.text(), f.iconUrl().toOptional().orElse(f.proxyIconUrl().toOptional().orElse(""))));
        embedData.timestamp().toOptional().ifPresent(t -> spec.timestamp(Instant.parse(t)));
        embedData.url().toOptional().ifPresent(spec::url);

        return MessageCreateSpec.builder()
                .embeds(Collections.singleton(spec.build()))
                .content(data.content())
                .build();
    }

    private Mono<Void> sendMessageNonEphemerally(ButtonInteractionEvent event) {
        Mono<User> selfUserMono = event.getClient().getSelf();
        Mono<MessageChannel> messageChannelMono = event.getInteraction().getChannel();

        return Mono.zip(messageChannelMono, selfUserMono, (messageChannel, self) -> {
                    DatabaseLoader.openConnectionIfClosed();
                    ServerMessage serverMessage = ServerMessage.findFirst("message_id_snowflake = ?", event.getMessageId().asString());
                    if (serverMessage != null) {
                        MessageData messageData = serverMessage.getMessageData();

                        if (messageData == null) {
                            DatabaseLoader.closeConnectionIfOpen();
                            return event.edit().withComponents(ActionRow.of(Button.danger("no-work", "Unable to Display").disabled()));
                        }

                        DatabaseLoader.closeConnectionIfOpen();
                        return messageChannel.createMessage(messageSpecFromData(messageData).withContent("Made visible by " + event.getInteraction().getUser().getMention() + ":"))
                                .then(event.edit().withComponents(ActionRow.of(Button.success("it-worked", "Done!").disabled())));
                        //.then(event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.success("it-worked", "Done!").disabled())));

                    } else {
                        DatabaseLoader.closeConnectionIfOpen();
                        return event.edit().withComponents(ActionRow.of(Button.danger("no-work", "Unable to Display").disabled()));
                    }

                }).flatMap($ -> $)
                .then();
    }

    private Mono<Void> banUser(String punishmentId, String userId, Guild guild, Member mod, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        DiscordUser moderator = DiscordUser.findFirst("user_id_snowflake = ?", mod.getId().asLong());

        String auditString = "Button: Ban " + discordUser.getUserIdSnowflake() + " for case " + punishmentId;

        return CommandChecks.commandChecks(event, "ban", auditString).flatMap(aBoolean -> {
            if (!aBoolean) {
                return AuditLogger.addCommandToDB(event, auditString, false).then(Mono.error(new NoPermissionsException("No Permission")));
            }

            Member punisher = event.getInteraction().getMember().get();

            return guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).flatMap(punished ->
                    punishmentManager.checkIfPunisherHasHighestRole(punisher, punished, guild, event).flatMap(theBool -> {

                        if (!theBool) {
                            return AuditLogger.addCommandToDB(event, auditString, false).then(Mono.error(new NoPermissionsException("No Permission")));
                        }

                        return event.deferReply().flatMap(unused -> {
                            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
                            Punishment initialMute = getPunishmentFromId(punishmentId);

                            String reason = "Banned after moderator review. " +
                                    "Original case number `" + initialMute.getPunishmentId() + "` with reason:\n > " + initialMute.getPunishmentMessage();

                            Punishment punishment = Punishment.create(
                                    "user_id_punished", discordUser.getUserId(),
                                    "name_punished", punished.getUsername(),
                                    "discrim_punished", punished.getDiscriminator(),
                                    "user_id_punisher", moderator.getUserId(),
                                    "name_punisher", punisher.getUsername(),
                                    "discrim_punisher", punisher.getDiscriminator(),
                                    "server_id", discordServer.getServerId(),
                                    "punishment_type", "ban",
                                    "punishment_date", Instant.now().toEpochMilli(),
                                    "punishment_message", reason,
                                    "did_dm", true,
                                    "end_date_passed", false,
                                    "permanent", true,
                                    "automatic", false,
                                    "punishment_end_reason", "No reason provided.");
                            punishment.save();
                            punishment.refresh();
                            DatabaseLoader.closeConnectionIfOpen();

                            return loggingListener.onPunishment(event, punishment)
                                    .then(Notifier.notifyPunisherOfBan(event, punishment, punishment.getPunishmentMessage()))
                                    .then(Notifier.notifyPunished(guild, punishment, reason))
                                    .then(punishmentManager.discordBanUser(guild, discordUser.getUserIdSnowflake(), 1, reason))
                                    .then(event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.danger("it-worked", "User Banned").disabled())))
                                    .then(AuditLogger.addCommandToDB(event, auditString, true));
                        });
                    }));
        });
    }

    private Mono<Void> kickUser(String punishmentId, String userId, Guild guild, Member mod, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        DiscordUser moderator = DiscordUser.findFirst("user_id_snowflake = ?", mod.getId().asLong());

        String auditString = "Button: Kick " + discordUser.getUserIdSnowflake() + " for case " + punishmentId;

        return CommandChecks.commandChecks(event, "kick", auditString)
                .then(event.deferReply())
                .flatMap(aBoolean -> {

            Member punisher = event.getInteraction().getMember().get();
            return guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).flatMap(punished ->
                    punishmentManager.checkIfPunisherHasHighestRole(punisher, punished, guild, event).flatMap(theBool -> {
                        if (!theBool) {
                            return AuditLogger.addCommandToDB(event, auditString, false).then(Mono.error(new NoPermissionsException("No Permission")));
                        }
                            logger.info("after defer");

                            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
                            Punishment initialMute = getPunishmentFromId(punishmentId);

                            String reason = "Kicked after moderator review. " +
                                    "Original case number `" + initialMute.getPunishmentId() + "` with reason:\n > " + initialMute.getPunishmentMessage();

                            Punishment punishment = Punishment.create(
                                    "user_id_punished", discordUser.getUserId(),
                                    "name_punished", punished.getUsername(),
                                    "discrim_punished", punished.getDiscriminator(),
                                    "user_id_punisher", moderator.getUserId(),
                                    "name_punisher", punisher.getUsername(),
                                    "discrim_punisher", punisher.getDiscriminator(),
                                    "server_id", discordServer.getServerId(),
                                    "punishment_type", "kick",
                                    "punishment_date", Instant.now().toEpochMilli(),
                                    "punishment_message", reason,
                                    "did_dm", true,
                                    "end_date_passed", true,
                                    "permanent", true,
                                    "automatic", false,
                                    "punishment_end_reason", "No reason provided.");
                            punishment.save();
                            punishment.refresh();
                            DatabaseLoader.closeConnectionIfOpen();

                            logger.info("returning final mono");

                            return loggingListener.onPunishment(event, punishment)
                                    .then(Notifier.notifyPunisherOfKick(event, punishment, punishment.getPunishmentMessage()))
                                    .then(Notifier.notifyPunished(guild, punishment, reason))
                                    .then(punishmentManager.discordKickUser(guild, discordUser.getUserIdSnowflake(), reason))
                                    .then(event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.danger("it-worked", "User Kicked").disabled())))
                                    .then(AuditLogger.addCommandToDB(event, auditString, true));
                    }));
        });
    }

    private Mono<Void> unmuteUser(String punishmentId, String userId, Guild guild, Member moderator, ButtonInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = getDiscordUserFromId(userId);
        Punishment punishment = getPunishmentFromId(punishmentId);

        logger.info("unmuting user");

        return guild.getMemberById(Snowflake.of(discordUser.getUserIdSnowflake())).flatMap(mutedUser -> {

            String reason = "Unmuted by moderators after review of case `" + punishment.getPunishmentId() + "`, with original reason:\n > " + punishment.getPunishmentMessage();
            DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());
            Long mutedRoleId = serverProperties.getMutedRoleSnowflake();

            String auditString = "Button: Unmute " + discordUser.getUserIdSnowflake() + " for case " + punishment.getPunishmentId();

            return CommandChecks.commandChecks(event, "unmute", auditString).flatMap(aBoolean -> {
                if (!aBoolean) {
                    return AuditLogger.addCommandToDB(event, auditString, false).then(Mono.error(new NoPermissionsException("No Permission")));
                }

                logger.info("Passed command check");

                Member punisher = event.getInteraction().getMember().get();

                return punishmentManager.checkIfPunisherHasHighestRole(punisher, mutedUser, guild, event).flatMap(theBool -> {
                    if (!theBool) {
                        return AuditLogger.addCommandToDB(event, auditString, false).then(Mono.error(new NoPermissionsException("No Permission")));
                    }

                    logger.info("Passed highest role check");

                    if (mutedRoleId == null) {
                        DatabaseLoader.closeConnectionIfOpen();
                        return AuditLogger.addCommandToDB(event, auditString, false).then(Mono.error(new NoMutedRoleException("No Muted Role")));
                    }

                    return guild.getRoleById(Snowflake.of(mutedRoleId)).flatMap(mutedRole ->
                            event.deferReply().then(mutedUser.getRoles().any(role -> role.equals(mutedRole)).flatMap(theBoolean -> {
                        if (mutedRole != null && theBoolean) {
                            logger.info("Returning final mono");
                            return mutedUser.removeRole(Snowflake.of(mutedRoleId), reason)
                                    .then(manualPunishmentEnder.databaseEndPunishment(discordUser.getUserIdSnowflake(), guild, "unmute", reason, moderator, mutedUser))
                                    .then(Notifier.notifyModOfUnmute(event, mutedUser.getDisplayName(), reason))
                                    .then(event.getInteraction().getMessage().get().edit().withComponents(ActionRow.of(Button.success("it-worked", "User Unmuted").disabled())))
                                    .then(AuditLogger.addCommandToDB(event, auditString, true));
                        } else {
                            return AuditLogger.addCommandToDB(event, auditString, false).then(Mono.error(new UserNotMutedException("User Not Muted")));
                        }
                    })));
                });
            });
        });
    }


    private Punishment getPunishmentFromId(String punishmentId) {
        DatabaseLoader.openConnectionIfClosed();
        return Punishment.findFirst("id = ?", Integer.valueOf(punishmentId));
    }

    private DiscordUser getDiscordUserFromId(String userId) {
        DatabaseLoader.openConnectionIfClosed();
        return DiscordUser.findFirst("id = ?", Integer.valueOf(userId));
    }
}
