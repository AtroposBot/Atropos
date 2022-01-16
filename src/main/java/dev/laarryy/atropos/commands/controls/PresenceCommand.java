package dev.laarryy.atropos.commands.controls;

import dev.laarryy.atropos.commands.Command;
import dev.laarryy.atropos.config.ConfigManager;
import dev.laarryy.atropos.utils.Notifier;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

import java.util.List;

public class PresenceCommand implements Command {

    List<ApplicationCommandOptionChoiceData> optionChoiceDataList = List.of(
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Online")
                    .value("online")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Do Not Disturb")
                    .value("dnd")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Idle")
                    .value("idle")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Offline")
                    .value("offline")
                    .build());

    List<ApplicationCommandOptionChoiceData> optionChoiceDataList2 = List.of(
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Playing")
                    .value("playing")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Watching")
                    .value("watching")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Competing")
                    .value("competing")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Listening")
                    .value("listening")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Streaming")
                    .value("streaming")
                    .build());


    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("presence")
            .description("Set bot presence")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("type")
                    .description("Presence type")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .choices(optionChoiceDataList)
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("activity")
                    .description("Activity type")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .choices(optionChoiceDataList2)
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("value")
                    .description("Presence value")
                    .type(ApplicationCommandOption.Type.STRING.getValue())
                    .required(true)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {
        if (!event.getInteraction().getUser().getId().equals(Snowflake.of(ConfigManager.getControllerId()))) {
            Notifier.notifyCommandUserOfError(event, "noPermission");
            return Mono.empty();
        }

        GatewayDiscordClient client = event.getClient();

        String statusInput = event.getOption("type").get().getValue().get().asString();
        Status status = switch (statusInput) {
            case "dnd" -> Status.DO_NOT_DISTURB;
            case "idle" -> Status.IDLE;
            case "offline" -> Status.OFFLINE;
            default -> Status.ONLINE;
        };

        String activityInput = event.getOption("activity").get().getValue().get().asString();
        String valueInput = event.getOption("value").get().getValue().get().asString();

        ClientActivity clientActivity = switch (activityInput) {
            case "playing" -> ClientActivity.playing(valueInput);
            case "competing" -> ClientActivity.competing(valueInput);
            case "listening" -> ClientActivity.listening(valueInput);
            case "streaming" -> ClientActivity.streaming(valueInput, "https://twitch.tv/null");
            default -> ClientActivity.watching(valueInput);
        };


        client.updatePresence(ClientPresence.of(status, clientActivity)).block();

        event.reply("Done.").block();
        return Mono.empty();
    }
}
