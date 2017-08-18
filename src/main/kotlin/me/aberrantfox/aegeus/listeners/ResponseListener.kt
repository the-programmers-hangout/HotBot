package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.commandframework.util.randomInt
import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter

class ResponseListener(val config: Configuration) : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(event.author.isBot) return
        if(config.ignoredIDs.contains(event.channel.id)) return

        val content = event.message.rawContent

        val num = randomInt(0, 999)

        if(num != 1) {
            return
        }

        if(content.endsWith("...")) {
            event.channel.sendMessage("ellipsis, edge lord level increased.").queue()
            return
        }

        if(content.endsWith(".")) {
            event.channel.sendMessage("*Allegedly...*").queue()
            return
        }

        if(content.endsWith("?")) {
            event.channel.sendMessage("Wow wow wow, what's with the 20 questions?").queue()
            return
        }

        if(content.endsWith(":100:")) {
            event.channel.sendMessage(":wtf: kids these days...").queue()
            return
        }

        if(content.trim().toLowerCase() == ":smiley:") {
            event.channel.sendMessage("Hey happy how about you say something more interesting than just smiling eh? punk").queue()
        }
    }
}