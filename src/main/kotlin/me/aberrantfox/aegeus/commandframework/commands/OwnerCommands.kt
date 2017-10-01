package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.CommandEvent


@Command(ArgumentType.String, ArgumentType.Joiner)
fun echo(event: CommandEvent) {
    val target = event.args[0] as String
    val message = event.args[1] as String

    event.jda.getTextChannelById(target).sendMessage(message).queue()
}