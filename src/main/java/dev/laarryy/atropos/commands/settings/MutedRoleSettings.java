package dev.laarryy.atropos.commands.settings;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.atropos.exceptions.MalformedInputException;
import dev.laarryy.atropos.exceptions.NotFoundException;
import dev.laarryy.atropos.managers.PropertiesCacheManager;
import dev.laarryy.atropos.models.guilds.DiscordServerProperties;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.AuditLogger;
import dev.laarryy.atropos.utils.Notifier;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.N;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class MutedRoleSettings {
    private final Logger logger = LogManager.getLogger(this);

    private final LoadingCache<Long, DiscordServerProperties> propertiesCache = PropertiesCacheManager.getManager().getPropertiesCache();

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        return event.getInteraction().getGuild().flatMap(guild -> {
            DatabaseLoader.openConnectionIfClosed();
            DiscordServerProperties discordServerProperties = propertiesCache.get(guild.getId().asLong());

            if (event.getOption("mutedrole").get().getOption("set").isPresent()
                    && event.getOption("mutedrole").get().getOption("set").get().getOption("role").isPresent()
                    && event.getOption("mutedrole").get().getOption("set").get().getOption("role").get().getValue().isPresent()
            ) {

               return event.getOption("mutedrole").get().getOption("set").get().getOption("role").get().getValue().get().asRole().flatMap(mutedRole -> {
                   if (mutedRole.isEveryone() || mutedRole.isManaged()) {
                       AuditLogger.addCommandToDB(event, false);
                       DatabaseLoader.closeConnectionIfOpen();
                       return Mono.error(new NotFoundException("404 Not Found"));
                   }

                   Long mutedRoleId = mutedRole.getId().asLong();

                   discordServerProperties.setMutedRoleSnowflake(mutedRoleId);
                   discordServerProperties.save();
                   discordServerProperties.refresh();
                   propertiesCache.invalidate(guild.getId().asLong());
                   EmbedCreateSpec embed = EmbedCreateSpec.builder()
                           .title("Success")
                           .color(Color.SEA_GREEN)
                           .description("Set muted role successfully!")
                           .timestamp(Instant.now())
                           .build();

                   DatabaseLoader.closeConnectionIfOpen();
                   return Notifier.sendResultsEmbed(event, embed);
               });
            }

            if (event.getOption("mutedrole").get().getOption("info").isPresent()) {

                DatabaseLoader.openConnectionIfClosed();
                Long mutedRoleId = discordServerProperties.getMutedRoleSnowflake();

                if (mutedRoleId == null) {
                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .title("Muted Role Info")
                            .color(Color.SEA_GREEN)
                            .description("There is currently no muted role set in this server. Muting a player while no muted role is set will cause the automatic creation of a muted role.")
                            .footer("Run /settings mutedrole set <role> to set a role as the muted role", "")
                            .timestamp(Instant.now())
                            .build();

                    DatabaseLoader.closeConnectionIfOpen();
                    return Notifier.sendResultsEmbed(event, embed);
                }

                return guild.getRoleById(Snowflake.of(mutedRoleId)).flatMap(mutedRole -> {
                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .title("Muted Role Info")
                            .color(Color.SEA_GREEN)
                            .description("The current muted role for this server. Muting a player while no muted role is set will cause the automatic creation of a muted role.")
                            .addField("Current Muted Role", "`" + mutedRole.getName() + "`:<@&" + mutedRoleId + ">:`" + mutedRoleId + "`", false)
                            .footer("Run /settings mutedrole set <role> to set a role as the muted role", "")
                            .timestamp(Instant.now())
                            .build();
                    return Notifier.sendResultsEmbed(event, embed);
                });
            }

            DatabaseLoader.closeConnectionIfOpen();
            return Mono.error(new MalformedInputException("Malformed Input"));
        });
    }
}
