package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent


@Command(ArgumentType.INTEGER)
fun nuke(event: GuildMessageReceivedEvent, args: List<Any>) {
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

@Command(ArgumentType.STRING)
fun ignore(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val target = args[0] as String

    println(event.channel.name)

    if(config.ignoredChannels.contains(target)) {
        config.ignoredChannels.remove(target)
        event.channel.sendMessage("Now accepting commands in #$target again.").queue()
    } else {
        config.ignoredChannels.add(target)
        event.channel.sendMessage("I will no longer accept commands from #$target.").queue()
    }

}