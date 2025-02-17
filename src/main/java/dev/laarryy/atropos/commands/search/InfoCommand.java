package dev.laarryy.atropos.commands.search;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NoUserException;
import dev.laarryy.atropos.exceptions.NotFoundException;
import dev.laarryy.atropos.exceptions.NullServerException;
import dev.laarryy.atropos.exceptions.TryAgainException;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.models.joins.ServerUser;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AddServerToDB;
import dev.laarryy.atropos.utils.CommandChecks;
import dev.laarryy.atropos.utils.LogExecutor;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import dev.laarryy.atropos.utils.TimestampMaker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collector;

public class InfoCommand implements Command {

    private final Logger logger = LogManager.getLogger(this);
    private final Pattern snowflakePattern = Pattern.compile("\\d{10,20}");
    private final PermissionChecker permissionChecker = new PermissionChecker();


    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("info")
            .description("Provides information about this server or a user.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("server")
                    .description("Show information for this server")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("Show information for a user")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("mention")
                            .description("Search for user info by mention")
                            .type(ApplicationCommandOption.Type.USER.getValue())
                            .required(true)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("bot")
                    .description("Show information about this bot")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {
        logger.info("Execute Received");
        return CommandChecks.commandChecks(event, request.name()).then(Mono.defer(() -> {

            logger.info("Command checks done");

            if (event.getOption("server").isPresent()) {
                return sendServerInfo(event);
            }

            if (event.getOption("user").isPresent()) {
                return sendUserInfo(event);
            }

            if (event.getOption("bot").isPresent()) {
                return sendBotInfo(event);
            }

            return Mono.error(new MalformedInputException("Malformed Input"));
        }));
    }

    private Mono<Void> sendBotInfo(ChatInputInteractionEvent event) {
        return event.getInteraction().getGuild().flatMap(guild ->
                guild.getSelfMember().flatMap(selfMember -> {

                    DiscordServer discordServer = DatabaseLoader.use(() -> DiscordServer.findFirst("server_id = ?", guild.getId().asLong()));
                    DiscordUser discordUser = DatabaseLoader.use(() -> DiscordUser.findFirst("user_id_snowflake = ?", selfMember.getId().asLong()));

                    if (discordUser == null) {
                        return Mono.error(new NoUserException("No User"));
                    }

                    if (discordServer == null) {
                        return Mono.error(new NullServerException("Null Server"));
                    }

                    ServerUser serverUser = DatabaseLoader.use(() -> ServerUser.findFirst("user_id = ? and server_id = ?", discordUser.getUserId(), discordServer.getServerId()));

                    if (serverUser == null) {
                        DatabaseLoader.use(() -> {
                            ServerUser serverUserCreate = ServerUser.create("user_id", discordUser.getUserId(), "server_id", discordServer.getServerId(), "date", Instant.now().toEpochMilli());
                            serverUserCreate.save();
                            serverUserCreate.refresh();
                        });
                    }

                    List<Punishment> punishments = DatabaseLoader.use(() -> Punishment.find("server_id = ?", discordServer.getServerId()));

                    int punishmentsSize;
                    if (punishments == null || punishments.isEmpty()) {
                        punishmentsSize = 0;
                    } else {
                        punishmentsSize = punishments.size();
                    }

                    long userDate = (serverUser != null && serverUser.getDate() != null) ? serverUser.getDate() : 0;
                    String joinedWhen = userDate != 0 ? TimestampMaker.getTimestampFromEpochSecond(Instant.ofEpochMilli(userDate).getEpochSecond(), TimestampMaker.TimestampType.LONG_DATETIME) : "Unknown";

                    String botInfo = EmojiManager.getDeveloperBadge() + " **My Information**\n" +
                            "First Joined: " + joinedWhen + "\n" +
                            "Infractions Handled Here: `" + punishmentsSize + "`\n\n" +
                            "**[Usage Guide](https://atropos.laarryy.dev)**\n" +
                            "**[Discord Server](https://discord.gg/zaUMT7rBsR)**";

                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .title(selfMember.getUsername())
                            .color(Color.ENDEAVOUR)
                            .thumbnail(selfMember.getAvatarUrl())
                            .description(botInfo)
                            .footer("For individual command information, start typing /<command>", "")
                            .timestamp(Instant.now())
                            .build();

                    return Notifier.sendResultsEmbed(event, embed);
                }));
    }

    private Mono<Void> sendUserInfo(ChatInputInteractionEvent event) {
        return Mono.defer(() -> {
            if (event.getOption("user").get().getOption("snowflake").isEmpty() && event.getOption("user").get().getOption("mention").isEmpty()) {
                return Mono.error(new MalformedInputException("Malformed Input"));
            }

            if (event.getOption("user").get().getOption("mention").isEmpty()) {
                return Mono.error(new NotFoundException("404 Not Found"));
            }

            return event.getOption("user").get().getOption("mention").get().getValue().get().asUser().flatMap(user -> {
                Snowflake userIdSnowflake = user.getId();

                return event.getInteraction().getGuild().flatMap(guild ->
                        guild.getMemberById(userIdSnowflake).flatMap(member -> {

                            if (member == null) {
                                StringBuilder field1Content = new StringBuilder(EmojiManager.getUserIdentification()).append(" **User Information**\n")
                                        .append("Profile: ").append(user.getMention()).append("\n")
                                        .append("ID: `").append(userIdSnowflake.asLong()).append("`\n");
                                field1Content.append("Created: ")
                                        .append(TimestampMaker.getTimestampFromEpochSecond(
                                                userIdSnowflake.getTimestamp().getEpochSecond(),
                                                TimestampMaker.TimestampType.RELATIVE)).append("\n");

                                return sendUserInfoEmbed(event, user, field1Content.toString());
                            }

                            DiscordUser discordUser = DatabaseLoader.use(() -> DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake.asLong()));
                            DiscordServer discordServer = DatabaseLoader.use(() -> DiscordServer.findFirst("server_id = ?", guild.getId().asLong()));

                            //todo: test this>
                            if (discordUser == null) {
                                return AddServerToDB.addUserToDatabase(member, guild).then(Mono.error(new TryAgainException("Try Again")));
                            }

                            if (discordServer == null) {
                                return AddServerToDB.addServerToDatabase(guild).then(Mono.error(new TryAgainException("Try Again")));
                            }

                            DatabaseLoader.use(discordUser::refresh);

                            ServerUser serverUser = DatabaseLoader.use(() -> ServerUser.findFirst("server_id = ? and user_id = ?", discordServer.getServerId(), discordUser.getUserId()));

                            if (serverUser == null) {
                                return AddServerToDB.addUserToDatabase(member, guild).then(Mono.error(new TryAgainException("Try Again")));
                            }

                            return guild.getSelfMember().flatMap(selfMember -> {
                                try (final var usage = DatabaseLoader.use()) {
                                    DiscordUser discordSelfUser = DiscordUser.findFirst("user_id_snowflake = ?", selfMember.getId().asLong());
                                    ServerUser serverSelfUser = ServerUser.findFirst("server_id = ? and user_id = ?", discordServer.getServerId(), discordSelfUser.getUserId());

                                    if (serverSelfUser == null) {
                                        return AddServerToDB.addUserToDatabase(selfMember, guild).then(Mono.error(new TryAgainException("Try Again")));
                                    }

                                    serverSelfUser.refresh();
                                    Instant discordJoin = Instant.ofEpochMilli(serverUser.getDate());
                                    Instant discordBotJoin = Instant.ofEpochMilli(serverSelfUser.getDate());

                                    String joinTimestamp;

                                    if (Duration.between(discordBotJoin, discordJoin).toMinutes() < 30) {
                                        joinTimestamp = "Unknown";
                                    } else {
                                        joinTimestamp = TimestampMaker.getTimestampFromEpochSecond(Instant.ofEpochMilli(serverUser.getDate()).getEpochSecond(), TimestampMaker.TimestampType.RELATIVE);
                                    }

                                    StringBuilder field1Content = new StringBuilder(EmojiManager.getUserIdentification()).append(" **User Information**\n")
                                            .append("Profile: ").append(user.getMention()).append("\n")
                                            .append("ID: `").append(userIdSnowflake.asLong()).append("`\n");
                                    LogExecutor.getBadges(member).ifPresent(badges -> field1Content.append("Badges: ").append(badges).append("\n"));
                                    field1Content.append("Created: ")
                                            .append(TimestampMaker.getTimestampFromEpochSecond(
                                                    userIdSnowflake.getTimestamp().getEpochSecond(),
                                                    TimestampMaker.TimestampType.RELATIVE)).append("\n")
                                            .append("First Joined: ").append(joinTimestamp);
                                    return sendUserInfoEmbed(event, user, field1Content.toString());
                                }
                            });
                        }));
            });
        });
    }

