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

public class NoteCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PunishmentManager punishmentManager = PunishmentManagerManager.getManager().getPunishmentManager();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("note")
            .description("Add a note to a user. Like adding a record to their file, but not an infraction like a warning is.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("User to add case to.")
                    .type(ApplicationCommandOption.Type.USER.getValue())
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("reason")
                    .description("Why?")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
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
