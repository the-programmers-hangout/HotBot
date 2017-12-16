package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.CommandEvent
import me.aberrantfox.aegeus.listeners.antispam.MutedRaiders


@Command
fun viewRaiders(event: CommandEvent) = event.respond("Raiders: " + MutedRaiders.list.reduce { a, b -> "$a, $b" })
