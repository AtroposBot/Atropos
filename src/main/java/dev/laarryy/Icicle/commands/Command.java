package dev.laarryy.Icicle.commands;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import reactor.core.publisher.Mono;

public interface Command {

    Mono<Void> execute(SlashCommandEvent event);

    Mono<Void> execute(MessageCreateEvent event);

    ApplicationCommandRequest getRequest();

}
