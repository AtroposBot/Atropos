package dev.laarryy.atropos.commands.raid;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.exceptions.DurationTooLongException;
import dev.laarryy.atropos.exceptions.InvalidDurationException;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NotFoundException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.joins.ServerUser;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.CommandChecks;
import dev.laarryy.atropos.utils.DurationParser;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public class RemoveSinceCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    List<ApplicationCommandOptionChoiceData> optionChoiceDataList = List.of(
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Ban")
                    .value("ban")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Kick")
                    .value("kick")
                    .build());

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("removesince")
            .description("Kick or ban users that have joined since a specified duration")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("type")
                    .description("Kick them, or ban them?")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .choices(optionChoiceDataList)
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("duration")
                    .description("Remove newly joined users since how long ago? Format: 1mo2w3d13h45m")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {
        return CommandChecks.commandChecks(event, request.name())
                .then(event.getInteraction().getGuild().flatMap(guild -> {
            if (event.getOption("type").isEmpty()
                    || event.getOption("duration").isEmpty()
                    || event.getOption("type").get().getValue().isEmpty()
                    || event.getOption("duration").get().getValue().isEmpty()) {
                return Mono.error(new MalformedInputException("Malformed Input"));
            }

            String type;
            List<ServerUser> serverUserList;
            try (final var usage = DatabaseLoader.use()) {
                DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
                if (discordServer == null) {
                    return Mono.error(new NullServerException("Null Server"));
                }

                type = event.getOption("type").get().getValue().get().asString();
                String durationInput = event.getOption("duration").get().getValue().get().asString();

                Duration duration;
                try {
                    duration = DurationParser.parseDuration(durationInput);
                } catch (Exception e) {
                    return Mono.error(new InvalidDurationException("Invalid Duration"));
                }

                if (duration.toDays() > 2) {
                    return AuditLogger.addCommandToDB(event, false).then(Mono.error(new DurationTooLongException("Duration Too Long")));
                }

                Instant startPoint = Instant.now().minus(duration);
                serverUserList = ServerUser.find("server_id = ? and date > ?", discordServer.getServerId(), startPoint.toEpochMilli());
                if (serverUserList == null || serverUserList.isEmpty()) {
                    return Mono.error(new NotFoundException("404 Not Found"));
                }
            }

            if (type.equals("ban")) {
                return Flux.fromIterable(serverUserList)
                        .flatMap(serverUser -> this.banUsers(serverUser, guild, event.getInteraction().getMember().get()))
//                            .filter(string -> !string.equals("none")) // banUsers returns either the snowflake or an empty mono
                        .reduce("%s %s"::formatted)
                        .map("```%n%s%n```"::formatted)
                        .flatMap(affectedUsers -> {
                            if (affectedUsers.length() >= 3980) {
                                affectedUsers = affectedUsers.substring(0, 4000) + "...```\n[Content too long, has been limited]";
                            }

                            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                    .title(EmojiManager.getUserBan() + " Success")
                                    .color(Color.SEA_GREEN)
                                    .description("**Affected Users**\n" + affectedUsers)
                                    .timestamp(Instant.now())
                                    .build();

                            return Notifier.sendResultsEmbed(event, embed);
                        }).then(AuditLogger.addCommandToDB(event, true));
            }

            if (type.equals("kick")) {
                return Flux.fromIterable(serverUserList)
                        .flatMap(serverUser -> this.kickUsers(serverUser, guild, event.getInteraction().getMember().get()))
//                            .filter(stringMono -> !stringMono.equals("none")) // kickUsers returns either the snowflake or an empty mono
                        .reduce("%s %s"::formatted)
                        .map("```%n%s%n```"::formatted)
                        .flatMap(affectedUsers -> {
                            if (affectedUsers.length() >= 3980) {
                                affectedUsers = affectedUsers.substring(0, 4000) + "...```\n[Content too long, has been limited]";
                            }

                            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                    .title(EmojiManager.getUserKick() + " Success")
                                    .color(Color.SEA_GREEN)
                                    .description("**Affected Users**\n" + affectedUsers)
                                    .timestamp(Instant.now())
                                    .build();

                            return Notifier.sendResultsEmbed(event, embed);
                        }).then(AuditLogger.addCommandToDB(event, true));
            }

            return Mono.empty();
        }));
    }

    private Mono<String> banUsers(ServerUser serverUser, Guild guild, Member member) {
        long userId = DatabaseLoader.use(() -> {
            DiscordUser discordUser = DiscordUser.findFirst("id = ?", serverUser.getUserId());
            return discordUser.getUserIdSnowflake();
        });

        return guild.getSelfMember().flatMap(selfMember -> {
            if (selfMember.getId().asLong() == userId) {
                return Mono.empty();
            } else {
                return guild.getMemberById(Snowflake.of(userId)).flatMap(memberById -> {
                    if (memberById.isBot()) {
                        return Mono.empty();
                    } else {
                        Set<Snowflake> snowflakeSet = memberById.getRoleIds();
                        return member.hasHigherRoles(snowflakeSet).flatMap(memberHasHigherRoles -> {
                            if (!snowflakeSet.isEmpty() && !memberHasHigherRoles) {
                                return Mono.empty();
                            } else {
                                return guild.ban(Snowflake.of(userId)).withReason("Banned as part of anti-raid measures").withDeleteMessageDays(2)
                                        .thenReturn(String.valueOf(userId));
                            }
                        });
                    }
                });
            }
        });
    }

    private Mono<String> kickUsers(ServerUser serverUser, Guild guild, Member member) {
        long userId = DatabaseLoader.use(() -> {
            DiscordUser discordUser = DiscordUser.findFirst("id = ?", serverUser.getUserId());
            return discordUser.getUserIdSnowflake();
        });

        return guild.getSelfMember().flatMap(selfMember -> {
            if (selfMember.getId().asLong() == userId) {
                return Mono.empty();
            } else {
                return guild.getMemberById(Snowflake.of(userId)).flatMap(memberById -> {
                    if (memberById.isBot()) {
                        return Mono.empty();
                    } else {
                        Set<Snowflake> snowflakeSet = memberById.getRoleIds();
                        return member.hasHigherRoles(snowflakeSet).flatMap(memberHasHigherRoles -> {
                            if (!snowflakeSet.isEmpty() && !memberHasHigherRoles) {
                                return Mono.empty();
                            } else {
                                return guild.kick(Snowflake.of(userId), "Kicked as part of Anti-Raid Measures")
                                        .thenReturn(String.valueOf(userId));
                            }
                        });
                    }
                });
            }
        });
    }
}
