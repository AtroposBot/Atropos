package dev.laarryy.eris.managers;

import dev.laarryy.eris.config.ConfigManager;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.shard.ShardingStrategy;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;

import java.time.Duration;

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
                            ClientActivity.watching("/modmail")))
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
