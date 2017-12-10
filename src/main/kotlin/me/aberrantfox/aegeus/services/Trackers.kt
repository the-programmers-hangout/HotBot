package me.aberrantfox.aegeus.services

import org.apache.commons.text.similarity.LevenshteinDistance
import org.joda.time.DateTime
import org.joda.time.Minutes
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.timerTask


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

class WeightTracker(trackTime: Int) : IdTracker<Int>(trackTime) {
    fun addOrUpdate(id: String) {
        map.putIfAbsent(id, 0)
        val get = this.map[id]!!
        map.put(id, get + 1)
    }
}

class MessageTracker(trackTime: Int) : IdTracker<ArrayList<String>>(trackTime) {
    private val calc = LevenshteinDistance()

    fun addMessage(who: String, msg: String): Int {
        map.putIfAbsent(who, ArrayList())

        val matches = map[who]!!.map { calc.apply(it, msg) }
            .filter { it <=  2 }
            .count()

        map[who]!!.add(msg)

        return matches
    }


    fun count(who: String) = map.getOrDefault(who, ArrayList()).size
}











