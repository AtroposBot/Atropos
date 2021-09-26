package dev.laarryy.Eris.utils;

import dev.laarryy.Eris.config.EmojiManager;
import dev.laarryy.Eris.managers.ClientManager;
import dev.laarryy.Eris.models.guilds.DiscordServer;
import dev.laarryy.Eris.models.guilds.ServerBlacklist;
import dev.laarryy.Eris.models.guilds.ServerMessage;
import dev.laarryy.Eris.models.users.DiscordUser;
import dev.laarryy.Eris.models.users.Punishment;
import dev.laarryy.Eris.storage.DatabaseLoader;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.InviteCreateEvent;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.event.domain.channel.NewsChannelCreateEvent;
import discord4j.core.event.domain.channel.NewsChannelDeleteEvent;
import discord4j.core.event.domain.channel.NewsChannelUpdateEvent;
import discord4j.core.event.domain.channel.StoreChannelCreateEvent;
import discord4j.core.event.domain.channel.StoreChannelDeleteEvent;
import discord4j.core.event.domain.channel.StoreChannelUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelCreateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.channel.TextChannelUpdateEvent;
import discord4j.core.event.domain.channel.VoiceChannelCreateEvent;
import discord4j.core.event.domain.channel.VoiceChannelDeleteEvent;
import discord4j.core.event.domain.channel.VoiceChannelUpdateEvent;
import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.guild.MemberUpdateEvent;
import discord4j.core.event.domain.guild.UnbanEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.audit.AuditLogPart;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.NewsChannel;
import discord4j.core.object.entity.channel.StoreChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public final class LogExecutor {
    private LogExecutor() {
    }

    public static void logInsubordination(SlashCommandEvent event, TextChannel logChannel, Member target) {
        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) {
            return;
        }

        long targetId = target.getId().asLong();
        String username = target.getUsername() + "#" + target.getDiscriminator();
        String targetInfo = "`" + username + "`:`" + targetId + "`:<@" + targetId + ">";

        String mutineerInfo;
        if (event.getInteraction().getMember().isPresent()) {
            Member mutineer = event.getInteraction().getMember().get();
            long mutineerId = mutineer.getId().asLong();
            String mutineerName = mutineer.getUsername() + "#" + mutineer.getDiscriminator();
            mutineerInfo = "`" + mutineerName + "`:`" + mutineerId + "`:<@" + mutineerId + ">";
        } else {
            mutineerInfo = "Unknown";
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(event.getCommandName());

        StringBuilder sb = new StringBuilder();

        Flux.fromIterable(event.getOptions())
                .subscribe(option -> stringBuffer.append(AuditLogger.generateOptionString(option, sb)));

        String commandContent = stringBuffer.toString();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserWarn() + " Insubordination Alert")
                .description("A mutineer has attempted to punish someone above them.")
                .addField("User", mutineerInfo, false)
                .addField("Target", targetInfo, false)
                .addField("Command", "`" + commandContent + "`", false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logBlacklistTrigger(MessageCreateEvent event, ServerBlacklist blacklist, Punishment punishment, TextChannel logChannel) {
        DatabaseLoader.openConnectionIfClosed();

        Member user = event.getMember().get();
        long userId = user.getId().asLong();
        String username = user.getUsername() + "#" + user.getDiscriminator();
        String userInfo = "`" + username + "`:" +"`" + userId + "`:<@" + userId + ">";

        String content = event.getMessage().getContent();

        String attachments;
        if (!event.getMessage().getAttachments().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Attachment a : event.getMessage().getAttachments()) {
                sb.append(a.getFilename()).append("\n");
            }
            attachments = sb.toString();
        } else {
            attachments = "none";
        }

        int blacklistId = blacklist.getBlacklistId();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserWarn() + " Blacklist Triggered")
                .color(Color.MOON_YELLOW)
                .description("Blacklist ID #`" + blacklistId + "` was triggered and the message detected has been deleted. " +
                        "A case has been opened for the user who triggered it with ID #`" + punishment.getPunishmentId() + "`")
                .addField("Content", getStringWithLegalLength(content, 1024), false)
                .footer("To see information about this blacklist entry, run /blacklist info " + blacklistId, "")
                .timestamp(Instant.now())
                .build();

        if (!attachments.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Attachments", attachments, false)
                    .build();
        }

        logChannel.createMessage(embed).subscribe();
    }

    public static void logPunishment(Punishment punishment, TextChannel logChannel) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser punishedUser = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        DiscordUser punishingUser = DiscordUser.findFirst("id = ?", punishment.getPunishingUserId());
        String type = switch (punishment.getPunishmentType()) {
            case "warn" -> EmojiManager.getUserWarn() + " Warn";
            case "case" -> EmojiManager.getUserCase() + " Case";
            case "mute" -> EmojiManager.getUserMute() + " Mute";
            case "kick" -> EmojiManager.getUserKick() + " Kick";
            case "ban" -> EmojiManager.getUserBan() + " Ban";
            default -> punishment.getPunishmentType();
        };
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(type + ": ID #" + punishment.getPunishmentId())
                .color(Color.ENDEAVOUR)
                .addField("Punished User", "`" + punishedUser.getUserIdSnowflake() + "`:<@" + punishedUser.getUserIdSnowflake() + ">", false)
                .addField("Punishing User", "`" + punishingUser.getUserIdSnowflake() + "`:<@" + punishingUser.getUserIdSnowflake() + ">", false)
                .addField("Reason", punishment.getPunishmentMessage(), false)
                .footer("For more information, run /inf search case " + punishment.getPunishmentId(), "")
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logMessageDelete(MessageDeleteEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) {
            return;
        }
        AuditLogEntry recentDelete = guild.getAuditLog().withActionType(ActionType.MESSAGE_DELETE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .filter(auditLogEntry -> auditLogEntry.getTargetId().isPresent())
                .next()
                .block();

        String responsibleUserId;
        if (recentDelete == null || recentDelete.getResponsibleUser().isEmpty()) {
            responsibleUserId = "Unknown";
        } else {
            String id = recentDelete.getResponsibleUser().get().getId().asString();
            String username = recentDelete.getResponsibleUser().get().getUsername() + "#" + recentDelete.getResponsibleUser().get().getDiscriminator();
            responsibleUserId = "`" + username + "`:`" + id + "`:<@" + id + ">";
        }

        String senderId;
        if (event.getMessage().isPresent() && event.getMessage().get().getAuthor().isPresent()) {
            String id = event.getMessage().get().getAuthor().get().getId().asString();
            String username = event.getMessage().get().getAuthor().get().getUsername() + "#" + event.getMessage().get().getAuthor().get().getDiscriminator();
            senderId = "`" + username + "`:" + "`" + id + "`:<@" + id + ">";
        } else {
            DatabaseLoader.openConnectionIfClosed();
            ServerMessage serverMessage = ServerMessage.findFirst("server_id_snowflake = ? and message_id_snowflake = ?", guild.getId().asLong(), event.getMessageId().asLong());
            if (serverMessage != null) {
                long id = serverMessage.getUserSnowflake();
                String username = guild.getMemberById(Snowflake.of(id)).block().getUsername() + "#" + guild.getMemberById(Snowflake.of(id)).block().getDiscriminator();
                senderId = "`" + username + "`:" + "`" + id + "`:<@" + id + ">";
            } else {
                senderId = "Unknown";
            }
        }

        String content;
        if (event.getMessage().isPresent()) {
            if (event.getMessage().get().getContent().isEmpty()) {
                content = "none";
            } else {
                String string = event.getMessage().get().getContent();
                content = getStringWithLegalLength(string, 4055);
            }
        } else {
            DatabaseLoader.openConnectionIfClosed();
            ServerMessage serverMessage = ServerMessage.findFirst("server_id_snowflake = ? and message_id_snowflake = ?", guild.getId().asLong(), event.getMessageId().asLong());
            if (serverMessage != null) {
                content = getStringWithLegalLength(serverMessage.getContent(), 3055);
            } else {
                content = "Unknown";
            }
        }

        String embeds;
        if (event.getMessage().isPresent() && !event.getMessage().get().getEmbeds().isEmpty()) {
            List<Embed> embedList = event.getMessage().get().getEmbeds();
            String embedString = makeEmbedsEntries(embedList);
            embeds = getStringWithLegalLength(embedString, 1024);
        } else {
            embeds = "none";
        }

        String attachmentURLs;
        if (event.getMessage().isPresent() && !event.getMessage().get().getAttachments().isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            List<Attachment> attachmentList = event.getMessage().get().getAttachments();
            for (Attachment a : attachmentList) {
                if (attachmentList.indexOf(a) == attachmentList.size() - 1) {
                    stringBuilder.append(a.getUrl());
                } else {
                    stringBuilder.append(a.getUrl()).append("\n");
                }
            }
            attachmentURLs = stringBuilder.toString();
        } else {
            attachmentURLs = "none";
        }

        String channelId = event.getChannelId().asString();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.JAZZBERRY_JAM)
                .title(EmojiManager.getMessageDelete() + " Message Deleted")
                .addField("Sent By", senderId, false)
                .addField("Deleted By", responsibleUserId, false)
                .addField("Channel", channel, false)
                .timestamp(Instant.now())
                .build();

        if (!content.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .description("**Content:**\n" + content)
                    .build();
        }

        if (!attachmentURLs.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Attachments", attachmentURLs, false)
                    .build();
        }

        if (!embeds.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Embeds", embeds, false)
                    .build();
        }

        logChannel.createMessage(embed).block();

    }

    private static String getStringWithLegalLength(String string, int length) {
        String content;
        if (string.length() >= length) {
            content = "```" + string.substring(0, length - 55).replaceAll("`", "") + "...``` [Content too large, has been limited]";
        } else {
            content = "```\n" + string.replaceAll("`", "") + "```";
        }
        return content;
    }

    private static String makeEmbedsEntries(List<Embed> embedList) {
        String embeds;
        StringBuilder stringBuilder = new StringBuilder();
        for (Embed e : embedList) {
            StringBuilder subStringBuilder = new StringBuilder();
            String embedData;
            subStringBuilder.append("```\n");
            if (e.getTitle().isPresent()) {
                subStringBuilder.append("Title: ").append(e.getTitle().get().replaceAll("```", "\\`")).append("\n");
            }
            if (e.getDescription().isPresent()) {
                subStringBuilder.append("Description: \n").append(e.getDescription().get().replaceAll("`", "")).append("\n");
            }
            if (e.getColor().isPresent()) {
                subStringBuilder.append("Colour: ").append(e.getColor().get().getRGB()).append("\n");
            }
            if (e.getImage().isPresent()) {
                subStringBuilder.append("Image URL: ").append(e.getImage().get().getUrl()).append("\n");
            }
            if (e.getThumbnail().isPresent()) {
                subStringBuilder.append("Thumbnail URL: ").append(e.getThumbnail().get().getUrl()).append("\n");
            }
            if (e.getVideo().isPresent()) {
                subStringBuilder.append("Video URL: ").append(e.getVideo().get().getUrl()).append("\n");
            }
            if (e.getAuthor().isPresent()) {
                if (e.getAuthor().get().getName().isPresent()) {
                    subStringBuilder.append("Author Name: ").append(e.getAuthor().get().getName()).append("\n");
                }
                if (e.getAuthor().get().getUrl().isPresent()) {
                    subStringBuilder.append("Author URL: ").append(e.getAuthor().get().getUrl().get()).append("\n");
                }
                if (e.getAuthor().get().getIconUrl().isPresent()) {
                    subStringBuilder.append("Author Icon URL: ").append(e.getAuthor().get().getIconUrl()).append("\n");
                }
            }
            if (!e.getFields().isEmpty()) {
                List<Embed.Field> fieldList = e.getFields();
                for (Embed.Field field : fieldList) {
                    subStringBuilder.append("Field: ").append(fieldList.indexOf(field) + 1).append("\n");
                    subStringBuilder.append("->Name: ").append(field.getName().replaceAll("```", "\\`")).append("\n");
                    subStringBuilder.append("->Content: ").append(field.getValue().replaceAll("```", "\\`")).append("\n");
                    if (field.isInline()) {
                        subStringBuilder.append("->Inline: Yes").append("\n");
                    } else {
                        subStringBuilder.append("->Inline: No").append("\n");
                    }
                }
            }
            if (e.getFooter().isPresent()) {
                subStringBuilder.append("Footer: ").append(e.getFooter().get().getText()).append("\n");
                if (e.getFooter().get().getIconUrl().isPresent()) {
                    subStringBuilder.append("Footer URL: ").append(e.getFooter().get().getIconUrl().get()).append("\n");
                }
            }
            if (e.getTimestamp().isPresent()) {
                subStringBuilder.append("Epoch Timestamp: ").append(e.getTimestamp().get().toEpochMilli()).append("\n");
            }
            subStringBuilder.append("```\n");
            embedData = subStringBuilder.toString();
            stringBuilder.append(embedData);
        }
        embeds = stringBuilder.toString();
        return embeds;
    }

    public static void logMessageUpdate(MessageUpdateEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        String title = EmojiManager.getMessageEdit() + " Message Edit";
        String oldContent;
        String newContent;
        if (event.isContentChanged()) {
            if (event.getOld().isPresent()) {
                oldContent = getStringWithLegalLength(event.getOld().get().getContent(), 1000);
            } else {
                DatabaseLoader.openConnectionIfClosed();
                ServerMessage serverMessage = ServerMessage
                        .findFirst("message_id_snowflake = ? and server_id_snowflake = ?",
                                event.getMessageId().asLong(), guild.getId().asLong());
                if (serverMessage != null) {
                    oldContent = getStringWithLegalLength(serverMessage.getContent(), 1000);
                } else {
                    oldContent = "Unknown";
                }
            }
            if (event.getCurrentContent().isPresent()) {
                newContent = getStringWithLegalLength(event.getCurrentContent().get(), 1000);
            } else {
                newContent = getStringWithLegalLength(event.getMessage().block().getContent(), 1000);
            }
        } else if (event.isEmbedsChanged()) {
            if (event.getOld().isPresent()) {
                oldContent = makeEmbedsEntries(event.getOld().get().getEmbeds());
            } else {
                oldContent = "Unknown embed(s)";
            }
            if (!event.getCurrentEmbeds().isEmpty()) {
                newContent = makeEmbedsEntries(event.getCurrentEmbeds());
            } else {
                newContent = makeEmbedsEntries(event.getMessage().block().getEmbeds());
            }
        } else {
            oldContent = "Unknown";
            newContent = "Unknown";
        }

        long userId;
        String user;
        if (event.getMessage().block().getAuthor().isPresent()) {
            userId = event.getMessage().block().getAuthor().get().getId().asLong();
            String username = event.getMessage().block().getAuthor().get().getUsername();
            user = "`" + username + "`:" +"`" + userId + "`:<@" + userId + ">";
        } else if (event.getOld().isPresent() && event.getOld().get().getAuthor().isPresent()) {
            userId = event.getOld().get().getAuthor().get().getId().asLong();
            String username = event.getOld().get().getAuthor().get().getUsername();
            user = "`" + username + "`:" + "`" + userId + "`:<@" + userId + ">";
        } else {
            user = "Unknown";
        }

        long channelId = event.getChannelId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(title)
                .color(Color.MOON_YELLOW)
                .addField("ID", "`" + event.getMessage().block().getId().asString() + "`", false)
                .addField("User", user, false)
                .addField("Channel", channel, false)
                .addField("Old", oldContent, false)
                .addField("New", newContent, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logBulkDelete(MessageBulkDeleteEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        AuditLogEntry recentDelete = guild.getAuditLog().withActionType(ActionType.MESSAGE_BULK_DELETE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUser;
        if (recentDelete == null || recentDelete.getResponsibleUser().isEmpty()) {
            responsibleUser = "Unknown";
        } else {
            long responsibleUserId = recentDelete.getResponsibleUser().get().getId().asLong();
            String username = recentDelete.getResponsibleUser().get().getUsername() + "#" + recentDelete.getResponsibleUser().get().getDiscriminator();
            responsibleUser = "`" + username + "`:" + "`" + responsibleUserId + "`:<@" + responsibleUserId + ">";
        }

        List<Message> messageList = event.getMessages().stream().toList();
        String messages;
        if (messageList.isEmpty()) {
            messages = "Unknown";
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("```\n");
            for (Message message : messageList) {
                if (message.getContent().length() > 17) {
                    stringBuilder.append(message.getId().asLong()).append(" | ").append(message.getContent(), 0, 17).append("...\n");
                } else {
                    stringBuilder.append(message.getId().asLong()).append(" | ").append(message.getContent()).append("\n");
                }
            }
            stringBuilder.append("```");
            messages = stringBuilder.toString();
        }

        if (messages.length() >= 4000) {
            messages = messages.substring(0, 3950) + "...```\n [Content too large, has been limited]";
        }

        long channelId = event.getChannelId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getMessageDelete() + " " + EmojiManager.getMessageDelete() + " " + EmojiManager.getMessageDelete() + " Bulk Delete")
                .color(Color.JAZZBERRY_JAM)
                .description(messages)
                .addField("Responsible User", responsibleUser, false)
                .addField("Channel", channel, false)
                .timestamp(Instant.now())
                .build();

        if (recentDelete.getReason().isPresent()) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Reason", recentDelete.getReason().get(), false)
                    .build();
        }

        logChannel.createMessage(embed).block();
    }

    public static void logMemberJoin(MemberJoinEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        long memberId = event.getMember().getId().asLong();
        String username = event.getMember().getUsername() + "#" + event.getMember().getDiscriminator();
        String member = "`" + username + "`:" + "`" + memberId + "`:<@" + memberId + ">";

        String avatarUrl = event.getMember().getAvatarUrl();

        String createDate = TimestampMaker.getTimestampFromEpochSecond(
                event.getMember().getId().getTimestamp().getEpochSecond(),
                TimestampMaker.TimestampType.LONG_DATETIME);

        String badges = getBadges(event.getMember());

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserJoin() + " User Joined")
                .color(Color.ENDEAVOUR)
                .addField("User", member, false)
                .image(avatarUrl)
                .addField("Account Created", createDate, false)
                .timestamp(Instant.now())
                .build();

        if (!badges.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Badges", badges, false)
                    .build();
        }

        logChannel.createMessage(embed).block();
    }

    private static String getIconUrl(Guild guild) {
        String guildIconUrl;
        if (guild.getIconUrl(Image.Format.GIF).isPresent()) {
            guildIconUrl = guild.getIconUrl(Image.Format.GIF).get();
        } else if (guild.getIconUrl(Image.Format.PNG).isPresent()) {
            guildIconUrl = guild.getIconUrl(Image.Format.PNG).get();
        } else if (guild.getIconUrl(Image.Format.JPEG).isPresent()) {
            guildIconUrl = guild.getIconUrl(Image.Format.JPEG).get();
        } else {
            guildIconUrl = "none";
        }
        return guildIconUrl;
    }

    private static String getBadges(Member member) {
        StringBuilder stringBuilder = new StringBuilder();
        if (member.getPublicFlags().contains(User.Flag.DISCORD_CERTIFIED_MODERATOR)) {
            stringBuilder.append(EmojiManager.getModeratorBadge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.EARLY_SUPPORTER)) {
            stringBuilder.append(EmojiManager.getEarlySupporterBadge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.BUG_HUNTER_LEVEL_1)) {
            stringBuilder.append(EmojiManager.getBugHunter1Badge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.BUG_HUNTER_LEVEL_2)) {
            stringBuilder.append(EmojiManager.getBugHunter2Badge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.DISCORD_EMPLOYEE)) {
            stringBuilder.append(EmojiManager.getEmployeeBadge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.DISCORD_PARTNER)) {
            stringBuilder.append(EmojiManager.getPartnerBadge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.VERIFIED_BOT_DEVELOPER)) {
            stringBuilder.append(EmojiManager.getDeveloperBadge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.HYPESQUAD_EVENTS)) {
            stringBuilder.append(EmojiManager.getHypeSquad2Badge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.HOUSE_BALANCE)) {
            stringBuilder.append(EmojiManager.getBalanceBadge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.HOUSE_BRAVERY)) {
            stringBuilder.append(EmojiManager.getBraveryBadge()).append(" ");
        }
        if (member.getPublicFlags().contains(User.Flag.HOUSE_BRILLIANCE)) {
            stringBuilder.append(EmojiManager.getBrillianceBadge()).append(" ");
        }
        if (member.getPremiumTime().isPresent()) {
            stringBuilder.append(EmojiManager.getNitroBadge()).append(" ");
        }
        if (stringBuilder.isEmpty()) {
            return "none";
        } else return stringBuilder.toString();
    }

    public static void logMemberLeave(MemberLeaveEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        long memberId = event.getUser().getId().asLong();
        String username = event.getUser().getUsername() + "#" + event.getUser().getDiscriminator();
        String memberName = "`" + username + "`:" + "`" + memberId + "`:<@" + memberId + ">";
        String avatarUrl = event.getUser().getAvatarUrl();

        Member member;
        if (event.getMember().isPresent()) {
            member = event.getMember().get();
        } else {
            member = null;
        }

        String badges;
        String roles;
        if (member != null) {
            badges = getBadges(member);
            roles = getRolesString(member);
        } else {
            badges = "none";
            roles = "none";
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserLeave() + " User Left")
                .addField("User", memberName, false)
                .image(avatarUrl)
                .timestamp(Instant.now())
                .build();

        if (!badges.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Badges", badges, false)
                    .build();
        }

        if (!roles.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Roles", roles, false)
                    .build();
        }

        logChannel.createMessage(embed).block();
    }

    public static String getRolesString(Member member) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Role> roleList = member.getRoles().collectList().block();
        if (roleList == null) {
            return "none";
        }
        if (roleList.isEmpty()) {
            return "No roles";
        }
        for (Role role : roleList) {
            if (roleList.indexOf(role) == roleList.size() - 1) {
                stringBuilder.append("`").append(role.getName()).append("`:<@&").append(role.getId().asLong()).append(">");
            } else {
                stringBuilder.append("`").append(role.getName()).append("`:<@&").append(role.getId().asLong()).append(">, ");
            }
        }
        return stringBuilder.toString();
    }

    public static void logMemberUpdate(MemberUpdateEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        if (event.getMember().block() == null) {
            return;
        }

        long memberId = event.getMember().block().getId().asLong();
        String username = event.getMember().block().getUsername() + "#" + event.getMember().block().getDiscriminator();
        String memberName = "`" + username + "`:" + "`" + memberId + "`:<@" + memberId + ">";

        String memberInfo;
        if (event.getOld().isPresent()) {
            if (event.getOld().get().getNickname().get().equals(event.getMember().block().getNickname())
                    && event.getOld().get().getRoles().collectList().block().equals(event.getMember().block().getRoles().collectList().block())) {
                return;
            }
            memberInfo = getMemberDiff(event.getOld().get().asFullMember().block(), event.getMember().block());
        } else {
            memberInfo = getMemberInformation(event.getMember().block());
        }

        String avatarUrl = event.getMember().block().getAvatarUrl();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserIdentification() + " Member Update")
                .color(Color.MOON_YELLOW)
                .description(memberInfo)
                .addField("Member", memberName, false)
                .thumbnail(avatarUrl)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static String getMemberInformation(Member member) {
        List<Role> roles = member.getRoles().collectList().block();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("```\n");
        if (member.getNickname().isPresent()) {
            stringBuilder.append("Nickname: ").append(member.getNickname().get()).append("\n");
        } else {
            stringBuilder.append("Username: ").append(member.getUsername()).append("\n");
        }
        if (roles != null && !roles.isEmpty()) {
            for (Role role : roles) {
                stringBuilder.append("Role: ").append(role.getName()).append(":").append(role.getId().asLong()).append("\n");
            }
        }
        stringBuilder.append("```");
        return stringBuilder.toString();
    }

    public static String getMemberDiff(Member oldMember, Member newMember) {
        List<Role> oldRoles = oldMember.getRoles().collectList().block();
        List<Role> newRoles = newMember.getRoles().collectList().block();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("```diff\n");
        if (oldMember.getNickname().isPresent() && newMember.getNickname().isPresent()) {
            if (oldMember.getNickname().get().equals(newMember.getNickname().get())) {
                stringBuilder.append("--- Nickname: ").append(oldMember.getNickname().get()).append("\n");
            } else {
                stringBuilder.append("- Nickname: ").append(oldMember.getNickname().get()).append("\n");
                stringBuilder.append("+ Nickname: ").append(newMember.getNickname().get()).append("\n");
            }
        } else if (oldMember.getNickname().isEmpty() && newMember.getNickname().isPresent()) {
            stringBuilder.append("+ Nickname: ").append(newMember.getNickname().get()).append("\n");
        } else if (oldMember.getNickname().isPresent() && newMember.getNickname().isEmpty()) {
            stringBuilder.append("- Nickname: ").append(oldMember.getNickname().get()).append("\n");
        } else if (oldMember.getNickname().isEmpty() && newMember.getNickname().isEmpty()) {
            stringBuilder.append("--- No nickname").append("\n");
        }
        if (oldRoles != null && newRoles != null) {
            if (oldRoles.equals(newRoles)) {
                stringBuilder.append("--- No role changes").append("\n");
            } else {
                for (Role role : oldRoles) {
                    if (!newRoles.contains(role)) {
                        stringBuilder.append("- Role: ").append(role.getName()).append(": ").append(role.getId().asLong()).append("\n");
                    }
                }
                for (Role role : newRoles) {
                    if (!oldRoles.contains(role)) {
                        stringBuilder.append("+ Role: ").append(role.getName()).append(": ").append(role.getId().asLong()).append("\n");
                    }
                }
            }
        }
        stringBuilder.append("```");
        return stringBuilder.toString();
    }

    public static void logPresenceUpdate(PresenceUpdateEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        if (event.getMember().block() == null) {
            return;
        }

        if (event.getOldUser().isEmpty()) {
            return;
        }

        long memberId = event.getMember().block().getId().asLong();
        String username = event.getMember().block().getUsername() + "#" + event.getMember().block().getDiscriminator();
        String memberName = "`" + username + "`:" + "`" + memberId + "`:<@" + memberId + ">";

        String presenceDiffInfo;
        User oldUser = event.getOldUser().get();
        User newUser = event.getUser().block();
        if (oldUser.getDiscriminator().equals(newUser.getDiscriminator())
                && oldUser.getUsername().equals(newUser.getUsername())
                && oldUser.getAvatarUrl().equals(newUser.getAvatarUrl())) {
            return;
        } else {
            presenceDiffInfo = getPresenceDiff(oldUser, newUser);
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserIdentification() + " Member Update")
                .color(Color.MOON_YELLOW)
                .description(presenceDiffInfo)
                .addField("Member", memberName, false)
                .thumbnail(event.getMember().block().getAvatarUrl())
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static String getPresenceDiff(User oldUser, User newUser) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("```diff\n");
        if (oldUser.getUsername().equals(newUser.getUsername())) {
                stringBuilder.append("--- Username: ").append(oldUser.getUsername()).append("\n");
        } else {
            stringBuilder.append("- Username: ").append(oldUser.getUsername()).append("\n");
            stringBuilder.append("+ Username: ").append(newUser.getUsername()).append("\n");
        }
        if (oldUser.getDiscriminator().equals(newUser.getDiscriminator())) {
            stringBuilder.append("--- Discriminator: #").append(oldUser.getDiscriminator()).append("\n");
        } else {
            stringBuilder.append("- Discriminator: #").append(oldUser.getDiscriminator()).append("\n");
            stringBuilder.append("+ Discriminator: #").append(newUser.getDiscriminator()).append("\n");
        }
        if (oldUser.getAvatarUrl().equals(newUser.getAvatarUrl())) {
            stringBuilder.append("--- Avatar URL: ").append(oldUser.getAvatarUrl()).append("\n");
        } else {
            stringBuilder.append("- Avatar URL: ").append(oldUser.getAvatarUrl()).append("\n");
            stringBuilder.append("+ Avatar URL: ").append(newUser.getAvatarUrl()).append("\n");
        }
        stringBuilder.append("```");
        return stringBuilder.toString();
    }

    public static void logInviteCreate(InviteCreateEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        String inviter;
        if (event.getInviter().isPresent()) {
            long inviterId = event.getInviter().get().getId().asLong();
            String username = event.getInviter().get().getUsername() + "#" + event.getInviter().get().getDiscriminator();
            inviter = "`" + username + "`:`" + inviterId + "`:<@" + inviterId + ">";
        } else {
            inviter = "Unknown";
        }

        long channelId = event.getChannelId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getInvite() + " Server Invite Created")
                .color(Color.ENDEAVOUR)
                .addField("Inviter", inviter, false)
                .addField("Invite Code", "`" + event.getCode() + "`", false)
                .addField("Channel", channel, false)
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logNewsCreate(NewsChannelCreateEvent event, TextChannel logChannel) {
        AuditLogEntry channelCreate = event.getChannel().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_CREATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(channelCreate);

        long channelId = event.getChannel().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getNewsChannel() + " News Channel Created")
                .color(Color.SEA_GREEN)
                .addField("Channel", channel, false)
                .addField("Created By", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logNewsDelete(NewsChannelDeleteEvent event, TextChannel logChannel) {
        if (event.getChannel().getGuild().block() == null) {
            return;
        }

        AuditLogEntry channelDelete = event.getChannel().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_DELETE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(channelDelete);

        long channelId = event.getChannel().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getNewsChannel() + " News Channel Deleted")
                .color(Color.JAZZBERRY_JAM)
                .addField("Channel", channel, false)
                .addField("Deleted By", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logNewsUpdate(NewsChannelUpdateEvent event, TextChannel logChannel) {
        if (event.getCurrent().getGuild().block() == null) {
            return;
        }

        AuditLogEntry newsUpdate = event.getCurrent().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_UPDATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(newsUpdate);

        long channelId = event.getCurrent().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        String information;
        if (event.getOld().isPresent() && event.getNewsChannel().isPresent()) {
            if (event.getOld().get().getName().equals(event.getCurrent().getName())
                && event.getOld().get().getCategory().block().equals(event.getNewsChannel().get().getCategory().block().getName())) {
                information = getNewsChannelDiff(event.getOld().get(), event.getNewsChannel().get());
            } else {
                information = getNewsChannelDiff(event.getOld().get(), event.getNewsChannel().get());
            }
        } else {
            information = "none";
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getNewsChannel() + " News Channel Updated")
                .addField("Channel", channel, false)
                .addField("Updated By", responsibleUserId, false)
                .color(Color.ENDEAVOUR)
                .timestamp(Instant.now())
                .footer("Check your server's audit log for more information", "")
                .build();

        if (!information.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .description(information)
                    .build();
        }

        logChannel.createMessage(embed).block();
    }

    private static String getNewsChannelDiff(NewsChannel oldChannel, NewsChannel newChannel) {
        return getChannelDiff(oldChannel.getName(), newChannel.getName(), oldChannel.getCategory(), newChannel.getCategory());
    }

    public static void logStoreCreate(StoreChannelCreateEvent event, TextChannel logChannel) {
        AuditLogEntry channelCreate = event.getChannel().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_CREATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(channelCreate);

        long channelId = event.getChannel().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getStoreChannel() + " Store Channel Created")
                .color(Color.SEA_GREEN)
                .addField("Channel", channel, false)
                .addField("Created By", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logStoreDelete(StoreChannelDeleteEvent event, TextChannel logChannel) {
        AuditLogEntry channelDelete = event.getChannel().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_DELETE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(channelDelete);

        long channelId = event.getChannel().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getStoreChannel() + " Store Channel Deleted")
                .color(Color.JAZZBERRY_JAM)
                .addField("Channel", channel, false)
                .addField("Deleted By", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logStoreUpdate(StoreChannelUpdateEvent event, TextChannel logChannel) {

        if (event.getCurrent().getGuild().block() == null) {
            return;
        }

        AuditLogEntry newsUpdate = event.getCurrent().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_UPDATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(newsUpdate);

        long channelId = event.getCurrent().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        String information;
        if (event.getOld().isPresent()) {
            if (event.getOld().get().getName().equals(event.getCurrent().getName())
                    && event.getOld().get().getCategory().block().equals(event.getCurrent().getCategory().block().getName())) {
                information = getStoreChannelDiff(event.getOld().get(), event.getCurrent());
            } else {
                information = getStoreChannelDiff(event.getOld().get(), event.getCurrent());
            }
        } else {
            information = "none";
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getStoreChannel() + " Store Channel Updated")
                .addField("Channel", channel, false)
                .addField("Updated By", responsibleUserId, false)
                .color(Color.ENDEAVOUR)
                .timestamp(Instant.now())
                .footer("Check your server's audit log for more information", "")
                .build();

        if (!information.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .description(information)
                    .build();
        }

        logChannel.createMessage(embed).block();
    }

    private static String getStoreChannelDiff(StoreChannel oldChannel, StoreChannel newChannel) {
        return getChannelDiff(oldChannel.getName(), newChannel.getName(), oldChannel.getCategory(), newChannel.getCategory());
    }

    public static void logVoiceCreate(VoiceChannelCreateEvent event, TextChannel logChannel) {
        AuditLogEntry channelCreate = event.getChannel().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_CREATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(channelCreate);

        long channelId = event.getChannel().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getVoiceChannel() + " Voice Channel Created")
                .color(Color.SEA_GREEN)
                .addField("Channel", channel, false)
                .addField("Created By", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logVoiceDelete(VoiceChannelDeleteEvent event, TextChannel logChannel) {
        AuditLogEntry channelDelete = event.getChannel().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_DELETE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(channelDelete);

        long channelId = event.getChannel().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getVoiceChannel() + " Voice Channel Deleted")
                .color(Color.JAZZBERRY_JAM)
                .addField("Channel", channel, false)
                .addField("Deleted By", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logVoiceUpdate(VoiceChannelUpdateEvent event, TextChannel logChannel) {

        if (event.getCurrent().getGuild().block() == null) {
            return;
        }

        AuditLogEntry newsUpdate = event.getCurrent().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_UPDATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(newsUpdate);

        long channelId = event.getCurrent().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        String information;
        if (event.getOld().isPresent()) {
            if (event.getOld().get().getName().equals(event.getCurrent().getName())
                    && event.getOld().get().getCategory().block().equals(event.getCurrent().getCategory().block().getName())) {
                information = getVoiceChannelDiff(event.getOld().get(), event.getCurrent());
            } else {
                information = getVoiceChannelDiff(event.getOld().get(), event.getCurrent());
            }
        } else {
            information = "none";
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getVoiceChannel() + " Voice Channel Updated")
                .addField("Channel", channel, false)
                .addField("Updated By", responsibleUserId, false)
                .color(Color.ENDEAVOUR)
                .timestamp(Instant.now())
                .footer("Check your server's audit log for more information", "")
                .build();

        if (!information.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .description(information)
                    .build();
        }

        logChannel.createMessage(embed).block();
    }

    private static String getVoiceChannelDiff(VoiceChannel oldChannel, VoiceChannel newChannel) {
        return getChannelDiff(oldChannel.getName(), newChannel.getName(), oldChannel.getCategory(), newChannel.getCategory());
    }

    public static void logTextCreate(TextChannelCreateEvent event, TextChannel logChannel) {
        AuditLogEntry channelCreate = event.getChannel().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_CREATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(channelCreate);

        long channelId = event.getChannel().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getTextChannel() + " Text Channel Created")
                .color(Color.SEA_GREEN)
                .addField("Channel", channel, false)
                .addField("Created By", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    private static String getAuditResponsibleUser(AuditLogEntry aud) {
        String responsibleUserId;
        if (aud == null || aud.getResponsibleUser().isEmpty()) {
            responsibleUserId = "Unknown";
        } else {
            String username = aud.getResponsibleUser().get().getUsername() + "#" + aud.getResponsibleUser().get().getDiscriminator();
            String id = aud.getResponsibleUser().get().getId().asString();
            responsibleUserId = "`" + username + "`:`" + id + "`:<@" + id + ">";
        }
        return responsibleUserId;
    }

    private static String getAuditTargetUser(AuditLogEntry aud) {
        String responsibleUserId;
        if (aud == null || aud.getTargetId().isEmpty()) {
            responsibleUserId = "Unknown";
        } else {
            String id = aud.getTargetId().get().asString();
            responsibleUserId = "`" + id + "`:<@" + id + ">";
        }
        return responsibleUserId;
    }

    public static void logTextDelete(TextChannelDeleteEvent event, TextChannel logChannel) {
        AuditLogEntry channelDelete = event.getChannel().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_DELETE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(channelDelete);

        long channelId = event.getChannel().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getTextChannel() + " Text Channel Deleted")
                .color(Color.JAZZBERRY_JAM)
                .addField("Channel", channel, false)
                .addField("Deleted By", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logTextUpdate(TextChannelUpdateEvent event, TextChannel logChannel) {
        if (event.getCurrent().getGuild().block() == null) {
            return;
        }

        AuditLogEntry newsUpdate = event.getCurrent().getGuild().block().getAuditLog().withActionType(ActionType.CHANNEL_UPDATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(newsUpdate);

        long channelId = event.getCurrent().getId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        String information;
        if (event.getOld().isPresent() && event.getTextChannel().isPresent()) {
            if (event.getOld().get().getName().equals(event.getCurrent().getName())
                    && event.getOld().get().getCategory().block().equals(event.getNewsChannel().get().getCategory().block().getName())) {
                information = getTextChannelDiff(event.getOld().get(), event.getTextChannel().get());
            } else {
                information = getTextChannelDiff(event.getOld().get(), event.getTextChannel().get());
            }
        } else {
            information = "none";
        }

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getTextChannel() + " Text Channel Updated")
                .addField("Channel", channel, false)
                .addField("Updated By", responsibleUserId, false)
                .color(Color.ENDEAVOUR)
                .timestamp(Instant.now())
                .footer("Check your server's audit log for more information", "")
                .build();

        if (!information.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .description(information)
                    .build();
        }

        logChannel.createMessage(embed).block();
    }

    private static String getTextChannelDiff(TextChannel oldChannel, TextChannel newChannel) {
        return getChannelDiff(oldChannel.getName(), newChannel.getName(), oldChannel.getCategory(), newChannel.getCategory());
    }

    private static String getChannelDiff(String name, String name2, Mono<Category> category, Mono<Category> category2) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("```diff\n");
        if (name.equals(name2)) {
            stringBuilder.append("--- Name: ").append(name).append("\n");
        } else {
            stringBuilder.append("- Name: ").append(name).append("\n");
            stringBuilder.append("+ Name: ").append(name2).append("\n");
        }
        if (category.block().getName().equals(category2.block().getName())) {
            stringBuilder.append("--- Category: ").append(category.block().getName()).append("\n");
        } else {
            stringBuilder.append("- Category: ").append(category.block().getName()).append("\n");
            stringBuilder.append("+ Category: ").append(category2.block().getName()).append("\n");
        }
        stringBuilder.append("```");
        return stringBuilder.toString();
    }

    public static void logBan(BanEvent event, TextChannel logChannel) {
        AuditLogEntry userBan = event.getGuild().block().getAuditLog().withActionType(ActionType.MEMBER_BAN_ADD)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .filter(auditLogEntry -> auditLogEntry.getTargetId().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(userBan);
        long targetUserId = event.getUser().getId().asLong();
        String reason;
        if (userBan.getReason().isPresent()) {
            reason = userBan.getReason().get();
        } else {
            reason = "No reason provided.";
        }

        String caseId;
        if (!userBan.getResponsibleUser().equals(ClientManager.getManager().getClient().getSelf().block())) {
            DatabaseLoader.openConnectionIfClosed();
            DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", event.getGuild().block().getId().asLong());
            DiscordUser punisher = DiscordUser.findFirst("user_id_snowflake = ?", userBan.getResponsibleUser().get().getId().asLong());
            DiscordUser punished = DiscordUser.findFirst("user_id_snowflake = ?", userBan.getTargetId().get().asLong());
            Punishment punishment = Punishment.create("user_id_punished", punished.getUserId(),
                    "user_id_punisher", punisher.getUserId(),
                    "server_id", discordServer.getServerId(),
                    "punishment_type", "ban",
                    "punishment_date", Instant.now().toEpochMilli(),
                    "punishment_message", reason);
            punishment.save();
            punishment.refresh();
            caseId = String.valueOf(punishment.getPunishmentId());
        } else {
            caseId = "none";
        }


        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserBan() + " User Banned")
                .color(Color.JAZZBERRY_JAM)
                .addField("Punished User", String.valueOf(targetUserId), false)
                .addField("Punishing User", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        if (!caseId.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Case ID", "#" + caseId, false)
                    .build();
        }

        logChannel.createMessage(embed).block();

    }

    public static void logUnban(UnbanEvent event, TextChannel logChannel) {

        AuditLogEntry userUnban = event.getGuild().block().getAuditLog().withActionType(ActionType.MEMBER_BAN_REMOVE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(userUnban);

        long userId = event.getUser().getId().asLong();
        String username = event.getUser().getUsername() + "#" + event.getUser().getDiscriminator();
        String user = "`" + username + "`:`" + userId + "`:<@" + userId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserBan() + " User Unbanned")
                .color(Color.SEA_GREEN)
                .addField("User", user, false)
                .addField("Unbanned By", responsibleUserId, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logRoleCreate(RoleCreateEvent event, TextChannel logChannel) {
        AuditLogEntry roleCreate = event.getGuild().block().getAuditLog().withActionType(ActionType.ROLE_CREATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(roleCreate);

        long roleId = event.getRole().getId().asLong();
        String roleName = event.getRole().getName();
        String role = "`" + roleName + "`:`" + roleId + "`:<@&" + roleId + ">";
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Role Created")
                .color(Color.ENDEAVOUR)
                .addField("User", responsibleUserId, false)
                .addField("Role", role, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logRoleDelete(RoleDeleteEvent event, TextChannel logChannel) {
        AuditLogEntry roleDelete = event.getGuild().block().getAuditLog().withActionType(ActionType.ROLE_DELETE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUserId = getAuditResponsibleUser(roleDelete);
        String role;
        if (event.getRole().isPresent()) {
            long roleId = event.getRole().get().getId().asLong();
            String roleName = event.getRole().get().getName();
            role = "`" + roleName + "`:`" + roleId + "`:<@&" + roleId + ">";
        } else {
            role = "Unknown";
        }
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Role Deleted")
                .color(Color.JAZZBERRY_JAM)
                .addField("User", responsibleUserId, false)
                .addField("Role", role, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logRoleUpdate(RoleUpdateEvent event, TextChannel logChannel) {

        AuditLogEntry roleUpdate = event.getCurrent().getGuild().block().getAuditLog().withActionType(ActionType.ROLE_UPDATE)
                .map(AuditLogPart::getEntries)
                .flatMap(Flux::fromIterable)
                .filter(auditLogEntry -> auditLogEntry.getResponsibleUser().isPresent())
                .next()
                .block();

        String responsibleUser = getAuditResponsibleUser(roleUpdate);

        Role oldRole;
        if (event.getOld().isPresent()) {
            oldRole = event.getOld().get();
        } else {
            oldRole = null;
        }

        String roleInfo;
        if (oldRole != null) {
            roleInfo = getRoleDiff(oldRole, event.getCurrent());
        } else {
            roleInfo = "none";
        }

        long roleId = event.getCurrent().getId().asLong();
        String name = "`" + event.getCurrent().getName() + "`:`" + roleId + "`:<@&" + roleId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserIdentification() + " Role Updated")
                .color(Color.ENDEAVOUR)
                .addField("Responsible User", responsibleUser, false)
                .addField("Role", name, false)
                .footer("Check your server's audit log for more information", "")
                .timestamp(Instant.now())
                .build();

        if (!roleInfo.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .description(roleInfo)
                    .build();
        }

        logChannel.createMessage(embed).block();

    }

    public static String getRoleDiff(Role oldRole, Role newRole) {
        StringBuilder stringBuilder = new StringBuilder();
        String roleInfo;
        stringBuilder.append("```diff\n");
        if (oldRole.getName().equals(newRole.getName())) {
            stringBuilder.append("--- Name: ").append(oldRole.getName()).append("\n");
        } else {
            stringBuilder.append("- Name: ").append(oldRole.getName()).append("\n");
            stringBuilder.append("+ Name: ").append(newRole.getName()).append("\n");
        }
        if (oldRole.getPosition().block().equals(newRole.getPosition().block())) {
            stringBuilder.append("--- Position: ").append(oldRole.getPosition().block()).append("\n");
        } else {
            stringBuilder.append("- Position: ").append(oldRole.getPosition().block()).append("\n");
            stringBuilder.append("+ Position: ").append(newRole.getPosition().block()).append("\n");
        }
        if (oldRole.getColor().equals(newRole.getColor())) {
            stringBuilder.append("--- Colour: ").append(oldRole.getColor().getRGB()).append("\n");
        } else {
            stringBuilder.append("- Colour: ").append(oldRole.getColor().getRGB()).append("\n");
            stringBuilder.append("+ Colour: ").append(newRole.getColor().getRGB()).append("\n");
        }
        if (oldRole.isMentionable() == newRole.isMentionable()) {
            if (oldRole.isMentionable()) {
                stringBuilder.append("--- Mentionable: Yes").append("\n");
            } else {
                stringBuilder.append("--- Mentionable: No").append("\n");
            }
        } else {
            if (oldRole.isMentionable()) {
                stringBuilder.append("- Mentionable: Yes").append("\n");
                stringBuilder.append("+ Mentionable: No").append("\n");
            } else {
                stringBuilder.append("- Mentionable: No").append("\n");
                stringBuilder.append("+ Mentionable: Yes").append("\n");
            }
        }
        if (oldRole.isHoisted() == newRole.isHoisted()) {
            if (oldRole.isHoisted()) {
                stringBuilder.append("--- Hoisted: Yes").append("\n");
            } else {
                stringBuilder.append("--- Hoisted: No").append("\n");
            }
        } else {
            if (oldRole.isHoisted()) {
                stringBuilder.append("- Hoisted: Yes").append("\n");
                stringBuilder.append("+ Hoisted: No").append("\n");
            } else {
                stringBuilder.append("- Hoisted: No").append("\n");
                stringBuilder.append("+ Hoisted: Yes").append("\n");

            }
        }
        stringBuilder.append("```");
        roleInfo = stringBuilder.toString();
        return roleInfo;
    }

    public static void logPunishmentUnban(TextChannel logChannel, String reason, Punishment punishment) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser punished = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        long punishedId = punished.getUserIdSnowflake();
        String punishedName = "`" + punishedId + "`:<@" + punishedId + ">";
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserBan() + " User Unbanned")
                .addField("User", punishedName, false)
                .addField("Reason", getStringWithLegalLength(reason, 1024), false)
                .footer("For more information, run /inf search case " + punishment.getPunishmentId(), "")
                .color(Color.SEA_GREEN)
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logPunishmentUnmute(TextChannel logChannel, String reason, Punishment punishment) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser punished = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        long punishedId = punished.getUserIdSnowflake();
        String punishedName = "`" + punishedId + "`:<@" + punishedId + ">";
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserMute() + " User Unmuted")
                .addField("User", punishedName, false)
                .addField("Reason", getStringWithLegalLength(reason, 1024), false)
                .footer("For more information, run /inf search case " + punishment.getPunishmentId(), "")
                .color(Color.SEA_GREEN)
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logMutedRoleDelete(Role mutedRole, TextChannel logChannel) {
        long roleId = mutedRole.getId().asLong();
        String role = "`" + mutedRole.getName() + "`:`" + roleId + "`";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.JAZZBERRY_JAM)
                .title("Muted Role Deleted")
                .description("Oh no! You've deleted the role that this bot uses to mute people! Worry not - next time " +
                        "you try to mute someone, the role will be recreated :sparkles: *automatically* :sparkles:.")
                .addField("Role", role, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

}
