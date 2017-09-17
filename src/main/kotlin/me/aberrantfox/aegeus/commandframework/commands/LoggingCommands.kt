package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.listeners.CommandEvent
import me.aberrantfox.aegeus.services.LastCommands
import me.aberrantfox.aegeus.services.VoiceMovements
import java.util.*


@Command
fun cmdLog(event: CommandEvent) {
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

@Command
fun voiceLog(event: CommandEvent) {
    if(VoiceMovements.queue.getQueue().isEmpty()) {
        event.channel.sendMessage("No logs to show.").queue()
        return
    }

    val joiner = StringJoiner("\n")
    VoiceMovements.queue.getQueue().forEach {
        joiner.add("${it.first.name} :: ${it.second}" )
    }
    event.channel.sendMessage(joiner.toString()).queue()
}