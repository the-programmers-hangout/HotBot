package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.util.sendPrivateMessage
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.joda.time.DateTime
import java.awt.Color
import java.util.*
import java.util.LinkedList

data class Suggestion(val member: String, val suggestion: String, val timeOf: DateTime)

object SuggestionPool {
    val pool: Queue<Suggestion> = LinkedList()
}

@Command(ArgumentType.Joiner)
fun suggest(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    if (SuggestionPool.pool.size > 20) {
        sendPrivateMessage(event.author, "There are too many suggestions in the pool to handle your request currently... sorry about that.")
        return
    }

    if (SuggestionPool.pool.filter { it.member == event.author.id }.size > 3) {
        sendPrivateMessage(event.author, "You have enough suggestions in the pool for now...")
    }

    val suggestion = args[0] as String

    SuggestionPool.pool.add(Suggestion(event.author.id, suggestion, DateTime.now()))
    sendPrivateMessage(event.author, "Your suggestion has been added to the review-pool. " +
            "If it passes it'll be pushed out to the suggestions channel.")

    event.message.delete().queue()
}

@Command
fun poolInfo(event: GuildMessageReceivedEvent) {
    sendPrivateMessage(event.author,
            EmbedBuilder().setTitle("Suggestion Pool Info")
                    .setColor(Color.cyan)
                    .setDescription("There are currently ${SuggestionPool.pool.size} suggestions in the pool.")
                    .build())

    event.message.delete().queue()
}