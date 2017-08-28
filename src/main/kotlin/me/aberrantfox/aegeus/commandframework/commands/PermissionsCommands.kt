package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.commandframework.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color
import java.util.*

@Command(ArgumentType.String, ArgumentType.String)
fun setPerm(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val commandName = args[0] as String
    val desiredPermission = (args[1] as String).toUpperCase()
    
    if( !(config.commandPermissionMap.contains(commandName)) ) {
        event.channel.sendMessage("Dunno what the command: $commandName is - run the help command?").queue()
        return
    }

    val permission = stringToPermission(desiredPermission)

    if (permission == null) {
        event.channel.sendMessage("Yup. That permission level doesn't exist. Sorry").queue()
        return
    }

    config.commandPermissionMap[commandName] = permission
    event.channel.sendMessage("Permission of $commandName is now $permission").queue()
}

@Command(ArgumentType.String)
fun getPerm(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val commandName = args[0] as String

    if( !(config.commandPermissionMap.containsKey(commandName)) ) {
        event.channel.sendMessage("What command is that, exactly?").queue()
        return
    }

    event.channel.sendMessage("Current permission level: ${config.commandPermissionMap[commandName]}").queue()
}

@Command
fun listCommands(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val messageBuilder = StringBuilder()
    config.commandPermissionMap.keys.forEach { messageBuilder.append(it).append(", ") }

    val message = messageBuilder.substring(0, messageBuilder.length -  2)
    event.channel.sendMessage("Currently there are the following commands: $message.").queue()
}

@Command
fun listAvailable(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val permLevel = getHighestPermissionLevel(event.member.roles, config)
    val available = config.commandPermissionMap.filter { it.value <= permLevel }.keys.reduce { acc, s -> "$acc, $s" }
    val response = EmbedBuilder()
            .setTitle("Available Commands")
            .setColor(Color.cyan)
            .setDescription("Below you can find a list of commands that are available to based on your permission level," +
                    " which is $permLevel - if you need help using any of them, simply type ${config.prefix}help <command>.")
            .addField("Commands", available, false)
            .build()

    event.channel.sendMessage(response).queue()
}


@Command
fun listPerms(event: GuildMessageReceivedEvent) {
    event.channel.sendMessage(Permission.values().map { it.name }.reduceRight{
        acc, s -> "$acc, $s"
    } + ".").queue()
}

@Command
fun listCommandPerms(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val joiner = StringJoiner("\n")
    config.commandPermissionMap.entries.forEach { joiner.add("${it.key} :: ${it.value}") }
    event.channel.sendMessage(joiner.toString()).queue()
}