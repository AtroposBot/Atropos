package dev.laarryy.Icicle.commands.punishments;

import dev.laarryy.Icicle.Icicle;
import dev.laarryy.Icicle.commands.Command;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

public class BanCommand implements Command {
    private final Logger logger = LogManager.getLogger(this);

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("ban")
            .description("Ban a user.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("User to ban.")
                    .type(ApplicationCommandOptionType.USER.getValue())
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("reason")
                    .description("Why?")
                    .type(ApplicationCommandOptionType.STRING.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("duration")
                    .description("How long should this ban be in effect? Defaults to forever. Format: 1mo2w3d13h45m")
                    .type(ApplicationCommandOptionType.STRING.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("days")
                    .description("How many days worth of messages should this ban delete? Defaults to none. Must be 1-7 or uses 0.")
                    .type(ApplicationCommandOptionType.INTEGER.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("dm")
                    .description("Attempt to DM user a notification? Defaults to true.")
                    .type(ApplicationCommandOptionType.BOOLEAN.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        Icicle icicle = new Icicle();
        PunishmentManager punishmentManager = icicle.getPunishmentManager();

        Mono.just(event)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(event1 -> punishmentManager.doPunishment(request, event1));

        return Mono.empty();
    }
}
