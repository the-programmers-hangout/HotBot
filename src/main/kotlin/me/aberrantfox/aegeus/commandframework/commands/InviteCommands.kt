package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.CommandEvent
import me.aberrantfox.aegeus.listeners.antispam.RecentInvites


@Command(ArgumentType.String)
fun whitelistInvite(event: CommandEvent) {
    val inv = event.args[0] as String
    RecentInvites.ignore.add(inv)
    event.respond("Added $inv to the invite whitelist, it can now be posted freely.")
}

@Command(ArgumentType.String)
fun unwhitelistInvite(event: CommandEvent) {
    val inv = event.args[0] as String
    RecentInvites.ignore.remove(inv)
    event.respond("$inv has been removed from the invite whitelist, posting it the configured amount of times will result in a ban.")
}