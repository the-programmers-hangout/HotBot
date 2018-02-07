package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.*
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.*

@CommandSet
fun permissionCommands() =
    commands {
        command("setPermission") {
            expect(ArgumentType.Word, ArgumentType.Word)
            execute {
                val commandName = it.args[0] as String
                val role = it.guild.getRoleByIdOrName(it.args[1] as String)

                if(role == null) {
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

                if( !(it.container.has(name)) ) {
                    it.safeRespond("I do not know what $name is")
                    return@execute
                }

                it.safeRespond("The required role is: ${it.manager.roleRequired(name)?.name ?: "Only the owner can invoke this."}")
            }
        }

        command("listcommandperms") {
            execute {
                it.safeRespond(it.manager.listAvailableCommands(it.author.toMember(it.guild).getHighestRole()?.id))
            }
        }

        command("roleids") {
            execute {
                it.guild.roles.forEach { role -> it.safeRespond("${role.name} :: ${role.id}") }
            }
        }

        command("setallPermissions") {
            expect(ArgumentType.Word)
            execute {
                val role = it.guild.getRoleByIdOrName(it.args.component1() as String)

                if(role == null) {
                    it.respond("Unknown role")
                    return@execute
                }

                if(it.config.serverInformation.ownerID != it.author.id) {
                    it.respond("Sorry, this command can only be run by the owner marked in the configuration file.")
                    return@execute
                }

                it.container.listCommands().forEach { command -> it.manager.addPermission(role.id, command) }
            }
        }
    }