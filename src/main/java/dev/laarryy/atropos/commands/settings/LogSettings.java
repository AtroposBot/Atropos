package dev.laarryy.atropos.commands.settings;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AddServerToDB;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.Notifier;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class LogSettings {
    private final Logger logger = LogManager.getLogger(this);
    private AddServerToDB addServerToDB = new AddServerToDB();

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        if (event.getOption("log").get().getOption("info").isPresent()) {
            logSettingsInfo(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("log").get().getOption("set").isPresent()) {
            setLogChannel(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("log").get().getOption("unset").isPresent()) {
            unsetLogChannel(event);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        AuditLogger.addCommandToDB(event, false);
        Notifier.notifyCommandUserOfError(event, "malformedInput");
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }

    private void unsetLogChannel(ChatInputInteractionEvent event) {
        if (event.getOption("log").get().getOption("unset").get().getOption("type").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
        }

        Guild guild = event.getInteraction().getGuild().block();
        DatabaseLoader.openConnectionIfClosed();

        PropertiesCacheManager.getManager().getPropertiesCache().invalidate(guild.getId().asLong());

        String logType = event.getOption("log").get().getOption("unset").get().getOption("type").get().getValue().get().asString();

        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        MessageChannel channel = event.getInteraction().getChannel().block();

        if (!(channel instanceof TextChannel textChannel)) {
            Notifier.notifyCommandUserOfError(event, "invalidChannel");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        if (serverProperties == null) {
            addServerToDB.addServerToDatabase(guild);
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        switch (logType) {
            case "message" -> serverProperties.setMessageLogChannelSnowflake(null);
            case "member" -> serverProperties.setMemberLogChannelSnowflake(null);
            case "guild" -> serverProperties.setGuildLogChannelSnowflake(null);
            case "punishment" -> serverProperties.setPunishmentLogChannelSnowflake(null);
            case "all" -> {
                serverProperties.setMessageLogChannelSnowflake(null);
                serverProperties.setMemberLogChannelSnowflake(null);
                serverProperties.setGuildLogChannelSnowflake(null);
                serverProperties.setPunishmentLogChannelSnowflake(null);
            }
            default -> {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                AuditLogger.addCommandToDB(event, false);
                return;
            }
        }

        event.deferReply().block();

        serverProperties.save();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Unset Log Channel Successfully")
                .color(Color.ENDEAVOUR)
                .description("Unset this channel as a logging channel of type: `" + logType + "`. Run `/settings log info` " +
                        "to learn more and `/settings log set " + logType + "` in this channel to undo.")
                .timestamp(Instant.now())
                .build();

        Notifier.replyDeferredInteraction(event, embed);
        AuditLogger.addCommandToDB(event, true);
    }

    private void setLogChannel(ChatInputInteractionEvent event) {
        if (event.getOption("log").get().getOption("set").get().getOption("type").isEmpty()) {
            Notifier.notifyCommandUserOfError(event, "malformedInput");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        DatabaseLoader.openConnectionIfClosed();

        String logType = event.getOption("log").get().getOption("set").get().getOption("type").get().getValue().get().asString();

        PropertiesCacheManager.getManager().getPropertiesCache().invalidate(guild.getId().asLong());

        DiscordServerProperties serverProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        if (serverProperties == null) {
            addServerToDB.addServerToDatabase(guild);
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        MessageChannel channel = event.getInteraction().getChannel().block();

        if (!(channel instanceof TextChannel textChannel)) {
            Notifier.notifyCommandUserOfError(event, "invalidChannel");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        Long targetChannelId = textChannel.getId().asLong();

        switch (logType) {
            case "message" -> serverProperties.setMessageLogChannelSnowflake(targetChannelId);
            case "member" -> serverProperties.setMemberLogChannelSnowflake(targetChannelId);
            case "guild" -> serverProperties.setGuildLogChannelSnowflake(targetChannelId);
            case "punishment" -> serverProperties.setPunishmentLogChannelSnowflake(targetChannelId);
            case "all" -> {
                serverProperties.setMessageLogChannelSnowflake(targetChannelId);
                serverProperties.setMemberLogChannelSnowflake(targetChannelId);
                serverProperties.setGuildLogChannelSnowflake(targetChannelId);
                serverProperties.setPunishmentLogChannelSnowflake(targetChannelId);
            }
            default -> {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                AuditLogger.addCommandToDB(event, false);
                return;
            }
        }
        serverProperties.save();

        event.deferReply().block();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Set Log Channel Successfully")
                .color(Color.ENDEAVOUR)
                .description("Set this channel to be a logging channel of type: `" + logType + "`. Run `/settings log info` " +
                        "to learn more and `/settings log unset " + logType + "` in this channel to undo.")
                .timestamp(Instant.now())
                .build();

        Notifier.replyDeferredInteraction(event, embed);
        AuditLogger.addCommandToDB(event, true);

    }

    private void logSettingsInfo(ChatInputInteractionEvent event) {
        Guild guild = event.getInteraction().getGuild().block();

        event.deferReply().block();

        DatabaseLoader.openConnectionIfClosed();
        LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();
        StringBuilder sb = new StringBuilder();
        sb.append("**Current Log Settings:**\n");
        if (cache.get(guild.getId().asLong()).getMemberLogChannelSnowflake() != null) {
            sb.append("Member Log: `").append(cache.get(guild.getId().asLong()).getMemberLogChannelSnowflake()).append("`\n");
        }
        if (cache.get(guild.getId().asLong()).getMessageLogChannelSnowflake() != null) {
            sb.append("Message Log: `").append(cache.get(guild.getId().asLong()).getMessageLogChannelSnowflake()).append("`\n");
        }
        if (cache.get(guild.getId().asLong()).getGuildLogChannelSnowflake() != null) {
            sb.append("Guild Log: `").append(cache.get(guild.getId().asLong()).getGuildLogChannelSnowflake()).append("`\n");
        }
        if (cache.get(guild.getId().asLong()).getPunishmentLogChannelSnowflake() != null) {
            sb.append("Punishment Log: `").append(cache.get(guild.getId().asLong()).getPunishmentLogChannelSnowflake()).append("`\n");
        }
        String settings = sb.toString();

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title("Log Settings")
                .color(Color.ENDEAVOUR)
                .description("There are four log types that you can configure: `Member`, `Message`, `Guild`, and `Punishment`. " +
                        "Each type can be logged to whichever channel you choose; there is no need to keep them separate nor together. " +
                        "Below are the four types explained along with what they keep track of.")
                .addField("Member Log",
                        "This type logs updates on members of the server. Presence and status updates as well as nickname changes.",
                        false)
                .addField("Message Log",
                        "This type logs message updates: deletes and edits.",
                        false)
                .addField("Guild Log",
                        "This type logs guild-sided updates. Role changes, joins, leaves, bans, and invite creations. " +
                                "*Note: this logs only what discord's API provides for bans. For detailed information, use punishment logs.*",
                        false)
                .addField("Punishment Log",
                        "This type logs punishments through this bot. Bans, warns, mutes, kicks, and cases. " +
                                "It will also log unmutes and unbans through the bot, in addition to cases where a " +
                                "user attempts insubordination (punishing someone above them).",
                        false)
                .addField("Command Use", "Run `/settings log set <type>` in the channel you'd like to set as a logging channel. " +
                        "Run `/settings log unset <type>` in the channel you'd like to unset as a logging channel.",
                        false)
                .addField("\u200B", settings, false)
                .build();

        Notifier.replyDeferredInteraction(event, embed);

    }
}
