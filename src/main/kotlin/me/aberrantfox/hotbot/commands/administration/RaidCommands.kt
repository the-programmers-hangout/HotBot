package me.aberrantfox.hotbot.commands.administration

import me.aberrantfox.hotbot.listeners.antispam.MutedRaiders
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.utility.removeMuteRole
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.command.arguments.IntegerArg
import me.aberrantfox.kjdautils.internal.command.arguments.UserArg
import net.dv8tion.jda.core.entities.User

@CommandSet
fun raidCommands(config: Configuration) = commands {
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
        expect(UserArg)
        execute {
            if (MutedRaiders.set.isEmpty()) {
                it.respond("There are no raiders...")
                return@execute
            }

            val user = it.args[0] as User
            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            if (!(MutedRaiders.set.contains(user.id))) {
                it.respond("That user is not a raider.")
                return@execute
            }

            MutedRaiders.set.remove(user.id)
            removeMuteRole(guild, user, config)

            it.respond("Removed ${user.fullName()} from the queue, and has been unmuted.")
        }
    }

    command("freeAllRaiders") {
        execute {
            if (MutedRaiders.set.isEmpty()) {
                it.respond("There are no raiders...")
                return@execute
            }

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            MutedRaiders.set
                    .mapNotNull { id -> it.jda.retrieveUserById(id).complete() }
                    .forEach { user -> removeMuteRole(guild, user, config) }

            MutedRaiders.set.clear()
            it.respond("Raiders unmuted, be nice bois!")
        }
    }

    command("banraider") {
        expect(UserArg, IntegerArg)
        execute {
            val user = it.args[0] as User
            val delDays = (it.args[1] as Int)

            if (!(MutedRaiders.set.contains(user.id))) {
                it.respond("That user is not a raider.")
                return@execute
            }

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            MutedRaiders.set.remove(user.id)
            guild.controller.ban(user, delDays).queue()
        }
    }

    command("banautodetectedraid") {
        expect(IntegerArg)
        execute {
            val delDays = (it.args[0]) as Int

            if (MutedRaiders.set.size == 0) {
                it.respond("There are currently no automatically detected raiders... ")
                return@execute
            }

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            MutedRaiders.set
                    .map { id -> it.jda.retrieveUserById(id).complete() }
                    .forEach { user -> guild.controller.ban(user, delDays).queue() }

            MutedRaiders.set.clear()

            it.respond("Performing raid ban.")
        }
    }
}