package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.CommandEvent
import me.aberrantfox.aegeus.commandframework.RequiresGuild
import me.aberrantfox.aegeus.extensions.isUserIDList
import me.aberrantfox.aegeus.services.BanQueue


@Command(ArgumentType.Joiner)
fun setQueueReason(event: CommandEvent) {
    val reason = event.args[0] as String
    BanQueue.setReason(event.author.id, reason)
    event.respond("Set Ban-Raid reason to: $reason")
}

@Command(ArgumentType.Splitter)
fun addQueueTargets(event: CommandEvent) {
    val targets = event.args[0] as List<String>

    if( !(targets.isUserIDList(event.jda)) )  {
        event.respond("One or more UserIDS were invalid.")
        return
    }

    targets.forEach { BanQueue.queueBan(event.author.id, it) }
    event.respond("Attempted to add ${targets.size} -- BanQueue now ${BanQueue.getBans(event.author.id)?.size}")
}

@Command(ArgumentType.Splitter)
fun removeTarget(event: CommandEvent) {
    val targets = event.args[0] as List<String>

    if( !(targets.isUserIDList(event.jda)) ) {
        event.respond("One or more UserIDS were invalid.")
        return
    }

    targets.forEach { BanQueue.removeBan(event.author.id, it) }
    event.respond("${targets.size} removed from the ban queue")
}

@Command
fun clearQueue(event: CommandEvent) {
    BanQueue.clearBans(event.author.id)
    event.respond("Targets cleared.")
}

@RequiresGuild
@Command
fun banRaid(event: CommandEvent) {
    if(event.guild == null) return

    val bans = BanQueue.getBans(event.author.id)
    val reason = BanQueue.getReason(event.author.id)

    if(bans == null || reason == null || bans.isEmpty()) {
        event.respond("You haven't added any bans... ")
        return
    }

    bans.forEach { event.guild.controller.ban(it, 1, reason).queue() }
    BanQueue.clearBans(event.author.id)
    event.respond("Raid cleared.")
}

@Command
fun showRaid(event: CommandEvent) {
    val targets = BanQueue.getBans(event.author.id)

    if(targets == null || targets.isEmpty()) {
        event.respond("You haven't added anyone to your ban queue")
        return
    }

    event.respond("Current targets: ${targets.reduce{a, b -> "$a, $b"}}.")
}