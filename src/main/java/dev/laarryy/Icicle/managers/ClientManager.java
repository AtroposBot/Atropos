package dev.laarryy.Icicle.managers;

import dev.laarryy.Icicle.config.ConfigManager;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.shard.ShardingStrategy;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class ClientManager {
    private static ClientManager instance;
    private final GatewayDiscordClient gateway;

    public ClientManager(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    public static ClientManager getManager() {
        if (instance == null) {
            instance = new ClientManager(
                    DiscordClientBuilder.create(ConfigManager.getToken())
                    .setDefaultAllowedMentions(AllowedMentions.suppressEveryone())
                    .build()
                    .gateway()
                    .setEnabledIntents(IntentSet.all())
                    .setSharding(ShardingStrategy.recommended())
                    .setInitialPresence(shardInfo -> ClientPresence.of(Status.DO_NOT_DISTURB,
                            ClientActivity.playing("Shard " + (shardInfo.getIndex() + 1) + " of " + shardInfo.getCount()
                                    + " | DM for ModMail | Last Activated "
                                    + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(Locale.CANADA).withZone(ZoneId.systemDefault()).format(Instant.now())))
                    )
                    .login()
                    .block(Duration.ofSeconds(30))
            );
        }
        return instance;
    }

    public GatewayDiscordClient getClient() {
        return gateway;
    }
}