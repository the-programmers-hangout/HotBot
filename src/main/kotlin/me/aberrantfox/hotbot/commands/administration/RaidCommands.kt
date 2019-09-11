package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.listeners.antispam.MutedRaiders
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.utility.removeMuteRole
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.jda.toMember
import me.aberrantfox.kjdautils.internal.command.arguments.IntegerArg
import me.aberrantfox.kjdautils.internal.command.arguments.UserArg
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.api.entities.User

@CommandSet("security")
fun raidCommands(config: Configuration, logger: BotLogger) = commands {
    command("viewRaiders") {
        description = "See what raiders are in the raidView"
        execute {
            if (MutedRaiders.set.isEmpty()) {
                it.respond("No raiders... yay? (I hope that is a yay moment :D)")
                return@execute
            }

            it.respond("Raiders: " + MutedRaiders.set.reduce { a, b -> "$a, $b" })
        }
    }

    command("freeRaider") {
        description = "Free a raider by an ID, unmuting them. Do this rather than ban people as it'll exit them from the raidView as well."
        requiresGuild = true
        expect(UserArg)
        execute {
            if (MutedRaiders.set.isEmpty()) {
                it.respond("There are no raiders...")
                return@execute
            }

            val user = it.args[0] as User

            if (!(MutedRaiders.set.contains(user.id))) {
                it.respond("That user is not a raider.")
                return@execute
            }

            MutedRaiders.set.remove(user.id)

            user.toMember(it.guild!!)?.let { member -> removeMuteRole(member, config, logger) }

            it.respond("Removed ${user.fullName()} from the queue, and has been unmuted.")
        }
    }

    command("freeAllRaiders") {
        description = "For freeing all raiders from the raid queue, unmuting them as well."
        requiresGuild = true
        execute {
            if (MutedRaiders.set.isEmpty()) {
                it.respond("There are no raiders...")
                return@execute
            }

            MutedRaiders.set
                    .mapNotNull { id -> it.discord.jda.retrieveUserById(id).complete()?.toMember(it.guild!!) }
                    .forEach { member -> removeMuteRole(member, config, logger) }

            MutedRaiders.set.clear()
            it.respond("Raiders unmuted, be nice bois!")
        }
    }

    command("banraider") {
        description = "Ban a raider by ID, deleting a given number of days messages"
        requiresGuild = true
        expect(UserArg, IntegerArg("Message Deletion Days"))
        execute {
            val user = it.args[0] as User
            val delDays = (it.args[1] as Int)

            if (!(MutedRaiders.set.contains(user.id))) {
                it.respond("That user is not a raider.")
                return@execute
            }

            MutedRaiders.set.remove(user.id)
            it.guild!!.ban(user, delDays).queue()
        }
    }

    command("banautodetectedraid") {
        description = "Ban all of the raiders in the raidview, deleting a given number of days messages"
        requiresGuild = true
        expect(IntegerArg("Message Deletion Days"))
        execute {
            val delDays = (it.args[0]) as Int

            if (MutedRaiders.set.size == 0) {
                it.respond("There are currently no automatically detected raiders... ")
                return@execute
            }

            MutedRaiders.set
                    .map { id -> it.discord.jda.retrieveUserById(id).complete() }
                    .forEach { user -> it.guild!!.ban(user, delDays).queue() }

            MutedRaiders.set.clear()

            it.respond("Performing raid ban.")
        }
    }
}