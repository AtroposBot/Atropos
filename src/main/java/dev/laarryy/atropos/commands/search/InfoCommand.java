package dev.laarryy.atropos.commands.search;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NoPermissionsException;
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
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

public class InfoCommand implements Command {

    private final Logger logger = LogManager.getLogger(this);
    private final Pattern snowflakePattern = Pattern.compile("\\d{10,20}");
    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final AddServerToDB addServerToDB = new AddServerToDB();


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
        return Mono.from(CommandChecks.commandChecks(event, request.name())).flatMap(aBoolean -> {
            if (!aBoolean) {
                return Mono.error(new NoPermissionsException("No Permission"));
            }

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
        });
    }

    private Mono<Void> sendBotInfo(ChatInputInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        return Mono.from(event.getInteraction().getGuild()).flatMap(guild ->
                Mono.from(guild.getSelfMember()).flatMap(selfMember -> {
                    DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
                    DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", selfMember.getId().asLong());

                    if (discordUser == null) {
                        return Mono.error(new NoUserException("No User"));
                    }

                    if (discordServer == null) {
                        return Mono.error(new NullServerException("Null Server"));
                    }

                    ServerUser serverUser = ServerUser.findFirst("user_id = ? and server_id = ?", discordUser.getUserId(), discordServer.getServerId());

                    if (serverUser == null) {
                        serverUser = ServerUser.create("user_id", discordUser.getUserId(), "server_id", discordServer.getServerId(), "date", Instant.now().toEpochMilli());
                        serverUser.save();
                        serverUser.refresh();
                    }

                    List<Punishment> punishments = Punishment.find("server_id = ?", discordServer.getServerId());

                    int punishmentsSize;
                    if (punishments == null || punishments.isEmpty()) {
                        punishmentsSize = 0;
                    } else {
                        punishmentsSize = punishments.size();
                    }

                    String botInfo = EmojiManager.getDeveloperBadge() + " **My Information**\n" +
                            "First Joined: " + TimestampMaker.getTimestampFromEpochSecond(
                            Instant.ofEpochMilli(serverUser.getDate()).getEpochSecond(),
                            TimestampMaker.TimestampType.LONG_DATETIME) + "\n" +
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

                    DatabaseLoader.closeConnectionIfOpen();
                    return Notifier.sendResultsEmbed(event, embed);
                }));
    }

    private Mono<Void> sendUserInfo(ChatInputInteractionEvent event) {

        if (event.getOption("user").get().getOption("snowflake").isEmpty() && event.getOption("user").get().getOption("mention").isEmpty()) {
            return Mono.error(new MalformedInputException("Malformed Input"));
        }

        if (event.getOption("user").get().getOption("mention").isPresent()) {
            return Mono.from(event.getOption("user").get().getOption("mention").get().getValue().get().asUser()).flatMap(user -> {
                Snowflake userIdSnowflake = user.getId();

                return Mono.from(event.getInteraction().getGuild()).flatMap(guild ->
                        Mono.from(guild.getMemberById(userIdSnowflake)).flatMap(member -> {
                            if (member == null) {
                                StringBuilder field1Content = new StringBuilder(EmojiManager.getUserIdentification()).append(" **User Information**\n")
                                        .append("Profile: ").append(user.getMention()).append("\n")
                                        .append("ID: `").append(userIdSnowflake.asLong()).append("`\n");
                                field1Content.append("Created: ")
                                        .append(TimestampMaker.getTimestampFromEpochSecond(
                                                userIdSnowflake.getTimestamp().getEpochSecond(),
                                                TimestampMaker.TimestampType.RELATIVE)).append("\n");


                                return sendUserInfoEmbed(event, user, field1Content);
                            }

                            DatabaseLoader.openConnectionIfClosed();
                            DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake.asLong());
                            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

                            //todo: test this>
                            if (discordUser == null) {
                                return Mono.from(addServerToDB.addUserToDatabase(member, guild)).then(Mono.error(new TryAgainException("Try Again")));
                            }

                            discordUser.refresh();


                            ServerUser serverUser = ServerUser.findFirst("server_id = ? and user_id = ?", discordServer.getServerId(), discordUser.getUserId());

                            return Mono.from(guild.getSelfMember()).flatMap(selfMember -> {
                                DiscordUser discordSelfUser = DiscordUser.findFirst("user_id_snowflake = ?", selfMember.getId().asLong());
                                ServerUser serverSelfUser = ServerUser.findFirst("server_id = ? and user_id = ?", discordServer.getServerId(), discordSelfUser.getUserId());

                                if (serverSelfUser == null) {
                                    return addServerToDB.addUserToDatabase(selfMember, guild).then(Mono.error(new TryAgainException("Try Again")));
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
                                if (!LogExecutor.getBadges(member).get().equals("none")) {
                                    field1Content.append("Badges: ").append(LogExecutor.getBadges(member)).append("\n");
                                }
                                field1Content.append("Created: ")
                                        .append(TimestampMaker.getTimestampFromEpochSecond(
                                                userIdSnowflake.getTimestamp().getEpochSecond(),
                                                TimestampMaker.TimestampType.RELATIVE)).append("\n")
                                        .append("First Joined: ").append(joinTimestamp);
                                DatabaseLoader.closeConnectionIfOpen();
                                return sendUserInfoEmbed(event, user, field1Content);
                            });
                        }));
            });
        } else {
            return Mono.error(new NotFoundException("404 Not Found"));
        }
    }

    private Mono<Void> sendUserInfoEmbed(ChatInputInteractionEvent event, User user, StringBuilder field1Content) {

        String username = user.getUsername() + "#" + user.getDiscriminator();
        String eventUser = event.getInteraction().getUser().getUsername() + "#" + event.getInteraction().getUser().getDiscriminator();

        String avatarUrl = user.getAvatarUrl();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(username)
                .color(Color.ENDEAVOUR)
                .description(field1Content.toString())
                .thumbnail(avatarUrl)
                .footer("Requested by " + eventUser, event.getInteraction().getUser().getAvatarUrl())
                .timestamp(Instant.now())
                .build();

        return Notifier.sendResultsEmbed(event, embed);
    }

    private Mono<Void> sendServerInfo(ChatInputInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();

        return Mono.from(event.getInteraction().getGuild()).flatMap(guild -> {
            Long guildId = guild.getId().asLong();

            DiscordServer server = DiscordServer.findOrCreateIt("server_id", guildId);
            int serverId = server.getServerId();
            DiscordServerProperties properties = DiscordServerProperties.findOrCreateIt("server_id", serverId, "server_id_snowflake", guildId);

            String url;
            if (guild.getIconUrl(Image.Format.GIF).isPresent()) {
                url = guild.getIconUrl(Image.Format.GIF).get();
            } else if (guild.getIconUrl(Image.Format.PNG).isPresent()) {
                url = guild.getIconUrl(Image.Format.PNG).get();
            } else if (guild.getIconUrl(Image.Format.JPEG).isPresent()) {
                url = guild.getIconUrl(Image.Format.JPEG).get();
            } else {
                url = guild.getIconUrl(Image.Format.UNKNOWN).orElse("");
            }

            Instant created = guild.getId().getTimestamp();

            return Mono.from(guild.getOwner()).flatMap(guildOwner -> {
                StringBuilder sb = new StringBuilder();
                sb.append("**Guild Information**\n");
                sb.append(EmojiManager.getModeratorBadge()).append(" **Owner:** ").append(guildOwner.getNicknameMention()).append("\n");
                sb.append(EmojiManager.getUserJoin()).append(" **Created:** ")
                        .append(TimestampMaker.getTimestampFromEpochSecond(created.getEpochSecond(), TimestampMaker.TimestampType.RELATIVE))
                        .append("\n");


                //TODO: Wait for this to work on D4J's end (issue #999)

        /*List<Region> regions = guild.getRegions().collectList().block();
        if (regions != null && !regions.isEmpty()) {
            sb.append(EmojiManager.getVoiceChannel()).append(" **Voice Regions:** ");
            for (Region region : regions) {
                if (regions.indexOf(region) == regions.size() - 1) {
                    sb.append("`").append(region.getName()).append("`\n");
                } else {
                    sb.append("`").append(region.getName()).append("`, ");
                }
            }
        }
        */

                List<String> features = guild.getFeatures().stream().toList();
                if (!features.isEmpty()) {
                    sb.append("**Features:** ");
                    for (String f : features) {
                        if (features.indexOf(f) == features.size() - 1) {
                            sb.append("`").append(f.toLowerCase().replaceAll("_", " ")).append("`\n\n");
                        } else {
                            sb.append("`").append(f.toLowerCase().replaceAll("_", " ")).append("`, ");
                        }
                    }
                }

                sb.append(EmojiManager.getUserIdentification()).append(" **Members**\n");
                sb.append("When I first joined: `").append(properties.getMembersOnFirstJoin()).append("`\n");
                if (guild.getMaxMembers().isPresent()) {
                    sb.append("Now: `").append(guild.getMemberCount()).append("`\n");
                    sb.append("Max Members: `").append(guild.getMaxMembers().getAsInt()).append("`");
                } else {
                    sb.append("Now: `").append(guild.getMemberCount()).append("`");
                }

                String description = sb.toString();

                return Mono.from(roleCount(guild)).flatMap(roleLong ->
                        Mono.from(categoryCount(guild)).flatMap(categoryLong ->
                        Mono.from(voiceCount(guild)).flatMap(voiceLong ->
                                Mono.from(textCount(guild)).flatMap(textLong ->
                                        Mono.from(stageCount(guild)).flatMap(stageLong ->
                                                Mono.from(storeCount(guild)).flatMap(storeLong ->
                                                        Mono.from(newsCount(guild)).flatMap(newsLong -> {
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
                                                                f3.append(" with `").append(boosters).append("` boosters ").append(EmojiManager.getServerBoostBadge()).append("\n");
                                                            } else {
                                                                f3.append("\n");
                                                            }
                                                            field3Content = f3.toString();

                                                            Member requester = event.getInteraction().getMember().get();
                                                            String username = requester.getUsername() + "#" + requester.getDiscriminator();

                                                            EmbedCreateSpec embedCreateSpec = EmbedCreateSpec.builder()
                                                                    .title(guild.getName())
                                                                    .color(Color.ENDEAVOUR)
                                                                    .thumbnail(url)
                                                                    .description(description)
                                                                    .addField("\u200B", field1Content, true)
                                                                    .addField("\u200B", field2Content, true)
                                                                    .addField("\u200B", field3Content, false)
                                                                    .footer("Requested by " + username, requester.getAvatarUrl())
                                                                    .build();
                                                            DatabaseLoader.closeConnectionIfOpen();
                                                            logger.info("SENDING SERVER INFO!!!");
                                                            return Notifier.sendResultsEmbed(event, embedCreateSpec);
                                                        })))))));
            });
        });
    }

    private Mono<Long> categoryCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_CATEGORY))
                .count();
    }

    private Mono<Long> voiceCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_CATEGORY))
                .count();
    }

    private Mono<Long> textCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_CATEGORY))
                .count();
    }

    private Mono<Long> stageCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_CATEGORY))
                .count();
    }

    private Mono<Long> storeCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_CATEGORY))
                .count();
    }

    private Mono<Long> newsCount(Guild guild) {
        return guild.getChannels()
                .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_CATEGORY))
                .count();
    }
    private Mono<Long> roleCount(Guild guild) {
        return guild.getRoles().count();
    }
}
