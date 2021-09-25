package dev.laarryy.Eris.commands.raid;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Eris.commands.Command;
import dev.laarryy.Eris.listeners.logging.LoggingListener;
import dev.laarryy.Eris.managers.LoggingListenerManager;
import dev.laarryy.Eris.managers.PropertiesCacheManager;
import dev.laarryy.Eris.models.guilds.DiscordServerProperties;
import dev.laarryy.Eris.storage.DatabaseLoader;
import dev.laarryy.Eris.utils.AuditLogger;
import dev.laarryy.Eris.utils.PermissionChecker;
import dev.laarryy.Eris.utils.SlashCommandChecks;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class StopJoinsCommand implements Command {

    LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    PermissionChecker permissionChecker = new PermissionChecker();
    private final Logger logger = LogManager.getLogger(this);


    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("stopjoins")
            .description("Anti-Raid: Prevents any user from joining this guild.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("enable")
                    .description("Enables this function")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("disable")
                    .description("Disables this function")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {
        if (!SlashCommandChecks.slashCommandChecks(event, request.name())) {
            return Mono.empty();
        }
        Guild guild = event.getInteraction().getGuild().block();

        DatabaseLoader.openConnectionIfClosed();
        DiscordServerProperties serverProperties = cache.get(guild.getId().asLong());

        if (event.getOption("enable").isPresent()) {

            if (serverProperties.getStopJoins()) {
                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .title("Already Enabled")
                        .description("Already kicking all new joins")
                        .color(Color.RUBY)
                        .timestamp(Instant.now())
                        .build();

                event.reply().withEmbeds(embed).subscribe();
                return Mono.empty();
            }
            serverProperties.setStopJoins(true);
            serverProperties.save();
            serverProperties.refresh();
            cache.invalidate(guild.getId().asLong());
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Success")
                    .description("Enabled the prevention of all joins.")
                    .color(Color.ENDEAVOUR)
                    .timestamp(Instant.now())
                    .build();

            AuditLogger.addCommandToDB(event, true);
            event.reply().withEmbeds(embed).subscribe();
            return Mono.empty();
        }

        if (event.getOption("disable").isPresent()) {
            if (!serverProperties.getStopJoins()) {
                EmbedCreateSpec embed = EmbedCreateSpec.builder()
                        .title("Already Disabled")
                        .description("Already not kicking all new joins.")
                        .color(Color.RUBY)
                        .timestamp(Instant.now())
                        .build();

                event.reply().withEmbeds(embed).subscribe();
                return Mono.empty();
            }
            serverProperties.setStopJoins(false);
            serverProperties.save();
            serverProperties.refresh();
            cache.invalidate(guild.getId().asLong());
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Success")
                    .description("Disabled the prevention of all joins.")
                    .color(Color.ENDEAVOUR)
                    .timestamp(Instant.now())
                    .build();

            AuditLogger.addCommandToDB(event, true);
            event.reply().withEmbeds(embed).subscribe();
            return Mono.empty();
        }

        return Mono.empty();
    }
}
