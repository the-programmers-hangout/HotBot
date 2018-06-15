package me.aberrantfox.hotbot.commands.utility

import me.aberrantfox.hotbot.commands.HexColourArg
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.arg
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.extensions.stdlib.isBooleanValue
import me.aberrantfox.kjdautils.extensions.stdlib.toBooleanValue
import me.aberrantfox.kjdautils.internal.command.arguments.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import java.time.Instant

private object EHolder {
    var embed: EmbedBuilder = EmbedBuilder()
    var message: Message? = null
}

private object FHolder {
    var name = ""
    var text = ""
}

@CommandSet("embed")
fun embedCommands() =
    commands {
        command("clearembed") {
            description = "Reset the embed in memory"
            execute {
                EHolder.embed = EmbedBuilder()
                EHolder.message = null

                it.respond("Embed cleared")
            }
        }

        command("sendembed") {
            description = "Send the embed in the channel. This will not call clearEmbed."
            execute {
                if (EHolder.embed.isEmpty) {
                    it.respond("No embed to send.")
                    return@execute
                }

                it.respond(EHolder.embed.build())
            }
        }

        command("updateEmbed") {
            description = "Update the original message of the embed copied with copyembed."
            execute {
                val message = EHolder.message

                if (message == null || EHolder.embed.isEmpty) {
                    it.respond("No embed currently copied.")
                    return@execute
                }

                if (message.author != it.jda.selfUser) {
                    it.respond("You can only edit messages sent by the bot.")
                    return@execute
                }

                message.editMessage(EHolder.embed.build()).queue(
                        { embedMessage ->
                            it.respond("Embed updated.")
                        },
                        { error ->
                            it.respond("Failed to edit message. It may have been deleted.")
                        }
                )
            }
        }

        command("copyembed") {
            description = "Copies the embed from the message with the given id into the embed builder. The channelId from which to get the embed is optional and will default to the one you invoked the command in."
            expect(arg(MessageArg),
                   arg(TextChannelArg, optional = true, default = { it.channel }))
            execute {
                val message = it.args.component1() as Message

                val embed = message.embeds.firstOrNull()
                if (embed == null) {
                    it.respond("Message doesn't contain any embeds")
                    return@execute
                }

                EHolder.embed = EmbedBuilder(embed)
                EHolder.message = message
            }
        }

        command("editfield") {
            description = "Edits a field in the current embed being worked on. The passed index should be from 0 to fields.size - 1. Properties: name, text (or value), inline."
            expect(IntegerArg, WordArg, SentenceArg)
            execute {
                val fields = EHolder.embed.fields

                val fieldIndex = it.args[0] as Int
                if (fieldIndex < 0 || fieldIndex > fields.size - 1) {
                    it.respond("Not a valid field index")
                    return@execute
                }

                val property = (it.args[1] as String).toLowerCase()
                val newValue = it.args[2] as String

                val field = fields[fieldIndex]
                var name = field.name
                var text = field.value
                var inline = field.isInline

                when(property) {
                    "name", "title" -> name = newValue
                    "text", "value" -> text = newValue
                    "inline" -> {
                        if(!newValue.isBooleanValue()) {
                            it.respond("You haven't supplied a boolean value")
                            return@execute
                        }

                        inline = newValue.toBooleanValue()
                    }
                    else -> {
                        it.respond("That isn't a valid property")
                        return@execute
                    }
                }

                fields[fieldIndex] = MessageEmbed.Field(name, text, inline)
            }
        }

        command("settitle") {
            description = "Set the title of the embed in memory"
            expect(SentenceArg)
            execute {
                val title = it.args[0] as String
                EHolder.embed.setTitle(title)
            }
        }

        command("setdescription") {
            description = "Set the description of the embed"
            expect(SentenceArg)
            execute {
                val description = it.args[0] as String
                EHolder.embed.setDescription(description)
            }
        }

        command("addtimestamp") {
            description = "Add a timestamp to the footer of the embed in memory"
            execute {
                EHolder.embed.setTimestamp(Instant.now())
            }
        }

        command("setcolour") {
            description = "Set the colour of the embed using a hex colour code."
            expect(HexColourArg)
            execute {
                EHolder.embed.setColor(it.args.component1() as Int)
            }
        }

        command("setselfauthor") {
            description = "Set yourself to be the author of the embed"
            execute {
                EHolder.embed.setAuthor(it.author.fullName(), null, it.author.effectiveAvatarUrl)
            }
        }

        command("setauthor") {
            description = "Set a user with the given ID to be the author"
            expect(UserArg)
            execute {
                val target = it.args.component1() as User
                EHolder.embed.setAuthor(target.fullName(), null, target.effectiveAvatarUrl)
            }
        }

        command("setimage") {
            description = "Set the image of the embed"
            expect(UrlArg)
            execute {
                val url = it.args[0] as String
                EHolder.embed.setImage(url)
            }
        }

        command("setfooter") {
            description = "Set the footer of the embed, including the URL and text"
            expect(UrlArg, UrlArg)
            execute {
                val iconURL = it.args[0] as String
                val text = it.args[1] as String

                EHolder.embed.setFooter(text, iconURL)
            }
        }

        command("addblankfield") {
            description = "Add a blank field to the embed"
            expect(ChoiceArg)
            execute {
                val inline = it.args[0] as Boolean
                EHolder.embed.addBlankField(inline)
            }
        }

        command("addfield") {
            description = "Add the field currently held in memory to the embed currently held in memory (Note: Use setFName and setFText to set the text)"
            execute {
                EHolder.embed.addField(FHolder.name, FHolder.text, false)
            }
        }

        command("addIfield") {
            description = "Inline and add the field currently held in memory to the embed currently held in memory (Note: Use setFName and setFText to set the text)"
            execute {
                EHolder.embed.addField(FHolder.name, FHolder.text, true)
            }
        }

        command("clearFieldHolder") {
            description = "Clear the fields out in the field in memory, resetting the field to a new one"
            execute {
                FHolder.name = ""
                FHolder.text = ""
            }
        }

        command("clearfields") {
            description = "Clear the fields in the embed."
            execute {
                EHolder.embed.clearFields()
            }
        }

        command("setfname") {
            description = "Set the field name or title"
            expect(SentenceArg)
            execute {
                val name = it.args[0] as String
                FHolder.name = name
            }
        }

        command("setftext") {
            description = "Set the field text in memory"
            expect(SentenceArg)
            execute {
                val text = it.args[0] as String
                FHolder.text = text
            }
        }

        command("setthumbnail") {
            description = "Set the thumbnail of the embed"
            expect(UrlArg)
            execute {
                val url = it.args[0] as String
                EHolder.embed.setThumbnail(url)
            }
        }
    }