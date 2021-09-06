package dev.laarryy.Icicle.commands.search;

import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.utils.AuditLogger;
import dev.laarryy.Icicle.commands.Command;
import dev.laarryy.Icicle.utils.Notifier;
import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

public class InfCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("inf")
            .description("Search and manage infractions.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("search")
                    .description("Search infractions.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("id")
                            .description("Search by user ID.")
                            .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("snowflake")
                                    .description("User ID to search.")
                                    .type(ApplicationCommandOptionType.INTEGER.getValue())
                                    .required(true)
                                    .build())
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("mention")
                            .description("Search by user mention.")
                            .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                            .required(false)
                            .addOption(ApplicationCommandOptionData.builder()
                                    .name("user")
                                    .description("User mention to search.")
                                    .type(ApplicationCommandOptionType.USER.getValue())
                                    .required(true)
                                    .build())
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("update")
                    .description("Update reason for infraction.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("id")
                            .description("ID of the infraction you want to modify.")
                            .type(ApplicationCommandOptionType.INTEGER.getValue())
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("reason")
                            .description("New reason for infraction.")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .required(true)
                            .build())
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        if (event.getOption("update").isPresent()) {
            Mono.just(event).subscribeOn(Schedulers.boundedElastic()).subscribe(this::updatePunishment);
        }

        return Mono.empty();
    }

    private void updatePunishment(SlashCommandEvent event) {

        if (event.getInteraction().getGuild().block() == null) {
            Notifier.notifyCommandUserOfError(event, "nullServer");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        DatabaseLoader.openConnectionIfClosed();

        int permissionID = Permission.findOrCreateIt("permission", "infupdate").getInteger("id");
        if (!permissionChecker.checkPermission(event.getInteraction().getGuild().block(), event.getInteraction().getUser(), permissionID)) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            AuditLogger.addCommandToDB(event, false);
            return;
        }

        if (event.getOption("update").get().getOption("id").isPresent()
                && event.getOption("update").get().getOption("id").get().getValue().isPresent()) {
            long caseIdLong = event.getOption("update").get().getOption("id").get().getValue().get().asLong();
            int caseId = (int) caseIdLong;

            Punishment punishment = Punishment.findFirst("id = ?", caseId);

            String newReason;
            if (event.getOption("update").get().getOption("reason").isPresent()
                    && event.getOption("update").get().getOption("reason").get().getValue().isPresent()) {
                newReason = event.getOption("update").get().getOption("reason").get().getValue().get().asString();
            } else {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                AuditLogger.addCommandToDB(event, false);
                return;
            }

            if (punishment == null) {
                Notifier.notifyCommandUserOfError(event, "404");
                AuditLogger.addCommandToDB(event, false);
                return;
            }

            EmbedCreateSpec spec = EmbedCreateSpec.builder()
                    .title("Punishment Updated")
                    .color(Color.ENDEAVOUR)
                    .description("Punishment successfully updated with new reason: `" + newReason + "`")
                    .footer("Case ID: " + punishment.getPunishmentId(), "")
                    .timestamp(Instant.now())
                    .build();


            punishment.setPunishmentMessage(newReason);
            punishment.save();
            AuditLogger.addCommandToDB(event, true);
            event.reply().withEmbeds(spec).subscribe();
        }

    }
}
