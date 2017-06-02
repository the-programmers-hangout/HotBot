package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.services.Configuration
import me.aberrantfox.aegeus.services.saveConfigurationFile
import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.awt.Color
import java.time.LocalDateTime.now
import java.util.*

val startTime = Date()

@Command fun ping(event: MessageReceivedEvent) = event.channel.sendMessage("Pong!").queue()

@Command fun help(event: MessageReceivedEvent, args: List<Any>, config: Configuration) {
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

    event.channel.sendMessage(builder.build()).queue()
}

@Command fun uptime(event: MessageReceivedEvent) {
    val minutes = Date().time - startTime.time / 1000 / 60
    val currentDate = startTime.toString()

    event.channel.sendMessage("I've been awake since ${currentDate}, so like... ${minutes} minutes").queue()
}

@Command
fun exit(event: MessageReceivedEvent, args: List<Any>, config: Configuration) {
    saveConfigurationFile(config)
    event.channel.sendMessage("Exiting").queue()
    System.exit(0)
}

@Command
fun kill(event: MessageReceivedEvent) {
    event.channel.sendMessage("Killing process, configurations will not be saved.").queue()
    System.exit(0)
}

@Command(ArgumentType.INTEGER)
fun nuke(event: MessageReceivedEvent, args: List<Any>) {
    val amount = args[0] as Int

    if(amount == 0) {
        event.channel.sendMessage("Yea, what exactly is the point in nuking nothing... ?").queue()
        return
    }

    event.channel.history.retrievePast(amount + 1).queue({
        it.forEach { it.delete().queue() }
        event.channel.sendMessage("Be nice. No spam.").queue()
    })
}