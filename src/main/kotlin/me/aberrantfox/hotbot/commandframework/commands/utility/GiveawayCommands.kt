package me.aberrantfox.hotbot.commandframework.commands.utility

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.dsls.embed.embed
import me.aberrantfox.hotbot.extensions.stdlib.convertToTimeString
import me.aberrantfox.hotbot.utility.randomInt
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.schedule
import kotlin.math.roundToLong

data class GiveawayContainer(val prize: String, val msg: Message, val channel: MessageChannel)

object Giveaways{
    val map = HashMap<String, GiveawayContainer>()
}

val delay = 5000L

@CommandSet
fun giveawayCommands() = commands {
    command("giveaway"){
        expect(ArgumentType.TimeString, ArgumentType.Sentence)
        execute {
            val timeMilliSecs = (it.args.component1() as Double).roundToLong() * 1000
            val prize = it.args.component2().toString()

            it.channel.sendMessage(buildGiveawayEmbed(timeMilliSecs, prize)).queue{msg ->
                msg.addReaction("\uD83C\uDF89").queue()

                Giveaways.map.put(msg.id, GiveawayContainer(prize, msg, it.channel))
                runGiveaway(msg.id, timeMilliSecs)
            }
        }
    }
    command("giveawayend"){
        expect(ArgumentType.Word)
        execute{
        }
    }
    command("giveawayreroll"){
        expect(ArgumentType.Word)
        execute {
        }
    }
}

private fun findWinner(channel: MessageChannel, id: String){
    val giveaway = channel.getMessageById(id)

    giveaway.queue{
        val reaction = it.reactions.first{ it.reactionEmote.name == "\uD83C\uDF89" }

        reaction.users.queue { users ->
            val user = users.dropLast(1)
            val winner = user[randomInt(0, user.size-1)]

            giveaway.complete().editMessage(buildWinnerEmbed(winner)).queue()
        }
    }
}

private fun runGiveaway(id: String, timeLeft: Long) {
    val giveaway = Giveaways.map[id]!!
    val msg = giveaway.msg

    Timer().schedule(delay) {
        when{
            timeLeft < delay -> {
                Giveaways.map.remove(id)
                findWinner(giveaway.channel, msg.id)
            }

            Giveaways.map.containsKey(id) -> {
                if(timeLeft-delay <= 0){
                    findWinner(giveaway.channel, msg.id)
                }else{
                    msg.editMessage(buildGiveawayEmbed(timeLeft-delay, giveaway.prize)).queue()
                    runGiveaway(id, timeLeft-delay)
                }
            }
            else -> {
                findWinner(giveaway.channel, msg.id)
            }
        }
    }
}

private fun buildGiveawayEmbed(timeMilliSecs: Long, prize: String) =
        embed{
            title("\uD83C\uDF89 GIVEAWAY! \uD83C\uDF89")
            setColor(Color.BLUE)
            field {
                name = "Giveaway event started."
                value = "React to this with \uD83C\uDF89 for a chance to win __**$prize**__"
                inline = false
            }
            field {
                name = "Time Left."
                value = "You have __**${timeMilliSecs.convertToTimeString()}**__ left to enter the giveaway"
                inline = false
            }
        }

private fun buildWinnerEmbed(winner: User) =
        embed {
            title("Giveaway event ended")
            description("${winner.asMention} has won the giveaway! \uD83C\uDF89")
            setColor(Color.BLACK)
            field {
                name = ""
                value = "Thank you for participating, better luck next time!"
                inline = false
            }
        }