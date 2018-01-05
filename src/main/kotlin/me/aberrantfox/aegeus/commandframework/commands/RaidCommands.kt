package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.CommandSet
import me.aberrantfox.aegeus.dsls.command.commands
import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.extensions.idToUser
import me.aberrantfox.aegeus.extensions.removeMuteRole
import me.aberrantfox.aegeus.listeners.antispam.MutedRaiders

@CommandSet
fun raidCommands() = commands {
    command("viewRaiders") {
        execute {
            if (MutedRaiders.set.isEmpty()) {
                it.respond("No raiders... yay? (I hope that is a yay moment :D)")
                return@execute
            }

            it.respond("Raiders: " + MutedRaiders.set.reduce { a, b -> "$a, $b" })
        }
    }

    command("freeRaider") {
        expect(ArgumentType.UserID)
        execute {
            if (MutedRaiders.set.isEmpty()) {
                it.respond("There are no raiders...")
                return@execute
            }

            val user = (it.args[0] as String).idToUser(it.jda)

            if (!(MutedRaiders.set.contains(user.id))) {
                it.respond("That user is not a raider.")
                return@execute
            }

            MutedRaiders.set.remove(user.id)
            removeMuteRole(it.guild, user, it.config)

            it.respond("Removed ${user.fullName()} from the queue, and has been unmuted.")
        }
    }

    command("freeAllRaiders") {
        execute {
            if (MutedRaiders.set.isEmpty()) {
                it.respond("There are no raiders...")
                return@execute
            }

            MutedRaiders.set.forEach { id -> removeMuteRole(it.guild, id.idToUser(it.jda), it.config) }
            MutedRaiders.set.clear()
            it.respond("Raiders unmuted, be nice bois!")
        }
    }

    command("banraider") {
        expect(ArgumentType.UserID, ArgumentType.Integer)
        execute {
            val user = (it.args[0] as String).idToUser(it.jda)
            val delDays = (it.args[1] as Int)

            if (!(MutedRaiders.set.contains(user.id))) {
                it.respond("That user is not a raider.")
                return@execute
            }

            MutedRaiders.set.remove(user.id)
            it.guild.controller.ban(user, delDays).queue()
        }
    }

    command("banautodetectedraid") {
        expect(ArgumentType.Integer)
        execute {
            val delDays = (it.args[0]) as Int

            if (MutedRaiders.set.size == 0) {
                it.respond("There are currently no automatically detected raiders... ")
                return@execute
            }

            MutedRaiders.set
                .map { id -> id.idToUser(it.jda) }
                .forEach { user -> it.guild.controller.ban(user, delDays).queue() }

            it.respond("Performing raid ban.")
        }
    }
}