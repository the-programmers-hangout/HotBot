package me.aberrantfox.hotbot.commandframework.commands

import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.dsls.command.arg
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.extensions.stdlib.isBooleanValue
import me.aberrantfox.hotbot.extensions.stdlib.isLong
import me.aberrantfox.hotbot.extensions.stdlib.toBooleanValue
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.*
import java.awt.Color
import java.time.Instant

private object EHolder {
    var embed: EmbedBuilder = EmbedBuilder()
    var message: Message? = null
}

private object FHolder {
    var name = ""
    var text = ""
}

@CommandSet
fun embedCommands() =
    commands {
        command("clearembed") {
            execute {
                EHolder.embed = EmbedBuilder()
                EHolder.message = null

                it.respond("Embed cleared")
            }
        }

        command("sendembed") {
            execute {
                if (EHolder.embed.isEmpty) {
                    it.respond("No embed to send.")
                    return@execute
                }

                it.respond(EHolder.embed.build())
            }
        }

        command("updateEmbed") {
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
            expect(arg(ArgumentType.Word),
                   arg(ArgumentType.Word, optional = true, default = { it.channel.id }))
            execute {
                val messageId = it.args.component1() as String
                val channelId = it.args.component2() as String

                // exception if channelId can't be converted to Long in getTextChannelById
                if (!channelId.isLong()) {
                    it.respond("Not a valid channel id")
                    return@execute
                }

                val channel = it.jda.getTextChannelById(channelId)
                if (channel == null) {
                    it.respond("Channel not found with the given id")
                    return@execute
                }

                channel.getMessageById(messageId).queue(
                        { msg ->
                            // Success
                            val embed = msg?.embeds?.firstOrNull()
                            if (embed == null) {
                                it.respond("Message doesn't contain any embeds")
                                return@queue
                            }

                            EHolder.embed = EmbedBuilder(embed)
                            EHolder.message = msg
                        },
                        { error ->
                            // Failure
                            it.respond("Message retrieval failed. A message with that id may not exist in this channel.")
                        }
                )
            }
        }

        command("editfield") {
            expect(ArgumentType.Integer, ArgumentType.Word, ArgumentType.Sentence)
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
            expect(ArgumentType.Sentence)
            execute {
                val title = it.args[0] as String
                EHolder.embed.setTitle(title)
            }
        }

        command("setdescription") {
            expect(ArgumentType.Sentence)
            execute {
                val description = it.args[0] as String
                EHolder.embed.setDescription(description)
            }
        }

        command("addtimestamp") {
            execute {
                EHolder.embed.setTimestamp(Instant.now())
            }
        }

        command("setcolour") {
            expect(ArgumentType.Double, ArgumentType.Double, ArgumentType.Double)
            execute {
                val r = it.args[0] as Double
                val g = it.args[1] as Double
                val b = it.args[2] as Double

                if (r > 1 || r < 0 ||
                    g > 1 || g < 0 ||
                    b > 1 || b < 0) {
                    it.respond("R/G/B values must be between 0 and 1")
                } else {
                    EHolder.embed.setColor(Color(r.toFloat(), g.toFloat(), b.toFloat()))
                }
            }
        }

        command("setselfauthor") {
            execute {
                EHolder.embed.setAuthor(it.author.fullName(), null, it.author.effectiveAvatarUrl)
            }
        }

        command("setauthor") {
            expect(ArgumentType.User)
            execute {
                val target = it.args.component1() as User
                EHolder.embed.setAuthor(target.fullName(), null, target.effectiveAvatarUrl)
            }
        }

        command("setimage") {
            expect(ArgumentType.URL)
            execute {
                val url = it.args[0] as String
                EHolder.embed.setImage(url)
            }
        }

        command("setfooter") {
            expect(ArgumentType.URL, ArgumentType.URL)
            execute {
                val iconURL = it.args[0] as String
                val text = it.args[1] as String

                EHolder.embed.setFooter(text, iconURL)
            }
        }

        command("addblankfield") {
            expect(ArgumentType.Choice)
            execute {
                val inline = it.args[0] as Boolean
                EHolder.embed.addBlankField(inline)
            }
        }

        command("addfield") {
            execute {
                EHolder.embed.addField(FHolder.name, FHolder.text, false)
            }
        }

        command("addIfield") {
            execute {
                EHolder.embed.addField(FHolder.name, FHolder.text, true)
            }
        }

        command("clearFieldHolder") {
            execute {
                FHolder.name = ""
                FHolder.text = ""
            }
        }

        command("clearfields") {
            execute {
                EHolder.embed.clearFields()
            }
        }

        command("setfname") {
            expect(ArgumentType.Sentence)
            execute {
                val name = it.args[0] as String
                FHolder.name = name
            }
        }

        command("setftext") {
            expect(ArgumentType.Sentence)
            execute {
                val text = it.args[0] as String
                FHolder.text = text
            }
        }

        command("setthumbnail") {
            expect(ArgumentType.URL)
            execute {
                val url = it.args[0] as String
                EHolder.embed.setThumbnail(url)
            }
        }
    }