package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.listeners.CommandEvent
import me.aberrantfox.aegeus.services.LastCommands
import java.util.*


@Command
fun lastCommands(event: CommandEvent) {
    if(LastCommands.queue.getQueue().isEmpty()) {
        event.channel.sendMessage("No commands since starting").queue()
        return
    }

    val joiner = StringJoiner("\n")
    LastCommands.queue.getQueue().forEach {
        joiner.add("${it.first.name}:${it.second.channel.name} :: ${it.second.author.asMention} :: ${it.second.rawContent}" )
    }

    event.channel.sendMessage(joiner.toString()).queue()
}