package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.UserID
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.command.arguments.SplitterArg
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg
import java.awt.Color

data class PollContainer(val question: String, val answers: List<String>, val creator: UserID, val channel: String)


object Polls {
    val map = HashMap<String, PollContainer>()
}

@CommandSet("polling")
fun dataCommands(config: Configuration) = commands {
    command("poll") {
        description = "Create a poll with one question and 2 to 9 answers."
        expect(SplitterArg)
        execute {
            val splitArgs = it.args.first() as List<String>

            if(splitArgs.size < 3) {
                it.respond("Error, you must put in a question, and at least two answers.")
                return@execute
            }

            if(splitArgs.size > 10) {
                it.respond("You can only add 9 answers/responses.")
                return@execute
            }

            val question = splitArgs.first()
            val answers = splitArgs.subList(1, splitArgs.size)

            it.channel.sendMessage(embed {
                title("${it.author.fullName()}'s poll")
                description("$question - react with your answer on this embed to submit a response.")
                setColor(Color.CYAN)
                answers.forEachIndexed { i, e ->
                    field {
                        name = "${numberMap[i + 1]} ${e}"
                        inline = false
                    }
                }
            }).queue { msg ->
                answers.forEachIndexed { i, _ ->
                    msg.addReaction(numberMap[i + 1]).queue()
                }
                Polls.map.put(msg.id, PollContainer(question, answers, it.author.id, it.channel.id))
            }
        }
    }
    command("finishpoll") {
        description = "Conclude a poll, based on the embed message ID of the poll."
        expect(WordArg)
        execute {
            val pollID = it.args.component1() as String

            if( !(Polls.map.containsKey(pollID)) ) {
                it.respond("Error, unknown poll ID: $pollID")
                return@execute
            }

            val poll = Polls.map[pollID]!!
            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            guild.getTextChannelById(poll.channel).getMessageById(pollID).queue { msg ->
                val highestAnswersSize = msg.reactions.maxBy { it.count }!!.count

                if(msg.reactions.all { it.count == highestAnswersSize }) {
                    it.respond("Poll inconclusive.")
                    Polls.map.remove(pollID)
                    return@queue
                }

                val highestAnswers = msg.reactions.filter { it.count == highestAnswersSize }
                val answerString = highestAnswers
                    .map { it.reactionEmote.name }
                    .reduceRight { a, b -> "$a, $b"}

                it.respond("The highest rated answers were: $answerString")
                println("test")
            }
        }
    }
}

val numberMap = mapOf(
        1 to "1⃣",
        2 to "2⃣",
        3 to "3⃣",
        4 to "4⃣",
        5 to "5⃣",
        6 to "6⃣",
        7 to "7⃣",
        8 to "8⃣",
        9 to "9⃣"
    )

val emoteMap = mapOf(
    "1⃣" to 1,
    "2⃣" to 2,
    "3⃣" to 3,
    "4⃣" to 4,
    "5⃣" to 5,
    "6⃣" to 6,
    "7⃣" to 7,
    "8⃣" to 8,
    "9⃣" to 9
)
