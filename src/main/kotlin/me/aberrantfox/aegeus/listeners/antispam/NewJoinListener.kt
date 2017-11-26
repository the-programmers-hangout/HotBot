package me.aberrantfox.aegeus.listeners.antispam

import me.aberrantfox.aegeus.extensions.fullName
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Minutes
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.timerTask

object NewPlayers {
    val cache: ConcurrentHashMap<String, DateTime> = ConcurrentHashMap()

    fun pastMins(min: Int) =
        cache.filterKeys {
            cache[it]!!.isAfter(DateTime.now().minus(Minutes.minutes(min)))
        }

    fun names(pastMins: Int, jda: JDA) = pastMins(pastMins).map { jda.getUserById(it.key) }.map { it.fullName() }
}

class NewJoinListener : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        NewPlayers.cache.put(event.user.id, DateTime.now())
        scheduleExit(event.user.id)
    }
}

private fun scheduleExit(key: String) = Timer().schedule(timerTask {
        NewPlayers.cache.remove(key)
    }, Hours.hours(12).hours.toLong())