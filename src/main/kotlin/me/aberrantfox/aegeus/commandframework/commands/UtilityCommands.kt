package me.aberrantfox.aegeus.commandframework.commands

import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.services.saveConfig
import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.commandframework.commands.dsl.commands
import me.aberrantfox.aegeus.extensions.idToUser
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageEmbed
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

fun utilCommands() = commands {
    command("ping") {
        execute {
            it.respond("Png!")
        }
    }

    command("serverinfo") {
        execute {
            if (it.guild == null) return@execute
            val embed = produceServerInfoEmbed(it.guild)
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
            (it.config)
            it.respond("Exiting")
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
        expect(ArgumentType.String, ArgumentType.Joiner)
        execute {
            val target = it.args[0] as String
            val message = it.args[1] as String

            it.jda.getTextChannelById(target).sendMessage(message).queue()
        }
    }

    command("viewcreationdate") {
        execute {
            val target = (it.args[0] as String).idToUser(it.jda)
            it.respond("${target.fullName()}'s account was made on ${target.creationTime}")
        }
    }
}

fun produceServerInfoEmbed(guild: Guild): MessageEmbed {
    val builder = EmbedBuilder()
    builder.setTitle(guild.name)
        .setColor(Color.MAGENTA)
        .setDescription("The programmer's hangout is a programming server, made for persons of all skill levels, " +
            "be you someone who has wrote 10 lines of code, or someone with 10 years of experience.")
        .setFooter("Guild creation date: ${guild.creationTime}", "http://i.imgur.com/iwwEprG.png")
        .setThumbnail("http://i.imgur.com/DFoaG7k.png")

    builder.addField("Users", "${guild.members.filter {
        it.onlineStatus != OnlineStatus.OFFLINE
    }.size}/${guild.members.size}", true)

    builder.addField("Total Roles", "${guild.roles.size}", true)
    builder.addField("Owner", guild.owner.fullName(), true)
    builder.addField("Region", "${guild.region}", true)
    builder.addField("Text Channels", "${guild.textChannels.size}", true)
    builder.addField("Voice Channels", "${guild.voiceChannels.size}", true)

    return builder.build()
}