package me.aberrantfox.hotbot.services

import me.aberrantfox.hotbot.utility.types.LimitedList
import me.aberrantfox.kjdautils.api.annotation.Service
import net.dv8tion.jda.api.entities.Message
import org.apache.commons.text.similarity.LevenshteinDistance
import org.joda.time.*
import java.util.Timer
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.timerTask

const val secondUnit = 1000
const val minuteUnit = 60 * secondUnit
const val hourUnit = 60 * minuteUnit

open class IdTracker<T>(private val trackTime: Int, private val timeUnit: Int = hourUnit) {
    val map: ConcurrentHashMap<String, T> = ConcurrentHashMap()

    fun clear() = map.clear()

    fun keyList() = map.keys().toList()

    fun put(key: String, value: T) {
        this.map[key] = value
        this.scheduleExit(key)
    }

    private fun scheduleExit(key: String) =
        Timer(true).schedule(timerTask {
            map.remove(key)
        }, (trackTime  * timeUnit).toLong())
}

class DateTracker(trackTime: Int, timeUnit: Int = hourUnit) : IdTracker<DateTime>(trackTime, timeUnit) {
    fun pastMins(min: Int) =
        map.filterValues {
            it.isAfter(DateTime.now().minus(Minutes.minutes(min)))
        }
}

class WeightTracker(trackTime: Int) : IdTracker<Int>(trackTime) {
    fun addOrUpdate(id: String) {
        val get = this.map.getOrPut(id) { 0 }
        map[id] = get + 1
    }
}

@Service
class MessageTracker : IdTracker<LimitedList<AccurateMessage>>(1) {
    private val calc = LevenshteinDistance()

    fun addMessage(acMsg: AccurateMessage): Int {
        val who = acMsg.message.author.id

        val msgs = map.getOrPut(who) { LimitedList(20) }

        val matches = msgs.map { calc.apply(it.message.contentRaw, acMsg.message.contentRaw) }
            .filter { it <=  2 }
            .count()

        msgs.add(acMsg)

        return matches
    }

    fun count(who: String) = map.getOrDefault(who, LimitedList(20)).size

    fun removeUser(who: String) = map.remove(who)

    fun list(who: String) = map[who]
}

data class AccurateMessage(val time: DateTime, val message: Message)