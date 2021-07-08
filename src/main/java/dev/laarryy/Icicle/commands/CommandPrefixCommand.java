package dev.laarryy.Icicle.commands;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class CommandPrefixCommand implements Command{

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("commandprefix")
            .description("Set server's command prefix")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("prefix")
                    .description("String that will be the command prefix")
                    .type(ApplicationCommandOptionType.STRING.getValue())
                    .required(true)
                    .build()
            )
            .defaultPermission(false)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {
        return null;
    }

    public Mono<Void> execute(MessageCreateEvent event) {
        return null;
    }


}
