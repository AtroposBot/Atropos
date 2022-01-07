package dev.laarryy.eris.commands.settings;

import dev.laarryy.eris.commands.Command;
import dev.laarryy.eris.models.guilds.DiscordServer;
import dev.laarryy.eris.models.guilds.permissions.Permission;
import dev.laarryy.eris.models.guilds.permissions.ServerRolePermission;
import dev.laarryy.eris.storage.DatabaseLoader;
import dev.laarryy.eris.utils.AuditLogger;
import dev.laarryy.eris.utils.CommandChecks;
import dev.laarryy.eris.utils.Notifier;
import dev.laarryy.eris.utils.PermissionChecker;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javalite.activejdbc.LazyList;
import reactor.core.publisher.Mono;

import java.time.Instant;
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
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/modmail")
                    .value("modmail")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/settings modmail")
                    .value("modmailsettings")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/warn")
                    .value("warn")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/ban")
                    .value("ban")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/forceban")
                    .value("forceban")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/mute")
                    .value("mute")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/kick")
                    .value("kick")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/case")
                    .value("case")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/unmute")
                    .value("unmute")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/unban")
                    .value("unban")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/inf update")
                    .value("infupdate")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/inf search")
                    .value("infsearch")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/audit")
                    .value("audit")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/settings log")
                    .value("logsettings")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/settings mutedrole")
                    .value("mutedrolesettings")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/settings blacklist")
                    .value("blacklistsettings")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/removesince")
                    .value("removesince")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/stopjoins")
                    .value("stopjoins")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/prune")
                    .value("prune")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/info")
                    .value("info")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("/settings antispam")
                    .value("antispamsettings")
                    .build(),
            ApplicationCommandOptionChoiceData
                    .builder()
                    .name("Every single permission")
                    .value("everything")
                    .build()
    );

    private final ApplicationCommandRequest request = ApplicationCommandRequest.builder()
            .name("permission")
            .description("Manage role permissions.")
            .addOption(ApplicationCommandOptionData.builder()
                    .name("add")
                    .description("Add permissions to roles in this guild")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("role")
                            .description("Role to add permission to.")
                            .type(ApplicationCommandOption.Type.ROLE.getValue())
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("permission")
                            .description("Permission to add to the role.")
                            .type(ApplicationCommandOption.Type.STRING.getValue())
                            .choices(optionChoiceDataList)
                            .required(true)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("remove")
                    .description("Remove permissions from roles in this guild.")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("role")
                            .description("Role to remove permission from.")
                            .type(ApplicationCommandOption.Type.ROLE.getValue())
                            .required(true)
                            .build())
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("permission")
                            .description("Permission to remove from the role.")
                            .type(ApplicationCommandOption.Type.STRING.getValue())
                            .choices(optionChoiceDataList)
                            .required(true)
                            .build())
                    .build())
            .addOption(ApplicationCommandOptionData.builder()
                    .name("list")
                    .description("List permissions of a role in this guild.")
                    .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                    .required(false)
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("role")
                            .description("Role to list permissions of.")
                            .type(ApplicationCommandOption.Type.ROLE.getValue())
                            .required(true)
                            .build())
                    .build())
            .defaultPermission(true)
            .build();

    public ApplicationCommandRequest getRequest() {
        return this.request;
    }

    public Mono<Void> execute(ChatInputInteractionEvent event) {
        if (!CommandChecks.commandChecks(event, request.name())) {
            return Mono.empty();
        }

        if (event.getInteraction().getMember().isEmpty()) {
            return Mono.empty();
        }

        Guild guild = event.getInteraction().getGuild().block();
        Long guildIdSnowflake = guild.getId().asLong();

        Member member = event.getInteraction().getMember().get();
        User user = event.getInteraction().getUser();
        Long userIdSnowflake = member.getId().asLong();

        DatabaseLoader.openConnectionIfClosed();

        DiscordServer discordServer = DiscordServer.findFirst("server_id = ?", guildIdSnowflake);
        int serverId;

        if (discordServer != null) {
            serverId = discordServer.getServerId();
        } else {
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("list").isPresent() && event.getOption("list").get().getOption("role").isPresent()) {
            if (event.getOption("list").get().getOption("role").get().getValue().isEmpty()) {
                Notifier.notifyCommandUserOfError(event, "malformedInput");
                AuditLogger.addCommandToDB(event, false);
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.empty();
            }
            Role role = event.getOption("list").get().getOption("role").get().getValue().get().asRole().block();
            long roleId = role.getId().asLong();
            String roleName = role.getName();
            String roleInfo = "`" + roleName + "`:`" + roleId + "`:<@&" + roleId + ">";

            LazyList<ServerRolePermission> permissions = ServerRolePermission.find("role_id_snowflake = ? and server_id = ?", roleId, serverId);

            String rolePermissionsInfo;
            if (permissions == null || permissions.isEmpty()) {
                rolePermissionsInfo = "none";
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("```diff\n");
                for (ServerRolePermission perm : permissions) {
                    Permission pId = Permission.findFirst("id = ?", perm.getPermissionId());
                    String name = pId.getName();
                    if (name.equals("everything")) {
                        stringBuilder.append("+ All Permissions").append("\n");
                    } else {
                        stringBuilder.append("+ /").append(name).append("\n");
                    }
                }
                stringBuilder.append("```");
                rolePermissionsInfo = stringBuilder.toString();
            }

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Role Permission Info")
                    .addField("Role", roleInfo, false)
                    .color(Color.ENDEAVOUR)
                    .timestamp(Instant.now())
                    .build();

            if (rolePermissionsInfo.equals("none")) {
                embed = EmbedCreateSpec.builder().from(embed)
                        .description("This role has no permissions. To add permissions, use /permission add <role> <permission>.")
                        .build();
            } else {
                embed = EmbedCreateSpec.builder().from(embed)
                        .description(rolePermissionsInfo)
                        .build();
            }
            event.reply().withEmbeds(embed).subscribe();
            AuditLogger.addCommandToDB(event, true);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();

        }

        if (event.getOption("add").isPresent()) {
            ApplicationCommandInteractionOption option = event.getOption("add").get();
            Role role = option.getOption("role").get().getValue().get().asRole().block();
            int permissionToAddId = getIdOfPermissionToHandle(option);

            if (ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", serverId, permissionToAddId, role.getId().asLong()) != null) {
                Notifier.notifyCommandUserOfError(event, "alreadyAssigned");
                AuditLogger.addCommandToDB(event, false);
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.empty();
            }

            ServerRolePermission serverRolePermission = ServerRolePermission.createIt("server_id", serverId, "permission_id", permissionToAddId, "role_id_snowflake", role.getId().asLong());
            serverRolePermission.save();
            serverRolePermission.refresh();
            Permission perm = Permission.findFirst("id = ?", serverRolePermission.getPermissionId());
            String permName = "`/" + perm.getName() + "`";

            long roleId = role.getId().asLong();
            String roleName = role.getName();
            String roleInfo = "`" + roleName + "`:`" + roleId + "`:<@&" + roleId + ">";

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Success")
                    .color(Color.SEA_GREEN)
                    .description("Added permission to use " + permName + " and all subcommands to <@&" + roleId + ">")
                    .addField("Role", roleInfo, false)
                    .build();

            event.reply().withEmbeds(embed).subscribe();
            AuditLogger.addCommandToDB(event, true);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        if (event.getOption("remove").isPresent()) {
            ApplicationCommandInteractionOption option = event.getOption("remove").get();
            Role role = option.getOption("role").get().getValue().get().asRole().block();
            int permissionToRemoveId = getIdOfPermissionToHandle(option);

            if (ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", serverId, permissionToRemoveId, role.getId().asLong()) == null) {
                Notifier.notifyCommandUserOfError(event, "404");
                AuditLogger.addCommandToDB(event, false);
                DatabaseLoader.closeConnectionIfOpen();
                return Mono.empty();
            }

            ServerRolePermission serverRolePermission = ServerRolePermission.findFirst("server_id = ? and permission_id = ? and role_id_snowflake = ?", serverId, permissionToRemoveId, role.getId().asLong());
            Permission perm = Permission.findFirst("id = ?", serverRolePermission.getPermissionId());

            long roleId = role.getId().asLong();
            String roleName = role.getName();
            String roleInfo = "`" + roleName + "`:`" + roleId + "`:<@&" + roleId + ">";
            String permName = "`/" + perm.getName() + "`";

            serverRolePermission.delete();

            EmbedCreateSpec embed = EmbedCreateSpec.builder()
                    .title("Success")
                    .color(Color.SEA_GREEN)
                    .description("Removed permission to use " + permName + " and all subcommands from <@&" + roleId + ">")
                    .addField("Role", roleInfo, false)
                    .build();

            event.reply().withEmbeds(embed).subscribe();
            AuditLogger.addCommandToDB(event, true);
            DatabaseLoader.closeConnectionIfOpen();
            return Mono.empty();
        }

        DatabaseLoader.closeConnectionIfOpen();
        return Mono.empty();
    }

    private int getIdOfPermissionToHandle(ApplicationCommandInteractionOption option) {
        String permissionName = option.getOption("permission").get().getValue().get().asString();
        dev.laarryy.eris.models.guilds.permissions.Permission permissionToHandle = dev.laarryy.eris.models.guilds.permissions.Permission.findOrCreateIt("permission", permissionName);
        permissionToHandle.save();
        permissionToHandle.refresh();
        return permissionToHandle.getInteger("id");
    }
}
