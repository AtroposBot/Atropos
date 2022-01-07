package dev.laarryy.eris.commands.settings;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.eris.managers.PropertiesCacheManager;
import dev.laarryy.eris.models.guilds.DiscordServerProperties;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.AuditLogger;
import dev.laarryy.eris.utils.Notifier;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class MutedRoleSettings {
    private final Logger logger = LogManager.getLogger(this);

    private final LoadingCache<Long, DiscordServerProperties> propertiesCache = PropertiesCacheManager.getManager().getPropertiesCache();

    public Mono<Void> execute(ChatInputInteractionEvent event) {
        Guild guild = event.getInteraction().getGuild().block();

        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties discordServerProperties = DiscordServerProperties.findFirst("server_id_snowflake = ?", guild.getId().asLong());

        if (event.getOption("mutedrole").get().getOption("set").isPresent()
                && event.getOption("mutedrole").get().getOption("set").get().getOption("role").isPresent()
                && event.getOption("mutedrole").get().getOption("set").get().getOption("role").get().getValue().isPresent()
        ) {
            Role mutedRole;
            try {
                mutedRole = event.getOption("mutedrole").get().getOption("set").get().getOption("role").get().getValue().get().asRole().block();
            } catch (Exception e) {
                Notifier.notifyCommandUserOfError(event, "404");
                AuditLogger.addCommandToDB(event, false);
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.empty();
            }

            if (mutedRole.isEveryone() || mutedRole.isManaged()) {
                Notifier.notifyCommandUserOfError(event, "404");
                AuditLogger.addCommandToDB(event, false);
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.empty();
            }

            Long mutedRoleId = mutedRole.getId().asLong();

            discordServerProperties.setModMailChannelSnowflake(mutedRoleId);
            discordServerProperties.save();
            propertiesCache.invalidate(guild.getId().asLong());
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Success")
                    .color(Color.SEA_GREEN)
                    .description("Set muted role successfully!")
                    .timestamp(Instant.now())
                    .build();
            event.reply().withEmbeds(embed).subscribe();
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("mutedrole").get().getOption("info").isPresent()) {
            DatabaseLoader.openConnectionIfClosed();
            Long mutedRoleId = discordServerProperties.getMutedRoleSnowflake();
            Role mutedRole = guild.getRoleById(Snowflake.of(mutedRoleId)).block();

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Muted Role Info")
                    .color(Color.SEA_GREEN)
                    .description("The current muted role for this server. Muting a player while no muted role is set will cause the automatic creation of a muted role.")
                    .addField("Current Muted Role", "`" + mutedRole.getName() + "`:<@&" + mutedRoleId + ">:`" + mutedRoleId + "`", false)
                    .footer("Run /settings mutedrole set <role> to set a role as the muted role", "")
                    .timestamp(Instant.now())
                    .build();
            event.reply().withEmbeds(embed).subscribe();
        }

        Notifier.notifyCommandUserOfError(event, "malformedInput");
        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }

}
