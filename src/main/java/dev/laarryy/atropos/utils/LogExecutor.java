package dev.laarryy.atropos.utils;

import dev.laarryy.atropos.config.EmojiManager;
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
import discord4j.discordjson.json.AuditLogEntryData;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public final class LogExecutor {

    private static final Logger logger = LogManager.getLogger(LogExecutor.class);

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
                    .flatMap(AuditLogger::generateOptionString)
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

    private static Mono<String> getResponsibleUserMono(Guild guild, ActionType actionType) {
        return guild.getAuditLog().withActionType(actionType)
                .flatMapIterable(LogExecutor::getValidAuditEntries)
                .collectList()
                .flatMap(entryList -> {
                    if (entryList.isEmpty()) {
                        return Mono.just("Unknown");
                    }
                    Optional<User> responsibleUser = entryList.get(0).getResponsibleUser();

                    if (responsibleUser.isEmpty()) {
                        return Mono.just("Unknown");
                    } else {
                        User theResponsibleUser = responsibleUser.get();
                        long id = theResponsibleUser.getId().asLong();
                        String username = theResponsibleUser.getUsername() + '#' + theResponsibleUser;
                        return Mono.just("`%s`:`%d`:%s".formatted(username, id, theResponsibleUser.getMention()));
                    }
                });
    }

    private static Mono<String> getAuditReasonMono(Guild guild, ActionType actionType) {
        return guild.getAuditLog().withActionType(actionType)
                .flatMapIterable(LogExecutor::getValidAuditEntries)
                .collectList()
                .flatMap(entryList -> {
                    if (entryList.isEmpty()) {
                        return Mono.empty();
                    }
                    Optional<String> reason = entryList.get(0).getReason();

                    if (reason.isEmpty()) {
                        return Mono.empty();
                    } else {
                        String theReason = reason.get();
                        return Mono.just(theReason);
                    }
                });
    }

    private static List<AuditLogEntry> getValidAuditEntries(AuditLogPart auditLogPart) {
        List<AuditLogEntry> entries = auditLogPart.getEntries();

        List<AuditLogEntry> validEntries = new ArrayList<>();

        for (AuditLogEntry entry : entries) {
            if (entry.getResponsibleUser().isPresent()
                    && entry.getTargetId().isPresent()
                    && entry.getId().getTimestamp().isAfter(Instant.now().minus(Duration.ofSeconds(15)))) {
                validEntries.add(entry);
            }
        }

        return validEntries;
    }

    private static Mono<String> getAuditReason(AuditLogEntry entry) {
        if (entry.getReason().isPresent()) {
            return Mono.just(entry.getReason().get());
        }
        return Mono.empty();
    }

    public static Mono<Void> logMessageDelete(MessageDeleteEvent event, TextChannel logChannel) {
        logger.info("Logging Message Delete");
        return event.getGuild().flatMap(guild -> {
            final Optional<Message> message = event.getMessage();

            return event.getChannel().flatMap(channel -> {

                Mono<String> responsibleUserMono = getResponsibleUserMono(guild, ActionType.MESSAGE_DELETE);

                Mono<String> senderDescriptorMono = message.flatMap(Message::getAuthor).map(author -> {
                    long id = author.getId().asLong();
                    String username = author.getUsername() + '#' + author.getDiscriminator();
                    return Mono.just("`%s`:`%d`:%s".formatted(username, id, author.getMention()));
                }).orElseGet(() -> {
                    DatabaseLoader.openConnectionIfClosed();
                    ServerMessage serverMessage = ServerMessage.findFirst("server_id_snowflake = ? and message_id_snowflake = ?", guild.getId().asLong(), event.getMessageId().asLong());
                    DatabaseLoader.closeConnectionIfOpen();
                    if (serverMessage != null) {
                        long id = serverMessage.getUserSnowflake();
                        return guild.getMemberById(Snowflake.of(id)).map(author -> {
                            String username = author.getUsername() + '#' + author.getDiscriminator();
                            return "`%s`:`%d`:%s".formatted(username, id, author.getMention());
                        });
                    } else {
                        return Mono.just("Unknown");
                    }
                });

                Optional<String> content;
                if (message.isPresent()) {
                    if (message.get().getContent().isEmpty()) {
                        content = Optional.empty();
                    } else {
                        String string = message.get().getContent();
                        content = Optional.of(getStringWithLegalLength(string, 4055));
                    }
                } else {
                    DatabaseLoader.openConnectionIfClosed();
                    ServerMessage serverMessage = ServerMessage.findFirst("server_id_snowflake = ? and message_id_snowflake = ?", guild.getId().asLong(), event.getMessageId().asLong());
                    DatabaseLoader.closeConnectionIfOpen();
                    if (serverMessage != null) {
                        content = Optional.of(getStringWithLegalLength(serverMessage.getContent(), 3055));
                    } else {
                        content = Optional.of("Unknown");
                    }
                }

                Optional<String> embeds = message
                        .map(Message::getEmbeds)
                        .filter(embeds1 -> !embeds1.isEmpty())
                        .map(LogExecutor::makeEmbedsEntries)
                        .map(embedEntries -> getStringWithLegalLength(embedEntries, 1024));

                Optional<String> attachmentURLs = message
                        .stream()
                        .map(Message::getAttachments)
                        .flatMap(List::stream)
                        .map(Attachment::getUrl)
                        .reduce("%s%n%s"::formatted);

                logger.info("Zipping!");

                return Mono.zip(responsibleUserMono, senderDescriptorMono, (responsibleUserDescriptor, senderDescriptor) -> {
                    String channelDescriptor = "`%d`:%s".formatted(channel.getId().asLong(), channel.getMention());

                    EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                            .color(Color.JAZZBERRY_JAM)
                            .title(EmojiManager.getMessageDelete() + " Message Deleted")
                            .addField("Sent By", senderDescriptor, false)
                            .addField("Channel", channelDescriptor, false)
                            .addField("Deleted By", responsibleUserDescriptor, false)
                            .timestamp(Instant.now());

                    content.ifPresent(s -> embed.description("**Content:**%n%s".formatted(s)));
                    attachmentURLs.ifPresent(s -> embed.addField("Attachments", s, false));
                    embeds.ifPresent(s -> embed.addField("Embeds", s, false));

                    logger.info("Creating Message in Channel: " + logChannel.getId().asString());
                    return logChannel.createMessage(embed.build());
                }).flatMap($ -> $);
            });
        }).then();
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
        final StringJoiner joiner = new StringJoiner("\n");

        for (Embed embed : embedList) {
            joiner.add("```");
            embed.getTitle().ifPresent(title -> joiner.add("Title: %s".formatted(title.replace("```", "\\`"))));
            embed.getDescription().ifPresent(description -> {
                joiner.add("Description:");
                joiner.add(description.replace("`", ""));
            });
            embed.getColor().ifPresent(color -> joiner.add("Colour: %x".formatted(color.getRGB())));
            embed.getImage().ifPresent(image -> joiner.add("Image URL: %s".formatted(image.getUrl())));
            embed.getThumbnail().ifPresent(thumbnail -> joiner.add("Thumbnail URL: %s".formatted(thumbnail.getUrl())));
            embed.getVideo().ifPresent(video -> joiner.add("Video URL: %s".formatted(video.getUrl())));
            embed.getAuthor().ifPresent(author -> {
                author.getName().ifPresent(name -> joiner.add("Author Name: %s".formatted(name)));
                author.getUrl().ifPresent(url -> joiner.add("Author URL: %s".formatted(url)));
                author.getIconUrl().ifPresent(iconUrl -> joiner.add("Author Icon URL: %s".formatted(iconUrl)));
            });

            List<Embed.Field> fields = embed.getFields();
            for (int i = 0, fieldsSize = fields.size(); i < fieldsSize; ++i) {
                final Embed.Field field = fields.get(i);
                joiner.add("Field: %d".formatted(i + 1));
                joiner.add("->Name: %s".formatted(field.getName().replace("```", "\\`")));
                joiner.add("->Content: %s".formatted(field.getValue().replace("```", "\\`")));
                if (field.isInline()) {
                    joiner.add("->Inline: Yes");
                } else {
                    joiner.add("->Inline: No");
                }
            }

            embed.getFooter().ifPresent(footer -> {
                joiner.add("Footer: %s".formatted(footer.getText()));
                footer.getIconUrl().ifPresent(iconUrl -> joiner.add("Footer URL: %s".formatted(iconUrl)));
            });

            embed.getTimestamp()
                    .map(Instant::toEpochMilli)
                    .ifPresent(timestamp -> joiner.add("Epoch Timestamp: %d".formatted(timestamp)));

            joiner.add("```");
        }

        return joiner.toString();
    }

    public static Mono<Void> logMessageUpdate(MessageUpdateEvent event, TextChannel logChannel) {
        logger.info("MESSAGE UPDATE");
        return Mono.zip(event.getGuild(), event.getMessage(), (guild, message) -> {
            String title = EmojiManager.getMessageEdit() + " Message Edit";
            String oldContent;
            String newContent;
            logger.info("Message Update");
            if (!event.isContentChanged()) {
                logger.info("Content not changed");
                if (!event.isEmbedsChanged()) {
                    logger.info("Embeds not changed");
                    oldContent = "Unknown";
                    newContent = getStringWithLegalLength(message.getContent(), 1000);
                } else {
                    logger.info("Embeds changed");
                    oldContent = event.getOld()
                            .map(Message::getEmbeds)
                            .filter(not(List::isEmpty))
                            .map(LogExecutor::makeEmbedsEntries)
                            .orElse("Unknown embed(s)");

                    final List<Embed> currentEmbeds = event.getCurrentEmbeds();
                    newContent = makeEmbedsEntries(currentEmbeds.isEmpty() ? message.getEmbeds() : currentEmbeds);
                }
            } else {
                logger.info("Content changed");
                oldContent = event.getOld()
                        .map(oldMessage -> getStringWithLegalLength(oldMessage.getContent(), 1000))
                        .orElseGet(() -> {
                            DatabaseLoader.openConnectionIfClosed();
                            ServerMessage serverMessage =
                                    ServerMessage.findFirst(
                                            "message_id_snowflake = ? and server_id_snowflake = ?",
                                            event.getMessageId().asLong(), guild.getId().asLong()
                                    );
                            DatabaseLoader.closeConnectionIfOpen();
                            if (serverMessage != null) {
                                return getStringWithLegalLength(serverMessage.getContent(), 1000);
                            } else {
                                return "Unknown";
                            }
                        });

                newContent = getStringWithLegalLength(event.getCurrentContent().orElse(message.getContent()), 1000);
            }

            String userDescriptor = message.getAuthor()
                    .or(() -> event.getOld().flatMap(Message::getAuthor))
                    .map(author -> "`%s`:`%d`:%s".formatted(author.getUsername(), author.getId().asLong(), author.getMention()))
                    .orElse("Unknown");

            logger.info("Returning message to channel");

            return event.getChannel().flatMap(channel -> {

                String channelDescriptor = "`%d`:%s".formatted(event.getChannelId().asLong(), channel.getMention());

                logger.info("Making embed");

                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .title(title)
                        .color(Color.MOON_YELLOW)
                        .addField("ID", '`' + message.getId().asString() + '`', false)
                        .addField("User", userDescriptor, false)
                        .addField("Channel", channelDescriptor, false)
                        .addField("Old", oldContent, false)
                        .addField("New", newContent, false)
                        .timestamp(Instant.now())
                        .build();

                logger.info("Creating message");

                return logChannel.createMessage(embed);
            }).then();
        }).flatMap(mono -> mono);
    }

    public static Mono<Void> logBulkDelete(MessageBulkDeleteEvent event, TextChannel logChannel) {
        return event.getGuild().flatMap(guild -> {
                    Mono<String> responsibleUserMono = getResponsibleUserMono(guild, ActionType.MESSAGE_BULK_DELETE);
                    Mono<String> reasonMono = getAuditReasonMono(guild, ActionType.MESSAGE_BULK_DELETE);

                    final Set<Message> messageSet = event.getMessages();
                    final Set<Snowflake> snowflakes = event.getMessageIds();
                    String messages;
                    DatabaseLoader.openConnectionIfClosed();
                    if (messageSet.isEmpty() && snowflakes.isEmpty()) {
                        messages = "Unknown";
                    } else {
                        final StringJoiner joiner = new StringJoiner("\n");
                        joiner.add("```");
                        if (!messageSet.isEmpty()) {
                            for (Message message : messageSet) {
                                if (!message.getContent().isEmpty()) {
                                    if (message.getContent().length() > 17) {
                                        joiner.add(message.getId().asLong() + " | " + message.getContent().substring(0, 17) + "...");
                                    } else {
                                        joiner.add(message.getId().asLong() + " | " + message.getContent());
                                    }
                                } else {
                                    ServerMessage serverMessage = ServerMessage.findFirst("message_id_snowflake = ?", message.getId().asLong());
                                    if (serverMessage != null) {
                                        if (serverMessage.getContent().length() > 17) {
                                            joiner.add(serverMessage.getMessageSnowflake() + " | " + serverMessage.getContent().substring(0, 17) + "...");
                                        } else {
                                            joiner.add(serverMessage.getMessageSnowflake() + " | " + serverMessage.getContent());
                                        }
                                    }
                                }
                            }
                        } else {
                            DatabaseLoader.openConnectionIfClosed();
                            List<ServerMessage> serverMessages = snowflakes.parallelStream()
                                    .map(Snowflake::asLong)
                                    .map(snowflake -> ServerMessage.<ServerMessage>findFirst("message_id_snowflake = ?", snowflake))
                                    .filter(Objects::nonNull)
                                    .toList();
                            DatabaseLoader.closeConnectionIfOpen();
                            for (ServerMessage serverMessage : serverMessages) {
                                if (serverMessage.getContent().length() > 17) {
                                    joiner.add(serverMessage.getMessageSnowflake() + " | " + serverMessage.getContent().substring(0, 17) + "...");
                                } else {
                                    joiner.add(serverMessage.getMessageSnowflake() + " | " + serverMessage.getContent());
                                }
                            }
                        }
                        joiner.add("```");
                        messages = joiner.toString();
                    }

                    if (messages.length() >= 4000) {
                        messages = messages.substring(0, 3950) + "...```\n[Content too large, has been limited]";
                    }

                    final String accumulatedMessages = messages;
                    return Mono.zip(event.getChannel(), responsibleUserMono, (channel, responsibleUserDescriptor) -> {
                        String channelDescriptor = "`%d`:%s".formatted(channel.getId().asLong(), channel.getMention());

                        EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                                .title((EmojiManager.getMessageDelete() + ' ').repeat(3) + "Bulk Delete")
                                .color(Color.JAZZBERRY_JAM)
                                .description(accumulatedMessages)
                                .addField("Responsible User", responsibleUserDescriptor, false)
                                .addField("Channel", channelDescriptor, false)
                                .timestamp(Instant.now());

                        return reasonMono.switchIfEmpty(logChannel.createMessage(embed.build()).thenReturn(" "))
                                .flatMap(reason -> {
                                    embed.addField("Reason", reason, false);
                                    return logChannel.createMessage(embed.build());
                        });
                    }).flatMap(mono -> mono);
                }).then();
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

    public static Mono<Void> logPresenceUpdate(PresenceUpdateEvent event, TextChannel logChannel) {
        return event.getOldUser().map(oldUser -> event.getGuild()
                        .flatMap($ -> event.getMember())
                        .flatMap(member -> {
                            long memberId = member.getId().asLong();
                            String username = member.getUsername() + '#' + member.getDiscriminator();
                            String memberName = "`%s`:`%d`:%s".formatted(username, memberId, member.getMention());

                            String presenceDiffInfo;
                            if (
                                    oldUser.getDiscriminator().equals(member.getDiscriminator())
                                            && oldUser.getUsername().equals(member.getUsername())
                                            && oldUser.getAvatarUrl().equals(member.getAvatarUrl())
                            ) {
                                return Mono.empty();
                            } else {
                                presenceDiffInfo = getPresenceDiff(oldUser, member);
                            }

                            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                                    .title(EmojiManager.getUserIdentification() + " Member Update")
                                    .color(Color.MOON_YELLOW)
                                    .description(presenceDiffInfo)
                                    .addField("Member", memberName, false)
                                    .thumbnail(member.getAvatarUrl())
                                    .timestamp(Instant.now())
                                    .build();

                            return logChannel.createMessage(embed);
                        }).then())
                .orElse(Mono.empty());
    }

    public static String getPresenceDiff(User oldUser, User newUser) {
        final StringJoiner joiner = new StringJoiner("\n");

        joiner.add("```diff");
        if (oldUser.getUsername().equals(newUser.getUsername())) {
            joiner.add("--- Username: %s".formatted(oldUser.getUsername()));
        } else {
            joiner.add("- Username: %s".formatted(oldUser.getUsername()));
            joiner.add("+ Username: %s".formatted(newUser.getUsername()));
        }

        if (oldUser.getDiscriminator().equals(newUser.getDiscriminator())) {
            joiner.add("--- Discriminator: #%s".formatted(oldUser.getDiscriminator()));
        } else {
            joiner.add("- Discriminator: #%s".formatted(oldUser.getDiscriminator()));
            joiner.add("+ Discriminator: #%s".formatted(newUser.getDiscriminator()));
        }

        if (oldUser.getAvatarUrl().equals(newUser.getAvatarUrl())) {
            joiner.add("--- Avatar URL: %s".formatted(oldUser.getAvatarUrl()));
        } else {
            joiner.add("- Avatar URL: %s".formatted(oldUser.getAvatarUrl()));
            joiner.add("+ Avatar URL: %s".formatted(newUser.getAvatarUrl()));
        }

        return joiner.add("```").toString();
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
        if (aud == null || aud.getTargetId().isEmpty() || aud.getId().getTimestamp().isBefore(Instant.now().minus(Duration.ofSeconds(15)))) {
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

    public static Mono<Void> logTextUpdate(TextChannelUpdateEvent event, TextChannel logChannel) {
        final GuildMessageChannel channel = event.getCurrent();
        return channel.getGuild()
                .map(Guild::getAuditLog)
                .flatMapMany(auditLog -> auditLog.withActionType(ActionType.CHANNEL_UPDATE))
                .flatMapIterable(AuditLogPart::getEntries)
                .filter(entry -> entry.getResponsibleUser().isPresent())
                .next()
                .flatMap(textUpdate -> {
                    String responsibleUserId = getAuditResponsibleUser(textUpdate);

                    long channelId = channel.getId().asLong();
                    String name = channel.getName();
                    String channelDescriptor = "`%d`:`%s`:%s".formatted(channelId, name, channel.getMention());

                    Mono<String> information = event.getOld()
                            .flatMap(oldChannel -> event.getTextChannel()
                                    .map(newChannel -> getTextChannelDiff(oldChannel, newChannel)))
                            .orElse(Mono.empty());

                    EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                            .title(EmojiManager.getTextChannel() + " Text Channel Updated")
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

    private static Mono<String> getTextChannelDiff(TextChannel oldChannel, TextChannel newChannel) {
        return getChannelDiff(oldChannel.getName(), newChannel.getName(), oldChannel.getCategory(), newChannel.getCategory());
    }

    private static Mono<String> getChannelDiff(String oldName, String newName, Mono<Category> oldCatMono, Mono<Category> newCatMono) {
        return Mono.zip(oldCatMono, newCatMono, (oldCategory, newCategory) -> {
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

    public static Mono<Void> logBan(BanEvent event, TextChannel logChannel) {
        return event.getGuild().flatMap(guild -> guild.getAuditLog().withActionType(ActionType.MEMBER_BAN_ADD)
                .flatMapIterable(AuditLogPart::getEntries)
                .filter(entry -> entry.getResponsibleUser().isPresent())
                .filter(entry -> entry.getTargetId().isPresent())
                .next()
                .zipWith(event.getClient().getSelf())
                .flatMap(tuple -> {
                    final AuditLogEntry userBan = tuple.getT1();
                    final User self = tuple.getT2();

                    String responsibleUserId = getAuditResponsibleUser(userBan);
                    long targetUserId = event.getUser().getId().asLong();
                    String reason = userBan.getReason().orElse("No reason provided.");

                    Optional<String> caseId = userBan.getResponsibleUser()
                            .filter(not(self::equals))
                            .filter($ -> !reason.equalsIgnoreCase("Mass API banned by staff."))
                            .map(responsibleUser -> {
                                DatabaseLoader.openConnectionIfClosed();
                                DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());
                                DiscordUser punisher = DiscordUser.findFirst("user_id_snowflake = ?", responsibleUser.getId().asLong());
                                DiscordUser punished = DiscordUser.findFirst("user_id_snowflake = ?", userBan.getTargetId().get().asLong());
                                Punishment punishment = Punishment.create(
                                        "user_id_punished", punished.getUserId(),
                                        "user_id_punisher", punisher.getUserId(),
                                        "server_id", discordServer.getServerId(),
                                        "punishment_type", "ban",
                                        "punishment_date", Instant.now().toEpochMilli(),
                                        "punishment_message", reason
                                );
                                punishment.save();
                                punishment.refresh();
                                DatabaseLoader.closeConnectionIfOpen();
                                return String.valueOf(punishment.getPunishmentId());
                            });

                    EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                            .title(EmojiManager.getUserBan() + " User Banned")
                            .color(Color.JAZZBERRY_JAM)
                            .addField("Punished User", String.valueOf(targetUserId), false)
                            .addField("Punishing User", responsibleUserId, false)
                            .addField("Reason", reason, false)
                            .timestamp(Instant.now());

                    caseId.ifPresent(s -> embed.addField("Case ID", '#' + s, false));
                    return logChannel.createMessage(embed.build());
                }).then()
        );
    }

    public static Mono<Void> logUnban(UnbanEvent event, TextChannel logChannel) {
        return event.getGuild()
                .map(Guild::getAuditLog)
                .flatMapMany(auditLog -> auditLog.withActionType(ActionType.MEMBER_BAN_REMOVE))
                .flatMapIterable(AuditLogPart::getEntries)
                .filter(entry -> entry.getResponsibleUser().isPresent())
                .next()
                .flatMap(userUnban -> {
                    String responsibleUserId = getAuditResponsibleUser(userUnban);

                    final User user = event.getUser();
                    long userId = user.getId().asLong();
                    String username = user.getUsername() + '#' + user.getDiscriminator();
                    String userDescriptor = "`%s`:`%d`:%s".formatted(username, userId, user.getMention());

                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .title(EmojiManager.getUserBan() + " User Unbanned")
                            .color(Color.SEA_GREEN)
                            .addField("User", userDescriptor, false)
                            .addField("Unbanned By", responsibleUserId, false)
                            .timestamp(Instant.now())
                            .build();

                    return logChannel.createMessage(embed);
                }).then();
    }

    public static Mono<Void> logRoleCreate(RoleCreateEvent event, TextChannel logChannel) {
        return event.getGuild()
                .map(Guild::getAuditLog)
                .flatMapMany(auditLog -> auditLog.withActionType(ActionType.ROLE_CREATE))
                .flatMapIterable(AuditLogPart::getEntries)
                .filter(entry -> entry.getResponsibleUser().isPresent())
                .next()
                .flatMap(roleCreate -> {
                    String responsibleUserId = getAuditResponsibleUser(roleCreate);

                    final Role role = event.getRole();
                    long roleId = role.getId().asLong();
                    String roleName = role.getName();
                    String roleDescriptor = "`%s`:`%d`:%s".formatted(roleName, roleId, role.getMention());
                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .title(EmojiManager.getServerRole() + " Role Created")
                            .color(Color.ENDEAVOUR)
                            .addField("User", responsibleUserId, false)
                            .addField("Role", roleDescriptor, false)
                            .timestamp(Instant.now())
                            .build();

                    return logChannel.createMessage(embed);
                }).then();
    }

    public static Mono<Void> logRoleDelete(RoleDeleteEvent event, TextChannel logChannel) {
        return event.getGuild()
                .map(Guild::getAuditLog)
                .flatMapMany(auditLog -> auditLog.withActionType(ActionType.ROLE_DELETE))
                .flatMapIterable(AuditLogPart::getEntries)
                .filter(entry -> entry.getResponsibleUser().isPresent())
                .next()
                .flatMap(roleDelete -> {
                    String responsibleUserId = getAuditResponsibleUser(roleDelete);
                    String roleDescriptor = event.getRole().map(role -> {
                        long roleId = role.getId().asLong();
                        String roleName = role.getName();
                        return "`%s`:`%d`:%s".formatted(roleName, roleId, role.getMention());
                    }).orElse("Unknown");
                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .title(EmojiManager.getServerRole() + " Role Deleted")
                            .color(Color.JAZZBERRY_JAM)
                            .addField("User", responsibleUserId, false)
                            .addField("Role", roleDescriptor, false)
                            .timestamp(Instant.now())
                            .build();

                    return logChannel.createMessage(embed);
                }).then();
    }

    public static Mono<Void> logRoleUpdate(RoleUpdateEvent event, TextChannel logChannel) {
        final Role role = event.getCurrent();
        return role.getGuild()
                .map(Guild::getAuditLog)
                .flatMapMany(auditLog -> auditLog.withActionType(ActionType.ROLE_UPDATE))
                .flatMapIterable(AuditLogPart::getEntries)
                .filter(entry -> entry.getResponsibleUser().isPresent())
                .next()
                .flatMap(roleUpdate -> {
                    String responsibleUser = getAuditResponsibleUser(roleUpdate);
                    Mono<String> roleInfo = Mono.justOrEmpty(event.getOld()).flatMap(oldRole -> getRoleDiff(oldRole, role));
                    long roleId = role.getId().asLong();
                    String roleDescriptor = "`%s`:`%d`:%s".formatted(role.getName(), roleId, role.getMention());

                    EmbedCreateSpec.Builder embed = EmbedCreateSpec.builder()
                            .title(EmojiManager.getServerRole() + "  Role Updated")
                            .color(Color.ENDEAVOUR)
                            .addField("Responsible User", responsibleUser, false)
                            .addField("Role", roleDescriptor, false)
                            .footer("Check your server's audit log for more information", "")
                            .timestamp(Instant.now());

                    return roleInfo.doOnNext(embed::description).thenReturn(embed);
                }).map(EmbedCreateSpec.Builder::build)
                .flatMap(logChannel::createMessage)
                .then();
    }

    public static Mono<String> getRoleDiff(Role oldRole, Role newRole) {
        return Mono.zip(oldRole.getPosition(), newRole.getPosition(), (oldPosition, newPosition) -> {
            final StringJoiner joiner = new StringJoiner("\n");

            joiner.add("```diff\n");
            if (oldRole.getName().equals(newRole.getName())) {
                joiner.add("--- Name: %s".formatted(oldRole.getName()));
            } else {
                joiner.add("- Name: %s".formatted(oldRole.getName()));
                joiner.add("+ Name: %s".formatted(newRole.getName()));
            }

            if (oldPosition.equals(newPosition)) {
                joiner.add("--- Position: %s".formatted(oldPosition));
            } else {
                joiner.add("- Position: %s".formatted(oldPosition));
                joiner.add("+ Position: %s".formatted(newPosition));
            }

            if (oldRole.getColor().equals(newRole.getColor())) {
                joiner.add("--- Colour: %s".formatted(Integer.toHexString(oldRole.getColor().getRGB())));
            } else {
                joiner.add("- Colour: %s".formatted(Integer.toHexString(oldRole.getColor().getRGB())));
                joiner.add("+ Colour: %s".formatted(Integer.toHexString(newRole.getColor().getRGB())));
            }

            if (oldRole.isMentionable() == newRole.isMentionable()) {
                if (oldRole.isMentionable()) {
                    joiner.add("--- Mentionable: Yes");
                } else {
                    joiner.add("--- Mentionable: No");
                }
            } else {
                if (oldRole.isMentionable()) {
                    joiner.add("- Mentionable: Yes");
                    joiner.add("+ Mentionable: No");
                } else {
                    joiner.add("- Mentionable: No");
                    joiner.add("+ Mentionable: Yes");
                }
            }

            if (oldRole.isHoisted() == newRole.isHoisted()) {
                if (oldRole.isHoisted()) {
                    joiner.add("--- Hoisted: Yes");
                } else {
                    joiner.add("--- Hoisted: No");
                }
            } else {
                if (oldRole.isHoisted()) {
                    joiner.add("- Hoisted: Yes");
                    joiner.add("+ Hoisted: No");
                } else {
                    joiner.add("- Hoisted: No");
                    joiner.add("+ Hoisted: Yes");

                }
            }

            return joiner.add("```").toString();
        });
    }

    public static Mono<Void> logPunishmentUnban(TextChannel logChannel, String reason, Punishment punishment) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser punished = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        DatabaseLoader.closeConnectionIfOpen();
        long punishedId = punished.getUserIdSnowflake();
        String punishedName = "`%d`:<@%1$d>".formatted(punishedId);
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserBan() + " User Unbanned")
                .addField("User", punishedName, false)
                .addField("Reason", getStringWithLegalLength(reason, 1024), false)
                .footer("For more information, run /case search id " + punishment.getPunishmentId(), "")
                .color(Color.SEA_GREEN)
                .build();

        return logChannel.createMessage(embed).then();
    }

    public static Mono<Void> logPunishmentUnmute(TextChannel logChannel, String reason, Punishment punishment) {
        DatabaseLoader.openConnectionIfClosed();
        DiscordUser punished = DiscordUser.findFirst("id = ?", punishment.getPunishedUserId());
        DatabaseLoader.closeConnectionIfOpen();
        long punishedId = punished.getUserIdSnowflake();
        String punishedName = "`%d`:<@%1$d>".formatted(punishedId);
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getUserMute() + " User Unmuted")
                .addField("User", punishedName, false)
                .addField("Reason", getStringWithLegalLength(reason, 1024), false)
                .footer("For more information, run /case search id " + punishment.getPunishmentId(), "")
                .color(Color.SEA_GREEN)
                .build();

        return logChannel.createMessage(embed).then();
    }

    public static Mono<Void> logMutedRoleDelete(Long roleId, TextChannel logChannel) {
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

        return logChannel.createMessage(embed).then();
    }

    public static Mono<Void> logMuteNotApplicable(Member memberToMute, TextChannel logChannel) {
        String memberName = "`%s`:`%d`:%s".formatted(memberToMute.getUsername(), memberToMute.getId().asLong(), memberToMute.getMention());

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.JAZZBERRY_JAM)
                .title("Muted Role Inapplicable")
                .description("Muted role could not be applied to the following member, likely because the bot has a lower role than them.")
                .addField("Member", memberName, false)
                .timestamp(Instant.now())
                .build();

        return logChannel.createMessage(embed).then();
    }

    public static Mono<Void> logStopJoinsEnabled(TextChannel logChannel) {
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.MOON_YELLOW)
                .title(EmojiManager.getUserWarn() + " Anti-Raid Enabled")
                .description("This server has had anti-raid (the stopjoins feature) enabled. To disable it, run `/stopjoins disable`. " +
                        "Until you do, all new members will be kicked with a message to try again later.")
                .timestamp(Instant.now())
                .build();

        return logChannel.createMessage(embed).then();
    }

    public static Mono<Void> logStopJoinsDisabled(TextChannel logChannel) {
        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .color(Color.MOON_YELLOW)
                .title(EmojiManager.getUserWarn() + " Anti-Raid Disabled")
                .description("This server has had anti-raid (the stopjoins feature) disabled.")
                .timestamp(Instant.now())
                .build();

        return logChannel.createMessage(embed).then();
    }
}
