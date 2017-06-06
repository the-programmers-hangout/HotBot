package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.saveConfigurationFile
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color
import java.time.LocalDateTime.now
import java.util.*

val startTime = Date()

@Command fun ping(event: GuildMessageReceivedEvent) = event.channel.sendMessage("Pong!").queue()

@Command fun help(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    val builder = EmbedBuilder()
    builder.setTitle("HotBot Help Menu")
            .setColor(Color.MAGENTA)
            .setDescription("Below you can find useful information about how to use this bot." +
                    "Please note, Arguments marked with [] are optional, and {} are required - in addition" +
                    ", anything marked with {a|b} means that only A and B are valid inputs.")
            .setFooter("Bot by Fox, made with Kotlin", "http://i.imgur.com/SJPggeJ.png")
            .setThumbnail("http://i.imgur.com/DFoaG7k.png")
            .setTimestamp(now())

    builder.addField("${config.prefix}Help",
            "Displays a help menu",
            false)

    builder.addField("${config.prefix}Ping",
            "Check if the bot is alive",
            false)

    builder.addField("${config.prefix}Exit",
            "Gracefully shut down the bot, saving configurations",
            false)

    builder.addField("${config.prefix}Kill",
            "Forcefully stop the bot, doesn't save configurations",
            false)

    builder.addField("${config.prefix}ListCommands",
            "List all currently registerd commands",
            false)

    builder.addField("${config.prefix}SetPerm {Command Name} {Guest|Moderator|Admin|Owner}",
            "Set the permission of a particular command",
            false)

    builder.addField("${config.prefix}GetPerm {Command Name}",
            "List the permission of a particular command",
            false)

    builder.addField("${config.prefix}Uptime",
            "Display how long the bot has been online",
            false)

    builder.addField("${config.prefix}Nuke {amount}",
            "Delete the last X messages. Note, amount must be greater than 0",
            false)

    builder.addField("${config.prefix}Ball [Your question here]",
            "Ask the 8ball api a question.",
            false)

    builder.addField("${config.prefix}Flip",
            "Flip a coin",
            false)

    builder.addField("${config.prefix}Ignore {channel-name}",
            "Force the bot to ignore commands from a particular channel",
            false)

    builder.addField("${config.prefix}Cat",
            "Display a picture of a kat",
            false)
    event.channel.sendMessage(builder.build()).queue()
}

@Command fun uptime(event: GuildMessageReceivedEvent) {
    val uptime = Date().time - startTime.time
    val minutes = uptime / 1000 / 60
    val currentDate = startTime.toString()

    event.channel.sendMessage("I've been awake since ${currentDate}, so like... ${minutes} minutes").queue()
}

@Command
fun exit(event: GuildMessageReceivedEvent, args: List<Any>, config: Configuration) {
    saveConfigurationFile(config)
    event.channel.sendMessage("Exiting").queue()
    System.exit(0)
}

@Command
fun kill(event: GuildMessageReceivedEvent) {
    event.channel.sendMessage("Killing process, configurations will not be saved.").queue()
    System.exit(0)
}