package me.aberrantfox.aegeus.commandframework.commands

import com.google.gson.Gson
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.services.saveConfig
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.listeners.CommandEvent
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
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

@Command fun ping(event: CommandEvent) = event.respond("Pong!")

@Command
fun serverinfo(event: CommandEvent) {
    if(event.guild == null) return
    val embed = produceServerInfoEmbed(event.guild)
    event.respond(embed)
}

@Command
fun uptime(event: CommandEvent) {
    val uptime = Date().time - startTime.time
    val minutes = uptime / 1000 / 60
    val currentDate = startTime.toString()

    event.respond("I've been awake since ${currentDate}, so like... ${minutes} minutes")
}

@Command
fun exit(event: CommandEvent) {
    saveConfig(event.config)
    event.respond("Exiting")
    System.exit(0)
}

@Command
fun kill(event: CommandEvent) {
    event.respond("Killing process, configurations will not be saved.")
    System.exit(0)
}

@Command
fun saveConfigurations(event: CommandEvent) {
    saveConfig(event.config)
    event.respond("Configurations saved. I hope you know what you are doing...")
}

@Command(ArgumentType.Manual)
fun info(event: CommandEvent) {
    val builder = EmbedBuilder()
            .setDescription("I'm Hotbot, the superior Auto-Titan replacement!")
            .setAuthor("Fox", "https://github.com/AberrantFox", "https://avatars1.githubusercontent.com/u/22015832")
            .setColor(Color.MAGENTA)
            .setThumbnail("http://i.imgur.com/DFoaG7k.png")
            .setFooter("Bot by Fox, made with Kotlin", "https://images-ext-1.discordapp.net/external/q9ZpQURnfAGbNxsxSqMzCiALNNVck5h4oWgRsHkG3bw/https/i.imgur.com/UymVLqf.png")


    builder.addField("Project Name", "Hotbot", true)
            .addField("Main Language(s)", "Kotlin", true)
            .addField("Libraries", "JDA, Gson, (soon) Apache CLI, Apache Reflections, kotson, junit, mockito", false)
            .addField("Progress to Completion", "100% for a basic version plus or minus 5%, needs a little more testing", false)
            .addField("Repo link", "https://github.com/AberrantFox/hotbot", false)

    event.respond(builder.build())
}

@Command
fun version(event: CommandEvent) = event.respond("**Hotbot version**: ${Project.properties.version}")

@Command
fun author(event: CommandEvent) = event.respond("**Project author**: ${Project.properties.author}")


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