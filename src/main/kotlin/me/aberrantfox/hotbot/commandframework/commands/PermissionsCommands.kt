package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.*
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.dsls.embed.embed
import me.aberrantfox.hotbot.extensions.*
import me.aberrantfox.hotbot.extensions.jda.getHighestRole
import me.aberrantfox.hotbot.extensions.jda.getRoleByIdOrName
import me.aberrantfox.hotbot.extensions.jda.toMember
import me.aberrantfox.hotbot.extensions.stdlib.sanitiseMentions
import me.aberrantfox.hotbot.services.CommandDescriptor
import me.aberrantfox.hotbot.services.HelpConf
import java.awt.Color

@CommandSet
fun permissionCommands() =
    commands {
        command("setPermission") {
            expect(ArgumentType.Word, ArgumentType.Word)
            execute {
                val commandName = it.args[0] as String
                val role = it.guild.getRoleByIdOrName(it.args[1] as String)

                if (role == null) {
                    it.respond("Unknown role.")
                    return@execute
                }

                if (!(it.container.has(commandName))) {
                    it.safeRespond("Dunno what the command: $commandName is - run the help command?")
                    return@execute
                }

                it.manager.addPermission(role.id, commandName)
                it.safeRespond("$commandName can now be invoked by ${role.name}")
            }
        }

        command("getPermission") {
            expect(ArgumentType.Word)
            execute {
                val name = it.args[0] as String

                if (!(it.container.has(name))) {
                    it.safeRespond("I do not know what $name is")
                    return@execute
                }

                it.safeRespond("The required role is: ${it.manager.roleRequired(name)?.name
                    ?: "Only the owner can invoke this."}")
            }
        }

        command("listcommandperms") {
            execute {
                it.safeRespond(it.manager.listAvailableCommands(it.author.toMember(it.guild).getHighestRole()?.id))
            }
        }

        command("roleids") {
            execute {
                it.safeRespond(it.guild.roles.joinToString("\n") { role -> "${role.name} :: ${role.id}" })
            }
        }

        command("setallPermissions") {
            expect(ArgumentType.Word)
            execute {
                val role = it.guild.getRoleByIdOrName(it.args.component1() as String)

                if (role == null) {
                    it.respond("Unknown role")
                    return@execute
                }

                if (it.config.serverInformation.ownerID != it.author.id) {
                    it.respond("Sorry, this command can only be run by the owner marked in the configuration file.")
                    return@execute
                }

                it.container.listCommands().forEach { command -> it.manager.addPermission(role.id, command) }
            }
        }

        command("setPermissions") {
            expect(ArgumentType.Word, ArgumentType.Word)
            execute {
                val target = it.args.component1() as String
                val role = it.guild.getRoleByIdOrName(it.args.component2() as String)

                if (role == null) {
                    it.safeRespond("Unknown role")
                    return@execute
                }

                val commands = HelpConf.listCommandsinCategory(target).map { it.name }

                if (commands.isEmpty()) {
                    it.respond("Either this category ($target) contains 0 commands, or it is not a real category :thinking:")
                    return@execute
                }

                commands.forEach { command -> it.manager.addPermission(role.id, command) }
                it.safeRespond("${role.name} now has access to: ${commands.joinToString(prefix = "`", postfix = "`")}")
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
                            val text = commandsInCategory.joinToString("\n") { cmd -> "${cmd.name} -- ${it.manager.roleRequired(cmd.name)?.name ?: "Owner"}" }
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
                        .filter { cmd -> it.manager.canUseCommand(it.author.id, cmd.name) }
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
    }