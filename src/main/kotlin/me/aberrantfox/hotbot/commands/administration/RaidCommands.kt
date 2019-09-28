package me.aberrantfox.hotbot.commands.administration


import me.aberrantfox.hotbot.listeners.antispam.Raiders
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.DatabaseService
import me.aberrantfox.hotbot.services.LoggingService
import me.aberrantfox.hotbot.utility.removeMuteRole
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.jda.toMember
import me.aberrantfox.kjdautils.internal.arguments.UserArg
import net.dv8tion.jda.api.entities.User

@CommandSet("security")
fun raidCommands(config: Configuration,
                 loggingService: LoggingService,
                 raiders: Raiders) = commands {
    command("viewRaiders") {
        description = "See what raiders are in the raidView"
        execute {
            if (raiders.set.isEmpty()) {
                it.respond("No raiders... yay? (I hope that is a yay moment :D)")
                return@execute
            }

            it.respond("Raiders: " + raiders.set.reduce { a, b -> "$a, $b" })
        }
    }

    command("freeRaider") {
        description = "Free a raider by an ID, unmuting them. Do this rather than ban people as it'll exit them from the raidView as well."
        requiresGuild = true
        expect(UserArg)
        execute {
            if (raiders.set.isEmpty()) {
                it.respond("There are no raiders...")
                return@execute
            }

            val user = it.args[0] as User

            if (!(raiders.set.contains(user.id))) {
                it.respond("That user is not a raider.")
                return@execute
            }

            raiders.set.remove(user.id)

            user.toMember(it.guild!!)?.let { member -> removeMuteRole(member, config, loggingService.logInstance) }

            it.respond("Removed ${user.fullName()} from the queue, and has been unmuted.")
        }
    }

    command("freeAllRaiders") {
        description = "For freeing all raiders from the raid queue, unmuting them as well."
        requiresGuild = true
        execute {
            if (raiders.set.isEmpty()) {
                it.respond("There are no raiders...")
                return@execute
            }

            raiders.set
                    .mapNotNull { id -> it.discord.jda.retrieveUserById(id).complete()?.toMember(it.guild!!) }
                    .forEach { member -> removeMuteRole(member, config, loggingService.logInstance) }

            raiders.set.clear()
            it.respond("Raiders unmuted, be nice bois!")
        }
    }
}