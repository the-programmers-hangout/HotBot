package me.aberrantfox.aegeus.commandframework.commands

import me.aberrantfox.aegeus.commandframework.ArgumentType
import me.aberrantfox.aegeus.commandframework.Command
import me.aberrantfox.aegeus.extensions.fullName
import me.aberrantfox.aegeus.extensions.idToUser
import me.aberrantfox.aegeus.listeners.CommandEvent
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

@Command
fun clearEmbed(event: CommandEvent) {
    EHolder.embed = EmbedBuilder()
    event.respond("Embed cleared")
}

@Command
fun sendEmbed(event: CommandEvent) {
    event.respond(EHolder.embed.build())
}

@Command(ArgumentType.Joiner)
fun setTitle(event: CommandEvent) {
    val title = event.args[0] as String
    EHolder.embed.setTitle(title)
}

@Command(ArgumentType.Joiner)
fun setDescription(event: CommandEvent) {
    val description = event.args[0] as String
    EHolder.embed.setDescription(description)
}

@Command
fun addTimestamp(event: CommandEvent) = EHolder.embed.setTimestamp(Instant.now())

@Command(ArgumentType.Double, ArgumentType.Double, ArgumentType.Double)
fun setColour(event: CommandEvent) {
    val r = event.args[0] as Double
    val g = event.args[1] as Double
    val b = event.args[2] as Double

    if(r > 1 || r < 0 ||
        g > 1 || g < 0 ||
            b > 1 || b < 0) {
        event.respond("R/G/B values must be between 0 and 1")
        return
    }

    EHolder.embed.setColor(Color(r.toFloat(),g.toFloat(),b.toFloat()))
}

@Command
fun setSelfAuthor(event: CommandEvent) = EHolder.embed.setAuthor(event.author.fullName(), null, event.author.avatarUrl)


@Command(ArgumentType.UserID)
fun setAuthor(event: CommandEvent) {
    val target = (event.args[0] as String).idToUser(event.jda)
    EHolder.embed.setAuthor(target.fullName(), null, target.avatarUrl)
}

@Command(ArgumentType.URL)
fun setImage(event: CommandEvent) {
    val url = event.args[0] as String
    EHolder.embed.setImage(url)
}

@Command(ArgumentType.URL, ArgumentType.Joiner)
fun setFooter(event: CommandEvent) {
    val iconURL = event.args[0] as String
    val text = event.args[1] as String

    EHolder.embed.setFooter(text, iconURL)
}

@Command(ArgumentType.Boolean)
fun addBlankField(event: CommandEvent) {
    val inline = event.args[0] as Boolean
    EHolder.embed.addBlankField(inline)
}

@Command
fun addField(event: CommandEvent) = EHolder.embed.addField(FHolder.name, FHolder.text, false)

@Command
fun addIField(event: CommandEvent) = EHolder.embed.addField(FHolder.name, FHolder.text, true)

@Command
fun clearFieldHolder(event: CommandEvent) {
    FHolder.name = ""
    FHolder.text = ""
}

@Command
fun clearFields(event: CommandEvent) = EHolder.embed.clearFields()

@Command(ArgumentType.Joiner)
fun setFName(event: CommandEvent) {
    val name = event.args[0] as String
    FHolder.name = name
}

@Command(ArgumentType.Joiner)
fun setFText(event: CommandEvent) {
    val text = event.args[0] as String
    FHolder.text = text
}

@Command(ArgumentType.URL)
fun setThumbnail(event: CommandEvent) {
    val url = event.args[0] as String
    EHolder.embed.setThumbnail(url)
}