package dev.laarryy.eris.commands.search;

import dev.laarryy.eris.commands.Command;
import dev.laarryy.eris.config.EmojiManager;
import dev.laarryy.eris.models.guilds.DiscordServer;
import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import dev.laarryy.eris.models.joins.ServerUser;
import dev.laarryy.eris.models.users.DiscordUser;
import dev.laarryy.eris.models.users.Punishment;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.AddServerToDB;
import dev.laarryy.eris.utils.CommandChecks;
import dev.laarryy.eris.utils.LogExecutor;
import dev.laarryy.eris.utils.Notifier;
import dev.laarryy.eris.utils.PermissionChecker;
import dev.laarryy.eris.utils.TimestampMaker;
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
                            .required(false)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("snowflake")
                            .description("Search for user info by snowflake")
                            .type(ApplicationCommandOption.Type.STRING.getValue())
                            .required(false)
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
        if (!CommandChecks.commandChecks(event, request.name())) {
            return Mono.empty();
        }

        if (event.getInteraction().getGuildId().isEmpty() || event.getInteraction().getGuild().block() == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return Mono.empty();
        }

        if (event.getOption("server").isPresent()) {
            sendServerInfo(event);
            return Mono.empty();
        }

        if (event.getOption("user").isPresent()) {
            sendUserInfo(event);
            return Mono.empty();
        }

        if (event.getOption("bot").isPresent()) {
            sendBotInfo(event);
            return Mono.empty();
        }

        return Mono.empty();
    }

    private void sendBotInfo(ChatInputInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        Guild guild = event.getInteraction().getGuild().block();
        Member selfMember = guild.getSelfMember().block();
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", selfMember.getId().asLong());

        if (discordUser == null) {
            Notifier.notifyCommandUserOfError(event, "noUser");
            return;
        }

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
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
                "Infractions Handled: `" + punishmentsSize + "`\n\n" +
                // TODO: Name, URL, and Guide
                "**[Usage Guide](https://google.com)**\n";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(selfMember.getUsername())
                .color(Color.ENDEAVOUR)
                .thumbnail(selfMember.getAvatarUrl())
                .description(botInfo)
                .footer("For individual command information, start typing /<command>", "")
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).block();
        DatabaseLoader.closeConnectionIfOpen();
    }

    private void sendUserInfo(ChatInputInteractionEvent event) {

        if (event.getOption("user").get().getOption("snowflake").isEmpty() && event.getOption("user").get().getOption("mention").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            return;
        }

        User user;
        Snowflake userIdSnowflake;
        if (event.getOption("user").get().getOption("mention").isPresent()) {
            user = event.getOption("user").get().getOption("mention").get().getValue().get().asUser().block();
            userIdSnowflake = user.getId();
        } else {
            String snowflakeString = event.getOption("user").get().getOption("snowflake").get().getValue().get().asString();
            if (!snowflakePattern.matcher(snowflakeString).matches()) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                return;
            }

            try {
                user = event.getClient().getUserById(Snowflake.of(snowflakeString)).block();
            } catch (Exception e) {
                Notifier.notifyCommandUserOfError(event, "404");
                return;
            }

            userIdSnowflake = Snowflake.of(snowflakeString);
        }

        Guild guild = event.getInteraction().getGuild().block();

        Member member;
        try {
            member = guild.getMemberById(userIdSnowflake).block();
        } catch (Exception e) {
            member = null;
        }

        if (member == null) {
            if (user != null) {
                StringBuilder field1Content = new StringBuilder(EmojiManager.getUserIdentification()).append(" **User Information**\n")
                        .append("Profile: ").append(user.getMention()).append("\n")
                        .append("ID: `").append(userIdSnowflake.asLong()).append("`\n");
                field1Content.append("Created: ")
                        .append(TimestampMaker.getTimestampFromEpochSecond(
                                userIdSnowflake.getTimestamp().getEpochSecond(),
                                TimestampMaker.TimestampType.RELATIVE)).append("\n");

                sendUserInfoEmbed(event, user, field1Content);
                return;
            }
            Notifier.notifyCommandUserOfError(event, "noUser");
            return;
        }

        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake.asLong());
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        if (discordUser == null) {
            addServerToDB.addUserToDatabase(guild.getMemberById(userIdSnowflake).block(), guild);
            discordUser.refresh();
        }

        ServerUser serverUser = ServerUser.findFirst("server_id = ? and user_id = ?", discordServer.getServerId(), discordUser.getUserId());

        if (serverUser == null) {
            serverUser = ServerUser.create("server_id", discordServer.getServerId(), "user_id", discordUser.getUserId(), "date", Instant.now().toEpochMilli());
            serverUser.save();
            serverUser.refresh();
        }

        Member selfMember = guild.getSelfMember().block();
        DiscordUser discordSelfUser = DiscordUser.findFirst("user_id_snowflake = ?", selfMember.getId().asLong());
        ServerUser serverSelfUser = ServerUser.findFirst("server_id = ? and user_id = ?", discordServer.getServerId(), discordSelfUser.getUserId());
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
        if (member != null && !LogExecutor.getBadges(member).equals("none")) {
            field1Content.append("Badges: ").append(LogExecutor.getBadges(member)).append("\n");
        }
                field1Content.append("Created: ")
                        .append(TimestampMaker.getTimestampFromEpochSecond(
                                userIdSnowflake.getTimestamp().getEpochSecond(),
                                TimestampMaker.TimestampType.RELATIVE)).append("\n")
                        .append("First Joined: ").append(joinTimestamp);

        sendUserInfoEmbed(event, user, field1Content);
        DatabaseLoader.closeConnectionIfOpen();
    }

    private void sendUserInfoEmbed(ChatInputInteractionEvent event, User user, StringBuilder field1Content) {
        String username = user.getUsername() + "#" + user.getDiscriminator();
        String eventUser = event.getInteraction().getUser().getUsername() + "#" + event.getInteraction().getUser().getDiscriminator();

        String avatarUrl = user.getAvatarUrl();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(username)
                .color(Color.ENDEAVOUR)
                .description(field1Content.toString())
                .thumbnail(avatarUrl)
                .footer("Requested by "+ eventUser, event.getInteraction().getUser().getAvatarUrl())
                .timestamp(Instant.now())
                .build();

        event.reply().withEmbeds(embed).block();
    }

    private void sendServerInfo(ChatInputInteractionEvent event) {
        DatabaseLoader.openConnectionIfClosed();
        Long guildId = event.getInteraction().getGuildId().get().asLong();
        Guild guild = event.getInteraction().getGuild().block();

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


        StringBuilder sb = new StringBuilder();
        sb.append("**Guild Information**\n");
        sb.append(EmojiManager.getModeratorBadge()).append(" **Owner:** ").append(guild.getOwner().block().getNicknameMention()).append("\n");
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

        String field1Content = EmojiManager.getServerCategory() + " `" +
                guild.getChannels()
                        .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_CATEGORY))
                        .count()
                        .block().toString() +
                "` Categories\n" +
                EmojiManager.getVoiceChannel() + " `" +
                guild.getChannels()
                        .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_VOICE))
                        .count()
                        .block().toString() +
                "` Voice Channels\n" +
                EmojiManager.getTextChannel() + " `" +
                guild.getChannels()
                        .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_TEXT))
                        .count()
                        .block().toString() +
                "` Text Channels\n" +
                EmojiManager.getServerRole() + " `" +
                guild.getRoles().count().block().toString() + "` Roles\n";

        String field2Content = EmojiManager.getStageChannel() + " `" +
                guild.getChannels()
                        .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_STAGE_VOICE))
                        .count()
                        .block().toString() +
                "` Stage Channels\n" +
                EmojiManager.getStoreChannel() + " `" +
                guild.getChannels()
                        .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_STORE))
                        .count()
                        .block().toString() +
                "` Store Channels\n" +
                EmojiManager.getNewsChannel() + " `" +
                guild.getChannels()
                        .filter(guildChannel -> guildChannel.getType().equals(Channel.Type.GUILD_NEWS))
                        .count()
                        .block().toString() +
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

        event.reply().withEmbeds(embedCreateSpec).block();
        DatabaseLoader.closeConnectionIfOpen();
    }
}
