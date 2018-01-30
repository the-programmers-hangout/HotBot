package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.*
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.getHighestRole
import me.aberrantfox.hotbot.extensions.sanitiseMentions
import me.aberrantfox.hotbot.extensions.toMember
import me.aberrantfox.hotbot.extensions.toRole

@CommandSet
fun permissionCommands() =
    commands {
        command("setPermission") {
            expect(ArgumentType.Word, ArgumentType.Word)
            execute {
                val commandName = it.args[0] as String
                val role = (it.args[1] as String).toRole(it.guild)

                if (!(it.container.has(commandName))) {
                    it.safeRespond("Dunno what the command: $commandName is - run the help command?")
                    return@execute
                }

                if (role == null) {
                    it.respond("Yup. That role doesn't exist. Sorry")
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