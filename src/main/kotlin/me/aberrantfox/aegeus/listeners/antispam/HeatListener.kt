package me.aberrantfox.aegeus.listeners.antispam

import me.aberrantfox.aegeus.extensions.isEmojiOnly
import me.aberrantfox.aegeus.extensions.isSingleCharacter
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter


class HeatListener : ListenerAdapter() {
    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val content = event.message.rawContent
        val target = event.author.id
    }
}

fun calculateHeat(content: String): Int {
    var heat = 140
    heat += content.length

    if(content.isSingleCharacter()) heat += 20

    if(content.isBlank()) heat += 20

    if(content.isEmojiOnly()) heat += 20

    return heat
}