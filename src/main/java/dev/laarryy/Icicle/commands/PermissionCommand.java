package dev.laarryy.Icicle.commands;

import dev.laarryy.Icicle.models.guilds.DiscordServer;
import dev.laarryy.Icicle.models.guilds.permissions.ServerRolePermission;
import dev.laarryy.Icicle.storage.DatabaseLoader;
import dev.laarryy.Icicle.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.service.ApplicationService;
import discord4j.rest.util.ApplicationCommandOptionType;
import discord4j.rest.util.Permission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;

public class PermissionCommand implements Command {

    private final Logger logger = LogManager.getLogger(this);
    private final PermissionChecker permissionChecker = new PermissionChecker();

    List<ApplicationCommandOptionChoiceData> optionChoiceDataList = List.of(
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/test")
                    .value("test")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/permission")
                    .value("permission")
                    .build()
    );

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("permission")
            .description("Manage role permissions.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("add")
                    .description("Add permissions to roles in this guild")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("role")
                            .description("Role to add permission to.")
                            .type(ApplicationCommandOptionType.ROLE.getValue())
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("permission")
                            .description("Permission to add to the role.")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .choices(optionChoiceDataList)
                            .required(true)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("remove")
                    .description("Remove permissions from roles in this guild.")
                    .type(ApplicationCommandOptionType.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("role")
                            .description("Role to remove permission from.")
                            .type(ApplicationCommandOptionType.ROLE.getValue())
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("permission")
                            .description("Permission to remove from the role.")
                            .type(ApplicationCommandOptionType.STRING.getValue())
                            .choices(optionChoiceDataList)
                            .required(true)
                            .build())
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(SlashCommandEvent event) {

        if (event.getInteraction().getGuild().block() == null || event.getInteraction().getMember().isEmpty()) {
            return Mono.empty();
        }

        Guild guild = event.getInteraction().getGuild().block();
        Long guildIdSnowflake = guild.getId().asLong();

        Member member = event.getInteraction().getMember().get();
        User user = event.getInteraction().getUser();
        Long userIdSnowflake = member.getId().asLong();

        DatabaseLoader.openConnectionIfClosed();

        dev.laarryy.Icicle.models.guilds.permissions.Permission permission = dev.laarryy.Icicle.models.guilds.permissions.Permission.findOrCreateIt("permission", request.name());
        permission.save();
        permission.refresh();
        int permissionId = permission.getInteger("id");



        if (!permissionChecker.checkIsAdministrator(guild, member) && !permissionChecker.checkPermission(guild, user, permissionId)) {
            event.reply("You must be an administrator (or be given permission by an administrator) to use this command.").withEphemeral(true).subscribe();
            return Mono.empty();
        }

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildIdSnowflake);
        int serverId;

        if (discordServer != null) {
            serverId = discordServer.getServerId();
        } else {
            serverId = 0;
            return Mono.empty();
        }

        if (event.getOption("add").isPresent()) {
            ApplicationCommandInteractionOption option = event.getOption("add").get();
            Role role = option.getOption("role").get().getValue().get().asRole().block();
            int permissionToAddId = getIdOfPermissionToHandle(option);

            if (ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", serverId, permissionToAddId, role.getId().asLong()) != null) {
                event.reply("This permission has already been assigned to this role in this guild.").withEphemeral(true).subscribe();
                return Mono.empty();
            }

            ServerRolePermission serverRolePermission = ServerRolePermission.createIt("server_id", serverId, "permission_id", permissionToAddId, "role_id_snowflake", role.getId().asLong());
            serverRolePermission.save();

            event.reply("Permission added to role " + role.getName() + ".").withEphemeral(true).subscribe();
            logger.info("Permission added.");
            return Mono.empty();
        }

        if (event.getOption("remove").isPresent()) {
            ApplicationCommandInteractionOption option = event.getOption("remove").get();
            Role role = option.getOption("role").get().getValue().get().asRole().block();
            int permissionToRemoveId = getIdOfPermissionToHandle(option);

            if (ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", serverId, permissionToRemoveId, role.getId().asLong()) == null) {
                event.reply("This permission has not been assigned to this role in this guild.").withEphemeral(true).subscribe();
                return Mono.empty();
            }

            ServerRolePermission serverRolePermission = ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", serverId, permissionToRemoveId, role.getId().asLong());
            serverRolePermission.delete();

            event.reply("Permission removed from role " + role.getName() + ".").withEphemeral(true).subscribe();
            logger.info("Permission removed.");
            return Mono.empty();
        }

        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }

    private int getIdOfPermissionToHandle(ApplicationCommandInteractionOption option) {
        String permissionName = option.getOption("permission").get().getValue().get().asString();
        dev.laarryy.Icicle.models.guilds.permissions.Permission permissionToHandle = dev.laarryy.Icicle.models.guilds.permissions.Permission.findFirst("permission = ?", permissionName);
        return permissionToHandle.getInteger("id");
    }
}
