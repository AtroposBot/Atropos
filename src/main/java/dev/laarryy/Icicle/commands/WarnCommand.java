package dev.laarryy.Icicle.commands;

import dev.laarryy.Icicle.Icicle;
import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.permissions.Permission;
import dev.laarryy.Icicle.models.users.DiscordUser;
import dev.laarryy.Icicle.models.users.Punishment;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Color;
import discord4j.rest.util.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class WarnCommand implements Command {

    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("warn")
            .description("Warn a user.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("user")
                    .description("User to warn.")
                    .type(ApplicationCommandOptionType.USER.getValue())
                    .required(true)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("reason")
                    .description("Warning to give.")
                    .type(ApplicationCommandOptionType.STRING.getValue())
                    .required(false)
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("dm")
                    .description("Attempt to DM user with warning? Defaults to false.")
                    .type(ApplicationCommandOptionType.BOOLEAN.getValue())
                    .required(false)
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        if (event.getInteraction().getGuild().block() == null || event.getInteraction().getMember().isEmpty()) {
            event.reply("This must be done in a guild.").withEphemeral(true).subscribe();
            return Mono.empty();
        }

        Guild guild = event.getInteraction().getGuild().block();


        Member member = event.getInteraction().getMember().get();
        User user = event.getInteraction().getUser();
        Long userIdSnowflake = member.getId().asLong();

        DatabaseLoader.openConnectionIfClosed();

        Permission permission = Permission.findOrCreateIt("permission", request.name());
        permission.save();
        permission.refresh();
        int permissionId = permission.getInteger("id");

        if (!permissionChecker.checkPermission(guild, user, permissionId)) {
            event.reply("No permission.").withEphemeral(true).subscribe();
            return Mono.empty();
        }

        Long guildIdSnowflake = guild.getId().asLong();
        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildIdSnowflake);
        int serverId;

        if (discordServer != null) {
            serverId = discordServer.getServerId();
        } else {
            serverId = 0;
            return Mono.empty();
        }

        if (event.getOption("user").isEmpty()) {
            return Mono.empty();
        }

        User warnedUser = event.getOption("user").get().getValue().get().asUser().block();
        DiscordUser punisher = DiscordUser.findFirst("user_id_snowflake = ?", userIdSnowflake);
        DiscordUser punished = DiscordUser.findFirst("user_id_snowflake = ?", warnedUser.getId().asLong());

        Punishment punishment = Punishment.createIt(
                "user_id_punished", punished.getUserId(),
                "user_id_punisher", punisher.getUserId(),
                "server_id", serverId,
                "punishment_type", request.name(),
                "punishment_date", Instant.now().toEpochMilli(),
                "punishment_end_date", 0
        );
        punishment.save();
        punishment.refresh();

        if (event.getOption("reason").isPresent()) {
            String punishmentMessage = event.getOption("reason").get().getValue().get().asString();
            punishment.refresh();
            punishment.setPunishmentMessage(punishmentMessage);
            punishment.save();
        }

        if (event.getOption("dm").isPresent() && event.getOption("dm").get().getValue().get().asBoolean()) {
            punishment.setDMed(true);
            punishment.save();
            punishment.refresh();

            String punishmentMessage;
            if (punishment.getPunishmentMessage() != null) {
                punishmentMessage = punishment.getPunishmentMessage();
            } else {
                punishmentMessage = "No reason provided.";
            }

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Warning from Guild: " + guild.getName())
                    .thumbnail(guild.getIconUrl(Image.Format.PNG).orElse(guild.getSelfMember().block().getAvatarUrl()))
                    .description(warnedUser.getMention() + ", you have been warned by the staff of " + guild.getName() + ". This incident will be recorded.")
                    .addField("Reason", punishmentMessage, false)
                    .color(Color.DARK_GOLDENROD)
                    .timestamp(Instant.now())
                    .build();

            PrivateChannel privateChannel = warnedUser.getPrivateChannel().block();

            try {
                privateChannel.createMessage(embed).block();
            } catch (Exception e) {
                event.reply("Successfully recorded warning, but unable to DM user. Punishment ID is " + punishment.getInteger("id") + ".").withEphemeral(true).subscribe();
                return Mono.empty();
            }
        }

        event.reply("Successfully warned " + warnedUser.getUsername() + ". Punishment ID is " + punishment.getInteger("id") + ".").withEphemeral(true).subscribe();

        return Mono.empty();
    }

}
