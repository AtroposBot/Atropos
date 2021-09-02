package dev.laarryy.Icicle.commands;

import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Random;


public class TestCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("test")
            .description("This is a test. Move along dearie.")
            .addOption(ApplicationCommandOptionData.builder()
                .name("random")
                .description("Generate a random number?")
                .type(ApplicationCommandOptionType.BOOLEAN.getValue())
                .required(false)
                .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        Guild guild = event.getInteraction().getGuild().block();
        User user = event.getInteraction().getUser();

        Permission permission = Permission.findOrCreateIt("permission", request.name());
        int permissionId = permission.getInteger("id");

        if (!permissionChecker.checkPermission(guild, user, permissionId)) {
            logger.info("Test permission check conducted, no permission found.");
            return Mono.empty();
        } else {
            logger.info("Test permission check conducted, permission granted!");
        }

        logger.info("Test command (slash) executed");

        if (event.getOption("random").isPresent()
                && event.getOption("random").get().getValue().isPresent()
                && event.getOption("random").get().getValue().get().asBoolean()) {

            event.reply("Random number request understood. Your number is " + new Random().nextInt(25))
                    .retry(3)
                    .subscribe();

            return Mono.empty();
        } else {
            event.reply("Hi. Your test was successful! Congratulation.").subscribe();
        }
        return Mono.empty();
    }

}
