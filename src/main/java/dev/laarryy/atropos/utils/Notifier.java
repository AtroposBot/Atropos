package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.ServerMessage;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.WebhookExecuteRequest;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import discord4j.rest.util.MultipartRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public final class Notifier {
    private Notifier() {
    }

    //TODO: Do notifications on COMPLETE so the error stream lines up

    private final Logger logger = LogManager.getLogger(this);

    public static Mono<Void> sendResultsEmbed(ChatInputInteractionEvent event, EmbedCreateSpec embed) {

        return replyDeferredInteraction(event, embed);

        /*return Mono.from(event.getInteractionResponse().deleteInitialResponse())
                .thenReturn(event.getInteractionResponse().createFollowupMessage(
                        MultipartRequest.ofRequest(WebhookExecuteRequest
                                .builder()
                                .addEmbed(embed.asRequest())
                                .build())))
                .then();*/
    }

    private static Mono<Void> replyDeferredInteraction(ButtonInteractionEvent event, EmbedCreateSpec embed) {
        return event.getInteractionResponse().createFollowupMessage(
                MultipartRequest.ofRequest(
                        WebhookExecuteRequest
                                .builder()
                                .addEmbed(embed.asRequest())
                                .build())
        ).then();


                /*WebhookMessageEditRequest
                        .builder()
                        .addEmbed(embed.asRequest())
                        .build()).then();*/
    }

    private static Mono<Void> replyDeferredInteraction(ChatInputInteractionEvent event, EmbedCreateSpec embed) {
        Button deEphemeralize = Button.primary("deephemeralize", "Display Non-Ephemerally");

        return event.getInteractionResponse().editInitialResponse(
                WebhookMessageEditRequest
                        .builder()
                        .addEmbed(embed.asRequest())
                        .addComponent(ActionRow.of(deEphemeralize).getData())
                        .build())
                .flatMap(messageData -> {
                    DatabaseLoader.openConnectionIfClosed();

                    long messageIdSnowflake = messageData.id().asLong();
                    long userIdSnowflake = messageData.author().id().asLong();

                    int serverId = 0;

                    DiscordUser user = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);

                    if (user == null) {
                        user = DiscordUser.createIt("user_id_snowflake", userIdSnowflake, "date", Instant.now().toEpochMilli());
                    }

                    int userId = user.getUserId();
                    user.save();
                    user.refresh();

                    // Create message row in the table
                    ServerMessage message = ServerMessage.findOrCreateIt("message_id_snowflake", messageIdSnowflake, "server_id", serverId, "user_id", userId);

                    // Populate it

                    message.setServerId(serverId);
                    message.setUserId(userId);
                    message.setUserSnowflake(userIdSnowflake);
                    message.setDateEpochMilli(Instant.now().toEpochMilli());
                    message.setMessageData(messageData);
                    message.setDeleted(false);

                    message.save();

                    DatabaseLoader.closeConnectionIfOpen();
                    return Mono.empty();
                }).then();
    }

    public static Mono<Void> notifyPunisherForcebanComplete(ChatInputInteractionEvent event, String idInput) {
        return replyDeferredInteraction(event, forceBanCompleteEmbed(idInput)).then();
    }

    public static Mono<Void> notifyPunisher(ChatInputInteractionEvent event, Punishment punishment, String punishmentReason) {

        if (punishment.getPunishmentType().equals("note")) {
            return event.deferReply().withEphemeral(true).then(handlePunisherNotification(event, punishment, punishmentReason));
        } else {
            return event.deferReply().then(handlePunisherNotification(event, punishment, punishmentReason));
        }
    }

    public static Mono<Void> handlePunisherNotification(ChatInputInteractionEvent event, Punishment punishment, String punishmentReason) {
        String punishmentEnd;
        if (punishment.getEndDate() != null) {
            Instant endDate = Instant.ofEpochMilli(punishment.getEndDate());
            punishmentEnd = TimestampMaker.getTimestampFromEpochSecond(
                    endDate.getEpochSecond(),
                    TimestampMaker.TimestampType.RELATIVE);
        } else {
            punishmentEnd = "Never.";
        }

        return event.getOption("user").get().getValue().get().asUser().flatMap(user -> {
            String userName = user.getUsername();
            String caseId = String.valueOf(punishment.getPunishmentId());

            switch (punishment.getPunishmentType()) {
                case "warn" -> {
                    return replyDeferredInteraction(event, warnEmbed(userName, punishmentReason, caseId));
                }
                case "kick" -> {
                    return replyDeferredInteraction(event, kickEmbed(userName, punishmentReason, caseId));
                }
                case "ban" -> {
                    return replyDeferredInteraction(event, banEmbed(userName, punishmentEnd, punishmentReason, caseId));
                }
                case "mute" -> {
                    return replyDeferredInteraction(event, muteEmbed(userName, punishmentEnd, punishmentReason, caseId));
                }
                case "note" -> {
                    return replyDeferredInteraction(event, noteEmbed(userName, punishmentReason, caseId));
                }
                default -> {
                    return Mono.empty();
                }
            }
        });
    }

    public static Mono<Void> notifyPunisherOfBan(ButtonInteractionEvent event, Punishment punishment, String punishmentReason) {

        String punishmentEnd;
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser user = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        String userName = String.valueOf(user.getUserIdSnowflake());
        if (punishment.getEndDate() != null) {
            Instant endDate = Instant.ofEpochMilli(punishment.getEndDate());
            punishmentEnd = TimestampMaker.getTimestampFromEpochSecond(
                    endDate.getEpochSecond(),
                    TimestampMaker.TimestampType.RELATIVE);
        } else {
            punishmentEnd = "Never.";
        }

        String caseId = String.valueOf(punishment.getPunishmentId());
        DatabaseLoader.closeConnectionIfOpen();

        return replyDeferredInteraction(event, banEmbed(userName, punishmentEnd, punishmentReason, caseId));

    }

    public static Mono<Void> notifyPunisherOfKick(ButtonInteractionEvent event, Punishment punishment, String punishmentReason) {

        String punishmentEnd;
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser user = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        String userName = String.valueOf(user.getUserIdSnowflake());
        if (punishment.getEndDate() != null) {
            Instant endDate = Instant.ofEpochMilli(punishment.getEndDate());
            punishmentEnd = TimestampMaker.getTimestampFromEpochSecond(
                    endDate.getEpochSecond(),
                    TimestampMaker.TimestampType.RELATIVE);
        } else {
            punishmentEnd = "Never.";
        }

        String caseId = String.valueOf(punishment.getPunishmentId());
        DatabaseLoader.closeConnectionIfOpen();

        return replyDeferredInteraction(event, kickEmbed(userName, punishmentReason, caseId));
    }

    public static Mono<Void> notifyPunished(Guild guild, Punishment punishment, String punishmentReason) {
        DatabaseLoader.openConnectionIfClosed();

        if (punishment.getPunishmentType().equals("note")) {
            // Never notify of cases
            return Mono.empty();
        }

        DiscordUser discordUser = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        return guild.getClient().getUserById(Snowflake.of(discordUser.getUserIdSnowflake())).flatMap(punishedUser -> {
            String punishmentEnd;
            if (punishment.getEndDate() != null) {
                Instant endDate = Instant.ofEpochMilli(punishment.getEndDate());
                punishmentEnd = TimestampMaker.getTimestampFromEpochSecond(
                        endDate.getEpochSecond(),
                        TimestampMaker.TimestampType.RELATIVE);
            } else {
                punishmentEnd = "No end date provided.";
            }

            return guild.getSelfMember().flatMap(selfMember -> {
                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .title(guild.getName())
                        .author("Notice from Guild:", "", guild.getIconUrl(Image.Format.PNG).orElse(selfMember.getAvatarUrl()))
                        .description(punishedUser.getMention() + ", this message is to notify you of moderation action taken by the staff of "
                                + guild.getName()
                                + ". This incident will be recorded.")
                        .addField("Action Taken", punishment.getPunishmentType().toUpperCase(), true)
                        .addField("Reason", punishmentReason, false)
                        .addField("End Date", punishmentEnd, false)
                        .color(Color.RUST)
                        .footer("Case: " + punishment.getPunishmentId(), "")
                        .timestamp(Instant.now())
                        .build();

                punishment.setDMed(true);
                punishment.save();
                DatabaseLoader.closeConnectionIfOpen();

                return punishedUser.getPrivateChannel().flatMap(privateChannel -> privateChannel.createMessage(embed).then());
            });
        });
    }

    public static Mono<Void> notifyModOfUnban(ChatInputInteractionEvent event, String reason, long userId) {
        return replyDeferredInteraction(event, unbanEmbed(userId, reason));
    }

    public static Mono<Void> notifyModOfUnmute(ChatInputInteractionEvent event, String username, String reason) {
        return replyDeferredInteraction(event, unmuteEmbed(username, reason));
    }

    public static Mono<Void> notifyModOfUnmute(ButtonInteractionEvent event, String username, String reason) {
        return replyDeferredInteraction(event, unmuteEmbed(username, reason));
    }

    private static EmbedCreateSpec warnEmbed(String userName, String reason, String caseId) {
        return EmbedCreateSpec.builder()
                .title("Warned User: " + userName)
                .description("Successfully stored a record of this warning.")
                .addField("Reason", reason, false)
                .color(Color.ENDEAVOUR)
                .footer("Case ID: " + caseId, "")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec noteEmbed(String userName, String reason, String caseId) {
        return EmbedCreateSpec.builder()
                .title("Logged Note for User: " + userName)
                .description("Successfully stored a record of this note.")
                .addField("Reason", reason, false)
                .color(Color.ENDEAVOUR)
                .footer("Case ID: " + caseId, "")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec kickEmbed(String userName, String reason, String caseId) {
        return EmbedCreateSpec.builder()
                .title("Kicked User: " + userName)
                .description("Successfully stored a record of this kick.")
                .addField("Reason", reason, false)
                .color(Color.ENDEAVOUR)
                .footer("Case ID: " + caseId, "")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec banEmbed(String userName, String punishmentEnd, String reason, String caseId) {
        return EmbedCreateSpec.builder()
                .title("Banned User: " + userName)
                .description("Successfully stored a record of this ban.")
                .addField("Reason", reason, false)
                .addField("Ends", punishmentEnd, false)
                .color(Color.ENDEAVOUR)
                .footer("Case ID: " + caseId, "")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec forceBanCompleteEmbed(String idInput) {
        return EmbedCreateSpec.builder()
                .title("Forceban Complete")
                .description("Banned the following IDs:\n" + idInput)
                .color(Color.SEA_GREEN)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec unbanEmbed(long userId, String reason) {
        return EmbedCreateSpec.builder()
                .title("Unbanned User: " + userId)
                .description("Successfully stored a record of this unban.")
                .addField("Reason", reason, false)
                .color(Color.ENDEAVOUR)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec muteEmbed(String userName, String punishmentEnd, String reason, String caseId) {
        return EmbedCreateSpec.builder()
                .title("Muted User: " + userName)
                .description("Successfully stored a record of this mute.")
                .addField("Reason", reason, false)
                .addField("Ends", punishmentEnd, false)
                .color(Color.ENDEAVOUR)
                .footer("Case ID: " + caseId, "")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec unmuteEmbed(String userName, String reason) {
        return EmbedCreateSpec.builder()
                .title("Unmuted User: " + userName)
                .description("Successfully stored a record of this unmute.")
                .addField("Reason", reason, false)
                .color(Color.ENDEAVOUR)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec noBotPermissionsEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("No Bot Permission")
                .description("Action aborted because I do not have the required permissions to operate.")
                .addField("Required Permissions",
                        """
                                ```diff
                                + View Channels
                                + Manage Channels
                                + Manage Roles
                                --- I need these to mute people and see chat
                                + View Audit Log
                                --- I need this to log who does what
                                + Manage Members
                                --- I need this to mute people and correct nickname hoists
                                + Kick Members
                                + Ban Members
                                --- I need these to kick and ban
                                + Send Messages
                                + Use Public Threads
                                + Use Private Threads
                                + Use Application Commands
                                --- I need these so that I can deny them to muted users - that's how discord's permission system works.
                                + Use External Emoji
                                --- I need these to log and interact with mods
                                + Manage Messages
                                + Read Message History
                                --- I need these to moderate chat
                                + Mute Members
                                --- I need this to mute people
                                ```
                                """, false)
                .addField("Additional Information", "Please set my highest role to be above anyone I " +
                        "may need to punish in the role hierarchy, and rest assured that I will only " +
                        "allow users to punish those who are below them (even if I am above everyone involved). " +
                        "\n While I lack any of the basic permissions required to function, none of my commands will " +
                        "work. This is so that I don't run into errors in trying to carry out your wishes.", false)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec botRoleTooLow() {
        return EmbedCreateSpec.builder()
                .title("Error: Bot Role Too Low")
                .description("Action aborted because my highest role is too low.")
                .addField("Information", "In order to take this action, I need to have a higher role than the target.", false)
                .color(Color.RUBY)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec unableToDmEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Unable to DM user.")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec inputTooLongEmbed() {
        return EmbedCreateSpec.builder()
                .title("Error: Input Too Long")
                .description("The entry must not be 200 characters or longer. It would be best if you aimed for a 2-15 character trigger.")
                .color(Color.RUBY)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec durationTooLongEmbed() {
        return EmbedCreateSpec.builder()
                .title("Error: Duration Too Long")
                .description("The duration must be no longer than 2 days.")
                .color(Color.RUBY)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec tooManyEntriesEmbed() {
        return EmbedCreateSpec.builder()
                .title("Error: Too Many Entries")
                .description("You may only set up to 30 entries in the blacklist. If you would like to catch more " +
                        "things, you can use RegEx (regular expressions) in the entry field to craft comprehensive entries.")
                .color(Color.RUBY)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec alreadyAssignedEmbed() {
        return EmbedCreateSpec.builder()
                .title("Error: Already Assigned")
                .description("This permission is already assigned to this user in this guild.")
                .color(Color.RUBY)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec alreadyBlacklistedEmbed() {
        return EmbedCreateSpec.builder()
                .title("Error: Already Blacklisted")
                .description("This entry is already blacklisted in this guild.")
                .color(Color.RUBY)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec fourOhFourEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: Not Found")
                .description("Action aborted because query could not be found.")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec cannotTargetBotsEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: Cannot Target Bots")
                .description("Action aborted because you may not punish bots.")
                .addField("Detail", "Administrative action against bot users must be done manually by " +
                        "someone with discord-side permission to do so.", false)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec malformedInputEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: Malformed Input")
                .description("Action aborted because input cannot be accepted.")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec noResultsEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("No Results")
                .description("No results found for specified query.")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec invalidChannelEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.ENDEAVOUR)
                .title("Invalid Channel")
                .description("This channel cannot be used for this.")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec noPermissionsEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: No Permission")
                .description("Action aborted due to lack of permission.")
                .addField("Detail", "In order to take this action, you must be an administrator or have " +
                                "its permission granted to you. If you have been given permission, this action was denied due" +
                                " to its target having higher roles than you, or being an administrator while you are not. ",
                        true)
                .footer("To gain permission, have an administrator run /permission add <role> <command name> for a " +
                        "role that you have. If you have permission to run the /permission command, you can run " +
                        "/permission list <role> to see a role's permissions", "")
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec noMutedRoleEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: No Muted Role")
                .description("Action aborted due to the fact that there is no muted role assigned to this server.")
                .addField("Detail", "In order to take this action, you must first mute someone with this " +
                                "bot. This is because the bot currently does not have a muted role in this guild which " +
                                "it can use to mute users with.",
                        true)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec nullServerEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: No Guild Found")
                .description("Action aborted due to lack of guild.")
                .addField("Detail", "In order to take this action, the bot needs to know what guild to do" +
                        " it in. For some reason, that guild cannot be found. If you're trying to take this action in" +
                        " DMs, please be aware that it will not work.", true)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec noUserEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: No User")
                .description("Action aborted due to lack of user.")
                .addField("Detail", "In order to take this action, the bot needs to know which user to " +
                        "act upon. For some reason, that user cannot be found.", true)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec noMemberEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: No Member")
                .description("Action aborted due to lack of member.")
                .addField("Detail", "In order to take this action, the bot needs to know which user to " +
                        "act upon, and the user needs to be a member of this guild. For some reason, that member cannot be found.", true)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec invalidDurationEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: Invalid Duration")
                .description("Action aborted due to invalid duration value.")
                .addField("Detail", "In order to take this action and apply an appropriate duration, the" +
                        " bot needs a valid input. The input provided was not decipherable by us, the small gnomes" +
                        " volunteering to run this bot. Please try again!", true)
                .addField("Valid Formats",
                        """
                                Here are a few valid example formats that you might find useful in formulating your next duration:\s
                                 `2 years 7 months 3 weeks 4 days 34 minutes`
                                 `2year7month3week4day34min`
                                 `2y7mo3w4d34m`""",
                        false)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec userNotMutedEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: User Not Muted")
                .description("Action aborted due to selected user not being muted.")
                .addField("Detail", "In order to take this action, the user to unmute needs first to be " +
                        "muted. Double check that you didn't accidentally select someone with a similar name.", true)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec punishmentAlreadyAppliedEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: Punishment Already Applied")
                .description("Action aborted due to selected user already having a current punishment of this type.")
                .addField("Detail", "In order to take this action, the user must not already have an " +
                                "active punishment of this type on their record. You must first unmute or unban the user.",
                        true)
                .timestamp(Instant.now())
                .build();
    }

    private static EmbedCreateSpec unknownErrorEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Error: Unknown")
                .description("Error unknown. You certainly should not be seeing this! Contact bot author with timestamp of this error, please.")
                .timestamp(Instant.now())
                .build();
    }
}
