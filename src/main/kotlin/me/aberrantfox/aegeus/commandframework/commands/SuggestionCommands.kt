package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.RequiresGuild
import me.aberrantfox.aegeus.commandframework.extensions.idToName
import me.aberrantfox.aegeus.commandframework.extensions.sendPrivateMessage
import me.aberrantfox.aegeus.listeners.CommandEvent
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.database.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.awt.Color
import java.util.*
import java.util.LinkedList

data class Suggestion(val member: String, val idea: String, val timeOf: DateTime, val avatarURL: String)

enum class SuggestionStatus(val colour: Color, val message: String) {
    Accepted(Color.green, "Accepted"),
    Review(Color.orange, "Under Review"),
    Denied(Color.red, "Denied")
}

object Suggestions {
    val pool: Queue<Suggestion> = LinkedList()
}

@Command(ArgumentType.Joiner)
fun suggest(event: CommandEvent) {
    val author = event.author
    if (Suggestions.pool.size > 20) {
        sendPrivateMessage(author, "There are too many suggestions in the pool to handle your request currently... sorry about that.")
        return
    }

    if (Suggestions.pool.count { it.member == author.id } == 3) {
        sendPrivateMessage(author, "You have enough suggestions in the pool for now...")
        return
    }

    val suggestion = event.args[0] as String

    Suggestions.pool.add(Suggestion(author.id, suggestion, DateTime.now(), author.avatarUrl ?: "http://i.imgur.com/HYkhEFO.jpg"))
    sendPrivateMessage(author, "Your suggestion has been added to the review-pool. " +
            "If it passes it'll be pushed out to the suggestions channel.")
}

@Command
fun poolInfo(event: CommandEvent) {
    sendPrivateMessage(event.author,
            EmbedBuilder().setTitle("Suggestion Pool Info")
                    .setColor(Color.cyan)
                    .setDescription("There are currently ${Suggestions.pool.size} suggestions in the pool.")
                    .build())
}

@Command
fun poolTop(event: CommandEvent) {
    if (Suggestions.pool.isEmpty()) {
        sendPrivateMessage(event.author, "The pool is empty.")
        return
    }

    val suggestion = Suggestions.pool.peek()

    sendPrivateMessage(event.author,
            EmbedBuilder()
                    .setTitle("Suggestion by ${suggestion.member.idToName(event.jda)}")
                    .setDescription(suggestion.idea)
                    .addField("Time of Creation",
                            suggestion.timeOf.toString(DateTimeFormat.forPattern("dd/MM/yyyy")),
                            false)
                    .addField("Member ID", suggestion.member, false)
                    .build())
}

@RequiresGuild
@Command
fun poolAccept(event: CommandEvent) {
    if(event.guild == null) return

    if (Suggestions.pool.isEmpty()) {
        sendPrivateMessage(event.author, "The suggestion pool is empty... :)")
        return
    }

    val channel = event.guild.textChannels.findLast { it.id == event.config.suggestionChannel }
    val suggestion = Suggestions.pool.remove()

    channel?.sendMessage(buildSuggestionMessage(suggestion, event.jda, SuggestionStatus.Review).build())?.queue {
        trackSuggestion(suggestion, SuggestionStatus.Review, it.id)

        it.addReaction("⬆").queue()
        it.addReaction("⬇").queue()
    }
}

@Command
fun poolDeny(event: CommandEvent) {
    if (Suggestions.pool.isEmpty()) {
        sendPrivateMessage(event.author, "The suggestion pool is empty... :)")
        return
    }

    val rejected = Suggestions.pool.remove()

    sendPrivateMessage(event.author, EmbedBuilder()
            .setTitle("Suggestion Removed")
            .addField("User ID", rejected.member, false)
            .addField("Time of Suggestion", rejected.timeOf.toString(DateTimeFormat.forPattern("dd/MM/yyyy")), false)
            .build())
}

@RequiresGuild
@Command(ArgumentType.String, ArgumentType.String, ArgumentType.Joiner)
fun respond(event: CommandEvent) {
    if(event.guild == null) return

    val (args, config) = event
    val target = args[0] as String
    val response = args[1] as String
    val reason = args[2] as String
    val status = inputToStatus(response)

    if (status == null) {
        event.channel.sendMessage("Valid responses are 'accepted', 'denied' and 'review'... use accordingly.").queue()
        return
    }

    val channel = fetchSuggestionChannel(event.guild, config)

    if (!(isTracked(target)) || channel.getMessageById(target) == null) {
        event.channel.sendMessage("That is not a valid message or a suggestion by the ID.").queue()
        return
    }

    channel.getMessageById(target).queue {
        val suggestion = obtainSuggestion(target)
        val message = buildSuggestionMessage(suggestion, event.jda, status)
        val reasonTitle = "Reason for Status"

        message.fields.removeIf { it.name == reasonTitle }

        message.addField(reasonTitle, reason, false)
        updateSuggestion(target, status)

        it.editMessage(message.build()).queue()
    }
}

private fun fetchSuggestionChannel(guild: Guild, config: Configuration) = guild.getTextChannelById(config.suggestionChannel)

private fun inputToStatus(input: String): SuggestionStatus? = SuggestionStatus.values().findLast { it.name.toLowerCase() == input.toLowerCase() }

private fun buildSuggestionMessage(suggestion: Suggestion, jda: JDA, status: SuggestionStatus) =
        EmbedBuilder()
                .setTitle("${suggestion.member.idToName(jda)}'s Suggestion")
                .setThumbnail(suggestion.avatarURL)
                .setColor(status.colour)
                .setDescription(suggestion.idea)
                .addField("Suggestion Status", status.message, false)