package dev.laarryy.atropos.commands.controls;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.models.guilds.DiscordServer;
import dev.laarryy.atropos.models.users.DiscordUser;
import dev.laarryy.atropos.storage.DatabaseLoader;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.Objects;

public class WipeCommand implements Command {

    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final Logger logger = LogManager.getLogger(this);


    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("wipe")
            .description("Request immediate removal of currently stored data")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("guild")
                    .description("Deletes all stored data about this guild and then Atropos leaves")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("Deletes all stored personal data for the person using this command")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        Mono<Void> deferMono = event.deferReply().withEphemeral(true);

        if (event.getOption("guild").isPresent()) {
            return Mono.just(event).flatMap(event1 -> Mono.from(deferMono).flatMap(v -> this.wipeGuild(event1)));
        }

        if (event.getOption("user").isPresent()) {
            return Mono.just(event).flatMap(event1 -> Mono.from(deferMono).flatMap(v -> this.wipeUser(event1)));
        }

        return Mono.empty();
    }

    private Mono<Void> wipeGuild(ChatInputInteractionEvent event) {

        return Mono.from(event.getInteraction().getGuild())
                .doFirst(DatabaseLoader::openConnectionIfClosed)
                .filter(Objects::nonNull)
                .flatMap(guild -> Mono.from(event.getInteraction().getChannel()).flatMap(messageChannel -> {
                    if (event.getInteraction().getMember().isEmpty() || !permissionChecker.checkIsAdministrator(guild, event.getInteraction().getMember().get())) {
                        Notifier.notifyCommandUserOfError(event, "noPermission");
                        return Mono.empty();
                    }

                    DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

                    if (discordServer == null) {
                        Notifier.notifyCommandUserOfError(event, "nullServer");
                        return Mono.empty();
                    }

                    return Mono.from(messageChannel.createMessage("Administrator Server Wipe Activated. Farewell!").flatMap(message -> {
                        discordServer.delete();
                        DatabaseLoader.closeConnectionIfOpen();
                        return Mono.from(guild.leave().retry(10));
                    }));
                }));
    }

    private Mono<Void> wipeUser(ChatInputInteractionEvent event) {

        User user = event.getInteraction().getUser();

        return Mono.from(event.getInteraction().getGuild())
                .doFirst(DatabaseLoader::openConnectionIfClosed)
                .filter(Objects::nonNull)
                .flatMap(guild -> {
                    DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", user.getId().asLong());

                    logger.info("User with ID " + user.getId().asLong() + " has requested the deletion of their data. Complying.");

                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .title(EmojiManager.getMessageDelete() + " User Information Cleared")
                            .description("Leave this guild immediately if you do not want further information recorded in any manner. " +
                                    "For information about how Atropos stores and uses data, please refer to [the Privacy Policy](https://atropos.laarryy.dev/privacy-policy/)" +
                                    "and [the Terms of Use](https://atropos.laarryy.dev/terms-of-use/)")
                            .timestamp(Instant.now())
                            .build();

                    discordUser.delete();
                    return Notifier.replyDeferredInteraction(event, embed);
                })
                .doFinally(s -> DatabaseLoader.closeConnectionIfOpen());
    }
}
