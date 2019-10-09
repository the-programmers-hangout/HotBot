package me.aberrantfox.hotbot.services

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

class WeightTracker(trackTime: Int) : IdTracker<Int>(trackTime) {
    fun addOrUpdate(id: String) {
        val get = this.map.getOrPut(id) { 0 }
        map[id] = get + 1
    }
}
