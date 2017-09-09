package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.commandframework.*
import me.aberrantfox.aegeus.listeners.CommandEvent
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color
import java.util.*

@Command(ArgumentType.String, ArgumentType.String)
fun setPerm(event: CommandEvent) {
    val (guildEvent, args, config) = event

    val commandName = args[0] as String
    val desiredPermission = (args[1] as String).toUpperCase()
    
    if( !(config.commandPermissionMap.contains(commandName)) ) {
        guildEvent.channel.sendMessage("Dunno what the command: $commandName is - run the help command?").queue()
        return
    }

    val permission = stringToPermission(desiredPermission)

    if (permission == null) {
        guildEvent.channel.sendMessage("Yup. That permission level doesn't exist. Sorry").queue()
        return
    }

    config.commandPermissionMap[commandName] = permission
    guildEvent.channel.sendMessage("Permission of $commandName is now $permission").queue()
}

@Command(ArgumentType.String)
fun getPerm(event: CommandEvent) {
    val (guildEvent, args, config) = event

    val commandName = args[0] as String

    if( !(config.commandPermissionMap.containsKey(commandName)) ) {
        guildEvent.channel.sendMessage("What command is that, exactly?").queue()
        return
    }

    guildEvent.channel.sendMessage("Current permission level: ${config.commandPermissionMap[commandName]}").queue()
}

@Command
fun listCommands(event: CommandEvent) {
    val (guildEvent, _, config) = event
    val messageBuilder = StringBuilder()
    config.commandPermissionMap.keys.forEach { messageBuilder.append(it).append(", ") }

    val message = messageBuilder.substring(0, messageBuilder.length -  2)
    guildEvent.channel.sendMessage("Currently there are the following commands: $message.").queue()
}

@Command
fun listAvailable(event: CommandEvent) {
    val (guildEvent, _, config) = event
    val permLevel = getHighestPermissionLevel(guildEvent.member.roles, config)
    val available = config.commandPermissionMap.filter { it.value <= permLevel }.keys.reduce { acc, s -> "$acc, $s" }
    val response = EmbedBuilder()
            .setTitle("Available Commands")
            .setColor(Color.cyan)
            .setDescription("Below you can find a list of commands that are available to based on your permission level," +
                    " which is $permLevel - if you need help using any of them, simply type ${config.prefix}help <command>.")
            .addField("Commands", available, false)
            .build()

    guildEvent.channel.sendMessage(response).queue()
}


@Command
fun listPerms(event: CommandEvent) {
    event.guildEvent.channel.sendMessage(Permission.values().map { it.name }.reduceRight{
        acc, s -> "$acc, $s"
    } + ".").queue()
}

@Command
fun listCommandPerms(event: CommandEvent) {
    val (guildEvent, args, config) = event
    val joiner = StringJoiner("\n")
    config.commandPermissionMap.entries.forEach { joiner.add("${it.key} :: ${it.value}") }
    guildEvent.channel.sendMessage(joiner.toString()).queue()
}