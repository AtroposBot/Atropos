package dev.laarryy.eris.utils;

import dev.laarryy.eris.models.users.DiscordUser;
import dev.laarryy.eris.models.users.Punishment;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;

public final class Notifier {
    private Notifier() {}
    private final Logger logger = LogManager.getLogger(this);

    public static void notifyPunisherForcebanComplete(ChatInputInteractionEvent event) {
        event.getInteractionResponse().editInitialResponse(
                        WebhookMessageEditRequest
                                .builder()
                                .addEmbed(forceBanCompleteEmbed().asRequest())
                                .build()).block();
    }

    public static void notifyPunisher(ChatInputInteractionEvent event, Punishment punishment, String punishmentReason) {

        String punishmentEnd;
        if (punishment.getEndDate() != null) {
            Instant endDate = Instant.ofEpochMilli(punishment.getEndDate());
            punishmentEnd = TimestampMaker.getTimestampFromEpochSecond(
                    endDate.getEpochSecond(),
                    TimestampMaker.TimestampType.RELATIVE);
        } else {
            punishmentEnd = "Never.";
        }

        String userName = event.getOption("user").get().getValue().get().asUser().block().getUsername();
        String caseId = String.valueOf(punishment.getPunishmentId());

        switch (punishment.getPunishmentType()) {
            case "warn" -> event.reply().withEmbeds(warnEmbed(userName, punishmentReason, caseId)).subscribe();
            case "kick" -> event.reply().withEmbeds(kickEmbed(userName, punishmentReason, caseId)).subscribe();
            case "ban" -> event.reply().withEmbeds(banEmbed(userName, punishmentEnd, punishmentReason, caseId)).subscribe();
            case "mute" -> event.reply().withEmbeds(muteEmbed(userName, punishmentEnd, punishmentReason, caseId)).subscribe();
            case "case" -> event.reply().withEmbeds(caseEmbed(userName, punishmentReason, caseId)).withEphemeral(true).subscribe();
        }
    }

