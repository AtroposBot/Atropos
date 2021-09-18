package dev.laarryy.Icicle;

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

    public GatewayDiscordClient createClient(String token) {
        GatewayDiscordClient client1 = DiscordClientBuilder.create(token)
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
                .block(Duration.ofSeconds(30));
        return client1;
    }
}
