package me.aberrantfox.hotbot.commands.community

import me.aberrantfox.hotbot.database.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.command.arguments.UserArg
import net.dv8tion.jda.core.entities.User

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
                title("Top 10 most helpful people")

                leaderBoard().forEachIndexed{ index, record ->
                    field {
                        name = "${index + 1}) ${it.jda.getUserById(record.who).fullName()}"
                        value = "${record.karma} karma"
                        inline = false
                    }
                }
            })
        }
    }
}