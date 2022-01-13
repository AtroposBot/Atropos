package dev.laarryy.eris.commands.controls;

import dev.laarryy.eris.commands.Command;
import dev.laarryy.eris.models.guilds.DiscordServer;
import dev.laarryy.eris.models.users.DiscordUser;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.Notifier;
import dev.laarryy.eris.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class WipeCommand implements Command {

    private final PermissionChecker permissionChecker = new PermissionChecker();
    private final Logger logger = LogManager.getLogger(this);


    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("wipe")
            .description("Request immediate removal of currently stored data")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("guild")
                    .description("Deletes all stored data about this guild and then Eris leaves")
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
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::wipeGuild);
        }

        if (event.getOption("user").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::wipeUser);
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

        event.getInteraction().getChannel().block().createMessage("Administrator Server Wipe Activated. Farewell!");

        discordServer.delete();
        discordServer.save();
        DatabaseLoader.closeConnectionIfOpen();

        event.getInteraction().getGuild().block().leave().retry(10).block();
    }

    private void wipeUser(ChatInputInteractionEvent event) {

        User user = event.getInteraction().getUser();

        DatabaseLoader.openConnectionIfClosed();

        DiscordUser discordUser = DiscordUser.findFirst("user_id_snowflake = ?", user.getId().asLong());

        logger.info("User with ID " + user.getId().asLong() + " has requested the deletion of their data. Complying.");

        discordUser.delete();
        discordUser.save();
        DatabaseLoader.closeConnectionIfOpen();
    }
}
