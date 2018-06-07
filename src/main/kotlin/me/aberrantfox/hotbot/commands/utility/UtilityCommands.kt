package me.aberrantfox.hotbot.commands.utility

import com.google.gson.Gson
import khttp.post
import me.aberrantfox.hotbot.permissions.PermissionManager
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.MService
import me.aberrantfox.hotbot.services.saveConfig
import me.aberrantfox.kjdautils.api.dsl.CommandSet
import me.aberrantfox.kjdautils.api.dsl.commands
import me.aberrantfox.kjdautils.api.dsl.embed
import me.aberrantfox.kjdautils.extensions.jda.fullName
import me.aberrantfox.kjdautils.internal.command.arguments.SentenceArg
import me.aberrantfox.kjdautils.internal.command.arguments.TextChannelArg
import me.aberrantfox.kjdautils.internal.command.arguments.UserArg
import me.aberrantfox.kjdautils.internal.logging.BotLogger
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color
import java.util.*

data class Properties(val version: String, val author: String)

object Project {
    val properties: Properties

    init {
        val propFile = Configuration::class.java.getResource("/properties.json").readText()
        val gson = Gson()
        properties = gson.fromJson(propFile, Properties::class.java)
    }
}

val startTime = Date()

@CommandSet
fun utilCommands(mService: MService, manager: PermissionManager, config: Configuration, log: BotLogger) = commands {
    command("ping") {
        execute {
            it.respond("Pong!")
        }
    }

    command("botinfo") {
        execute {
            it.respond(embed {
                title(it.jda.selfUser.fullName())
                description(mService.messages.botDescription)
                setColor(Color.red)
                setThumbnail(it.jda.selfUser.effectiveAvatarUrl)

                field {
                    name = "Author"
                    value = "Fox#0001"
                    inline = false
                }
                field {
                    name = "Technologies"
                    value = "Kotlin, JDA, SQL, Maven"
                    inline = false
                }
                field {
                    name = "Repository link"
                    value = "https://github.com/AberrantFox/hotbot"
                    inline = false
                }
            })
        }
    }

    command("serverinfo") {
        execute {
            val guild = it.jda.getGuildById(config.serverInformation.guildid)
            val embed = produceServerInfoEmbed(guild, mService)
            it.respond(embed)
        }
    }

    command("uptime") {
        execute {
            val milliseconds = Date().time - startTime.time
            val seconds = (milliseconds / 1000) % 60
            val minutes = (milliseconds / (1000 * 60)) % 60
            val hours = (milliseconds / (1000 * 60 * 60)) % 24
            val days = (milliseconds / (1000 * 60 * 60 * 24))

            it.respond(embed {
                setColor(Color.WHITE)
                setTitle("I have been running since")
                setDescription(startTime.toString())

                field {
                    name = "That's been"
                    value = "$days day(s), $hours hour(s), $minutes minute(s) and $seconds second(s)"
                }
            })
        }
    }

    command("exit") {
        execute {
            it.respond("Exiting")
            saveConfig(config)
            log.info("saved configurations")
            manager.save()
            log.info("saved permissions to database prior to shut down.")
            System.exit(0)
        }
    }

    command("kill") {
        execute {
            it.respond("Killing process, configurations will not be saved.")
            System.exit(0)
        }
    }

    command("saveconfigurations") {
        execute {
            saveConfig(config)
            it.respond("Configurations saved. I hope you know what you are doing...")
        }
    }

    command("version") {
        execute {
            it.respond("**Hotbot version**: ${Project.properties.version}")
        }
    }

    command("author") {
        execute {
            it.respond("**Project author**: ${Project.properties.author}")
        }
    }

    command("echo") {
        expect(TextChannelArg, SentenceArg)
        execute {
            val target = it.args[0] as TextChannel
            val message = it.args[1] as String

            target.sendMessage(message).queue()
        }
    }

    command("viewcreationdate") {
        expect(UserArg)
        execute {
            val target = it.args.component1() as User
            it.respond("${target.fullName()}'s account was made on ${target.creationTime}")
        }
    }

    command("uploadtext") {
        expect(SentenceArg)
        execute {
            val text = it.args.component1() as String
            val response = post("https://hastebin.com/documents", data = text).jsonObject

            it.message.delete().queue()

            it.respond("${it.author.fullName()}'s paste: https://hastebin.com/" + response.getString("key"))
        }
    }
}

fun produceServerInfoEmbed(guild: Guild, mService: MService) =
    embed {
        title(guild.name)
        setColor(Color.MAGENTA)
        description(mService.messages.serverDescription)
        setFooter("Guild creation date: ${guild.creationTime}", "http://i.imgur.com/iwwEprG.png")
        setThumbnail("http://i.imgur.com/DFoaG7k.png")

        field {
            name = "Users"
            value = "${guild.members.filter { it.onlineStatus != OnlineStatus.OFFLINE }.size}/${guild.members.size}"
        }

        ifield {
            name = "Total Roles"
            value = guild.roles.size.toString()
        }

        ifield {
            name = "Owner"
            value = guild.owner.fullName()
        }

        ifield {
            name = "Region"
            value = guild.region.toString()
        }

        ifield {
            name = "Text Channels"
            value = guild.textChannelCache.size().toString()
        }

        ifield {
            name = "Voice Channels"
            value = guild.voiceChannels.size.toString()
        }
    }
