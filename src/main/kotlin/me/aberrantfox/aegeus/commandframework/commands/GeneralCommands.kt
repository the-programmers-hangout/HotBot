package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.*

val startTime = Date()

@Command fun ping(event: MessageReceivedEvent) = event.channel.sendMessage("Pong!").queue()

@Command fun help(event: MessageReceivedEvent) = event.channel.sendMessage("url-to-help-to-be-added").queue()

@Command fun uptime(event: MessageReceivedEvent) = event.channel.sendMessage("I've been awake since " +
        "${startTime.toString()}, so like... ${(Date().time - startTime.time) / 1000 / 60} minutes").queue()

@Command(ArgumentType.INTEGER, ArgumentType.INTEGER)
fun add(event: MessageReceivedEvent, args: List<Any>) {
    val left = args[0] as Int
    val right = args[1] as Int

    event.channel.sendMessage("Result: ${left + right}").queue()
}