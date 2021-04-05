package dev.laarryy.Icicle.commands;

import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.utils.IcicleMemberHandler;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class AddUserToDatabase extends ListenerAdapter {
    private final Logger logger = LogManager.getLogger(this);
    private final IcicleMemberHandler handler = new IcicleMemberHandler();

    public AddUserToDatabase() {
    }

    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (event.getMessage().getContentRaw().equals("!adduser")) {

            if (event.getMember() == null) {
                logger.error("Null Member");
                return;
            }

            DiscordUser discordUser = DiscordUser.findOrCreateIt(handler.getDiscordUser(event.getMember()));

            DatabaseLoader.openConnectionIfClosed();

            if (!discordUser.exists()) { discordUser.insert(); }

            discordUser.set("date", event.getAuthor().getTimeCreated().toInstant().toEpochMilli());

            discordUser.saveIt();

            logger.debug(discordUser.getDateEntry());
            logger.debug(discordUser.getUserId());
            logger.debug(discordUser.getUserIdSnowflake());

            DatabaseLoader.closeConnectionifOpen();
        }
    }
}
