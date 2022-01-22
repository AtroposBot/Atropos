package dev.laarryy.atropos.commands.raid;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
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
        if (!CommandChecks.commandChecks(event, request.name())) {
            return Mono.empty();
        }

        Guild guild = event.getInteraction().getGuild().block();

        if (event.getOption("type").isEmpty()
                || event.getOption("duration").isEmpty()
                || event.getOption("type").get().getValue().isEmpty()
                || event.getOption("duration").get().getValue().isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
            return Mono.empty();
        }

        DatabaseLoader.openConnectionIfClosed();
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        String type = event.getOption("type").get().getValue().get().asString();

        String durationInput = event.getOption("duration").get().getValue().get().asString();

        Duration duration = DurationParser.parseDuration(durationInput);

        if (duration.toDays() > 2) {
            Notifier.notifyCommandUserOfError(event, "durationTooLong");
            AuditLogger.addCommandToDB(event, false);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        Instant startPoint = Instant.now().minus(duration);

        List<ServerUser> serverUserList = ServerUser.find("server_id = ? and date > ?", discordServer.getServerId(), startPoint.toEpochMilli());

        if (serverUserList == null || serverUserList.isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "404");
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        event.deferReply().block();

        StringBuilder sb = new StringBuilder();
        sb.append("```\n");

        if (type.equals("ban")) {
            Flux.fromIterable(serverUserList)
                    .map(serverUser -> this.banUsers(serverUser, guild, event.getInteraction().getMember().get()))
                    .filter(string -> !string.equals("none"))
                    .map(string -> sb.append(string).append(" "))
                    
                    .subscribe(stb -> {
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

                        Notifier.replyDeferredInteraction(event, embed);
                    });
        }

        if (type.equals("kick")) {
            Flux.fromIterable(serverUserList)
                    .map(serverUser -> this.kickUsers(serverUser, guild, event.getInteraction().getMember().get()))
                    .filter(string -> !string.equals("none"))
                    .map(string -> sb.append(string).append(" "))
                    
                    .subscribe(stb -> {
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

                        Notifier.replyDeferredInteraction(event, embed);
                    });
        }
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }

    private String banUsers(ServerUser serverUser, Guild guild, Member member) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("id = ?", serverUser.getUserId());
        long userId = discordUser.getUserIdSnowflake();
        Set<Snowflake> snowflakeSet = guild.getMemberById(Snowflake.of(userId)).block().getRoleIds();

        if (guild.getSelfMember().block().getId().asLong() == userId) {
            return "";
        }

        if (guild.getMemberById(Snowflake.of(userId)).block().isBot()) {
            return "";
        }

        if (!snowflakeSet.isEmpty() && !member.hasHigherRoles(snowflakeSet).block()) {
            return "none";
        }

        guild.ban(Snowflake.of(userId)).withReason("Banned as part of anti-raid measures").withDeleteMessageDays(2).block();
        return String.valueOf(userId);
    }

    private String kickUsers(ServerUser serverUser, Guild guild, Member member) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("id = ?", serverUser.getUserId());
        long userId = discordUser.getUserIdSnowflake();

        if (guild.getSelfMember().block().getId().asLong() == userId) {
            return "";
        }

        if (guild.getMemberById(Snowflake.of(userId)).block().isBot()) {
            return "";
        }

        Set<Snowflake> snowflakeSet = guild.getMemberById(Snowflake.of(userId)).block().getRoleIds();

        if (!snowflakeSet.isEmpty() && !member.hasHigherRoles(snowflakeSet).block()) {
            return "";
        }

        guild.kick(Snowflake.of(userId), "Kicked as part of anti-raid measures").block();
        return String.valueOf(userId);
    }
}
