package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.businessobjects.Configuration
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

//setPerm command newperm
@Command(ArgumentType.STRING, ArgumentType.STRING)
fun setPerm(event: MessageReceivedEvent, args: List<String>,  config: Configuration) {

}
