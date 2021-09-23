package dev.laarryy.Icicle.commands.raid;

import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.laarryy.Icicle.commands.Command;
import dev.laarryy.Icicle.listeners.EventListener;
import dev.laarryy.Icicle.listeners.logging.LoggingListener;
import dev.laarryy.Icicle.managers.LoggingListenerManager;
import dev.laarryy.Icicle.managers.PropertiesCacheManager;
import dev.laarryy.Icicle.models.guilds.DiscordServerProperties;
import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.AuditLogger;
import dev.laarryy.Icicle.utils.Notifier;
import dev.laarryy.Icicle.utils.PermissionChecker;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

public class StopJoinsCommand implements Command {

    LoadingCache<Long, DiscordServerProperties> cache = PropertiesCacheManager.getManager().getPropertiesCache();
    LoggingListener loggingListener = LoggingListenerManager.getManager().getLoggingListener();
    PermissionChecker permissionChecker = new PermissionChecker();

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

    @EventListener
    public Mono<Void> on(MemberJoinEvent event) {
        Guild guild = event.getGuild().block();
        if (guild == null) {
            return Mono.empty();
        }

        DatabaseLoader.openConnectionIfClosed();

        DiscordServerProperties discordServerProperties = cache.get(guild.getId().asLong());

        if (discordServerProperties.getStopJoins()) {
            event.getMember().kick("Automatically kicked as part of anti-raid measures.");
        }
        return Mono.empty();
    }

    public Mono<Void> execute(SlashCommandEvent event) {
        Guild guild = event.getInteraction().getGuild().block();
        if (guild == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return Mono.empty();
        }

        Permission permission = Permission.findOrCreateIt("permission", request.name());
        permission.save();
        permission.refresh();
        int permissionId = permission.getInteger("id");

        if (!permissionChecker.checkPermission(guild, event.getInteraction().getUser(), permissionId)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            return Mono.empty();
        }

        DiscordServerProperties serverProperties = cache.get(guild.getId().asLong());

        if (event.getOption("enable").isPresent()) {
            serverProperties.setStopJoins(true);
            serverProperties.save();
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Success")
                    .description("Enabled the prevention of all joins.")
                    .color(Color.ENDEAVOUR)
                    .build();

            AuditLogger.addCommandToDB(event, true);
            event.reply().withEmbeds(embed).subscribe();
            return Mono.empty();
        }

        if (event.getOption("disable").isPresent()) {
            serverProperties.setStopJoins(false);
            serverProperties.save();
            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Success")
                    .description("Disabled the prevention of all joins.")
                    .color(Color.ENDEAVOUR)
                    .build();

            AuditLogger.addCommandToDB(event, true);
            event.reply().withEmbeds(embed).subscribe();
            return Mono.empty();
        }

        return Mono.empty();
    }
}
