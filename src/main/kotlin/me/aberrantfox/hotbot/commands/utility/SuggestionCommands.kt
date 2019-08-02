package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.database.*
import me.aberrantfox.hotbot.services.*
import me.aberrantfox.hotbot.utility.dataclasses.*
import me.aberrantfox.kjdautils.api.dsl.*
import me.aberrantfox.kjdautils.extensions.jda.*
import me.aberrantfox.kjdautils.internal.command.arguments.*
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.*
import net.dv8tion.jda.core.entities.*
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

            var status = SuggestionStatus.Denied

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

                status = SuggestionStatus.Accepted
            }

            it.respond(embed {
                setTitle("${status.message} suggestion")
                setDescription(suggestion.message)
                setColor(status.colour)
            })
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

            val suggestionChannel = fetchSuggestionChannel(guild, config)

            if (!isTracked(target)) {
                it.respond("That is not a valid message or a suggestion by the ID.")
                return@execute
            }

            suggestionChannel.getMessageById(target).queue({ msg ->
                val suggestion = obtainSuggestion(target)
                val message = buildArchiveMessage(suggestion.poolInfo, it.jda, status, msg.reactions)
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

                    val archiveChannel = fetchArchiveChannel(guild, config)

                    if (suggestionChannel.id != archiveChannel.id && response != "review") {
                        archiveChannel.sendMessage(message.build()).queue()
                        msg.deleteIfExists()
                    }
                    else {
                        msg.editMessage(message.build()).queue()
                    }

                    it.respond(embed {
                        setTitle("$status suggestion")
                        setDescription(suggestion.idea)
                        setColor(status.colour)
                    })
                }
            }, { error ->
                it.respond("Couldn't retrieve the message with the given ID. Does the message/channel exist and does the bot have sufficient permissions?")
            })
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

private fun buildArchiveMessage(suggestion: PoolRecord, jda: JDA, status: SuggestionStatus, reactions: List<MessageReaction>): EmbedBuilder {
    val embed = buildSuggestionMessage(suggestion, jda, status)

    if (reactions.size < 2)
        return embed

    val reactionText =
        "${reactions[0].reactionEmote.name} ${reactions[0].count - 1}   " +
        "${reactions[1].reactionEmote.name} ${reactions[1].count - 1}"

    embed.addField("Community Response", reactionText, false)

    return embed
}