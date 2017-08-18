package me.aberrantfox.aegeus.listeners

import me.aberrantfox.aegeus.services.Configuration
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class InviteListener(val config: Configuration) : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(event.author.isBot) return

        if(event.message.rawContent.matches(Regex("(.|\n)*(https://discord.gg/)+(.|\n)*"))) {

            if(event.message.rawContent.contains('@'))

            event.message.delete().queue()
            event.guild.textChannels.findLast { it.id == config.leaveChannel }
                    ?.sendMessage("Deleted message: ${event.message.rawContent} " +
                            "by ${event.author.asMention} " +
                            "in ${event.channel.asMention}")
                    ?.queue()
        }
    }
}