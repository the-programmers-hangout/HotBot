package me.aberrantfox.hotbot.commandframework.commands.permissions

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.Command
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.dsls.embed.embed
import me.aberrantfox.hotbot.extensions.stdlib.sanitiseMentions
import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.hotbot.services.CommandDescriptor
import me.aberrantfox.hotbot.services.HelpConf
import net.dv8tion.jda.core.entities.Role
import java.awt.Color

@CommandSet
fun permissionCommands() =
    commands {
        command("setPermission") {
            expect(ArgumentType.Command, ArgumentType.PermissionLevel)
            execute {
                val command = it.args.component1() as Command
                val level = it.args.component2() as PermissionLevel

                it.manager.setPermission(command.name, level)
                it.safeRespond("${command.name} is now accessible to ${level.name} and higher")
            }
        }

        command("getPermission") {
            expect(ArgumentType.Command)
            execute {
                val name = (it.args.component1() as Command).name
                it.safeRespond("The required role is: ${it.manager.roleRequired(name).name}")
            }
        }

        command("listcommandperms") {
            execute {
                it.respond(it.manager.listAvailableCommands(it.author))
            }
        }

        command("roleids") {
            execute {
                it.safeRespond(it.guild.roles.joinToString("\n") { role -> "${role.name} :: ${role.id}" })
            }
        }

        command("setallPermissions") {
            expect(ArgumentType.PermissionLevel)
            execute {
                val level = it.args.component1() as PermissionLevel

                if (it.config.serverInformation.ownerID != it.author.id) {
                    it.respond("Sorry, this command can only be run by the owner marked in the configuration file.")
                    return@execute
                }

                it.container.listCommands().forEach { command -> it.manager.setPermission(command, level) }
            }
        }

        command("setPermissions") {
            expect(ArgumentType.Category, ArgumentType.PermissionLevel)
            execute {
                val category = it.args.component1() as String
                val level = it.args.component2() as PermissionLevel

                val commands = HelpConf.listCommandsinCategory(category).map { it.name }

                if (commands.isEmpty()) {
                    it.respond("Either this category ($category) contains 0 commands, or it is not a real category :thinking:")
                    return@execute
                }

                commands.forEach { command -> it.manager.setPermission(command, level) }
                it.safeRespond("${level.name} now has access to: ${commands.joinToString(prefix = "`", postfix = "`")}")
            }
        }

        command("viewpermissions") {
            execute {
                it.respond(embed {
                    title("Command Permissions")
                    description("Below you can see all of the different command categories, along with all of their " +
                        "respective commands and the associated permission required to use those commands.")


                    HelpConf.listCategories()
                        .map { cat ->
                            val commandsInCategory = HelpConf.listCommandsinCategory(cat)
                            val text = commandsInCategory.joinToString("\n") { cmd -> "${cmd.name} -- ${it.manager.roleRequired(cmd.name)}" }
                            Pair(cat, text)
                        }
                        .forEach {
                            field {
                                name = it.first
                                value = it.second.sanitiseMentions()
                                inline = false
                            }
                        }
                })
            }
        }

        command("listavailable") {
            execute {
                val available = HelpConf.listCategories().map { cat ->
                    val cmds =  HelpConf.listCommandsinCategory(cat)
                        .filter { cmd -> it.manager.canUseCommand(it.author, cmd.name) }
                        .map(CommandDescriptor::name)
                        .joinToString()

                    Pair(cat, cmds)
                }.filter { it.second.isNotEmpty() }

                it.respond(embed {
                    title("Commands available to you")
                    setColor(Color.green)
                    setThumbnail(it.author.effectiveAvatarUrl)
                    available.forEach {
                        field {
                            name = it.first
                            value = it.second
                            inline = false
                        }
                    }
                })
            }
        }

        command("setRoleLevel") {
            expect(ArgumentType.Role, ArgumentType.PermissionLevel)
            execute {
                val role = it.args.component1() as Role
                val level = it.args.component2() as PermissionLevel

                it.manager.assignRoleLevel(role, level)

                it.respond("${role.name} is now assigned the permission level ${level.name}")
            }
        }

        command("viewRoleAssignments") {
            execute {
                it.respond(embed {
                    title("Role Assignments")
                    description("Below you can see what roles have been assigned what permission levels")

                    val assignments = it.manager.roleAssignemts()

                    val assignmentsText = if(assignments.isEmpty()) {
                        "None"
                    } else {
                        assignments.joinToString("\n") { pair ->
                            val roleName = it.guild.getRoleById(pair.key).name
                            "$roleName :: PermissionLevel.${pair.value}"
                        }
                    }

                    field {
                        this.name = "Assignments"
                        this.value = assignmentsText
                        inline = false
                    }
                })
            }
        }
    }