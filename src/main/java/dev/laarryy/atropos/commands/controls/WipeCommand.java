package dev.laarryy.atropos.commands.controls;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.EmojiManager;
import dev.laarryy.atropos.utils.Notifier;
import dev.laarryy.atropos.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static dev.laarryy.atropos.jooq.Tables.SERVERS;
import static dev.laarryy.atropos.jooq.Tables.USERS;
import static dev.laarryy.atropos.storage.DatabaseLoader.sqlContext;

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

        if (event.getOption("guild").isPresent()) {
            return wipeGuild(event);
        }

        if (event.getOption("user").isPresent()) {
            return wipeUser(event);
        }

        return Mono.empty();
    }

    private Mono<Void> wipeGuild(ChatInputInteractionEvent event) {

        return event.getInteraction().getGuild()
                .filterWhen(guild -> permissionChecker.checkIsAdministrator(event.getInteraction().getMember().get()))
                .flatMap(guild ->
                        event.reply("Administrator Server Wipe Activated. Farewell!")
                                .then(Mono.fromDirect(sqlContext.deleteFrom(SERVERS).where(SERVERS.SERVER_ID.eq(guild.getId()))))
                                .then(guild.leave().retry(10))
                );
    }

    private Mono<Void> wipeUser(ChatInputInteractionEvent event) {

        User user = event.getInteraction().getUser();

        return Mono.fromRunnable(() -> logger.info("User with ID " + user.getId().asString() + " has requested the deletion of their data. Complying."))
                .then(Mono.fromDirect(sqlContext.deleteFrom(USERS).where(USERS.USER_ID_SNOWFLAKE.eq(user.getId()))))
                .then(event.getInteraction().getGuild().flatMap(guild -> {
                    EmbedCreateSpec embed = EmbedCreateSpec.builder()
                            .title(EmojiManager.getMessageDelete() + " User Information Cleared")
                            .description("Leave this guild immediately if you do not want further information recorded in any manner. " +
                                    "For information about how Atropos stores and uses data, please refer to [the Privacy Policy](https://atropos.laarryy.dev/privacy-policy/)" +
                                    "and [the Terms of Use](https://atropos.laarryy.dev/terms-of-use/)")
                            .timestamp(Instant.now())
                            .build();
                    return Notifier.sendResultsEmbed(event, embed);
                }));
    }
}
