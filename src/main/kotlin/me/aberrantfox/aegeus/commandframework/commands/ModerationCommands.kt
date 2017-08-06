package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.*


@Command(ArgumentType.Integer)
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

@Command(ArgumentType.String)
fun ignore(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val target = args[0] as String

    if(config.ignoredChannels.contains(target)) {
        config.ignoredChannels.remove(target)
        event.channel.sendMessage("Now accepting commands in #$target again.").queue()
    } else {
        config.ignoredChannels.add(target)
        event.channel.sendMessage("I will no longer accept commands from #$target.").queue()
    }
}

@Command(ArgumentType.Integer, ArgumentType.String)
fun mute(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val minutes = args[0] as Int
    val target = args[1] as String
    val targetID = event.message.mentionedUsers.map { it.id }.first()
    val time = minutes.toLong() * 1000 * 60

    config.mutedMembers.add(targetID)
    event.channel.sendMessage("$target is now muted for $minutes minutes").queue()

    println(targetID)

    val timer = Timer()
    timer.schedule(object: TimerTask() {
        override fun run() {
            config.mutedMembers.remove(targetID)
            event.channel.sendMessage("$target - you have been unmuted. Please respect our rules to prevent" +
                    " further infractions.").queue()
        }
    }, time)
}