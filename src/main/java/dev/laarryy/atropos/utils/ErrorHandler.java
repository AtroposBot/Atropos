package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.exceptions.AlreadyAppliedException;
import dev.laarryy.atropos.exceptions.AlreadyAssignedException;
import dev.laarryy.atropos.exceptions.AlreadyBlacklistedException;
import dev.laarryy.atropos.exceptions.BotPermissionsException;
import dev.laarryy.atropos.exceptions.BotRoleException;
import dev.laarryy.atropos.exceptions.CannotTargetBotsException;
import dev.laarryy.atropos.exceptions.DurationTooLongException;
import dev.laarryy.atropos.exceptions.InputTooLongException;
import dev.laarryy.atropos.exceptions.InvalidChannelException;
import dev.laarryy.atropos.exceptions.InvalidDurationException;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NoMemberException;
import dev.laarryy.atropos.exceptions.NoMutedRoleException;
import dev.laarryy.atropos.exceptions.NoPermissionsException;
import dev.laarryy.atropos.exceptions.NoResultsException;
import dev.laarryy.atropos.exceptions.NoUserException;
import dev.laarryy.atropos.exceptions.NotFoundException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.exceptions.TooManyEntriesException;
import dev.laarryy.atropos.exceptions.UserNotMutedExcception;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class ErrorHandler {

    public static Mono<Void> handleError(Throwable error, ChatInputInteractionEvent event) {

        if (error instanceof NoPermissionsException) {
            return Mono.from(
                    event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                            .builder()
                            .addEmbed(noPermissionsEmbed().asRequest())
                            .build()))
                    .then();
        }

        if (error instanceof BotPermissionsException) {
            return Mono.from(
                    event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                            .builder()
                            .addEmbed(noBotPermissionsEmbed().asRequest())
                            .build()))
                    .then();
        }

        if (error instanceof NullServerException) {
            return Mono.from(
                    event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                            .builder()
                            .addEmbed(nullServerEmbed().asRequest())
                            .build()))
                    .then();
        }

        if (error instanceof BotRoleException) {
            return Mono.from(
                    event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                            .builder()
                            .addEmbed(botRoleTooLow().asRequest())
                            .build()))
                    .then();
        }

        if (error instanceof AlreadyAppliedException) {
            return Mono.from(
                    event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                            .builder()
                            .addEmbed(punishmentAlreadyAppliedEmbed().asRequest())
                            .build()))
                    .then();
        }

        if (error instanceof AlreadyAssignedException) {
            return Mono.from(
                    event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                            .builder()
                            .addEmbed(alreadyAssignedEmbed().asRequest())
                            .build()))
                    .then();
        }

        if (error instanceof AlreadyBlacklistedException) {
            return Mono.from(
                    event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                            .builder()
                            .addEmbed(alreadyBlacklistedEmbed().asRequest())
                            .build()))
                    .then();
        }

        if (error instanceof CannotTargetBotsException) {
            return Mono.from(
                    event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                            .builder()
                            .addEmbed(cannotTargetBotsEmbed().asRequest())
                            .build()))
                    .then();
        }

        if (error instanceof DurationTooLongException) {
            return Mono.from(
                    event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                            .builder()
                            .addEmbed(durationTooLongEmbed().asRequest())
                            .build()))
                    .then();
        }

        if (error instanceof InputTooLongException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(inputTooLongEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof InvalidChannelException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(invalidChannelEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof InvalidDurationException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(invalidDurationEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof MalformedInputException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(malformedInputEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof NoMemberException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(noMemberEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof NoMutedRoleException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(noMutedRoleEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof NoResultsException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(noResultsEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof NotFoundException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(fourOhFourEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof NoUserException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(noUserEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof TooManyEntriesException) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(tooManyEntriesEmbed().asRequest())
                                    .build()))
                    .then();
        }

        if (error instanceof UserNotMutedExcception) {
            return Mono.from(
                            event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                    .builder()
                                    .addEmbed(userNotMutedEmbed().asRequest())
                                    .build()))
                    .then();
        }


        return Mono.from(
                        event.getInteractionResponse().editInitialResponse(WebhookMessageEditRequest
                                .builder()
                                .addEmbed(unknownErrorEmbed().asRequest())
                                .build()))
                .then();

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
