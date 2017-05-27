package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.businessobjects.Configuration
import me.aberrantfox.aegeus.businessobjects.saveConfigurationFile
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.*

val startTime = Date()

@Command fun ping(event: MessageReceivedEvent) = event.channel.sendMessage("Pong!").queue()

@Command fun help(event: MessageReceivedEvent) = event.channel.sendMessage("url-to-help-to-be-added").queue()

@Command fun uptime(event: MessageReceivedEvent) {
    val minutes = Date().time - startTime.time / 1000 / 60
    val currentDate = startTime.toString()

    event.channel.sendMessage("I've been awake since ${currentDate}, so like... ${minutes} minutes").queue()
}

@Command(ArgumentType.INTEGER, ArgumentType.INTEGER)
fun add(event: MessageReceivedEvent, args: List<Any>) {
    val left = args[0] as Int
    val right = args[1] as Int

    event.channel.sendMessage("Result: ${left + right}").queue()
}

@Command(ArgumentType.STRING)
fun exit(event: MessageReceivedEvent, args: List<Any>, config: Configuration) {
    val saveConfiguration = args[0] as String

    if(saveConfiguration.toLowerCase() == "true") {
        saveConfigurationFile(config)
    }

    event.channel.sendMessage("Exiting").queue()

    System.exit(0)
}