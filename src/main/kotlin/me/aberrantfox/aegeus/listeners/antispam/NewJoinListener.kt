package me.aberrantfox.aegeus.listeners.antispam


import me.aberrantfox.aegeus.extensions.idToName
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.joda.time.DateTime
import org.joda.time.Minutes
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.timerTask


class NewJoinListener : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        NewPlayers.cache.put(event.user.id, DateTime.now())
    }
}

object NewPlayers {
    val cache = DateTracker(12)
    fun names(jda: JDA) = cache.keyList().map { it.idToName(jda) }
}

open class IdTracker<T>(val trackTime: Int) {
    val map: ConcurrentHashMap<String, T> = ConcurrentHashMap()

    fun clear() = map.clear()

    fun keyList() = map.keys().toList()

    fun put(key: String, value: T) {
        this.map.put(key, value)
        this.scheduleExit(key)
    }

    private fun scheduleExit(key: String) =
        Timer().schedule(timerTask {
            map.remove(key)
        }, (trackTime * 1000 * 60 * 60).toLong())
}

class DateTracker(trackTime: Int) : IdTracker<DateTime>(trackTime) {
    fun pastMins(min: Int) =
        map.filterKeys {
            map[it]!!.isAfter(DateTime.now().minus(Minutes.minutes(min)))
        }
}