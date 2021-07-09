package dev.laarryy.Icicle.commands;

import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class CommandInfo {

    private final String commandName;
    private final String commandDescription;
    private final Possible<List<ApplicationCommandOptionData>> commandOptions;
    private final boolean commandDefaultPermission;
    private final LinkedList<String> args;
    private final MessageCreateEvent event;

    public CommandInfo(MessageCreateEvent event, ApplicationCommandRequest request) {
        LinkedList<String> splitMessage = new LinkedList<>(Arrays.asList(event.getMessage().getContent().split(" ")));
        splitMessage.removeFirst();

        this.commandName = request.name();
        this.commandDescription = request.description();
        this.commandOptions = request.options();
        this.commandDefaultPermission = request.defaultPermission().get();
        this.args = splitMessage;
        this.event = event;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getCommandDescription() {
        return commandDescription;
    }

    public Possible<List<ApplicationCommandOptionData>> getCommandOptions() {
        return commandOptions;
    }

    public boolean isCommandDefaultPermission() {
        return commandDefaultPermission;
    }

    public LinkedList<String> getArgs() {
        return args;
    }

    public MessageCreateEvent getEvent() {
        return event;
    }
}
