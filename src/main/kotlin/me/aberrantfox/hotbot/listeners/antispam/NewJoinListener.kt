package me.aberrantfox.hotbot.listeners.antispam


import me.aberrantfox.hotbot.extensions.stdlib.idToName
import me.aberrantfox.hotbot.services.DateTracker
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.joda.time.DateTime

object NewPlayers {
    val cache = DateTracker(12)
    fun names(jda: JDA) = cache.keyList().map { it.idToName(jda) }
}

class NewJoinListener : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        NewPlayers.cache.put(event.user.id, DateTime.now())
    }
}