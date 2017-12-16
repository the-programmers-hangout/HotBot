package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.CommandEvent
import me.aberrantfox.aegeus.commandframework.RequiresGuild
import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.extensions.idToUser
import me.aberrantfox.aegeus.extensions.removeMuteRole
import me.aberrantfox.aegeus.listeners.antispam.MutedRaiders


@Command
fun viewRaiders(event: CommandEvent){

    if(MutedRaiders.set.isEmpty()) {
        event.respond("No raiders... yay? (I hope that is a yay moment :D)")
        return
    }

    event.respond("Raiders: " + MutedRaiders.set.reduce { a, b -> "$a, $b" })
}

@RequiresGuild
@Command(ArgumentType.UserID)
fun freeRaider(event: CommandEvent) {
    if(event.guild == null) return

    if(MutedRaiders.set.isEmpty()) {
        event.respond("There are no raiders...")
        return
    }

    val user = (event.args[0] as String).idToUser(event.jda)

    if( !(MutedRaiders.set.contains(user.id)) ) {
        event.respond("That user is not a raider.")
        return
    }

    MutedRaiders.set.remove(user.id)
    removeMuteRole(event.guild, user, event.config)

    event.respond("Removed ${user.fullName()} from the queue, and has been unmuted.")
}

@Command
@RequiresGuild
fun freeAllRaiders(event: CommandEvent) {
    if(event.guild == null) return

    if(MutedRaiders.set.isEmpty()) {
        event.respond("There are no raiders...")
        return
    }

    MutedRaiders.set.forEach { removeMuteRole(event.guild, it.idToUser(event.jda), event.config) }
    MutedRaiders.set.clear()
    event.respond("Raiders unmuted, be nice bois!")
}

@RequiresGuild
@Command(ArgumentType.UserID, ArgumentType.Integer)
fun banRaider(event: CommandEvent) {
    if(event.guild == null) return

    val user = (event.args[0] as String).idToUser(event.jda)
    val delDays = (event.args[1] as Int)

    if( !(MutedRaiders.set.contains(user.id)) ) {
        event.respond("That user is not a raider.")
        return
    }

    MutedRaiders.set.remove(user.id)
    event.guild.controller.ban(user, delDays).queue()
}

@RequiresGuild
@Command(ArgumentType.Integer)
fun banAutodetectedRaid(event: CommandEvent) {
    if(event.guild == null) return

    val delDays = (event.args[0]) as Int

    if(MutedRaiders.set.size == 0) {
        event.respond("There are currently no automatically detected raiders... ")
        return
    }

    MutedRaiders.set
        .map { it.idToUser(event.jda) }
        .forEach { event.guild.controller.ban(it, delDays).queue() }

    event.respond("Performing raid ban.")
}