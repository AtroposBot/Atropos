package dev.laarryy.Icicle.commands;

import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;

import java.util.List;

public class CommandDataConverter {

    private final String name;
    private final String description;
    private final Possible<Boolean> defaultPermission;
    private final Possible<List<ApplicationCommandOptionData>> optionData;


    CommandDataConverter(ApplicationCommandRequest request) {
        this.name = request.name();
        this.description = request.description();
        this.defaultPermission = request.defaultPermission();
        this.optionData = request.options();
    }

    public CommandData getCommandData() {
        CommandData commandData = new CommandData() {
            @Override
            public String name() {
                return this.name();
            }

            @Override
            public String description() {
                return this.description();
            }

            @Override
            public Possible<List<ApplicationCommandOptionData>> options() {
                if (!this.options().get().isEmpty()) {
                    return this.options();
                } else return null;
            }

            @Override
            public Possible<Boolean> defaultPermission() {
                return this.defaultPermission();
            }
        };
        return commandData;
    }

    private class OptionDataConverter {

        private final List<ApplicationCommandOptionData> commandOptionDataList;


    }

}
