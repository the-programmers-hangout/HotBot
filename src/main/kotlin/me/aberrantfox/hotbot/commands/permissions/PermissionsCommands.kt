package me.aberrantfox.hotbot.commands.permissions

import me.aberrantfox.hotbot.arguments.CategoryArg
import me.aberrantfox.hotbot.arguments.PermissionLevelArg
import me.aberrantfox.hotbot.commands.utility.macroCommandCategory
import me.aberrantfox.hotbot.permissions.PermissionLevel
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.kjdautils.api.dsl.Command
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.stdlib.sanitiseMentions
import me.aberrantfox.kjdautils.internal.command.arguments.CommandArg
import me.aberrantfox.kjdautils.internal.command.arguments.RoleArg
import me.aberrantfox.kjdautils.internal.command.arguments.TextChannelArg
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import java.awt.Color

@CommandSet("permissions")
fun permissionCommands(manager: PermissionManager, config: Configuration) =
        commands {
            command("setPermission") {
                description = "Set the permission level of the given command to the given permission level."
                expect(CommandArg, PermissionLevelArg)
                execute {
                    val command = it.args.component1() as Command
                    val level = it.args.component2() as PermissionLevel

                    manager.setPermission(command.name, level)
                    it.safeRespond("${command.name} is now accessible to ${level.name} and higher")
                }
            }

            command("getPermission") {
                description = "Get the current required permission level for a particular command."
                expect(CommandArg)
                execute {
                    val name = (it.args.component1() as Command).name
                    it.safeRespond("The required role is: ${manager.roleRequired(name).name}")
                }
            }

            command("roleids") {
                description = "Display each role in the server with its corresponding ID"
                execute {
                    val guild = it.jda.getGuildById(config.serverInformation.guildid)
                    it.safeRespond(guild.roles.joinToString("\n") { role -> "${role.name} :: ${role.id}" })
                }
            }

            command("setallPermissions") {
                description = "Set the permission of all commands to a specific role. Only available to the bot owner."
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
                description = "Set the permission level of all commands in a category to the given role."
                expect(CategoryArg, PermissionLevelArg)
                execute {
                    val category = it.args.component1() as String
                    val level = it.args.component2() as PermissionLevel

                    val commands = it.container.commands.values
                            .filter { it.category.toLowerCase() == category.toLowerCase() }
                            .map { it.name }

                    if (commands.isEmpty()) {
                        it.respond("Either this category ($category) contains 0 commands, or it is not a real category :thinking:")
                        return@execute
                    }

                    commands.forEach { command -> manager.setPermission(command, level) }
                    it.safeRespond("${level.name} now has access to: ${commands.joinToString(prefix = "`", postfix = "`")}")
                }
            }

            command("viewpermissions") {
                description = "View all of the commands by category, listed with their associated permission level."
                execute {
                    it.respond(embed {
                        title("Command Permissions")
                        description("Below you can see all of the different command categories, along with all of their " +
                                "respective commands and the associated permission required to use those commands.")


                        val grouped = it.container.commands.values
                                .groupBy { it.category }
                                .filterNot { it.key == macroCommandCategory }

                        grouped.forEach { (category, cmds) ->
                            field {
                                name = category
                                value = cmds.map { it.name }.sorted().joinToString("\n") { "$it -- ${manager.roleRequired(it)}" }.sanitiseMentions()
                                inline = false
                            }
                        }
                    })
                }
            }

            command("listavailable") {
                description = "View commands available to you, based on your permission level."
                execute {
                    val grouped = it.container.commands.values.groupBy { it.category }

                    val available = grouped
                            .map { (category, cmds) ->
                                val availableCmds = cmds.filter { cmd ->
                                    manager.canUseCommand(it.author, cmd.name)
                                }

                                category to availableCmds
                            }
                            .filter { it.second.isNotEmpty() }
                            .filterNot { it.first == macroCommandCategory }
                            .sortedByDescending { it.second.size }

                    it.respond(embed {
                        title("Commands available to you")
                        setColor(Color.green)
                        setThumbnail(it.author.effectiveAvatarUrl)
                        available.forEach { (category, cmds) ->
                            field {
                                name = category
                                value = cmds.map { it.name }.sorted().joinToString()
                                inline = false
                            }
                        }
                    })
                }
            }

            command("setRoleLevel") {
                description = "Set the PermissionLevel of a particular role."
                expect(RoleArg, PermissionLevelArg)
                execute {
                    val role = it.args.component1() as Role
                    val level = it.args.component2() as PermissionLevel

                    manager.assignRoleLevel(role, level)

                    it.respond("${role.name} is now assigned the permission level ${level.name}")
                }
            }

            command("viewRoleAssignments") {
                description = "View the permission levels any roles have been assigned."
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

            command("setChannelCommandIgnore") {
                description = "Set the minimum role required to invoke a command in the given channel"
                expect(TextChannelArg, PermissionLevelArg)
                execute {
                    val channel = it.args.component1() as TextChannel
                    val level = it.args.component2() as PermissionLevel
                    manager.setChannelCommandIgnore(channel.id, level)

                    it.respond("Commands in ${channel.name} now require a minimum role of $level")
                }
            }

            command("setChannelMentionIgnore") {
                description = "Set the minimum role required for the bot to reply to a mention, if mentions are enabled"
                expect(TextChannelArg, PermissionLevelArg)
                execute {
                    val channel = it.args.component1() as TextChannel
                    val level = it.args.component2() as PermissionLevel
                    manager.setChannelMentionIgnore(channel.id, level)

                    it.respond("Mentions in ${channel.name} now require a minimum role of $level")
                }
            }

            command("viewChannelIgnoreLevels") {
                description = "Display the required roles to invoke/mention in the overridden channels"
                execute { event ->
                    val map = manager.allChannelIgnoreLevels()

                    event.respond(embed {
                        setTitle("Channel Ignore Levels")
                        setColor(Color.BLUE)
                        map.forEach {
                            ifield {
                                name = "Channel"
                                value = "<#${it.key}>"
                            }
                            ifield {
                                name = "Command"
                                value = it.value.command.toString()
                            }
                            ifield {
                                name = "Mention"
                                value = it.value.mention.toString()
                            }
                        }
                    })
                }
            }
        }