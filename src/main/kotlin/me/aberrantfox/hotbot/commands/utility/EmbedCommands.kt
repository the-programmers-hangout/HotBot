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
            expect(SentenceArg)
            execute {
                val title = it.args[0] as String
                EHolder.embed.setTitle(title)
            }
        }

        command("setdescription") {
            expect(SentenceArg)
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
            expect(HexColourArg)
            execute {
                EHolder.embed.setColor(it.args.component1() as Int)
            }
        }

        command("setselfauthor") {
            execute {
                EHolder.embed.setAuthor(it.author.fullName(), null, it.author.effectiveAvatarUrl)
            }
        }

        command("setauthor") {
            expect(UserArg)
            execute {
                val target = it.args.component1() as User
                EHolder.embed.setAuthor(target.fullName(), null, target.effectiveAvatarUrl)
            }
        }

        command("setimage") {
            expect(UrlArg)
            execute {
                val url = it.args[0] as String
                EHolder.embed.setImage(url)
            }
        }

        command("setfooter") {
            expect(UrlArg, UrlArg)
            execute {
                val iconURL = it.args[0] as String
                val text = it.args[1] as String

                EHolder.embed.setFooter(text, iconURL)
            }
        }

        command("addblankfield") {
            expect(ChoiceArg)
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
            expect(SentenceArg)
            execute {
                val name = it.args[0] as String
                FHolder.name = name
            }
        }

        command("setftext") {
            expect(SentenceArg)
            execute {
                val text = it.args[0] as String
                FHolder.text = text
            }
        }

        command("setthumbnail") {
            expect(UrlArg)
            execute {
                val url = it.args[0] as String
                EHolder.embed.setThumbnail(url)
            }
        }
    }