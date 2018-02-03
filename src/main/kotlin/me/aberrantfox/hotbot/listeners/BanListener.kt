package me.aberrantfox.hotbot.listeners

import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.UserID
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.ConcurrentHashMap

typealias MessageID = String

object WelcomeMessages {
    val map = ConcurrentHashMap<UserID, MessageID>()
}

class BanListener(val config: Configuration) : ListenerAdapter() {
    override fun onGuildBan(event: GuildBanEvent) {
        if(WelcomeMessages.map.containsKey(event.user.id)) {
            val messageID = WelcomeMessages.map[event.user.id]
            event.jda.getTextChannelById(config.messageChannels.welcomeChannel).getMessageById(messageID).queue {
                it.delete().queue()
            }
        }
    }
}