package me.aberrantfox.hotbot.listeners.antispam


import com.google.common.eventbus.Subscribe
import me.aberrantfox.hotbot.services.DateTracker
import me.aberrantfox.hotbot.services.hourUnit
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import org.joda.time.DateTime

object NewPlayers {
    val cache = DateTracker(12, hourUnit)
    fun names(jda: JDA) = cache.keyList().map { jda.retrieveUserById(it).complete().name }
}

class NewJoinListener  {
    @Subscribe
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        NewPlayers.cache.put(event.user.id, DateTime.now())
    }
}