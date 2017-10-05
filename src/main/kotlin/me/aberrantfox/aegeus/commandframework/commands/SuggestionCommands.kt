package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.commandframework.RequiresGuild
import me.aberrantfox.aegeus.commandframework.CommandEvent
import me.aberrantfox.aegeus.extensions.idToName
import me.aberrantfox.aegeus.services.AddResponse
import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.PoolRecord
import me.aberrantfox.aegeus.services.UserElementPool
import me.aberrantfox.aegeus.services.database.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import org.joda.time.format.DateTimeFormat
import java.awt.Color



enum class SuggestionStatus(val colour: Color, val message: String) {
    Accepted(Color.green, "Accepted"),
    Review(Color.orange, "Under Review"),
    Denied(Color.red, "Denied")
}

object Suggestions {
    val pool: UserElementPool = UserElementPool(poolName = "Suggestions")
}

@Command(ArgumentType.Joiner)
fun suggest(event: CommandEvent) {
    val author = event.author.id
    val message = event.args[0] as String

    val response = Suggestions.pool.addRecord(author, event.author.avatarUrl, message)

    when (response) {
        AddResponse.PoolFull -> event.respond("You have enough suggestions in the pool for now...")
        AddResponse.UserFull -> event.respond("There are too many suggestions in the pool to handle your request currently... sorry about that.")
        AddResponse.Accepted -> event.respond("Your suggestion has been added to the review-pool. If it passes it'll be pushed out to the suggestions channel.")
    }
}

@Command
fun poolInfo(event: CommandEvent) =
    event.respond(EmbedBuilder().setTitle("Suggestion Pool Info")
        .setColor(Color.cyan)
        .setDescription("There are currently ${Suggestions.pool.entries()} suggestions in the pool.")
        .build())


@Command
fun poolTop(event: CommandEvent) {
    if (Suggestions.pool.isEmpty()) {
        event.respond("The pool is empty.")
        return
    }

    val suggestion = Suggestions.pool.peek()

    event.respond(EmbedBuilder()
            .setTitle("Suggestion by ${suggestion.sender.idToName(event.jda)}")
            .setDescription(suggestion.message)
            .addField("Time of Creation",
                suggestion.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy")),
                false)
            .addField("Member ID", suggestion.sender, false)
            .build())
}

@RequiresGuild
@Command
fun poolAccept(event: CommandEvent) {
    if (event.guild == null) return

    if (Suggestions.pool.isEmpty()) {
        event.respond("The suggestion pool is empty... :)")
        return
    }

    val channel = event.guild.textChannels.findLast { it.id == event.config.suggestionChannel }
    val suggestion = Suggestions.pool.top()

    channel?.sendMessage(buildSuggestionMessage(suggestion, event.jda, SuggestionStatus.Review).build())?.queue {
        trackSuggestion(suggestion, SuggestionStatus.Review, it.id)

        it.addReaction("⬆").queue()
        it.addReaction("⬇").queue()
    }
}

@Command
fun poolDeny(event: CommandEvent) {
    if (Suggestions.pool.isEmpty()) {
        event.respond("The suggestion pool is empty... :)")
        return
    }

    val rejected = Suggestions.pool.top()

    event.respond(EmbedBuilder()
        .setTitle("Suggestion Removed")
        .addField("User ID", rejected.sender, false)
        .addField("Time of Suggestion", rejected.dateTime.toString(DateTimeFormat.forPattern("dd/MM/yyyy")), false)
        .build())
}

@RequiresGuild
@Command(ArgumentType.String, ArgumentType.String, ArgumentType.Joiner)
fun respond(event: CommandEvent) {
    if (event.guild == null) return

    val (args, config) = event
    val target = args[0] as String
    val response = args[1] as String
    val reason = args[2] as String
    val status = inputToStatus(response)

    if (status == null) {
        event.respond("Valid responses are 'accepted', 'denied' and 'review'... use accordingly.")
        return
    }

    val channel = fetchSuggestionChannel(event.guild, config)

    if (!(isTracked(target)) || channel.getMessageById(target) == null) {
        event.respond("That is not a valid message or a suggestion by the ID.")
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

private fun buildSuggestionMessage(suggestion: PoolRecord, jda: JDA, status: SuggestionStatus) =
    EmbedBuilder()
        .setTitle("${suggestion.sender.idToName(jda)}'s Suggestion")
        .setThumbnail(suggestion.avatarURL)
        .setColor(status.colour)
        .setDescription(suggestion.message)
        .addField("Suggestion Status", status.message, false)