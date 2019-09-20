package me.aberrantfox.hotbot.commands.community

import me.aberrantfox.hotbot.arguments.LowerUserArg
import me.aberrantfox.hotbot.database.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.arguments.IntegerArg
import me.aberrantfox.kjdautils.internal.arguments.UserArg
import net.dv8tion.jda.api.entities.User

@CommandSet("karmacmds")
fun karmaCommands() = commands {
    command("viewkarma") {
        description = "View the karma of a specified user. Defaults to the user who invokes the command."
        expect(arg(UserArg, optional = true, default = {it.author}))
        execute{
            val user = it.args.component1() as User
            val karma = getKarma(user)
            it.respond("${user.fullName()}'s karma is $karma")
        }
    }

    command("leaderboard") {
        description = "View the users with the most Karma"
        execute {
            it.respond(embed {
                title = "Top 10 most helpful people"

                leaderBoard().forEachIndexed{ index, record ->
                    field {
                        name = "${index + 1}) ${it.discord.jda.getUserById(record.who)?.fullName() ?: record.who}"
                        value = "${record.karma} karma"
                        inline = false
                    }
                }
            })
        }
    }

    command("setkarma") {
        description = "Set the karma of a user to a new amount"
        expect(LowerUserArg, IntegerArg)

        execute {
            val user = it.args.component1() as User
            val amount = it.args.component2() as Int

            if(amount < 0) {
                it.respond("Woah. this guy can't really be *that* bad.. right?")
                return@execute
            }

            setKarma(user, amount)
            it.respond("Updated the karma value for ${user.fullName()}.")
        }
    }
}