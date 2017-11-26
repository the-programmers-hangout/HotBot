package me.aberrantfox.aegeus.listeners.antispam

import me.aberrantfox.aegeus.services.security.NewPlayers
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Minutes
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.timerTask


class NewJoinListener : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        NewPlayers.cache.put(event.user.id, DateTime.now())
        scheduleExit(event.user.id)
    }
}

private fun scheduleExit(key: String) = Timer().schedule(timerTask {
        NewPlayers.cache.remove(key)
    }, Hours.ONE.hours.toLong())