package dev.laarryy.Eris.commands;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public interface Command {

    Mono<Void> execute(SlashCommandEvent event);

    ApplicationCommandRequest getRequest();

}
