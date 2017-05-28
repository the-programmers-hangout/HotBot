package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.businessobjects.Configuration
import me.aberrantfox.aegeus.commandframework.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@Command(ArgumentType.STRING, ArgumentType.STRING)
fun setPerm(event: MessageReceivedEvent, args: List<Any>,  config: Configuration) {
    val commandName = args[0] as String
    val desiredPermission = args[1] as String
    
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
}

@Command(ArgumentType.STRING)
fun getPerm(event: MessageReceivedEvent, args: List<Any>, config: Configuration) {
    val commandName = args[0] as String

    if( !(config.commandPermissionMap.containsKey(commandName)) ) {
        event.channel.sendMessage("What command is that, exactly?").queue()
        return
    }

    event.channel.sendMessage("Current permission level: ${config.commandPermissionMap[commandName]}").queue()
}