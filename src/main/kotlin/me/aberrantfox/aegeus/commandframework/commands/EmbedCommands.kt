package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.CommandSet
import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.extensions.idToUser
import me.aberrantfox.aegeus.dsls.command.commands
import net.dv8tion.jda.core.EmbedBuilder
import java.awt.Color
import java.time.Instant

private object EHolder {
    var embed: EmbedBuilder = EmbedBuilder()
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
                it.respond("Embed cleared")
            }
        }

        command("sendembed") {
            execute {
                it.respond(EHolder.embed.build())
            }
        }

        command("settitle") {
            expect(ArgumentType.Joiner)
            execute {
                val title = it.args[0] as String
                EHolder.embed.setTitle(title)
            }
        }

        command("setdescription") {
            expect(ArgumentType.Joiner)
            execute {
                val description = it.args[0] as String
                EHolder.embed.setDescription(description)
            }
        }

        command("addtimestamp") { EHolder.embed.setTimestamp(Instant.now()) }

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
                EHolder.embed.setAuthor(it.author.fullName(), null, it.author.avatarUrl)
            }
        }

        command("setauthor") {
            expect(ArgumentType.UserID)
            execute {
                val target = (it.args[0] as String).idToUser(it.jda)
                EHolder.embed.setAuthor(target.fullName(), null, target.avatarUrl)
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
            expect(ArgumentType.Boolean)
            execute {
                val inline = it.args[0] as Boolean
                EHolder.embed.addBlankField(inline)
            }
        }

        command("addfield") {
            EHolder.embed.addField(FHolder.name, FHolder.text, false)
        }

        command("addIfield") {
            execute {
                EHolder.embed.addField(FHolder.name, FHolder.text, true)
            }
        }

        command("clearFieldHolder") {
            FHolder.name = ""
            FHolder.text = ""
        }

        command("clearfields") { EHolder.embed.clearFields() }

        command("setfname") {
            expect(ArgumentType.Joiner)
            execute {
                val name = it.args[0] as String
                FHolder.name = name
            }
        }

        command("setftext") {
            expect(ArgumentType.Joiner)
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