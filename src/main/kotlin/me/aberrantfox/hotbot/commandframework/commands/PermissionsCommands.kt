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
                val roleName = it.args[1] as String

                if(!(it.guild.roles.any { it.id == roleName })) {
                    it.respond("Unknown role.")
                    return@execute
                }

                val role = it.guild.getRoleById(roleName)

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
    }