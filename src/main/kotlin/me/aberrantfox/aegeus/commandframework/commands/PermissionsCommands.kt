package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.*
import me.aberrantfox.aegeus.listeners.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import java.awt.Color
import java.util.*

@RequiresGuild
@Command(ArgumentType.String, ArgumentType.String)
fun setPerm(event: CommandEvent) {
    if(event.guild == null) return

    val commandName = event.args[0] as String
    val desiredPermission = (event.args[1] as String).toUpperCase()
    
    if( !(event.config.commandPermissionMap.contains(commandName)) ) {
        event.channel.sendMessage("Dunno what the command: $commandName is - run the help command?").queue()
        return
    }

    val permission = stringToPermission(desiredPermission)

    if (permission == null) {
        event.channel.sendMessage("Yup. That permission level doesn't exist. Sorry").queue()
        return
    }

    event.config.commandPermissionMap[commandName] = permission
    event.channel.sendMessage("Permission of $commandName is now $permission").queue()
}

@Command(ArgumentType.String)
fun getPerm(event: CommandEvent) {
    val commandName = event.args[0] as String

    if( !(event.config.commandPermissionMap.containsKey(commandName)) ) {
        event.channel.sendMessage("What command is that, exactly?").queue()
        return
    }

    event.channel.sendMessage("Current permission level: ${event.config.commandPermissionMap[commandName]}").queue()
}

@Command
fun listCommands(event: CommandEvent) {
    val messageBuilder = StringBuilder()
    event.config.commandPermissionMap.keys.forEach { messageBuilder.append(it).append(", ") }

    val message = messageBuilder.substring(0, messageBuilder.length -  2)
    event.channel.sendMessage("Currently there are the following commands: $message.").queue()
}

@RequiresGuild
@Command
fun listAvailable(event: CommandEvent) {
    if(event.guild == null) return

    val permLevel = getHighestPermissionLevel(event.guild, event.config, event.jda)
    val available = event.config.commandPermissionMap.filter { it.value <= permLevel }.keys.reduce { acc, s -> "$acc, $s" }
    val response = EmbedBuilder()
            .setTitle("Available Commands")
            .setColor(Color.cyan)
            .setDescription("Below you can find a list of commands that are available to based on your permission level," +
                    " which is $permLevel - if you need help using any of them, simply type ${event.config.prefix}help <command>.")
            .addField("Commands", available, false)
            .build()

    event.channel.sendMessage(response).queue()
}


@Command
fun listPerms(event: CommandEvent) {
    event.channel.sendMessage(Permission.values().map { it.name }.reduceRight{
        acc, s -> "$acc, $s"
    } + ".").queue()
}

@Command
fun listCommandPerms(event: CommandEvent) {
    val joiner = StringJoiner("\n")
    event.config.commandPermissionMap.entries.forEach { joiner.add("${it.key} :: ${it.value}") }
    event.channel.sendMessage(joiner.toString()).queue()
}