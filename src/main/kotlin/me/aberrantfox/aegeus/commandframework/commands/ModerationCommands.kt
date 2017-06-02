package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

@Command(ArgumentType.INTEGER)
fun nuke(event: MessageReceivedEvent, args: List<Any>) {
    val amount = args[0] as Int

    if(amount <= 0) {
        event.channel.sendMessage("Yea, what exactly is the point in nuking nothing... ?").queue()
        return
    }

    event.channel.history.retrievePast(amount + 1).queue({
        it.forEach { it.delete().queue() }
        event.channel.sendMessage("Be nice. No spam.").queue()
    })
}