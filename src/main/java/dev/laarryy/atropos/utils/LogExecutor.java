package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.managers.ClientManager;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.guilds.ServerBlacklist;
import dev.laarryy.atropos.models.guilds.ServerMessage;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.models.users.Punishment;
import dev.laarryy.atropos.storage.DatabaseLoader;
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
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
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
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Category;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.entity.channel.NewsChannel;
import discord4j.core.object.entity.channel.StoreChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public final class LogExecutor {
    private LogExecutor() {
    }

    public static Mono<Void> logInsubordination(ChatInputInteractionEvent event, TextChannel logChannel, Member target) {
        return event.getInteraction().getGuild().flatMap($ -> {
            long targetId = target.getId().asLong();
            String username = target.getUsername() + '#' + target.getDiscriminator();
            String targetInfo = "`%s`:`%d`:%s".formatted(username, targetId, target.getMention());

            String mutineerInfo = event.getInteraction().getMember()
                .map(mutineer -> {
                    long mutineerId = mutineer.getId().asLong();
                    String mutineerName = mutineer.getUsername() + '#' + mutineer.getDiscriminator();
                    return "`%s`:`%d`:%s".formatted(mutineerName, mutineerId, mutineer.getMention());
                }).orElse("Unknown");

            return Flux.fromIterable(event.getOptions())
                .map(AuditLogger::generateOptionString)
                .reduce(event.getCommandName(), String::concat)
                .map(commandContent -> EmbedCreateSpec.builder()
                    .title(EmojiManager.getUserWarn() + " Insubordination Alert")
                    .description("A mutineer has attempted to punish someone above them.")
                    .addField("User", mutineerInfo, false)
                    .addField("Target", targetInfo, false)
                    .addField("Command", '`' + commandContent + '`', false)
                    .timestamp(Instant.now())
                    .build())
                .flatMap(logChannel::createMessage);
        }).then();
    }

    public static Mono<Void> logInsubordination(ButtonInteractionEvent event, TextChannel logChannel, Member target) {
        return event.getInteraction().getGuild().flatMap($ -> {
            long targetId = target.getId().asLong();
            String username = target.getUsername() + '#' + target.getDiscriminator();
            String targetInfo = "`%s`:`%d`:%s".formatted(username, targetId, target.getMention());

            String mutineerInfo = event.getInteraction().getMember()
                .map(mutineer -> {
                    long mutineerId = mutineer.getId().asLong();
                    String mutineerName = mutineer.getUsername() + '#' + mutineer.getDiscriminator();
                    return "`%s`:`%d`:%s".formatted(mutineerName, mutineerId, mutineer.getMention());
                }).orElse("Unknown");

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserWarn() + " Insubordination Alert")
                .description("A mutineer has attempted to punish someone above them.")
                .addField("User", mutineerInfo, false)
                .addField("Target", targetInfo, false)
                .addField("Command", '`' + event.getCustomId() + '`', false)
                .timestamp(Instant.now())
                .build();

            return logChannel.createMessage(embed);
        }).then();
    }

    public static Mono<Void> logBlacklistTrigger(MessageCreateEvent event, ServerBlacklist blacklist, Punishment punishment, TextChannel logChannel) {
        return Mono.justOrEmpty(event.getMember()).flatMap(user -> {
            DatabaseLoader.openConnectionIfClosed();
            long userId = user.getId().asLong();
            String username = user.getUsername() + '#' + user.getDiscriminator();
            String userInfo = "`%s`:`%d`:%s".formatted(username, userId, user.getMention());

            String content = event.getMessage().getContent();

            int blacklistId = blacklist.getBlacklistId();

            EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserWarn() + " Blacklist Triggered")
                .color(Color.MOON_YELLOW)
                .description(
                    "Blacklist ID #`" + blacklistId + "` was triggered and the message detected has been deleted. " +
                    "A case has been opened for the user who triggered it with ID #`" + punishment.getPunishmentId() + '`'
                ).addField("Content", getStringWithLegalLength(content, 1024), false)
                .footer("To see information about this blacklist entry, run /settings blacklist info " + blacklistId, "")
                .timestamp(Instant.now());

            final List<Attachment> attachments = event.getMessage().getAttachments();
            if (!attachments.isEmpty()) {
                embed.addField("Attachments", attachments.stream().map(Attachment::getFilename).collect(Collectors.joining("\n")), false);
            }

            DatabaseLoader.closeConnectionIfOpen();
            return logChannel.createMessage(embed.build());
        }).then();
    }

    public static Mono<Void> logPunishment(Punishment punishment, TextChannel logChannel) {
        return Mono.defer(() -> {
            DatabaseLoader.openConnectionIfClosed();
            DiscordUser punishedUser = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
            DiscordUser punishingUser = DiscordUser.findFirst("id = ?", punishment.getPunishingUserId());
            String type = switch (punishment.getPunishmentType()) {
                case "warn" -> EmojiManager.getUserWarn() + " Warn";
                case "note" -> EmojiManager.getUserCase() + " Note";
                case "mute" -> EmojiManager.getUserMute() + " Mute";
                case "kick" -> EmojiManager.getUserKick() + " Kick";
                case "ban" -> EmojiManager.getUserBan() + " Ban";
                default -> punishment.getPunishmentType();
            };
            DatabaseLoader.closeConnectionIfOpen();
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(type + ": ID #" + punishment.getPunishmentId())
                .color(Color.ENDEAVOUR)
                .addField("Punished User", "`%d`:<@%1$d>".formatted(punishedUser.getUserIdSnowflake()), false)
                .addField("Punishing User", "`%d`:<@%1$d>".formatted(punishingUser.getUserIdSnowflake()), false)
                .addField("Reason", punishment.getPunishmentMessage(), false)
                .footer("For more information, run /case search id " + punishment.getPunishmentId(), "")
                .timestamp(Instant.now())
                .build();

            return logChannel.createMessage(embed);
        }).then();
    }

    public static Mono<Void> logAutoMute(Punishment punishment, TextChannel logChannel) {
        return Mono.defer(() -> {
            DatabaseLoader.openConnectionIfClosed();
            DiscordUser punishedUser = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
            DatabaseLoader.closeConnectionIfOpen();

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Automatic Mute")
                .color(Color.ENDEAVOUR)
                .addField("Punished User", "`%d`:<@%1$d>".formatted(punishedUser.getUserIdSnowflake()), false)
                .addField("Reason", punishment.getPunishmentMessage(), false)
                .footer("Use the buttons to take appropriate action. ID: " + punishment.getPunishmentId(), "")
                .timestamp(Instant.now())
                .build();

            Button banButton = Button.danger(punishment.getPunishmentId() + "-atropos-ban-" + punishedUser.getUserId(), "Ban User");
            Button unmuteButton = Button.success(punishment.getPunishmentId() + "-atropos-unmute-" + punishedUser.getUserId(), "Unmute User");
            Button kickButton = Button.primary(punishment.getPunishmentId() + "-atropos-kick-" + punishedUser.getUserId(), "Kick User");

            return logChannel.createMessage(embed).withComponents(ActionRow.of(banButton, kickButton, unmuteButton));
        }).then();
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
        if (recentDelete == null
                || recentDelete.getResponsibleUser().isEmpty()
                || !recentDelete.getId().getTimestamp().isAfter(Instant.now().minus(Duration.ofSeconds(15)))
        ) {
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
        DatabaseLoader.closeConnectionIfOpen();
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
                subStringBuilder.append("Colour: ").append(Integer.toHexString(e.getColor().get().getRGB())).append("\n");
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
            if (event.getOld().isPresent() && !event.getOld().get().getEmbeds().isEmpty()) {
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
        DatabaseLoader.closeConnectionIfOpen();
    }

    public static void logBulkDelete(MessageBulkDeleteEvent event, TextChannel logChannel) {
        Guild guild = event.getGuild().block();
        if (guild == null) return;

        AuditLogEntry recentDelete = guild.getAuditLog().withActionType(ActionType.MESSAGE_BULK_DELETE)
                .flatMapIterable(AuditLogPart::getEntries)
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
        List<Snowflake> snowflakes = event.getMessageIds().stream().toList();
        String messages;
        DatabaseLoader.openConnectionIfClosed();
        if (messageList.isEmpty() && snowflakes.isEmpty()) {
            messages = "Unknown";
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("```\n");
            if (!messageList.isEmpty()) {
                for (Message message : messageList) {
                    if (!message.getContent().isEmpty()) {
                        if (message.getContent().length() > 17) {
                            stringBuilder.append(message.getId().asLong()).append(" | ").append(message.getContent(), 0, 17).append("...\n");
                        } else {
                            stringBuilder.append(message.getId().asLong()).append(" | ").append(message.getContent()).append("\n");
                        }
                    } else {
                        ServerMessage serverMessage = ServerMessage.findFirst("message_id_snowflake = ?", message.getId().asLong());
                        if (serverMessage != null) {
                            if (serverMessage.getContent().length() > 17) {
                                stringBuilder.append(serverMessage.getMessageSnowflake()).append(" | ").append(serverMessage.getContent(), 0, 17).append("...\n");
                            } else {
                                stringBuilder.append(serverMessage.getMessageSnowflake()).append(" | ").append(serverMessage.getContent()).append("\n");
                            }
                        }
                    }
                }
            } else {
                List<ServerMessage> serverMessages = new ArrayList<>();
                for (Snowflake snowflake : snowflakes) {
                    ServerMessage message = ServerMessage.findFirst("message_id_snowflake = ?", snowflake.asLong());
                    serverMessages.add(message);
                }
                for (ServerMessage serverMessage : serverMessages) {
                    if (serverMessage != null) {
                        if (serverMessage.getContent().length() > 17) {
                            stringBuilder.append(serverMessage.getMessageSnowflake()).append(" | ").append(serverMessage.getContent(), 0, 17).append("...\n");
                        } else {
                            stringBuilder.append(serverMessage.getMessageSnowflake()).append(" | ").append(serverMessage.getContent()).append("\n");
                        }
                    }
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
        DatabaseLoader.closeConnectionIfOpen();
    }

    public static Mono<Void> logMemberJoin(MemberJoinEvent event, TextChannel logChannel) {
        return event.getGuild().flatMap($ -> {
            final Member eventMember = event.getMember();
            long memberId = eventMember.getId().asLong();
            String username = eventMember.getUsername() + '#' + eventMember.getDiscriminator();
            String member = "`%s`:`%d`:%s".formatted(username, memberId, eventMember.getMention());

            String avatarUrl = eventMember.getAvatarUrl();

            String createDate = TimestampMaker.getTimestampFromEpochSecond(
                eventMember.getId().getTimestamp().getEpochSecond(),
                TimestampMaker.TimestampType.LONG_DATETIME
            );

            final EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserJoin() + " User Joined")
                .color(Color.ENDEAVOUR)
                .addField("User", member, false)
                .thumbnail(avatarUrl)
                .addField("Account Created", createDate, false)
                .timestamp(Instant.now());
            getBadges(eventMember).ifPresent(s -> embed.addField("Badges", s, false));

            return logChannel.createMessage(embed.build());
        }).then();
    }

    private static Optional<String> getIconUrl(Guild guild) {
        return guild.getIconUrl(Image.Format.GIF)
            .or(() -> guild.getIconUrl(Image.Format.PNG))
            .or(() -> guild.getIconUrl(Image.Format.JPEG));
    }

    public static Optional<String> getBadges(Member member) {
        return Stream.concat(
            member.getPublicFlags().stream()
                .map(LogExecutor::getEmojiForBadge)
                .flatMap(Optional::stream),
            member.getPremiumTime().map($ -> EmojiManager.getNitroBadge()).stream()
        ).reduce((lhs, rhs) -> lhs + ' ' + rhs);
    }

    private static Optional<String> getEmojiForBadge(final User.Flag badge) {
        return switch (badge) {
            case DISCORD_CERTIFIED_MODERATOR -> Optional.ofNullable(EmojiManager.getModeratorBadge());
            case EARLY_SUPPORTER -> Optional.ofNullable(EmojiManager.getEarlySupporterBadge());
            case BUG_HUNTER_LEVEL_1 -> Optional.ofNullable(EmojiManager.getBugHunter1Badge());
            case BUG_HUNTER_LEVEL_2 -> Optional.ofNullable(EmojiManager.getBugHunter2Badge());
            case DISCORD_EMPLOYEE -> Optional.ofNullable(EmojiManager.getEmployeeBadge());
            case DISCORD_PARTNER -> Optional.ofNullable(EmojiManager.getPartnerBadge());
            case VERIFIED_BOT_DEVELOPER -> Optional.ofNullable(EmojiManager.getDeveloperBadge());
            case HYPESQUAD_EVENTS -> Optional.ofNullable(EmojiManager.getHypeSquad2Badge());
            case HOUSE_BALANCE -> Optional.ofNullable(EmojiManager.getBalanceBadge());
            case HOUSE_BRAVERY -> Optional.ofNullable(EmojiManager.getBraveryBadge());
            case HOUSE_BRILLIANCE -> Optional.ofNullable(EmojiManager.getBrillianceBadge());
            default -> Optional.empty();
        };
    }

    public static Mono<Void> logMemberLeave(MemberLeaveEvent event, TextChannel logChannel) {
        return event.getGuild().flatMap($ -> {
            final User eventUser = event.getUser();
            long memberId = eventUser.getId().asLong();
            String username = eventUser.getUsername() + '#' + eventUser.getDiscriminator();
            String memberName = "`%s`:`%d`:%s".formatted(username, memberId, eventUser.getMention());
            String avatarUrl = eventUser.getAvatarUrl();


            Member member = event.getMember().orElse(null);
            Optional<String> badges;
            Mono<String> rolesMono;
            if (member != null) {
                badges = getBadges(member);
                rolesMono = getRolesString(member);
            } else {
                badges = Optional.empty();
                rolesMono = Mono.just("No roles");
            }

            final EmbedCreateSpec.Builder embedBuilder = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserLeave() + " User Left")
                .addField("User", memberName, false)
                .thumbnail(avatarUrl)
                .timestamp(Instant.now());
            badges.ifPresent(s -> embedBuilder.addField("Badges", s, false));

            return rolesMono.map(roles -> embedBuilder.addField("Roles", roles, false).build())
                .flatMap(logChannel::createMessage);
        }).then();
    }

    public static Mono<String> getRolesString(Member member) {
        return member.getRoles().collectList().map(roles -> {
            if (roles.isEmpty()) {
                return "No roles";
            } else {
                return roles.stream()
                    .map(role -> "`%s`:%s".formatted(role.getName(), role.getMention()))
                    .collect(Collectors.joining(", "));
            }
        });
    }

    public static Mono<Void> logMemberUpdate(MemberUpdateEvent event, TextChannel logChannel) {
        return Mono.zip(event.getGuild(), event.getMember(), ($, member) -> {
            long memberId = member.getId().asLong();
            String username = member.getUsername() + '#' + member.getDiscriminator();
            String memberName = "`%s`:`%d`:%s".formatted(username, memberId, member.getMention());
            String avatarUrl = member.getAvatarUrl();

            return event.getOld()
                .map(oldMember -> getMemberDiff(oldMember, member))
                .orElseGet(() -> getMemberInformation(member))
                .map(memberInfo -> EmbedCreateSpec.builder()
                    .title(EmojiManager.getUserIdentification() + " Member Update")
                    .color(Color.MOON_YELLOW)
                    .description(memberInfo)
                    .addField("Member", memberName, false)
                    .thumbnail(avatarUrl)
                    .timestamp(Instant.now())
                    .build())
                .flatMap(logChannel::createMessage);
        }).flatMap($ -> $).then();
    }

    public static Mono<String> getMemberInformation(Member member) {
        return member.getRoles().collectList().map(roles -> {
            final StringJoiner joiner = new StringJoiner("\n");
            joiner.add("```");

            joiner.add(
                member.getNickname()
                    .map("Nickname: "::concat)
                    .orElse("Username: " + member.getUsername())
            );
            for (final Role role : roles) {
                joiner.add("Role: %s:%d".formatted(role.getName(), role.getId().asLong()));
            }

            joiner.add("```");
            return joiner.toString();
        });
    }

    public static Mono<String> getMemberDiff(Member oldMember, Member newMember) {
        return oldMember.getRoles().collectList().flatMap(oldRoles ->
            newMember.getRoles().collectList().map(newRoles -> {
                final StringJoiner joiner = new StringJoiner("\n");
                joiner.add("```diff");

                final Optional<String> maybeOldNickname = oldMember.getNickname();
                final Optional<String> maybeNewNickname = newMember.getNickname();
                maybeOldNickname.ifPresentOrElse(
                    oldNickname -> maybeNewNickname.ifPresentOrElse(
                        newNickname -> {
                            if (oldNickname.equals(newNickname)) {
                                joiner.add("--- Nickname: " + oldNickname);
                            } else {
                                joiner.add("- Nickname: " + oldNickname);
                                joiner.add("+ Nickname: " + newNickname);
                            }
                        },
                        () -> joiner.add("- Nickname: " + oldNickname)
                    ),
                    () -> maybeNewNickname.ifPresentOrElse(
                        newNickname -> joiner.add("+ Nickname: " + newNickname),
                        () -> joiner.add("- No nickname")
                    )
                );

                final Set<Role> oldRoleSet = new LinkedHashSet<>(oldRoles);
                final Set<Role> newRoleSet = new LinkedHashSet<>(newRoles);
                if (oldRoleSet.equals(newRoleSet)) {
                    joiner.add("--- No role changes");
                } else {
                    oldRoleSet.stream()
                        .filter(not(newRoleSet::contains))
                        .map(role -> "- Role: %s: %d".formatted(role.getName(), role.getId().asLong()))
                        .forEach(joiner::add);
                    newRoleSet.stream()
                        .filter(not(oldRoleSet::contains))
                        .map(role -> "+ Role: %s: %d".formatted(role.getName(), role.getId().asLong()))
                        .forEach(joiner::add);
                }

                joiner.add("```");
                return joiner.toString();
            })
        );
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

    public static Mono<Void> logInviteCreate(InviteCreateEvent event, TextChannel logChannel) {
        return event.getGuild()
            .flatMap(guild -> guild.getChannelById(event.getChannelId()))
            .flatMap(channel -> {
                String inviter = event.getInviter().map(user -> {
                    long inviterId = user.getId().asLong();
                    String username = user.getUsername() + '#' + user.getDiscriminator();
                    return "`%s`:`%d`:%s".formatted(username, inviterId, user.getMention());
                }).orElse("Unknown");
                String channelDescriptor = "`%d`:%s".formatted(channel.getId().asLong(), channel.getMention());
                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getInvite() + " Server Invite Created")
                    .color(Color.ENDEAVOUR)
                    .addField("Inviter", inviter, false)
                    .addField("Invite Code", '`' + event.getCode() + '`', false)
                    .addField("Channel", channelDescriptor, false)
                    .build();
                return logChannel.createMessage(embed);
            }).then();
    }

    public static Mono<Void> logNewsCreate(NewsChannelCreateEvent event, TextChannel logChannel) {
        final NewsChannel channel = event.getChannel();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_CREATE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(newsCreate -> {
                String responsibleUserId = getAuditResponsibleUser(newsCreate);
                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getNewsChannel() + " News Channel Created")
                    .color(Color.SEA_GREEN)
                    .addField("Channel", channelDescriptor, false)
                    .addField("Created By", responsibleUserId, false)
                    .timestamp(Instant.now())
                    .build();

                return logChannel.createMessage(embed);
            }).then();
    }

    public static Mono<Void> logNewsDelete(NewsChannelDeleteEvent event, TextChannel logChannel) {
        final NewsChannel channel = event.getChannel();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_DELETE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(newsDelete -> {
                String responsibleUserId = getAuditResponsibleUser(newsDelete);
                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getNewsChannel() + " News Channel Deleted")
                    .color(Color.JAZZBERRY_JAM)
                    .addField("Channel", channelDescriptor, false)
                    .addField("Deleted By", responsibleUserId, false)
                    .timestamp(Instant.now())
                    .build();

                return logChannel.createMessage(embed);
            }).then();
    }

    public static Mono<Void> logNewsUpdate(NewsChannelUpdateEvent event, TextChannel logChannel) {
        final GuildMessageChannel currentChannel = event.getCurrent();
        return currentChannel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_UPDATE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(newsUpdate -> {
                String responsibleUserId = getAuditResponsibleUser(newsUpdate);

                long channelId = currentChannel.getId().asLong();
                String name = currentChannel.getName();
                String channel = "`%d`:`%s`:%s".formatted(channelId, name, currentChannel.getMention());

                Mono<String> information = event.getOld()
                    .flatMap(oldChannel -> event.getNewsChannel()
                        .map(newsChannel -> getNewsChannelDiff(oldChannel, newsChannel)))
                    .orElse(Mono.empty());

                EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getNewsChannel() + " News Channel Updated")
                    .addField("Channel", channel, false)
                    .addField("Updated By", responsibleUserId, false)
                    .color(Color.ENDEAVOUR)
                    .timestamp(Instant.now())
                    .footer("Check your server's audit log for more information", "");
                return information.doOnNext(embed::description).thenReturn(embed);
            }).map(EmbedCreateSpec.Builder::build)
            .flatMap(logChannel::createMessage)
            .then();
    }

    private static Mono<String> getNewsChannelDiff(NewsChannel oldChannel, NewsChannel newChannel) {
        return getChannelDiff(oldChannel.getName(), newChannel.getName(), oldChannel.getCategory(), newChannel.getCategory());
    }

    public static Mono<Void> logStoreCreate(StoreChannelCreateEvent event, TextChannel logChannel) {
        final StoreChannel channel = event.getChannel();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_CREATE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(storeCreate -> {
                String responsibleUserId = getAuditResponsibleUser(storeCreate);

                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getStoreChannel() + " Store Channel Created")
                    .color(Color.SEA_GREEN)
                    .addField("Channel", channelDescriptor, false)
                    .addField("Created By", responsibleUserId, false)
                    .timestamp(Instant.now())
                    .build();

                return logChannel.createMessage(embed);
            }).then();
    }

    public static Mono<Void> logStoreDelete(StoreChannelDeleteEvent event, TextChannel logChannel) {
        final StoreChannel channel = event.getChannel();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_DELETE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(storeDelete -> {
                String responsibleUserId = getAuditResponsibleUser(storeDelete);

                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getStoreChannel() + " Store Channel Deleted")
                    .color(Color.JAZZBERRY_JAM)
                    .addField("Channel", channelDescriptor, false)
                    .addField("Deleted By", responsibleUserId, false)
                    .timestamp(Instant.now())
                    .build();

                return logChannel.createMessage(embed);
            }).then();
    }

    public static Mono<Void> logStoreUpdate(StoreChannelUpdateEvent event, TextChannel logChannel) {
        final StoreChannel channel = event.getCurrent();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_UPDATE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(storeUpdate -> {
                String responsibleUserId = getAuditResponsibleUser(storeUpdate);

                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                Mono<String> information = event.getOld()
                    .map(oldChannel -> getStoreChannelDiff(oldChannel, channel))
                    .orElse(Mono.empty());

                EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getStoreChannel() + " Store Channel Updated")
                    .addField("Channel", channelDescriptor, false)
                    .addField("Updated By", responsibleUserId, false)
                    .color(Color.ENDEAVOUR)
                    .timestamp(Instant.now())
                    .footer("Check your server's audit log for more information", "");

                return information.doOnNext(embed::description).thenReturn(embed);
            }).map(EmbedCreateSpec.Builder::build)
            .flatMap(logChannel::createMessage)
            .then();
    }

    private static Mono<String> getStoreChannelDiff(StoreChannel oldChannel, StoreChannel newChannel) {
        return getChannelDiff(oldChannel.getName(), newChannel.getName(), oldChannel.getCategory(), newChannel.getCategory());
    }

    public static Mono<Void> logVoiceCreate(VoiceChannelCreateEvent event, TextChannel logChannel) {
        final VoiceChannel channel = event.getChannel();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_CREATE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(voiceCreate -> {
                String responsibleUserId = getAuditResponsibleUser(voiceCreate);

                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getVoiceChannel() + " Voice Channel Created")
                    .color(Color.SEA_GREEN)
                    .addField("Channel", channelDescriptor, false)
                    .addField("Created By", responsibleUserId, false)
                    .timestamp(Instant.now())
                    .build();

                return logChannel.createMessage(embed);
            }).then();
    }

    public static Mono<Void> logVoiceDelete(VoiceChannelDeleteEvent event, TextChannel logChannel) {
        final VoiceChannel channel = event.getChannel();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_DELETE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(voiceDelete -> {
                String responsibleUserId = getAuditResponsibleUser(voiceDelete);

                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getVoiceChannel() + " Voice Channel Deleted")
                    .color(Color.JAZZBERRY_JAM)
                    .addField("Channel", channelDescriptor, false)
                    .addField("Deleted By", responsibleUserId, false)
                    .timestamp(Instant.now())
                    .build();

                return logChannel.createMessage(embed);
            }).then();
    }

    public static Mono<Void> logVoiceUpdate(VoiceChannelUpdateEvent event, TextChannel logChannel) {
        final VoiceChannel channel = event.getCurrent();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_UPDATE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(voiceUpdate -> {
                String responsibleUserId = getAuditResponsibleUser(voiceUpdate);

                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                Mono<String> information = event.getOld()
                    .map(oldChannel -> getVoiceChannelDiff(oldChannel, channel))
                    .orElse(Mono.empty());

                EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getVoiceChannel() + " Voice Channel Updated")
                    .addField("Channel", channelDescriptor, false)
                    .addField("Updated By", responsibleUserId, false)
                    .color(Color.ENDEAVOUR)
                    .timestamp(Instant.now())
                    .footer("Check your server's audit log for more information", "");

                return information.doOnNext(embed::description).thenReturn(embed);
            }).map(EmbedCreateSpec.Builder::build)
            .flatMap(logChannel::createMessage)
            .then();
    }

    private static Mono<String> getVoiceChannelDiff(VoiceChannel oldChannel, VoiceChannel newChannel) {
        return getChannelDiff(oldChannel.getName(), newChannel.getName(), oldChannel.getCategory(), newChannel.getCategory());
    }

    public static Mono<Void> logTextCreate(TextChannelCreateEvent event, TextChannel logChannel) {
        final TextChannel channel = event.getChannel();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_CREATE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(textCreate -> {
                String responsibleUserId = getAuditResponsibleUser(textCreate);

                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getTextChannel() + " Text Channel Created")
                    .color(Color.SEA_GREEN)
                    .addField("Channel", channelDescriptor, false)
                    .addField("Created By", responsibleUserId, false)
                    .timestamp(Instant.now())
                    .build();

                return logChannel.createMessage(embed);
            }).then();
    }

    private static String getAuditResponsibleUser(AuditLogEntry aud) {
        String responsibleUserId;
        if (aud == null || aud.getResponsibleUser().isEmpty()
                || aud.getId().getTimestamp().isBefore(Instant.now().minus(Duration.ofSeconds(15)))) {
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
        if (aud == null || aud.getTargetId().isEmpty() || aud.getId().getTimestamp().isAfter(Instant.now().minus(Duration.ofSeconds(15)))) {
            responsibleUserId = "Unknown";
        } else {
            String id = aud.getTargetId().get().asString();
            responsibleUserId = "`" + id + "`:<@" + id + ">";
        }
        return responsibleUserId;
    }

    public static Mono<Void> logTextDelete(TextChannelDeleteEvent event, TextChannel logChannel) {
        final TextChannel channel = event.getChannel();
        return channel.getGuild()
            .map(Guild::getAuditLog)
            .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_DELETE))
            .flatMapIterable(AuditLogPart::getEntries)
            .filter(entry -> entry.getResponsibleUser().isPresent())
            .next()
            .flatMap(textDelete -> {
                String responsibleUserId = getAuditResponsibleUser(textDelete);

                long channelId = channel.getId().asLong();
                String name = channel.getName();
                String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title(EmojiManager.getTextChannel() + " Text Channel Deleted")
                    .color(Color.JAZZBERRY_JAM)
                    .addField("Channel", channelDescriptor, false)
                    .addField("Deleted By", responsibleUserId, false)
                    .timestamp(Instant.now())
                    .build();

                return logChannel.createMessage(embed);
            }).then();
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
        String name = event.getCurrent().getName();
        String channel = "`" + channelId + "`:`" + name + "`:<#" + channelId + ">";

        String information;
        if (event.getOld().isPresent() && event.getTextChannel().isPresent()) {
            information = getTextChannelDiff(event.getOld().get(), event.getTextChannel().get());
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

    private static Mono<String> getChannelDiff(String oldName, String newName, Mono<Category> oldCatMono, Mono<Category> newCatMono) {
        return oldCatMono.zipWith(newCatMono, (oldCategory, newCategory) -> {
            final StringJoiner joiner = new StringJoiner("\n");
            joiner.add("```diff");
            if (oldName.equals(newName)) {
                joiner.add("--- Name: %s".formatted(oldName));
            } else {
                joiner.add("- Name: %s".formatted(oldName));
                joiner.add("+ Name: %s".formatted(newName));
            }
            if (oldCategory.getName().equals(newCategory.getName())) {
                joiner.add("--- Category: %s".formatted(oldCategory.getName()));
            } else {
                joiner.add("- Category: %s".formatted(oldCategory.getName()));
                joiner.add("+ Category: %s".formatted(newCategory.getName()));
            }
            return joiner.add("```").toString();
        });
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
        if (!userBan.getResponsibleUser().equals(ClientManager.getManager().getClient().getSelf().block()) && !reason.equalsIgnoreCase("Mass API banned by staff.")) {
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
                .addField("Reason", reason, false)
                .timestamp(Instant.now())
                .build();

        if (!caseId.equals("none")) {
            embed = EmbedCreateSpec.builder().from(embed)
                    .addField("Case ID", "#" + caseId, false)
                    .build();
        }

        logChannel.createMessage(embed).block();
        DatabaseLoader.closeConnectionIfOpen();

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
                .title(EmojiManager.getServerRole() + " Role Created")
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
                .title(EmojiManager.getServerRole() + " Role Deleted")
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
                .title(EmojiManager.getServerRole() + "  Role Updated")
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
            stringBuilder.append("--- Colour: ").append(Integer.toHexString(oldRole.getColor().getRGB())).append("\n");
        } else {
            stringBuilder.append("- Colour: ").append(Integer.toHexString(oldRole.getColor().getRGB())).append("\n");
            stringBuilder.append("+ Colour: ").append(Integer.toHexString(newRole.getColor().getRGB())).append("\n");
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
                .footer("For more information, run /case search id " + punishment.getPunishmentId(), "")
                .color(Color.SEA_GREEN)
                .build();

        logChannel.createMessage(embed).block();
        DatabaseLoader.closeConnectionIfOpen();
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
                .footer("For more information, run /case search id " + punishment.getPunishmentId(), "")
                .color(Color.SEA_GREEN)
                .build();

        logChannel.createMessage(embed).block();
        DatabaseLoader.closeConnectionIfOpen();
    }

    public static void logMutedRoleDelete(Long roleId, TextChannel logChannel) {
        String role = "`" + roleId + "`";

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.JAZZBERRY_JAM)
                .title("Muted Role Deleted")
                .description("Oh no! You've deleted the role that this bot uses to mute people! Worry not - next time " +
                        "you try to mute someone, the role will be recreated :sparkles: *automatically* :sparkles:. " +
                        "You could also manually set a role as the muted role, and it will be applied to users who " +
                        "are muted, using `/settings mutedrole set <role>`")
                .addField("Role", role, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logMuteNotApplicable(Member memberToMute, TextChannel logChannel) {

        String memberName = "`" + memberToMute.getUsername() + "`:" + "`" + memberToMute.getId().asLong() + "`:<@" + memberToMute.getId().asLong() + ">";


        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.JAZZBERRY_JAM)
                .title("Muted Role Inapplicable")
                .description("Muted role could not be applied to the following member, likely because the bot has a lower role than them.")
                .addField("Member", memberName, false)
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logStopJoinsEnabled(TextChannel logChannel) {
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.MOON_YELLOW)
                .title(EmojiManager.getUserWarn() + " Anti-Raid Enabled")
                .description("This server has had anti-raid (the stopjoins feature) enabled. To disable it, run `/stopjoins disable`. " +
                        "Until you do, all new members will be kicked with a message to try again later.")
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

    public static void logStopJoinsDisabled(TextChannel logChannel) {
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.MOON_YELLOW)
                .title(EmojiManager.getUserWarn() + " Anti-Raid Disabled")
                .description("This server has had anti-raid (the stopjoins feature) disabled.")
                .timestamp(Instant.now())
                .build();

        logChannel.createMessage(embed).block();
    }

}
