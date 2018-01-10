package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.CommandSet
import me.aberrantfox.aegeus.dsls.command.commands
import me.aberrantfox.aegeus.extensions.idToName
import me.aberrantfox.aegeus.services.AddResponse
import me.aberrantfox.aegeus.services.UserElementPool


private val RejectionMessage = "There are no profiles in the pool."

object Profiles {
    val pool: UserElementPool = UserElementPool(userLimit = 1, poolName = "Profiles")
}

@CommandSet
fun profileCommands() = commands {
    command("submitprofile") {
        expect(ArgumentType.Sentence)
        execute {
            val profileText = it.args[0] as String

            val response = Profiles.pool.addRecord(it.author.id, it.author.avatarUrl, profileText)

            when (response) {
                AddResponse.Accepted -> it.respond("Your profile has been added to the review pool. An administrator will review it shortly.")
                AddResponse.UserFull -> it.respond("You've already submitted a profile...")
                AddResponse.PoolFull -> it.respond("The pool is currently full. Try again in a few days.")
            }
        }
    }

    command("viewprofile") {
        execute {
            val record = Profiles.pool.peek()

            if (record == null) {
                it.respond(RejectionMessage)
                return@execute
            }

            it.respond(record.describe(it.jda, "Profile"))
        }
    }

    command("acceptprofile") {
        execute {
            val record = Profiles.pool.top()

            if (record == null) {
                it.respond(RejectionMessage)
                return@execute
            }

            val target = it.jda.getTextChannelById(it.config.profileChannel)
            target.sendMessage(
                record.prettyPrint(it.jda, "Profile")
                    .setTitle("")
                    .setAuthor(record.sender.idToName(it.jda), record.avatarURL, record.avatarURL)
                    .build())
                .queue()
        }
    }

    command("declineprofile") {
        execute {
            val top = Profiles.pool.top()

            if (top == null) {
                it.respond(RejectionMessage)
                return@execute
            }

            it.respond(top.describe(it.jda, "Rejected Profile"))
        }
    }
}