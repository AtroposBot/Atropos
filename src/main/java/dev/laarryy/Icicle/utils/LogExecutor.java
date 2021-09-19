package dev.laarryy.Icicle.utils;

import dev.laarryy.Icicle.config.EmojiManager;
import dev.laarryy.Icicle.models.guilds.ServerMessage;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
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
import discord4j.core.event.domain.message.MessageBulkDeleteEvent;
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
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

public final class LogExecutor {
    private LogExecutor() {
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
                .footer("For more information, run /inf search case <id>", "")
                .timestamp(Instant.now())
                .build();
        logChannel.createMessage("A punishment has been recorded").subscribe();
        logChannel.createMessage(embed).subscribe();
    }

    public static void logMessageDelete(MessageDeleteEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;
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
            responsibleUserId = "`" + id + "`:<@" + id + ">";
        }

        String senderId;
        if (event.getMessage().isPresent() && event.getMessage().get().getAuthor().isPresent()) {
            String id = event.getMessage().get().getAuthor().get().getId().asString();
            senderId = "`" + id + "`:<@" + id + ">";
        } else {
            DatabaseLoader.openConnectionIfClosed();
            ServerMessage serverMessage = ServerMessage.findFirst("server_id_snowflake = ? and message_id_snowflake = ?", guild.getId().asLong(), event.getMessageId().asLong());
            if (serverMessage != null) {
                long id = serverMessage.getUserSnowflake();
                senderId = "`" + id + "`:<@" + id + ">";
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

        logChannel.createMessage(embed).subscribe();

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
            user = "`" + userId + "`:<@" + userId + ">";
        } else if (event.getOld().isPresent() && event.getOld().get().getAuthor().isPresent()) {
            userId = event.getOld().get().getAuthor().get().getId().asLong();
            user = "`" + userId + "`:<@" + userId + ">";
        } else {
            user = "Unknown";
        }

        long channelId = event.getChannelId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(title)
                .color(Color.MOON_YELLOW)
                .addField("User", user, false)
                .addField("Channel", channel, false)
                .addField("Old", oldContent, false)
                .addField("New", newContent, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).subscribe();
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
            responsibleUser = "`" + responsibleUserId + "`<@" + responsibleUserId + ">";
        }

        List<Message> messageList = event.getMessages().stream().toList();
        String messages;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("```\n");
        for (Message message : messageList) {
            stringBuilder.append(message.getId().asLong()).append(" | ").append(message.getContent(), 0, 17).append("...\n");
        }
        stringBuilder.append("```");
        messages = stringBuilder.toString();

        if (messages.length() >= 4000) {
            messages = messages.substring(0, 3950) + "...```\n [Content too large, has been limited]";
        }

        long channelId = event.getChannelId().asLong();
        String channel = "`" + channelId + "`:<#" + channelId + ">";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getMessageDelete() + " " + EmojiManager.getMessageDelete() + " " + EmojiManager.getMessageDelete() + " Bulk Delete")
                .description(messages)
                .addField("Responsible User", responsibleUser, false)
                .addField("Channel", channel, false)
                .build();

        if (recentDelete.getReason().isPresent()) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Reason", recentDelete.getReason().get(), false)
                    .build();
        }

        logChannel.createMessage(embed).subscribe();
    }

    public static void logMemberJoin(MemberJoinEvent event, TextChannel logChannel) {

    }

    public static void logMemberLeave(MemberLeaveEvent event, TextChannel logChannel) {

    }

    public static void logMemberUpdate(MemberUpdateEvent event, TextChannel logChannel) {

    }

    public static void logPresenceUpdate(PresenceUpdateEvent event, TextChannel logChannel) {

    }

    public static void logInviteCreate(InviteCreateEvent event, TextChannel logChannel) {

    }

    public static void logNewsCreate(NewsChannelCreateEvent event, TextChannel logChannel) {

    }

    public static void logNewsDelete(NewsChannelDeleteEvent event, TextChannel logChannel) {

    }

    public static void logNewsUpdate(NewsChannelUpdateEvent event, TextChannel logChannel) {

    }

    public static void logStoreCreate(StoreChannelCreateEvent event, TextChannel logChannel) {

    }

    public static void logStoreDelete(StoreChannelDeleteEvent event, TextChannel logChannel) {

    }

    public static void logStoreUpdate(StoreChannelUpdateEvent event, TextChannel logChannel) {

    }

    public static void logVoiceCreate(VoiceChannelCreateEvent event, TextChannel logChannel) {

    }

    public static void logVoiceDelete(VoiceChannelDeleteEvent event, TextChannel logChannel) {

    }

    public static void logVoiceUpdate(VoiceChannelUpdateEvent event, TextChannel logChannel) {

    }

    public static void logTextCreate(TextChannelCreateEvent event, TextChannel logChannel) {

    }

    public static void logTextDelete(TextChannelDeleteEvent event, TextChannel logChannel) {

    }

    public static void logTextUpdate(TextChannelUpdateEvent event, TextChannel logChannel) {

    }

    public static void logBan(BanEvent event, TextChannel logChannel) {

    }

    public static void logUnban(UnbanEvent event, TextChannel logChannel) {

    }

    public static void logRoleCreate(RoleCreateEvent event, TextChannel logChannel) {

    }

    public static void logRoleDelete(RoleDeleteEvent event, TextChannel logChannel) {

    }

    public static void logRoleUpdate(RoleUpdateEvent event, TextChannel logChannel) {

    }

    public static void logPunishmentUnban(Long unBannedUserId, TextChannel logChannel, String reason) {

    }

    public static void logPunishmentUnmute(Long unMutedUserId, TextChannel logChannel, String reason) {

    }

}
