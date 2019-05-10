package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.utility.randomInt
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.stdlib.convertToTimeString
import me.aberrantfox.kjdautils.internal.command.arguments.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.entities.*
import java.awt.Color
import kotlin.concurrent.timer
import kotlin.math.roundToLong


data class Giveaway(val prize: String, val timeRemainingMs: Long)

object Giveaways {
    val giveaways = HashMap<String, Giveaway>()
}

private const val timeUpdatePeriod = 5000L

private const val giveawayEmbedTitle = "\uD83C\uDF89 GIVEAWAY! \uD83C\uDF89"
private const val prizeFieldTitle = "Prize"

@CommandSet("giveaway")
fun giveawayCommands(log: BotLogger) = commands {
    command("giveawaystart") {
        description = "Starts a giveaway in the channel it is invoked in."
        expect(TimeStringArg, SentenceArg("Giveaway Prize"))
        execute {
            val timeMilliSecs = (it.args.component1() as Double).roundToLong() * 1000
            val prize = it.args.component2().toString()

            val giveawayEmbed = buildGiveawayEmbed(timeMilliSecs, prize)

            it.channel.sendMessage(giveawayEmbed).queue { msg ->
                msg.addReaction("\uD83C\uDF89").queue()

                Giveaways.giveaways[msg.id] = Giveaway(prize, timeMilliSecs)
                runGiveaway(msg)
            }
        }
    }

    command("giveawayend") {
        description = "Force end a giveaway"
        expect(arg(MessageArg("Embed Message ID")), arg(TextChannelArg, true, { it.channel }))
        execute {
            val message = it.args.component1() as Message
            if (!message.isGiveaway()) {
                it.respond("Message given isn't a giveaway.")
                return@execute
            }

            val messageID = message.id
            val prize = retrievePrize(message)

            announceWinner(message, prize)
            Giveaways.giveaways.remove(messageID)

            log.info("Ended giveaway for $prize (Message ID: $messageID)")
        }
    }

    command("giveawayreroll") {
        description = "Reroll an ended giveaway to get a new winner"
        expect(arg(MessageArg("Embed Message ID")), arg(TextChannelArg, true, { it.channel }))
        execute {
            val message = it.args.component1() as Message
            if (!message.isGiveaway()) {
                it.respond("Message given isn't a giveaway.")
                return@execute
            }

            val prize = retrievePrize(message)

            announceWinner(message, prize)
        }
    }
}

private fun runGiveaway(message: Message) {
    val messageID = message.id
    val prize = retrievePrize(message)

    timer(period = timeUpdatePeriod, initialDelay = timeUpdatePeriod) {
        val timeLeft = Giveaways.giveaways[messageID]?.timeRemainingMs

        if (timeLeft == null) {
            this.cancel()
            return@timer
        }

        val newTimeLeftMs = timeLeft - timeUpdatePeriod

        if (newTimeLeftMs <= 0) {
            message.channel.getMessageById(messageID).queue { updatedMessage ->
                announceWinner(updatedMessage, prize)
                Giveaways.giveaways.remove(messageID)
            }

            return@timer
        }

        Giveaways.giveaways[messageID] = Giveaway(prize, newTimeLeftMs)

        message.editMessage(buildGiveawayEmbed(newTimeLeftMs, prize)).queue()
    }
}

private fun announceWinner(message: Message, prize: String) {
    val reaction = message.reactions.first { it.reactionEmote.name == "\uD83C\uDF89" }

    reaction.users.queue { allUsers ->
        val entered = allUsers.filterNot(User::isBot)

        if (entered.isEmpty()) {
            message.editMessage(buildWinnerEmbed(null, prize)).queue()
            return@queue
        }

        val winner = entered[randomInt(0, entered.size - 1)]

        message.editMessage(buildWinnerEmbed(winner, prize)).queue()
    }
}

private fun Message.isGiveaway(): Boolean =
        this.embeds.firstOrNull()?.title == giveawayEmbedTitle

private fun retrievePrize(message: Message): String {
    val giveawayEmbed = message.embeds.firstOrNull()

    return Giveaways.giveaways[message.id]?.prize
            ?: if (giveawayEmbed?.title == giveawayEmbedTitle) {
                   giveawayEmbed
                           .fields
                           .firstOrNull { it.name == prizeFieldTitle }?.value ?: "prize"
               } else "prize"
}

private fun buildGiveawayEmbed(timeMilliSecs: Long, prize: String) =
        embed {
            title(giveawayEmbedTitle)
            setColor(Color.BLUE)
            field {
                name = "Giveaway event started."
                value = "React to this with \uD83C\uDF89 for a chance to win "
                inline = false
            }

            field {
                name = prizeFieldTitle
                value = prize
                inline = false
            }

            field {
                name = "Time Left."
                value = "You have __**${timeMilliSecs.convertToTimeString()}**__ left to enter the giveaway"
                inline = false
            }
        }

private fun buildWinnerEmbed(winner: User?, prize: String) =
        embed {
            title(giveawayEmbedTitle)
            description("Thank you for participating, better luck next time!")
            setColor(Color.BLACK)

            field {
                name = "Winner"
                value = winner?.asMention ?: "Nobody!"
                inline = false
            }

            field {
                name = "Prize"
                value = prize
                inline = false
            }
        }
