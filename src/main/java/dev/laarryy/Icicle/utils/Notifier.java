package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.models.users.Punishment;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public final class Notifier {
    private Notifier() {}
    private final Logger logger = LogManager.getLogger(this);

    public static void notifyPunisherForcebanComplete(SlashCommandEvent event) {
        event.reply("Forceban completed successfully.").withEphemeral(true).subscribe();
    }

    public static void notifyPunisher(SlashCommandEvent event, Punishment punishment, String punishmentReason) {

        String punishmentEnd;
        if (punishment.getEndDate() != null) {
            Instant endDate = Instant.ofEpochMilli(punishment.getEndDate());
            punishmentEnd = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(ZoneId.systemDefault()).format(endDate);
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

    public static void notifyPunished(SlashCommandEvent event, Punishment punishment, String punishmentReason) {

        if (punishment.getPunishmentType().equals("case")) {
            // Never notify of cases
            return;
        }

        Guild guild = event.getInteraction().getGuild().block();
        User punishedUser = event.getOption("user").get().getValue().get().asUser().block();

        String punishmentEnd;
        if (punishment.getEndDate() != null) {
            Instant endDate = Instant.ofEpochMilli(punishment.getEndDate());
            punishmentEnd = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(ZoneId.systemDefault()).format(endDate);
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
        } catch (Exception exception) {
            event.getInteraction().getChannel().block().createMessage(unableToDmEmbed()).block();
        }
    }

    public static void notifyCommandUserOfError(SlashCommandEvent event, String errorType) {
        switch (errorType) {
            case "noPermission" -> event.reply().withEmbeds(noPermissionsEmbed()).withEphemeral(true).subscribe();
            case "noBotPermission" -> event.reply().withEmbeds(noBotPermissionsEmbed()).withEphemeral(true).subscribe();
            case "nullServer" -> event.reply().withEmbeds(nullServerEmbed()).withEphemeral(true).subscribe();
            case "noUser" -> event.reply().withEmbeds(noUserEmbed()).withEphemeral(true).subscribe();
            case "invalidDuration" -> event.reply().withEmbeds(invalidDurationEmbed()).withEphemeral(true).subscribe();
            case "noMutedRole" -> event.reply().withEmbeds(noMutedRoleEmbed()).withEphemeral(true).subscribe();
            case "userNotMuted" -> event.reply().withEmbeds(userNotMutedEmbed()).withEphemeral(true).subscribe();
            case "alreadyApplied" -> event.reply().withEmbeds(punishmentAlreadyAppliedEmbed()).withEphemeral(true).subscribe();
            case "404" -> event.reply().withEmbeds(fourOhFourEmbed()).withEphemeral(true).subscribe();
            case "malformedInput" -> event.reply().withEmbeds(malformedInputEmbed()).withEphemeral(true).subscribe();
            case "noResults" -> event.reply().withEmbeds(noResultsEmbed()).subscribe();
            case "cannotTargetBots" -> event.reply().withEmbeds(cannotTargetBotsEmbed()).subscribe();
            case "invalidChannel" -> event.reply().withEmbeds(invalidChannelEmbed()).subscribe();
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

    private static EmbedCreateSpec unbanEmbed(String userId, String reason) {
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

    private static EmbedCreateSpec unmuteEmbed(String userName, String reason, String caseId) {
        return EmbedCreateSpec.builder()
                .title("Muted User: " + userName)
                .description("Successfully stored a record of this mute.")
                .addField("Reason", reason, false)
                .color(Color.ENDEAVOUR)
                .footer("Case ID: " + caseId, "")
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

    private static EmbedCreateSpec unableToDmEmbed() {
        return EmbedCreateSpec.builder()
                .color(Color.RUBY)
                .title("Unable to DM user.")
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