    public static void notifyPunished(Guild guild, Punishment punishment, String punishmentReason) {

        if (punishment.getPunishmentType().equals("case")) {
            // Never notify of cases
            return;
        }

        DiscordUser discordUser = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        User punishedUser = guild.getClient().getUserById(Snowflake.of(discordUser.getUserIdSnowflake())).block();

        String punishmentEnd;
        if (punishment.getEndDate() != null) {
            Instant endDate = Instant.ofEpochMilli(punishment.getEndDate());
            punishmentEnd = TimestampMaker.getTimestampFromEpochSecond(
                    endDate.getEpochSecond(),
                    TimestampMaker.TimestampType.RELATIVE);
        } else {
            punishmentEnd = "No end date provided.";
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(guild.getName())
                .author("Notice from Guild:", "", guild.getIconUrl(Image.Format.PNG).orElse(guild.getSelfMember().block().getAvatarUrl()))
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

        try {
            PrivateChannel privateChannel = punishedUser.getPrivateChannel().block();
            privateChannel.createMessage(embed).block();
        } catch (Exception ignored) {}
    }

    public static void notifyModOfUnban(ChatInputInteractionEvent event, String reason, long userId) {
        event.reply().withEmbeds(unbanEmbed(userId, reason)).subscribe();
    }

    public static void notifyModOfUnmute(ChatInputInteractionEvent event, String username, String reason) {
        event.reply().withEmbeds(unmuteEmbed(username, reason)).subscribe();
    }

    public static void notifyModOfUnmute(ButtonInteractionEvent event, String username, String reason) {
        event.reply().withEmbeds(unmuteEmbed(username, reason)).subscribe();
    }

    public static void notifyCommandUserOfError(ChatInputInteractionEvent event, String errorType) {
        switch (errorType) {
            case "noPermission" -> event.reply().withEmbeds(noPermissionsEmbed()).withEphemeral(true).subscribe();
            case "noBotPermission" -> event.reply().withEmbeds(noBotPermissionsEmbed()).withEphemeral(true).subscribe();
            case "botRoleTooLow" -> event.reply().withEmbeds(botRoleTooLow()).withEphemeral(true).subscribe();
            case "nullServer" -> event.reply().withEmbeds(nullServerEmbed()).withEphemeral(true).subscribe();
            case "noUser" -> event.reply().withEmbeds(noUserEmbed()).withEphemeral(true).subscribe();
            case "invalidDuration" -> event.reply().withEmbeds(invalidDurationEmbed()).withEphemeral(true).subscribe();
            case "alreadyAssigned" -> event.reply().withEmbeds(alreadyAssignedEmbed()).withEphemeral(true).subscribe();
            case "alreadyBlacklisted" -> event.reply().withEmbeds(alreadyBlacklistedEmbed()).withEphemeral(true).subscribe();
            case "noMutedRole" -> event.reply().withEmbeds(noMutedRoleEmbed()).withEphemeral(true).subscribe();
            case "userNotMuted" -> event.reply().withEmbeds(userNotMutedEmbed()).withEphemeral(true).subscribe();
            case "alreadyApplied" -> event.reply().withEmbeds(punishmentAlreadyAppliedEmbed()).withEphemeral(true).subscribe();
            case "404" -> event.reply().withEmbeds(fourOhFourEmbed()).withEphemeral(true).subscribe();
            case "malformedInput" -> event.reply().withEmbeds(malformedInputEmbed()).withEphemeral(true).subscribe();
            case "noResults" -> event.reply().withEmbeds(noResultsEmbed()).subscribe();
            case "inputTooLong" -> event.reply().withEmbeds(inputTooLongEmbed()).withEphemeral(true).subscribe();
            case "tooManyEntries" -> event.reply().withEmbeds(tooManyEntriesEmbed()).withEphemeral(true).subscribe();
            case "cannotTargetBots" -> event.reply().withEmbeds(cannotTargetBotsEmbed()).withEphemeral(true).subscribe();
            case "invalidChannel" -> event.reply().withEmbeds(invalidChannelEmbed()).subscribe();
            case "durationTooLong" -> event.reply().withEmbeds(durationTooLongEmbed()).withEphemeral(true).subscribe();
            default -> event.reply().withEmbeds(unknownErrorEmbed()).withEphemeral(true).subscribe();
        }
    }

    public static void notifyCommandUserOfError(ButtonInteractionEvent event, String errorType) {
        switch (errorType) {
            case "noPermission" -> event.reply().withEmbeds(noPermissionsEmbed()).withEphemeral(true).subscribe();
            case "noBotPermission" -> event.reply().withEmbeds(noBotPermissionsEmbed()).withEphemeral(true).subscribe();
            case "botRoleTooLow" -> event.reply().withEmbeds(botRoleTooLow()).withEphemeral(true).subscribe();
            case "nullServer" -> event.reply().withEmbeds(nullServerEmbed()).withEphemeral(true).subscribe();
            case "noUser" -> event.reply().withEmbeds(noUserEmbed()).withEphemeral(true).subscribe();
            case "invalidDuration" -> event.reply().withEmbeds(invalidDurationEmbed()).withEphemeral(true).subscribe();
            case "alreadyAssigned" -> event.reply().withEmbeds(alreadyAssignedEmbed()).withEphemeral(true).subscribe();
            case "alreadyBlacklisted" -> event.reply().withEmbeds(alreadyBlacklistedEmbed()).withEphemeral(true).subscribe();
            case "noMutedRole" -> event.reply().withEmbeds(noMutedRoleEmbed()).withEphemeral(true).subscribe();
            case "userNotMuted" -> event.reply().withEmbeds(userNotMutedEmbed()).withEphemeral(true).subscribe();
            case "alreadyApplied" -> event.reply().withEmbeds(punishmentAlreadyAppliedEmbed()).withEphemeral(true).subscribe();
            case "404" -> event.reply().withEmbeds(fourOhFourEmbed()).withEphemeral(true).subscribe();
            case "malformedInput" -> event.reply().withEmbeds(malformedInputEmbed()).withEphemeral(true).subscribe();
            case "noResults" -> event.reply().withEmbeds(noResultsEmbed()).subscribe();
            case "inputTooLong" -> event.reply().withEmbeds(inputTooLongEmbed()).withEphemeral(true).subscribe();
            case "tooManyEntries" -> event.reply().withEmbeds(tooManyEntriesEmbed()).withEphemeral(true).subscribe();
            case "cannotTargetBots" -> event.reply().withEmbeds(cannotTargetBotsEmbed()).withEphemeral(true).subscribe();
            case "invalidChannel" -> event.reply().withEmbeds(invalidChannelEmbed()).subscribe();
            case "durationTooLong" -> event.reply().withEmbeds(durationTooLongEmbed()).withEphemeral(true).subscribe();
            default -> event.reply().withEmbeds(unknownErrorEmbed()).withEphemeral(true).subscribe();
        }
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

    private static EmbedCreateSpec caseEmbed(String userName, String reason, String caseId) {
        return EmbedCreateSpec.builder()
                .title("Logged Case for User: " + userName)
                .description("Successfully stored a record of this case.")
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

    private static EmbedCreateSpec forceBanCompleteEmbed() {
        return EmbedCreateSpec.builder()
                .title("Forceban Complete")
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
