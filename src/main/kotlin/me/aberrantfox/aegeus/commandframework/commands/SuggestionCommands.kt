package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.util.idToName
import me.aberrantfox.aegeus.commandframework.util.sendPrivateMessage
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.database.isTracked
import me.aberrantfox.aegeus.services.database.obtainSuggestion
import me.aberrantfox.aegeus.services.database.trackSuggestion
import me.aberrantfox.aegeus.services.database.updateSuggestion
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
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
fun suggest(event: GuildMessageReceivedEvent, args: List<Any>) {
    if (Suggestions.pool.size > 20) {
        sendPrivateMessage(event.author, "There are too many suggestions in the pool to handle your request currently... sorry about that.")
        return
    }

    if (Suggestions.pool.count { it.member == event.author.id } == 3) {
        sendPrivateMessage(event.author, "You have enough suggestions in the pool for now...")
        return
    }

    val suggestion = args[0] as String

    Suggestions.pool.add(Suggestion(event.author.id, suggestion, DateTime.now(), event.author.avatarUrl))
    sendPrivateMessage(event.author, "Your suggestion has been added to the review-pool. " +
            "If it passes it'll be pushed out to the suggestions channel.")

    event.message.delete().queue()
}

@Command
fun poolInfo(event: GuildMessageReceivedEvent) {
    sendPrivateMessage(event.author,
            EmbedBuilder().setTitle("Suggestion Pool Info")
                    .setColor(Color.cyan)
                    .setDescription("There are currently ${Suggestions.pool.size} suggestions in the pool.")
                    .build())

    event.message.delete().queue()
}

@Command
fun poolTop(event: GuildMessageReceivedEvent) {
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

    event.message.delete().queue()
}

@Command
fun poolAccept(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    if (Suggestions.pool.isEmpty()) {
        sendPrivateMessage(event.author, "The suggestion pool is empty... :)")
        return
    }

    val channel = event.guild.textChannels.findLast { it.id == config.suggestionChannel }
    val suggestion = Suggestions.pool.remove()

    channel?.sendMessage(buildSuggestionMessage(suggestion, event.jda, SuggestionStatus.Review).build())?.queue {
        trackSuggestion(suggestion, SuggestionStatus.Review, it.id)

        it.addReaction("⬆").queue()
        it.addReaction("⬇").queue()
    }

    event.message.delete().queue()
}

@Command
fun poolDeny(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
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

@Command(ArgumentType.String, ArgumentType.String, ArgumentType.Joiner)
fun respond(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
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
        event.message.delete().queue()
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

    event.message.delete().queue()
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