package dev.laarryy.Icicle.users;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface IPersonUser {
    User getUser();
    MessageChannel getChannel();
    MessageReceivedEvent getEvent();
    boolean hasPermission(String permission);
}
