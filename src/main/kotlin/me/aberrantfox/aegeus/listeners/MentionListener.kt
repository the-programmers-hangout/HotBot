package me.aberrantfox.aegeus.listeners

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class MentionListener : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if(event.message.rawContent.toLowerCase().contains(event.jda.selfUser.name.toLowerCase())
                || event.message.isMentioned(event.jda.selfUser)) {
            event.message.addReaction("\uD83D\uDC40").queue()
        }
    }
}