    private Mono<Void> sendUserInfoEmbed(ChatInputInteractionEvent event, User user, String description) {
        String username = user.getTag();
        String eventUser = event.getInteraction().getUser().getTag();

        String avatarUrl = user.getAvatarUrl();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(username)
                .color(Color.ENDEAVOUR)
                .description(description)
                .thumbnail(avatarUrl)
                .footer("Requested by " + eventUser, event.getInteraction().getUser().getAvatarUrl())
                .timestamp(Instant.now())
                .build();

        return Notifier.sendResultsEmbed(event, embed);
    }

    private Mono<Void> sendServerInfo(ChatInputInteractionEvent event) {
        logger.info("Sending server info");

        return event.getInteraction().getGuild().flatMap(guild -> {
            Long guildId = guild.getId().asLong();

            final String membersOnFirstJoin;
            String membersOnFirstJoinTemp;
            try (final var usage = DatabaseLoader.use()) {
                DiscordServer server = DiscordServer.findOrCreateIt("server_id", guildId);
                int serverId = server.getServerId();
                DiscordServerProperties properties = DiscordServerProperties.findOrCreateIt("server_id", serverId, "server_id_snowflake", guildId);
                membersOnFirstJoinTemp = String.valueOf(properties.getMembersOnFirstJoin());
            } catch (Exception e) {
                membersOnFirstJoinTemp = "Unknown";
            }
            membersOnFirstJoin = membersOnFirstJoinTemp;

            Optional<String> maybeUrl = guild.getIconUrl(Image.Format.GIF)
                    .or(() -> guild.getIconUrl(Image.Format.PNG))
                    .or(() -> guild.getIconUrl(Image.Format.JPEG))
                    .or(() -> guild.getIconUrl(Image.Format.UNKNOWN));

            Instant created = guild.getId().getTimestamp();

            return guild.getOwner().flatMap(guildOwner -> {
                StringBuilder descriptionBuilder = new StringBuilder();
                descriptionBuilder.append("**Guild Information**\n")
                        .append(EmojiManager.getModeratorBadge()).append(" **Owner:** ").append(guildOwner.getNicknameMention()).append("\n")
                        .append(EmojiManager.getUserJoin()).append(" **Created:** ").append(TimestampMaker.getTimestampFromEpochSecond(created.getEpochSecond(), TimestampMaker.TimestampType.RELATIVE)).append("\n");


                //TODO: Wait for this to work on D4J's end (issue #999)

        /*List<Region> regions = guild.getRegions().collectList().block();
        if (regions != null && !regions.isEmpty()) {
            descriptionBuilder.append(EmojiManager.getVoiceChannel()).append(" **Voice Regions:** ");
            for (Region region : regions) {
                if (regions.indexOf(region) == regions.size() - 1) {
                    descriptionBuilder.append("`").append(region.getName()).append("`\n");
                } else {
                    descriptionBuilder.append("`").append(region.getName()).append("`, ");
                }
            }
        }
        */

                descriptionBuilder.append(
                        guild.getFeatures().stream()
                                .map(feat -> feat.replace('_', ' '))
                                .collect(Collector.of(
                                        () -> new StringJoiner("`, `", "**Features:** `", "`\n\n").setEmptyValue(""),
                                        StringJoiner::add,
                                        StringJoiner::merge,
                                        StringJoiner::toString
                                ))
                );

                descriptionBuilder.append(EmojiManager.getUserIdentification()).append(" **Members**\n");
                descriptionBuilder.append("When Atropos first joined: `").append(membersOnFirstJoin).append("`\n");
                descriptionBuilder.append("Now: `").append(guild.getMemberCount()).append('`');
                guild.getMaxMembers().ifPresent(maxMembers ->
                        descriptionBuilder.append("\nMax Members: `").append(maxMembers).append('`')
                );

                String description = descriptionBuilder.toString();

                return roleCount(guild).flatMap(roleLong ->
                        categoryCount(guild).flatMap(categoryLong ->
                                voiceCount(guild).flatMap(voiceLong ->
                                        textCount(guild).flatMap(textLong ->
                                                stageCount(guild).flatMap(stageLong ->
                                                        storeCount(guild).flatMap(storeLong ->
                                                                newsCount(guild).flatMap(newsLong -> {
                                                                            String field1Content = EmojiManager.getServerCategory() + " `" +
                                                                                    categoryLong +
                                                                                    "` Categories\n" +
                                                                                    EmojiManager.getVoiceChannel() + " `" +
                                                                                    voiceLong +
                                                                                    "` Voice Channels\n" +
                                                                                    EmojiManager.getTextChannel() + " `" +
                                                                                    textLong +
                                                                                    "` Text Channels\n" +
                                                                                    EmojiManager.getServerRole() + " `" +
                                                                                    roleLong + "` Roles\n";

                                                                            String field2Content = EmojiManager.getStageChannel() + " `" +
                                                                                    stageLong +
                                                                                    "` Stage Channels\n" +
                                                                                    EmojiManager.getStoreChannel() + " `" +
                                                                                    storeLong +
                                                                                    "` Store Channels\n" +
                                                                                    EmojiManager.getNewsChannel() + " `" +
                                                                                    newsLong +
                                                                                    "` News Channels\n";

                                                                            String field3Content;
                                                                            StringBuilder f3 = new StringBuilder();
                                                                            f3.append(EmojiManager.getBoosted()).append(" **Nitro**\n");
                                                                            int boostLevel = guild.getPremiumTier().getValue();
                                                                            f3.append("Level: `").append(boostLevel).append("`");
                                                                            if (guild.getPremiumSubscriptionCount().isPresent()) {
                                                                                int boosters = guild.getPremiumSubscriptionCount().getAsInt();
                                                                                f3.append(" with `").append(boosters).append("` boosters ").append(EmojiManager.getServerBoostBadge());
                                                                            }

                                                                            field3Content = f3.append("\n").toString();
                                                                            Member requester = event.getInteraction().getMember().get();
                                                                            EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder()
                                                                                    .title(guild.getName())
                                                                                    .color(Color.ENDEAVOUR)
                                                                                    .description(description)
                                                                                    .addField("\u200B", field1Content, true)
                                                                                    .addField("\u200B", field2Content, true)
                                                                                    .addField("\u200B", field3Content, false)
                                                                                    .footer("Requested by " + requester.getTag(), requester.getAvatarUrl());
                                                                            maybeUrl.ifPresent(embedBuilder::thumbnail);
                                                                            return Notifier.sendResultsEmbed(event, embedBuilder.build());
                                                                        }
                                                                )))))));
            });
        });
    }

    private Mono<Long> categoryCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType() == Channel.Type.GUILD_CATEGORY)
                .count();
    }

    private Mono<Long> voiceCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType() == Channel.Type.GUILD_VOICE)
                .count();
    }

    private Mono<Long> textCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType() == Channel.Type.GUILD_TEXT)
                .count();
    }

    private Mono<Long> stageCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType() == Channel.Type.GUILD_STAGE_VOICE)
                .count();
    }

    private Mono<Long> storeCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType() == Channel.Type.GUILD_STORE)
                .count();
    }

    private Mono<Long> newsCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType() == Channel.Type.GUILD_NEWS)
                .count();
    }

    private Mono<Long> roleCount(Guild guild) {
        return guild.getRoles().count();
    }
}
