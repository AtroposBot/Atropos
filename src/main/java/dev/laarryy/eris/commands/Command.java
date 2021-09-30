package dev.laarryy.eris.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public interface Command {

    Mono<Void> execute(ChatInputInteractionEvent event);

    ApplicationCommandRequest getRequest();

}
