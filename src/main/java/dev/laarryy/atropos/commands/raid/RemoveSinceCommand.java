package dev.laarryy.atropos.commands.raid;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.exceptions.DurationTooLongException;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NoPermissionsException;
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
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
        return Mono.from(CommandChecks.commandChecks(event, request.name()))
                .flatMap(aBoolean -> {
                    if (!aBoolean) {
                        return Mono.error(new NoPermissionsException("No Permission"));
                    }
                    return Mono.from(event.getInteraction().getGuild()).publishOn(Schedulers.boundedElastic()).publishOn(Schedulers.boundedElastic()).flatMap(guild -> {
                        if (event.getOption("type").isEmpty()
                                || event.getOption("duration").isEmpty()
                                || event.getOption("type").get().getValue().isEmpty()
                                || event.getOption("duration").get().getValue().isEmpty()) {
                            return Mono.error(new MalformedInputException("Malformed Input"));
                        }

                        DatabaseLoader.openConnectionIfClosed();
                        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
                        if (discordServer == null) {
                            DatabaseLoader.closeConnectionIfOpen();
                            return Mono.error(new NullServerException("Null Server"));
                        }

                        String type = event.getOption("type").get().getValue().get().asString();

                        String durationInput = event.getOption("duration").get().getValue().get().asString();

                        Duration duration = DurationParser.parseDuration(durationInput);

                        if (duration.toDays() > 2) {
                            AuditLogger.addCommandToDB(event, false);
                            DatabaseLoader.closeConnectionIfOpen();
                            return Mono.error(new DurationTooLongException("Duration Too Long"));
                        }

                        Instant startPoint = Instant.now().minus(duration);

                        List<ServerUser> serverUserList = ServerUser.find("server_id = ? and date > ?", discordServer.getServerId(), startPoint.toEpochMilli());

                        if (serverUserList == null || serverUserList.isEmpty()) {
                            DatabaseLoader.closeConnectionIfOpen();
                            return Mono.error(new NotFoundException("404 Not Found"));
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("```\n");

                        if (type.equals("ban")) {
                            return Flux.fromIterable(serverUserList)
                                    .map(serverUser -> this.banUsers(serverUser, guild, event.getInteraction().getMember().get()))
                                    .filter(string -> !string.equals("none"))
                                    .map(string -> sb.append(string).append(" "))
                                    .flatMap(stb -> {
                                        sb.append("```");

                                        String affectedUsers = sb.toString();

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
                                    }).then();
                        }

                        if (type.equals("kick")) {
                            return Flux.fromIterable(serverUserList)
                                    .map(serverUser -> this.kickUsers(serverUser, guild, event.getInteraction().getMember().get()))
                                    .filter(stringMono -> !stringMono.equals("none"))
                                    .map(string -> sb.append(string).append(" "))
                                    .flatMap(stb -> {
                                        sb.append("```");

                                        String affectedUsers = sb.toString();

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
                                    }).then();
                        }
                        DatabaseLoader.closeConnectionIfOpen();
                        return Mono.empty();
                    });


                });
    }

        private Mono<String> banUsers (ServerUser serverUser, Guild guild, Member member){
            DatabaseLoader.openConnectionIfClosed();
            DiscordUser discordUser = DiscordUser.findFirst("id = ?", serverUser.getUserId());
            long userId = discordUser.getUserIdSnowflake();

            return Mono.from(guild.getSelfMember()).flatMap(selfMember -> {
                if (selfMember.getId().asLong() == userId) {
                    return Mono.just("");
                } else {
                    return Mono.from(guild.getMemberById(Snowflake.of(userId))).flatMap(memberById -> {
                        if (memberById.isBot()) {
                            return Mono.just("");
                        }
                        else {
                            Set<Snowflake> snowflakeSet = memberById.getRoleIds();
                            return Mono.from(member.hasHigherRoles(snowflakeSet)).flatMap(memberHasHigherRoles -> {
                                if (!snowflakeSet.isEmpty() && !memberHasHigherRoles) {
                                    return Mono.just("");
                                } else {
                                    return Mono.from(guild.ban(Snowflake.of(userId)).withReason("Banned as part of anti-raid measures").withDeleteMessageDays(2))
                                            .thenReturn(String.valueOf(userId));
                                }
                            });
                        }
                    });
                }
            });
        }

        private Mono<String> kickUsers (ServerUser serverUser, Guild guild, Member member) {
            DatabaseLoader.openConnectionIfClosed();
            DiscordUser discordUser = DiscordUser.findFirst("id = ?", serverUser.getUserId());
            long userId = discordUser.getUserIdSnowflake();

            return Mono.from(guild.getSelfMember()).flatMap(selfMember -> {
                if (selfMember.getId().asLong() == userId) {
                    return Mono.just("");
                } else {
                    return Mono.from(guild.getMemberById(Snowflake.of(userId))).flatMap(memberById -> {
                        if (memberById.isBot()) {
                            return Mono.just("");
                        }
                        else {
                            Set<Snowflake> snowflakeSet = memberById.getRoleIds();
                            return Mono.from(member.hasHigherRoles(snowflakeSet)).flatMap(memberHasHigherRoles -> {
                                if (!snowflakeSet.isEmpty() && !memberHasHigherRoles) {
                                    return Mono.just("");
                                } else {
                                    return Mono.from(guild.kick(Snowflake.of(userId), "Kicked as part of Anti-Raid Measures"))
                                            .thenReturn(String.valueOf(userId));
                                }
                            });
                        }
                    });
                }
            });
        }
    }
