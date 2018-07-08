package me.aberrantfox.hotbot.commands.community

import me.aberrantfox.hotbot.database.getKarma
import me.aberrantfox.hotbot.database.leaderBoard
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.fullName

@CommandSet("karmacmds")
fun karmaCommands() = commands {
    command("karma") {
        description = "View your karma level"
        execute {
            val karma = getKarma(it.author)
            it.respond("Your karma is $karma")
        }
    }

    command("leaderboard") {
        description = "View the users with the most Karma"
        execute {
            it.respond(embed {
                title("Top 10 most helpful people")


                val top10 = leaderBoard()

                top10.forEachIndexed{ index, record ->
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