package me.aberrantfox.hotbot.listeners

import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.Configuration
import me.aberrantfox.hotbot.services.UserID
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import java.util.concurrent.ConcurrentHashMap

typealias MessageID = String

object WelcomeMessages {
    val map = ConcurrentHashMap<UserID, MessageID>()
}

class BanListener(val config: Configuration) {
    @Subscribe
    fun onGuildBan(event: GuildBanEvent) {
        val messageID = WelcomeMessages.map[event.user.id] ?: return

        event.jda.getTextChannelById(config.messageChannels.welcomeChannel)
                .getMessageById(messageID)
                .queue { it.delete().queue() }
    }
}