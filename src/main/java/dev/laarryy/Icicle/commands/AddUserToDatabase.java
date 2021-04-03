package dev.laarryy.Icicle.commands;

import dev.laarryy.Icicle.models.User;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class AddUserToDatabase extends ListenerAdapter {
    private Logger logger = LogManager.getLogger(this);

    public AddUserToDatabase() {
    }

    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (event.getMessage().getContentRaw().equals("!adduser")) {

            DatabaseLoader.openConnectionIfClosed();

            User user = User.findOrCreateIt("id", event.getAuthor().getIdLong());

            if (!user.exists()) { user.insert(); }

            user.set("creation_time", event.getAuthor().getTimeCreated().toInstant().toEpochMilli());

            user.saveIt();

            DatabaseLoader.closeConnectionifOpen();
        }
    }
}
