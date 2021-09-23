package dev.laarryy.Icicle.commands;

import dev.laarryy.Icicle.config.EmojiManager;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.models.joins.ServerUser;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.Notifier;
import dev.laarryy.Icicle.utils.PermissionChecker;
import dev.laarryy.Icicle.utils.TimestampMaker;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

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
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("Show information for a user")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("mention")
                            .description("Search for user info by mention")
                            .type(ApplicationCommandOptionType.USER.getValue())
                            .required(false)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("snowflake")
                            .description("Search for user info by snowflake")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(false)
                            .build())
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        DatabaseLoader.openConnectionIfClosed();

        User user = event.getInteraction().getUser();
        Permission permission = Permission.findOrCreateIt("permission", request.name());
        permission.save();
        permission.refresh();
        int permissionId = permission.getInteger("id");
        Guild guild = event.getInteraction().getGuild().block();


        if (!permissionChecker.checkPermission(guild, user, permissionId)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
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

        return Mono.empty();
    }

    private void sendUserInfo(SlashCommandEvent event) {

        if (event.getOption("user").get().getOption("snowflake").isEmpty() && event.getOption("user").get().getOption("mention").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            return;
        }

        User user;
        Snowflake userIdSnowflake;
        if (event.getOption("user").get().getOption("mention").isPresent()) {
            user = event.getOption("user").get().getOption("mention").get().getValue().get().asUser().block();
            if (user == event.getClient().getSelf().block()) {
                Notifier.notifyCommandUserOfError(event, "noUser");
            }
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

        logger.info("got the user, proceeding");

        Guild guild = event.getInteraction().getGuild().block();

        DatabaseLoader.openConnectionIfClosed();
        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake.asLong());
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        if (discordUser == null) {
            Notifier.notifyCommandUserOfError(event, "noUser");
            return;
        }

        ServerUser serverUser = ServerUser.findFirst("server_id = ? and user_id = ?", discordServer.getServerId(), discordUser.getUserId());

        if (serverUser == null) {
            Notifier.notifyCommandUserOfError(event, "noUser");
            return;
        }

        String field1Content = EmojiManager.getUserIdentification() + " **User Information**\n" +
                "Profile: " + user.getMention() + "\n" +
                "ID: `" + userIdSnowflake.asLong() + "`\n" +
                "Created: " + TimestampMaker.getTimestampFromEpochSecond(userIdSnowflake.getTimestamp().getEpochSecond(), TimestampMaker.TimestampType.RELATIVE) + "\n" +
                "First Joined: " + TimestampMaker.getTimestampFromEpochSecond(Instant.ofEpochMilli(serverUser.getDate()).getEpochSecond(), TimestampMaker.TimestampType.RELATIVE);

        logger.info("built the string");

        String username = user.getUsername() + "#" + user.getDiscriminator();
        String eventUser = event.getInteraction().getUser().getUsername() + "#" + event.getInteraction().getUser().getDiscriminator();

        String avatarUrl = user.getAvatarUrl();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(username)
                .color(Color.ENDEAVOUR)
                .description(field1Content)
                .thumbnail(avatarUrl)
                .footer("Requested by "+ eventUser, event.getInteraction().getUser().getAvatarUrl())
                .build();

        event.reply().withEmbeds(embed).block();

    }

    private void sendServerInfo(SlashCommandEvent event) {
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


        logger.info("building strings");

        StringBuilder sb = new StringBuilder();
        sb.append("**Guild Information**\n");
        sb.append(EmojiManager.getModeratorBadge()).append(" **Owner:** ").append(guild.getOwner().block().getNicknameMention()).append("\n");
        sb.append(EmojiManager.getUserJoin()).append(" **Created:** ")
                .append(TimestampMaker.getTimestampFromEpochSecond(created.getEpochSecond(), TimestampMaker.TimestampType.RELATIVE))
                .append("\n");


        logger.info("1 done");

        //TODO: Wait for this to work on D4J's end (issue #999)

        /*List<Region> regions = guild.getRegions().collectList().block();
        if (regions != null && !regions.isEmpty()) {
            sb.append(EmojiManager.getVoiceChannel()).append(" **Voice Regions:** ");
            logger.info("regions not null - appended");
            for (Region region : regions) {
                if (regions.indexOf(region) == regions.size() - 1) {
                    sb.append("`").append(region.getName()).append("`\n");
                    logger.info("appended last region");
                } else {
                    sb.append("`").append(region.getName()).append("`, ");
                    logger.info("appended region");
                }
            }
        }
        logger.info("2 done");*/

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
        logger.info("3 done");


        sb.append(EmojiManager.getUserIdentification()).append(" **Members**\n");
        sb.append("When I first joined: `").append(properties.getMembersOnFirstJoin()).append("`\n");
        if (guild.getMaxMembers().isPresent()) {
            sb.append("Now: `").append(guild.getMemberCount()).append("/").append(guild.getMaxMembers().getAsInt()).append("`");
        } else {
            sb.append("Now: `").append(guild.getMemberCount()).append("`");
        }
        logger.info("4 done");


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
        logger.info("5 done");


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
        logger.info("6 done");


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

        logger.info("done with strings doing me a send");

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
    }
}
