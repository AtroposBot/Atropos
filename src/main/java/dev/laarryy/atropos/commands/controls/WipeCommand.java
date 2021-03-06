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

import java.time.Instant;

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

        event.deferReply().withEphemeral(true).block();

        if (event.getOption("guild").isPresent()) {
            Mono.just(event).subscribe(this::wipeGuild);
        }

        if (event.getOption("user").isPresent()) {
            Mono.just(event).subscribe(this::wipeUser);
        }

        return Mono.empty();
    }

    private void wipeGuild(ChatInputInteractionEvent event) {
        if (event.getInteraction().getGuild().block() == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
        }

        Guild guild = event.getInteraction().getGuild().block();

        if (event.getInteraction().getMember().isEmpty() || !permissionChecker.checkIsAdministrator(guild, event.getInteraction().getMember().get())) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            return;
        }

        DatabaseLoader.openConnectionIfClosed();

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guild.getId().asLong());

        if (discordServer == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            return;
        }

        event.getInteraction().getChannel().block().createMessage("Administrator Server Wipe Activated. Farewell!").block();

        discordServer.delete();
        DatabaseLoader.closeConnectionIfOpen();

        event.getInteraction().getGuild().block().leave().retry(10).block();
    }

    private void wipeUser(ChatInputInteractionEvent event) {

        User user = event.getInteraction().getUser();

        DatabaseLoader.openConnectionIfClosed();

        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", user.getId().asLong());

        logger.info("User with ID " + user.getId().asLong() + " has requested the deletion of their data. Complying.");

        EmbedCreateSpec embed = EmbedCreateSpec.builder()
                .title(EmojiManager.getMessageDelete() + " User Information Cleared")
                .description("Leave this guild immediately if you do not want further information recorded in any manner. " +
                        "For information about how Atropos stores and uses data, please refer to [the Privacy Policy](https://atropos.laarryy.dev/privacy-policy/)" +
                        "and [the Terms of Use](https://atropos.laarryy.dev/terms-of-use/)")
                .timestamp(Instant.now())
                .build();

        Notifier.replyDeferredInteraction(event, embed);

        discordUser.delete();
        DatabaseLoader.closeConnectionIfOpen();
    }
}
