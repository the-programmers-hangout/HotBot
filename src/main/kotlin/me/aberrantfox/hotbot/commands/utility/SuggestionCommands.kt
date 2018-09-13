package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.services.AddResponse
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.PoolRecord
import me.aberrantfox.hotbot.services.UserElementPool
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.internal.command.arguments.ChoiceArg
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.WordArg
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import java.awt.Color


enum class SuggestionStatus(val colour: Color, val message: String) {
    Accepted(Color.green, "Accepted"),
    Review(Color.orange, "Under Review"),
    Denied(Color.red, "Denied")
}

object Suggestions {
    val pool: UserElementPool = UserElementPool(poolName = "Suggestions")
}

@CommandSet("suggestions")
fun suggestionCommands(config: Configuration, log: BotLogger) = commands {
    command("suggest") {
        description = "Send a suggestion to the pre-lim pool. Suggestions are reviewed by a mod before they are reviewed by the community."
        expect(SentenceArg("Suggestion Message"))
        execute {
            val author = it.author.id
            val message = it.args[0] as String

            val response = Suggestions.pool.addRecord(author, it.author.effectiveAvatarUrl, message)

            when (response) {
                AddResponse.UserFull -> it.respond("You have enough suggestions in the pool for now...")
                AddResponse.PoolFull -> it.respond("There are too many suggestions in the pool to handle your request currently... sorry about that.")
                AddResponse.Accepted -> it.respond("Your suggestion has been added to the review-pool. If it passes it'll be pushed out to the suggestions channel.")
            }
        }
    }

    command("poolinfo") {
        description = "Display how many suggestions are in the staging area."
        execute {
            it.respond(EmbedBuilder().setTitle("Suggestion Pool Info")
                .setColor(Color.cyan)
                .setDescription("There are currently ${Suggestions.pool.entries()} suggestions in the pool.")
                .build())
        }
    }

    command("pooltop") {
        description = "See the suggestion in the pool next in line for review."
        execute {
            val suggestion = Suggestions.pool.peek()
            if (suggestion == null) {
                it.respond("The pool is empty.")
                return@execute
            }

            it.respond(suggestion.describe(it.jda, "Suggestion"))
        }
    }

    command("pool") {
        description = "Accept or deny the suggestion at the top of the pool. If accepted, move to the community review stage"
        expect(ChoiceArg(name="Response", choices=*arrayOf("accept", "deny")))
        execute {
            val response = it.args.component1() as String
            val suggestion = Suggestions.pool.top()
            if (suggestion == null) {
                it.respond("The suggestion pool is empty... :)")
                return@execute
            }

            if (response == "accept") {
                val guild = it.jda.getGuildById(config.serverInformation.guildid)

                val channel = guild.textChannels.findLast { channel ->
                    channel.id == config.messageChannels.suggestionChannel
                }

                channel?.sendMessage(buildSuggestionMessage(suggestion, it.jda, SuggestionStatus.Review).build())?.queue {
                    trackSuggestion(SuggestionRecord(it.id, SuggestionStatus.Review, suggestion))

                    it.addReaction("⬆").queue()
                    it.addReaction("⬇").queue()
                }
                it.respond(embed {
                    setTitle("Accepted suggestion")
                    setDescription(suggestion.message)
                    setColor(Color.GREEN)
                })
            } else {
                it.respond(embed {
                    setTitle("Denied suggestion")
                    setDescription(suggestion.message)
                    setColor(Color.RED)
                })
            }
        }
    }

    command("respond") {
        description = "Respond to a suggestion in the review stage, given the target id, response (accepted, denied, review), and reason."
        expect(WordArg("Message ID"),
                ChoiceArg(name="Status", choices=*arrayOf("accepted", "denied", "review")),
                SentenceArg("Response Message"))
        execute {
            val args = it.args

            val target = args[0] as String
            val response = args[1] as String
            val reason = args[2] as String
            val status = inputToStatus(response)!!

            val guild = it.jda.getGuildById(config.serverInformation.guildid)

            val channel = fetchSuggestionChannel(guild, config)

            if (!(isTracked(target)) || channel.getMessageById(target) == null) {
                it.respond("That is not a valid message or a suggestion by the ID.")
                return@execute
            }

            channel.getMessageById(target).queue { msg ->
                val suggestion = obtainSuggestion(target)
                val message = buildSuggestionMessage(suggestion.poolInfo, it.jda, status)
                val reasonTitle = "Reason for Status"

                val suggestionUpdateMessage = buildSuggestionUpdateEmbed(suggestion, reason, status)

                try {
                    msg.jda.retrieveUserById(suggestion.member).complete()
                            .sendPrivateMessage(suggestionUpdateMessage, log)
                }
                finally {
                    message.fields.removeIf { it.name == reasonTitle }

                    message.addField(reasonTitle, reason, false)
                    updateSuggestion(target, status)

                    val archive = fetchArchiveChannel(guild, config)
                    archive.sendMessage(message.build()).queue()
                    msg.deleteIfExists()

                    it.respond(embed {
                        setTitle("$status suggestion")
                        setDescription(suggestion.idea)
                        setColor(status.colour)
                    })
                }
            }
        }
    }
}

private fun fetchSuggestionChannel(guild: Guild, config: Configuration) = guild.getTextChannelById(config.messageChannels.suggestionChannel)

private fun fetchArchiveChannel(guild: Guild, config: Configuration) = guild.getTextChannelById(config.messageChannels.suggestionArchive)

private fun inputToStatus(input: String): SuggestionStatus? = SuggestionStatus.values().findLast { it.name.toLowerCase() == input.toLowerCase() }

private fun buildSuggestionUpdateEmbed(suggestion: SuggestionRecord, response: String, newStatus: SuggestionStatus) =
        embed {
            title("Suggestion Status Update")
            description("A suggestion that you submitted has changed status.")

            ifield {
                name = "ID"
                value = suggestion.messageID
            }

            ifield {
                name = "Old Status"
                value = suggestion.status.toString()
            }

            ifield {
                name = "New Status"
                value = newStatus.toString()
            }

            field {
                name = "Suggestion"
                value = suggestion.idea
                inline = false
            }

            field {
                name = "Response"
                value = response
                inline = false
            }

            setColor(newStatus.colour)
        }

private fun buildSuggestionMessage(suggestion: PoolRecord, jda: JDA, status: SuggestionStatus) =
    EmbedBuilder()
        .setTitle("${jda.retrieveUserById(suggestion.sender).complete().name}'s Suggestion")
        .setThumbnail(suggestion.avatarURL)
        .setColor(status.colour)
        .setDescription(suggestion.message)
        .addField("Suggestion Status", status.message, false)