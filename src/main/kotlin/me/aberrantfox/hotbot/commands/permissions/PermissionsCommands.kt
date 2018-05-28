package me.aberrantfox.hotbot.commands.permissions

import me.aberrantfox.hotbot.commands.CategoryArg
import me.aberrantfox.hotbot.commands.PermissionLevelArg
import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.CommandDescriptor
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.HelpConf
import me.aberrantfox.kjdautils.api.dsl.Command
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.stdlib.sanitiseMentions
import me.aberrantfox.kjdautils.internal.command.arguments.CommandArg
import me.aberrantfox.kjdautils.internal.command.arguments.RoleArg
import net.dv8tion.jda.core.entities.Role
import java.awt.Color

@CommandSet
fun permissionCommands(manager: PermissionManager, config: Configuration) =
        commands {
            command("setPermission") {
                expect(CommandArg, PermissionLevelArg)
                execute {
                    val command = it.args.component1() as Command
                    val level = it.args.component2() as PermissionLevel

                    manager.setPermission(command.name, level)
                    it.safeRespond("${command.name} is now accessible to ${level.name} and higher")
                }
            }

            command("getPermission") {
                expect(CommandArg)
                execute {
                    val name = (it.args.component1() as Command).name
                    it.safeRespond("The required role is: ${manager.roleRequired(name).name}")
                }
            }

            command("listcommandperms") {
                execute {
                    it.respond(manager.listAvailableCommands(it.author))
                }
            }

            command("roleids") {
                execute {
                    val guild = it.jda.getGuildById(config.serverInformation.guildid)
                    it.safeRespond(guild.roles.joinToString("\n") { role -> "${role.name} :: ${role.id}" })
                }
            }

            command("setallPermissions") {
                expect(PermissionLevelArg)
                execute {
                    val level = it.args.component1() as PermissionLevel

                    if (config.serverInformation.ownerID != it.author.id) {
                        it.respond("Sorry, this command can only be run by the owner marked in the configuration file.")
                        return@execute
                    }

                    it.container.listCommands().forEach { command -> manager.setPermission(command, level) }
                }
            }

            command("setPermissions") {
                expect(CategoryArg, PermissionLevelArg)
                execute {
                    val category = it.args.component1() as String
                    val level = it.args.component2() as PermissionLevel

                    val commands = HelpConf.listCommandsinCategory(category).map { it.name }

                    if (commands.isEmpty()) {
                        it.respond("Either this category ($category) contains 0 commands, or it is not a real category :thinking:")
                        return@execute
                    }

                    commands.forEach { command -> manager.setPermission(command, level) }
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
                                    val text = commandsInCategory.joinToString("\n") { cmd -> "${cmd.name} -- ${manager.roleRequired(cmd.name)}" }
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
                        val cmds = HelpConf.listCommandsinCategory(cat)
                                .filter { cmd -> manager.canUseCommand(it.author, cmd.name) }
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
                expect(RoleArg, PermissionLevelArg)
                execute {
                    val role = it.args.component1() as Role
                    val level = it.args.component2() as PermissionLevel

                    manager.assignRoleLevel(role, level)

                    it.respond("${role.name} is now assigned the permission level ${level.name}")
                }
            }

            command("viewRoleAssignments") {
                execute {
                    it.respond(embed {
                        title("Role Assignments")
                        description("Below you can see what roles have been assigned what permission levels")

                        val assignments = manager.roleAssignments()

                        val guild = it.jda.getGuildById(config.serverInformation.guildid)

                        val assignmentsText = if (assignments.isEmpty()) {
                            "None"
                        } else {
                            assignments.joinToString("\n") { pair ->
                                val roleName = guild.getRoleById(pair.key).name
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