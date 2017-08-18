package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.insertInfraction
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent


//warn id <message>
@Command(ArgumentType.UserID, ArgumentType.Joiner)
fun warn(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val target = args[0] as String
    val reason = args[1] as String

    if( !(event.guild.members.map { it.user.id }.contains(target)) ) {
        event.channel.sendMessage("Cannot find the member by the id: $target").queue()
        return
    }

    insertInfraction(target, event.author.id, 0, reason, config)
}