package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.*
import me.aberrantfox.aegeus.dsls.command.commands
import me.aberrantfox.aegeus.permissions.Permission
import me.aberrantfox.aegeus.permissions.getHighestPermissionLevel
import me.aberrantfox.aegeus.permissions.stringToPermission
import net.dv8tion.jda.core.EmbedBuilder
import java.awt.Color
import java.util.*

@CommandSet
fun permissionCommands() =
    commands {
        command("setperm") {
            expect(ArgumentType.Word, ArgumentType.Word)
            execute {
                val commandName = it.args[0] as String
                val desiredPermission = (it.args[1] as String).toUpperCase()

                if (!(it.config.commandPermissionMap.contains(commandName))) {
                    it.respond("Dunno what the command: $commandName is - run the help command?")
                    return@execute
                }

                val permission = stringToPermission(desiredPermission)

                if (permission == null) {
                    it.respond("Yup. That permission level doesn't exist. Sorry")
                    return@execute
                }

                it.config.commandPermissionMap[commandName] = permission
                it.respond("Permission of $commandName is now $permission")
            }
        }

        command("getPerm") {
            expect(ArgumentType.Word)
            execute {
                val commandName = it.args[0] as String

                if (!(it.config.commandPermissionMap.containsKey(commandName))) {
                    it.respond("What command is that, exactly?")
                    return@execute
                }

                it.respond("Current permission level: ${it.config.commandPermissionMap[commandName]}")
            }
        }

        command("listcommands") {
            execute {
                val messageBuilder = StringBuilder()
                it.config.commandPermissionMap.keys.forEach { messageBuilder.append(it).append(", ") }

                val message = messageBuilder.substring(0, messageBuilder.length - 2)
                it.respond("Currently there are the following commands: $message.")
            }
        }

        command("listavailable") {
            execute {
                val permLevel = getHighestPermissionLevel(it.guild, it.config, it.jda, it.author.id)
                val available = it.config.commandPermissionMap.filter { it.value <= permLevel }.keys.reduce { acc, s -> "$acc, $s" }
                val response = EmbedBuilder()
                    .setTitle("Available Commands")
                    .setColor(Color.cyan)
                    .setDescription("Below you can find a set of commands that are available to based on your permission level," +
                        " which is $permLevel - if you need help using any of them, simply type ${it.config.prefix}help <command>.")
                    .addField("Commands", available, false)
                    .build()

                it.respond(response)
            }
        }

        command("listperms") {
            execute {
                it.channel.sendMessage(Permission.values().map { it.name }.reduceRight { acc, s ->
                    "$acc, $s"
                } + ".").queue()
            }
        }

        command("listcommandperms") {
            execute {
                val joiner = StringJoiner("\n")
                it.config.commandPermissionMap.entries.forEach { joiner.add("${it.key} :: ${it.value}") }
                it.respond(joiner.toString())
            }
        }
    }