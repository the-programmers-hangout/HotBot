package me.aberrantfox.aegeus.services.security

import org.joda.time.DateTime
import org.joda.time.Minutes
import java.util.concurrent.ConcurrentHashMap

object NewPlayers {
    val cache: ConcurrentHashMap<String, DateTime> = ConcurrentHashMap()

    fun pastMins(min: Int) =
        cache.filterKeys {
            cache[it]!!.isAfter(DateTime.now().minus(Minutes.minutes(min)))
        }
}
