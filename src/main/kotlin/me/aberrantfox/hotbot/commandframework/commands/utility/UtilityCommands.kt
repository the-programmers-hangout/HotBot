package me.aberrantfox.hotbot.commandframework.commands.utility

import com.google.gson.Gson
import me.aberrantfox.hotbot.commandframework.parsing.ArgumentType
import me.aberrantfox.hotbot.dsls.command.CommandSet
import me.aberrantfox.hotbot.database.savePermissions
import me.aberrantfox.hotbot.dsls.command.CommandEvent
import me.aberrantfox.hotbot.services.saveConfig
import me.aberrantfox.hotbot.dsls.command.commands
import me.aberrantfox.hotbot.dsls.embed.embed
import me.aberrantfox.hotbot.extensions.jda.fullName
import me.aberrantfox.hotbot.services.Configuration
import net.dv8tion.jda.core.OnlineStatus
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
fun utilCommands() = commands {
    command("ping") {
        execute {
            it.respond("Pong!")
        }
    }

    command("botinfo") {
        execute {
            it.respond(embed {
                title(it.jda.selfUser.fullName())
                description(it.mService.messages.botDescription)
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
            val embed = produceServerInfoEmbed(it)
            it.respond(embed)
        }
    }

    command("uptime") {
        execute {
            val uptime = Date().time - startTime.time
            val minutes = uptime / 1000 / 60
            val currentDate = startTime.toString()

            it.respond("I've been awake since ${currentDate}, so like... ${minutes} minutes")
        }
    }

    command("exit") {
        execute {
            it.respond("Exiting")
            saveConfig(it.config)
            info("saved configurations")
            savePermissions(it.manager)
            info("saved permissions to database prior to shut down.")
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
            saveConfig(it.config)
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
        expect(ArgumentType.TextChannel, ArgumentType.Sentence)
        execute {
            val target = it.args[0] as TextChannel
            val message = it.args[1] as String

            target.sendMessage(message).queue()
        }
    }

    command("viewcreationdate") {
        expect(ArgumentType.User)
        execute {
            val target = it.args.component1() as User
            it.respond("${target.fullName()}'s account was made on ${target.creationTime}")
        }
    }
}

fun produceServerInfoEmbed(event: CommandEvent) =
    embed {
        val guild = event.guild

        title(guild.name)
        setColor(Color.MAGENTA)
        description(event.mService.messages.serverDescription)
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
