package dev.laarryy.atropos.commands.punishments;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.managers.PunishmentManagerManager;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

public class BanCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("ban")
            .description("Ban a user.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("User to ban.")
                    .type(ApplicationCommandOption.Type.USER.getValue())
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("reason")
                    .description("Why?")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("duration")
                    .description("How long should this ban be in effect? Defaults to forever. Format: 1mo2w3d13h45m")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("days")
                    .description("How many days worth of messages should this ban delete? Defaults to none. Must be 1-7 or uses 0.")
                    .type(ApplicationCommandOption.Type.INTEGER.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("dm")
                    .description("Attempt to DM user a notification? Defaults to true.")
                    .type(ApplicationCommandOption.Type.BOOLEAN.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {

        return punishmentManager.doPunishment(request, event);

    }
}
